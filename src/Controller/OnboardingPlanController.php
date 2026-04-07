<?php

namespace App\Controller;

use App\Entity\Onboardingplan;
use App\Form\OnboardingPlanType;
use App\Onboarding\ViewerContext;
use App\Repository\OnboardingplanRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class OnboardingPlanController extends AbstractController
{
    #[Route('/admin/plans', name: 'app_admin_plans')]
    #[Route('/workspace/plans', name: 'app_workspace_plans')]
    public function index(Request $request, OnboardingplanRepository $planRepository, ViewerContext $viewerContext): Response|RedirectResponse
    {
        if ($redirect = $this->redirectForArea($request, $viewerContext)) {
            return $redirect;
        }

        $viewer = $viewerContext->getCurrentUser();
        $searchTerm = trim((string) $request->query->get('q', ''));
        $caseSensitive = $request->query->getBoolean('case_sensitive');
        $plans = $viewer ? $planRepository->findVisibleFor($viewer, $searchTerm, $caseSensitive) : [];
        $viewData = [
            'plans' => $plans,
            'search_term' => $searchTerm,
            'case_sensitive' => $caseSensitive,
        ];

        if ($request->isXmlHttpRequest()) {
            return $this->render('admin/plans/_results.html.twig', $viewData);
        }

        return $this->render('admin/plans/index.html.twig', $viewData);
    }

    #[Route('/admin/plans/new', name: 'app_admin_plans_new')]
    #[Route('/workspace/plans/new', name: 'app_workspace_plans_new')]
    public function new(Request $request, EntityManagerInterface $entityManager, ViewerContext $viewerContext): Response|RedirectResponse
    {
        if ($redirect = $this->redirectForArea($request, $viewerContext)) {
            return $redirect;
        }

        if (!$viewerContext->canCreatePlans()) {
            $this->addFlash('error', 'Candidates cannot create onboarding plans.');

            return $this->redirectToRoute($this->plansRoute($request));
        }

        $plan = new Onboardingplan();
        $form = $this->createForm(OnboardingPlanType::class, $plan, [
            'editor_mode' => 'full',
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($plan);
            if (!$plan->getStatus()) {
                $plan->setStatus(Onboardingplan::STATUS_PENDING);
            }
            if (!$plan->getQrToken()) {
                $plan->setQrToken($this->generateQrToken());
            }
            $entityManager->flush();

            return $this->redirectToRoute($this->plansRoute($request));
        }

        return $this->render('admin/plans/form.html.twig', [
            'form' => $form->createView(),
            'page_title' => 'Add Onboarding Plan',
            'limited_editor' => false,
            'plan' => $plan,
        ]);
    }

    #[Route('/admin/plans/{id}/edit', name: 'app_admin_plans_edit')]
    #[Route('/workspace/plans/{id}/edit', name: 'app_workspace_plans_edit')]
    public function edit(Onboardingplan $plan, Request $request, EntityManagerInterface $entityManager, ViewerContext $viewerContext): Response|RedirectResponse
    {
        if ($redirect = $this->redirectForArea($request, $viewerContext)) {
            return $redirect;
        }

        if (!$viewerContext->canEditPlan($plan)) {
            $this->addFlash('error', 'You are not allowed to update this onboarding plan.');

            return $this->redirectToRoute($this->plansRoute($request));
        }

        $limitedEditor = !$viewerContext->canFullyEditPlan($plan);
        $form = $this->createForm(OnboardingPlanType::class, $plan, [
            'editor_mode' => $limitedEditor ? 'candidate' : 'full',
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            if (!$plan->getStatus()) {
                $plan->setStatus(Onboardingplan::STATUS_PENDING);
            }
            if (!$plan->getQrToken()) {
                $plan->setQrToken($this->generateQrToken());
            }
            $entityManager->flush();

            return $this->redirectToRoute($this->plansRoute($request));
        }

        return $this->render('admin/plans/form.html.twig', [
            'form' => $form->createView(),
            'page_title' => $limitedEditor ? 'Update My Plan Status' : 'Edit Onboarding Plan',
            'limited_editor' => $limitedEditor,
            'plan' => $plan,
        ]);
    }

    #[Route('/admin/plans/{id}/delete', name: 'app_admin_plans_delete', methods: ['POST'])]
    #[Route('/workspace/plans/{id}/delete', name: 'app_workspace_plans_delete', methods: ['POST'])]
    public function delete(Onboardingplan $plan, Request $request, EntityManagerInterface $entityManager, ViewerContext $viewerContext): Response|RedirectResponse
    {
        if ($redirect = $this->redirectForArea($request, $viewerContext)) {
            return $redirect;
        }

        if (!$viewerContext->canDeletePlan($plan)) {
            $this->addFlash('error', 'You are not allowed to delete this onboarding plan.');

            return $this->redirectToRoute($this->plansRoute($request));
        }

        if ($this->isCsrfTokenValid('delete_plan_' . $plan->getPlanId(), $request->request->get('_token'))) {
            $entityManager->remove($plan);
            $entityManager->flush();
        }

        return $this->redirectToRoute($this->plansRoute($request));
    }

    #[Route('/admin/plans/{id}/qr', name: 'app_admin_plans_qr')]
    #[Route('/workspace/plans/{id}/qr', name: 'app_workspace_plans_qr')]
    public function qr(Onboardingplan $plan, Request $request, EntityManagerInterface $entityManager, ViewerContext $viewerContext): Response|RedirectResponse
    {
        if ($redirect = $this->redirectForArea($request, $viewerContext)) {
            return $redirect;
        }

        if (!$viewerContext->canViewQr($plan)) {
            $this->addFlash('error', 'You are not allowed to view this QR code.');

            return $this->redirectToRoute($this->plansRoute($request));
        }

        if (!$plan->getQrToken()) {
            $plan->setQrToken($this->generateQrToken());
            $entityManager->flush();
        }

        return $this->render('admin/plans/qr.html.twig', [
            'plan' => $plan,
        ]);
    }

    private function generateQrToken(): string
    {
        return rtrim(strtr(base64_encode(random_bytes(24)), '+/', '-_'), '=');
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
}
