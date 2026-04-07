<?php

namespace App\Controller;

use App\Onboarding\ViewerContext;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

final class ViewerController extends AbstractController
{
    #[Route('/viewer/switch', name: 'app_viewer_switch', methods: ['POST'])]
    public function switch(Request $request, ViewerContext $viewerContext): RedirectResponse
    {
        if (!$this->isCsrfTokenValid('switch_viewer', (string) $request->request->get('_token'))) {
            throw $this->createAccessDeniedException('Invalid viewer switch token.');
        }

        $viewerContext->setViewerUserId($request->request->getInt('viewer_user_id'));

        $redirectTo = (string) $request->request->get('redirect_to', '/home');
        if (!str_starts_with($redirectTo, '/')) {
            $redirectTo = '/home';
        }

        return $this->redirect($redirectTo);
    }
}
