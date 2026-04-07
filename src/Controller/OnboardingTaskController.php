<?php

namespace App\Controller;

use App\Entity\Onboardingplan;
use App\Entity\Onboardingtask;
use App\Form\OnboardingTaskType;
use App\Onboarding\ViewerContext;
use App\Repository\OnboardingtaskRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class OnboardingTaskController extends AbstractController
{
    #[Route('/admin/plans/{id}/tasks', name: 'app_admin_plan_tasks')]
    #[Route('/workspace/plans/{id}/tasks', name: 'app_workspace_plan_tasks')]
    public function index(Request $request, Onboardingplan $plan, OnboardingtaskRepository $taskRepository, ViewerContext $viewerContext): Response|RedirectResponse
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
        $viewData = [
            'plan' => $plan,
            'tasks' => $taskRepository->findByPlan($plan, $searchTerm, $caseSensitive),
            'search_term' => $searchTerm,
            'case_sensitive' => $caseSensitive,
        ];

        if ($request->isXmlHttpRequest()) {
            return $this->render('admin/tasks/_results.html.twig', $viewData);
        }

        return $this->render('admin/tasks/index.html.twig', $viewData);
    }

    #[Route('/admin/plans/{id}/tasks/new', name: 'app_admin_plan_tasks_new')]
    #[Route('/workspace/plans/{id}/tasks/new', name: 'app_workspace_plan_tasks_new')]
    public function new(Onboardingplan $plan, Request $request, EntityManagerInterface $entityManager, ViewerContext $viewerContext): Response|RedirectResponse
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
        ]);
    }

    #[Route('/admin/tasks/{id}/edit', name: 'app_admin_plan_tasks_edit')]
    #[Route('/workspace/tasks/{id}/edit', name: 'app_workspace_plan_tasks_edit')]
    public function edit(Onboardingtask $task, Request $request, EntityManagerInterface $entityManager, ViewerContext $viewerContext): Response|RedirectResponse
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
}
