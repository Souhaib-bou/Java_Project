<?php

namespace App\Controller;

use App\Entity\Onboardingplan;
use App\Form\OnboardingPlanType;
use App\Repository\OnboardingplanRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class OnboardingPlanController extends AbstractController
{
    #[Route('/admin/plans', name: 'app_admin_plans')]
    public function index(OnboardingplanRepository $planRepository): Response
    {
        $plans = $planRepository->findAll();

        return $this->render('admin/plans/index.html.twig', [
            'plans' => $plans,
        ]);
    }

    #[Route('/admin/plans/new', name: 'app_admin_plans_new')]
    public function new(Request $request, EntityManagerInterface $entityManager): Response
    {
        $plan = new Onboardingplan();
        $form = $this->createForm(OnboardingPlanType::class, $plan);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($plan);
            if (!$plan->getStatus()) {
                $plan->setStatus('pending');
            }
            $entityManager->flush();

            return $this->redirectToRoute('app_admin_plans');
        }

        return $this->render('admin/plans/form.html.twig', [
            'form' => $form->createView(),
            'page_title' => 'Add Onboarding Plan',
        ]);
    }

    #[Route('/admin/plans/{id}/edit', name: 'app_admin_plans_edit')]
    public function edit(Onboardingplan $plan, Request $request, EntityManagerInterface $entityManager): Response
    {
        $form = $this->createForm(OnboardingPlanType::class, $plan);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            if (!$plan->getStatus()) {
                $plan->setStatus('pending');
                }
            $entityManager->flush();

            return $this->redirectToRoute('app_admin_plans');
        }

        return $this->render('admin/plans/form.html.twig', [
            'form' => $form->createView(),
            'page_title' => 'Edit Onboarding Plan',
        ]);
    }

    #[Route('/admin/plans/{id}/delete', name: 'app_admin_plans_delete', methods: ['POST'])]
    public function delete(Onboardingplan $plan, Request $request, EntityManagerInterface $entityManager): Response
    {
        if ($this->isCsrfTokenValid('delete_plan_' . $plan->getPlanId(), $request->request->get('_token'))) {
            $entityManager->remove($plan);
            $entityManager->flush();
        }

        return $this->redirectToRoute('app_admin_plans');
    }
}