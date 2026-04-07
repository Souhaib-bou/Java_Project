<?php

namespace App\Twig;

use App\Onboarding\ViewerContext;
use Twig\Extension\AbstractExtension;
use Twig\Extension\GlobalsInterface;

final class ViewerContextExtension extends AbstractExtension implements GlobalsInterface
{
    public function __construct(
        private readonly ViewerContext $viewerContext,
    ) {
    }

    public function getGlobals(): array
    {
        return [
            'viewer_context' => $this->viewerContext,
        ];
    }
}
