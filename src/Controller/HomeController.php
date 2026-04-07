<?php

namespace App\Controller;

use App\Onboarding\ViewerContext;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class HomeController extends AbstractController
{
    #[Route('/home', name: 'app_home')]
    public function index(ViewerContext $viewerContext): Response
    {
        return $this->render('home/index.html.twig', [
            'controller_name' => 'HomeController',
            'viewer' => $viewerContext->getCurrentUser(),
        ]);
    }
}
