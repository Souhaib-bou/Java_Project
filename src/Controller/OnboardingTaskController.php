<?php

namespace App\Controller;

use App\Entity\Onboardingplan;
use App\Entity\Onboardingtask;
use App\Form\OnboardingTaskType;
use App\Onboarding\AttachmentUploadConfiguration;
use App\Onboarding\LocalTaskAttachmentStorage;
use App\Onboarding\TaskDecisionGuideService;
use App\Onboarding\TaskRecommendation;
use App\Onboarding\ViewerContext;
use App\Repository\OnboardingtaskRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class OnboardingTaskController extends AbstractController
{
    #[Route('/attachments/upload', name: 'app_task_attachment_upload', methods: ['POST'])]
    public function uploadAttachment(Request $request, ViewerContext $viewerContext, AttachmentUploadConfiguration $attachmentUploadConfiguration, LocalTaskAttachmentStorage $localTaskAttachmentStorage): JsonResponse
    {
        if (!$viewerContext->getCurrentUser()) {
            return $this->json(['error' => ['message' => 'No active user found for this session.']], Response::HTTP_FORBIDDEN);
        }

        $uploadedFile = $request->files->get('file');
        if (!$uploadedFile) {
            return $this->json(['error' => ['message' => 'Choose a file before uploading.']], Response::HTTP_BAD_REQUEST);
        }

        if ($attachmentUploadConfiguration->isEnabled()) {
            return $this->json([
                'error' => ['message' => 'Direct browser upload should use the configured external provider.'],
            ], Response::HTTP_BAD_REQUEST);
        }

        $storedFile = $localTaskAttachmentStorage->store($uploadedFile);
        $publicUrl = rtrim($request->getSchemeAndHttpHost(), '/') . $storedFile['public_path'];

        return $this->json([
            'secure_url' => $publicUrl,
            'public_id' => 'local/' . $storedFile['stored_name'],
            'original_filename' => pathinfo($storedFile['original_name'], \PATHINFO_FILENAME),
            'format' => pathinfo($storedFile['stored_name'], \PATHINFO_EXTENSION),
            'resource_type' => 'raw',
            'content_type' => $storedFile['content_type'],
        ]);
    }

    #[Route('/admin/plans/{id}/tasks', name: 'app_admin_plan_tasks')]
    #[Route('/workspace/plans/{id}/tasks', name: 'app_workspace_plan_tasks')]
    public function index(Request $request, Onboardingplan $plan, OnboardingtaskRepository $taskRepository, ViewerContext $viewerContext, TaskDecisionGuideService $taskDecisionGuideService): Response|RedirectResponse
    {
        if ($redirect = $this->redirectForArea($request, $viewerContext)) {
            return $redirect;
        }

        if (!$viewerContext->canViewPlan($plan)) {
            $this->addFlash('error', 'You are not allowed to view tasks for this onboarding plan.');

            return $this->redirectToRoute($this->plansRoute($request));
        }

        $searchTerm = trim((string) $request->query->get('q', ''));
        $caseSensitive = $request->query->getBoolean('case_sensitive');
        $filters = [
            'status' => trim((string) $request->query->get('status', '')),
            'sort' => trim((string) $request->query->get('sort', 'newest')),
            'attachment_only' => $request->query->getBoolean('attachment_only'),
        ];
        $tasks = $taskRepository->findByPlan($plan, $searchTerm, $caseSensitive, $filters);
        $taskMetrics = $this->buildTaskMetrics($tasks);
        $taskRecommendations = \array_slice($taskDecisionGuideService->buildRecommendations($viewerContext->getRoleId() ?? ViewerContext::ROLE_CANDIDATE, $tasks), 0, 5);
        $viewData = [
            'plan' => $plan,
            'tasks' => $tasks,
            'search_term' => $searchTerm,
            'case_sensitive' => $caseSensitive,
            'selected_status' => $filters['status'],
            'selected_sort' => $filters['sort'],
            'attachment_only' => $filters['attachment_only'],
            'task_metrics' => $taskMetrics,
            'task_recommendations' => $taskRecommendations,
            'task_guide_overview' => $this->buildTaskGuideOverview($tasks, $taskMetrics, $taskRecommendations),
            'task_status_choices' => Onboardingtask::getStatusChoices(),
        ];

        if ($request->isXmlHttpRequest()) {
            return $this->render('admin/tasks/_results.html.twig', $viewData);
        }

        return $this->render('admin/tasks/index.html.twig', $viewData);
    }

    #[Route('/admin/plans/{id}/tasks/new', name: 'app_admin_plan_tasks_new')]
    #[Route('/workspace/plans/{id}/tasks/new', name: 'app_workspace_plan_tasks_new')]
    public function new(Onboardingplan $plan, Request $request, EntityManagerInterface $entityManager, ViewerContext $viewerContext, AttachmentUploadConfiguration $attachmentUploadConfiguration): Response|RedirectResponse
    {
        if ($redirect = $this->redirectForArea($request, $viewerContext)) {
            return $redirect;
        }

        if (!$viewerContext->canCreateTasks() || !$viewerContext->canViewPlan($plan)) {
            $this->addFlash('error', 'Only admin and recruiter users can create onboarding tasks.');

            return $this->redirectToRoute($this->planTasksRoute($request), [
                'id' => $plan->getPlanId(),
            ]);
        }

        $task = new Onboardingtask();
        $task->setPlan($plan);
        $task->setStatus(Onboardingtask::STATUS_NOT_STARTED);

        $form = $this->createForm(OnboardingTaskType::class, $task, [
            'editor_mode' => 'full',
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            if (!$task->getStatus()) {
                $task->setStatus(Onboardingtask::STATUS_NOT_STARTED);
            }
            if (!$task->getFilePath()) {
                $task->clearAttachment();
            }

            $entityManager->persist($task);
            $entityManager->flush();

            return $this->redirectToRoute($this->planTasksRoute($request), [
                'id' => $plan->getPlanId(),
            ]);
        }

        return $this->render('admin/tasks/form.html.twig', [
            'form' => $form->createView(),
            'plan' => $plan,
            'page_title' => 'Add Task',
            'limited_editor' => false,
            'task' => $task,
            'attachment_upload' => $this->buildAttachmentUploadViewData($attachmentUploadConfiguration),
        ]);
    }

    #[Route('/admin/tasks/{id}/edit', name: 'app_admin_plan_tasks_edit')]
    #[Route('/workspace/tasks/{id}/edit', name: 'app_workspace_plan_tasks_edit')]
    public function edit(Onboardingtask $task, Request $request, EntityManagerInterface $entityManager, ViewerContext $viewerContext, AttachmentUploadConfiguration $attachmentUploadConfiguration): Response|RedirectResponse
    {
        if ($redirect = $this->redirectForArea($request, $viewerContext)) {
            return $redirect;
        }

        $plan = $task->getPlan();

        if (!$viewerContext->canEditTask($task)) {
            $this->addFlash('error', 'You are not allowed to update this onboarding task.');

            return $this->redirectToRoute($this->plansRoute($request));
        }

        if (!$task->getStatus()) {
            $task->setStatus(Onboardingtask::STATUS_NOT_STARTED);
        }

        $limitedEditor = !$viewerContext->canFullyEditTask($task);
        $form = $this->createForm(OnboardingTaskType::class, $task, [
            'editor_mode' => $limitedEditor ? 'candidate' : 'full',
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            if (!$task->getStatus()) {
                $task->setStatus(Onboardingtask::STATUS_NOT_STARTED);
            }
            if (!$task->getFilePath()) {
                $task->clearAttachment();
            }

            $entityManager->flush();

            return $this->redirectToRoute($this->planTasksRoute($request), [
                'id' => $plan->getPlanId(),
            ]);
        }

        return $this->render('admin/tasks/form.html.twig', [
            'form' => $form->createView(),
            'plan' => $plan,
            'page_title' => $limitedEditor ? 'Update Task Progress' : 'Edit Task',
            'limited_editor' => $limitedEditor,
            'task' => $task,
            'attachment_upload' => $this->buildAttachmentUploadViewData($attachmentUploadConfiguration),
        ]);
    }

    #[Route('/admin/tasks/{id}/delete', name: 'app_admin_plan_tasks_delete', methods: ['POST'])]
    #[Route('/workspace/tasks/{id}/delete', name: 'app_workspace_plan_tasks_delete', methods: ['POST'])]
    public function delete(Onboardingtask $task, Request $request, EntityManagerInterface $entityManager, ViewerContext $viewerContext): Response|RedirectResponse
    {
        if ($redirect = $this->redirectForArea($request, $viewerContext)) {
            return $redirect;
        }

        $plan = $task->getPlan();

        if (!$viewerContext->canDeleteTask($task)) {
            $this->addFlash('error', 'You are not allowed to delete this onboarding task.');

            return $this->redirectToRoute($this->planTasksRoute($request), [
                'id' => $plan->getPlanId(),
            ]);
        }

        if ($this->isCsrfTokenValid('delete_task_' . $task->getTaskId(), $request->request->get('_token'))) {
            $entityManager->remove($task);
            $entityManager->flush();
        }

        return $this->redirectToRoute($this->planTasksRoute($request), [
            'id' => $plan->getPlanId(),
        ]);
    }

    private function redirectForArea(Request $request, ViewerContext $viewerContext): ?RedirectResponse
    {
        if ($this->isAdminArea($request) && !$viewerContext->isAdmin()) {
            $this->addFlash('error', 'The admin backend is reserved for admin accounts.');

            return $this->redirectToRoute('app_workspace');
        }

        if (!$this->isAdminArea($request) && $viewerContext->isAdmin()) {
            return $this->redirectToRoute('app_admin');
        }

        return null;
    }

    private function isAdminArea(Request $request): bool
    {
        return str_starts_with((string) $request->attributes->get('_route'), 'app_admin');
    }

    private function plansRoute(Request $request): string
    {
        return $this->isAdminArea($request) ? 'app_admin_plans' : 'app_workspace_plans';
    }

    private function planTasksRoute(Request $request): string
    {
        return $this->isAdminArea($request) ? 'app_admin_plan_tasks' : 'app_workspace_plan_tasks';
    }

    /**
     * @return array{enabled: bool, provider: string, cloud_name: string, unsigned_preset: string, upload_url: string}
     */
    private function buildAttachmentUploadViewData(AttachmentUploadConfiguration $attachmentUploadConfiguration): array
    {
        $provider = $attachmentUploadConfiguration->isEnabled() ? 'cloudinary' : 'local';

        return $attachmentUploadConfiguration->toViewData() + [
            'enabled' => true,
            'provider' => $provider,
            'upload_url' => $this->generateUrl('app_task_attachment_upload'),
        ];
    }

    /**
     * @param Onboardingtask[] $tasks
     * @return array{total: int, completed: int, in_progress: int, blocked: int, on_hold: int, not_started: int, attachments: int}
     */
    private function buildTaskMetrics(array $tasks): array
    {
        $completed = 0;
        $inProgress = 0;
        $blocked = 0;
        $onHold = 0;
        $notStarted = 0;
        $attachments = 0;

        foreach ($tasks as $task) {
            if (Onboardingtask::STATUS_COMPLETED === $task->getStatus()) {
                ++$completed;
            }

            if (Onboardingtask::STATUS_IN_PROGRESS === $task->getStatus()) {
                ++$inProgress;
            }

            if (Onboardingtask::STATUS_BLOCKED === $task->getStatus()) {
                ++$blocked;
            }

            if (Onboardingtask::STATUS_ON_HOLD === $task->getStatus()) {
                ++$onHold;
            }

            if (Onboardingtask::STATUS_NOT_STARTED === $task->getStatus()) {
                ++$notStarted;
            }

            if ($task->hasAttachment()) {
                ++$attachments;
            }
        }

        return [
            'total' => \count($tasks),
            'completed' => $completed,
            'in_progress' => $inProgress,
            'blocked' => $blocked,
            'on_hold' => $onHold,
            'not_started' => $notStarted,
            'attachments' => $attachments,
        ];
    }

    /**
     * @param Onboardingtask[] $tasks
     * @param array{total: int, completed: int, in_progress: int, blocked: int, on_hold: int, not_started: int, attachments: int} $taskMetrics
     * @param TaskRecommendation[] $taskRecommendations
     * @return array{health_score: int, health_label: string, focus_label: string, due_soon_count: int, missing_proof_count: int, urgent_actions: int, next_deadline_label: string}
     */
    private function buildTaskGuideOverview(array $tasks, array $taskMetrics, array $taskRecommendations): array
    {
        $dueSoonCount = 0;
        $missingProofCount = 0;
        $urgentActions = 0;
        $nextDeadline = null;
        $today = new \DateTimeImmutable('today');
        $soonLimit = $today->modify('+3 days');

        foreach ($tasks as $task) {
            $deadline = $task->getDeadline();
            if ($deadline && Onboardingtask::STATUS_COMPLETED !== $task->getStatus()) {
                $deadlineDate = \DateTimeImmutable::createFromInterface($deadline)->setTime(0, 0);
                if ($deadlineDate <= $soonLimit) {
                    ++$dueSoonCount;
                }

                if (null === $nextDeadline || $deadlineDate < $nextDeadline) {
                    $nextDeadline = $deadlineDate;
                }
            }

            if (Onboardingtask::STATUS_COMPLETED === $task->getStatus() && !$task->hasAttachment()) {
                ++$missingProofCount;
            }
        }

        foreach ($taskRecommendations as $recommendation) {
            if (TaskRecommendation::PRIORITY_HIGH === $recommendation->getPriority()) {
                ++$urgentActions;
            }
        }

        $healthScore = 100;
        $healthScore -= ($taskMetrics['blocked'] * 18);
        $healthScore -= ($taskMetrics['on_hold'] * 10);
        $healthScore -= ($taskMetrics['not_started'] * 6);
        $healthScore -= ($missingProofCount * 7);
        $healthScore += ($taskMetrics['completed'] * 4);
        $healthScore += ($taskMetrics['attachments'] * 2);
        $healthScore = max(18, min(96, $healthScore));

        if ($healthScore >= 80) {
            $healthLabel = 'Healthy flow';
        } elseif ($healthScore >= 60) {
            $healthLabel = 'Watch closely';
        } else {
            $healthLabel = 'Needs attention';
        }

        if ($taskMetrics['blocked'] > 0) {
            $focusLabel = 'Resolve blockers';
        } elseif ($missingProofCount > 0) {
            $focusLabel = 'Collect missing proof';
        } elseif ($taskMetrics['in_progress'] > 0) {
            $focusLabel = 'Push active work forward';
        } elseif ($taskMetrics['not_started'] > 0) {
            $focusLabel = 'Kick off pending work';
        } else {
            $focusLabel = 'Maintain momentum';
        }

        $nextDeadlineLabel = $nextDeadline ? $nextDeadline->format('Y-m-d') : 'No active deadline';

        return [
            'health_score' => $healthScore,
            'health_label' => $healthLabel,
            'focus_label' => $focusLabel,
            'due_soon_count' => $dueSoonCount,
            'missing_proof_count' => $missingProofCount,
            'urgent_actions' => $urgentActions,
            'next_deadline_label' => $nextDeadlineLabel,
        ];
    }
}
