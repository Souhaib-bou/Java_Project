<?php

namespace App\Controller;

use App\Entity\Onboardingplan;
use App\Entity\Onboardingtask;
use App\Onboarding\ViewerContext;
use App\Repository\OnboardingplanRepository;
use App\Repository\OnboardingtaskRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class WorkspaceController extends AbstractController
{
    #[Route('/workspace', name: 'app_workspace')]
    public function index(
        OnboardingplanRepository $planRepository,
        OnboardingtaskRepository $taskRepository,
        ViewerContext $viewerContext,
    ): Response|RedirectResponse {
        if ($viewerContext->isAdmin()) {
            return $this->redirectToRoute('app_admin');
        }

        $viewer = $viewerContext->getCurrentUser();
        $plans = $viewer ? $planRepository->findVisibleFor($viewer) : [];
        $tasks = $viewer ? $taskRepository->findVisibleFor($viewer) : [];

        $completedPlans = array_filter($plans, static fn (Onboardingplan $plan) => Onboardingplan::STATUS_COMPLETED === $plan->getStatus());
        $activeTasks = array_filter($tasks, static fn (Onboardingtask $task) => Onboardingtask::STATUS_IN_PROGRESS === $task->getStatus());
        $attachments = array_filter($tasks, static fn (Onboardingtask $task) => null !== $task->getFilePath() && '' !== trim($task->getFilePath()));

        return $this->render('admin/dashboard.html.twig', [
            'plans' => $plans,
            'recent_plans' => array_slice($plans, 0, 3),
            'metrics' => [
                'plans' => count($plans),
                'completed_plans' => count($completedPlans),
                'active_tasks' => count($activeTasks),
                'attachments' => count($attachments),
            ],
        ]);
    }
}
