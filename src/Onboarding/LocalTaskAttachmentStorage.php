<?php

namespace App\Onboarding;

use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\String\Slugger\SluggerInterface;

final class LocalTaskAttachmentStorage
{
    public function __construct(
        private readonly string $projectDir,
        private readonly SluggerInterface $slugger,
    ) {
    }

    /**
     * @return array{public_path: string, stored_name: string, original_name: string, content_type: string}
     */
    public function store(UploadedFile $file): array
    {
        $originalName = $file->getClientOriginalName() ?: 'attachment';
        $baseName = pathinfo($originalName, \PATHINFO_FILENAME);
        $safeBaseName = (string) $this->slugger->slug($baseName);
        $safeBaseName = '' !== $safeBaseName ? $safeBaseName : 'attachment';

        $extension = $file->guessExtension() ?: $file->getClientOriginalExtension() ?: 'bin';
        $storedName = sprintf(
            '%s-%s.%s',
            $safeBaseName,
            bin2hex(random_bytes(6)),
            strtolower($extension)
        );

        $targetDirectory = $this->projectDir . DIRECTORY_SEPARATOR . 'public' . DIRECTORY_SEPARATOR . 'uploads' . DIRECTORY_SEPARATOR . 'onboarding' . DIRECTORY_SEPARATOR . 'tasks';
        if (!is_dir($targetDirectory)) {
            mkdir($targetDirectory, 0777, true);
        }

        $file->move($targetDirectory, $storedName);

        return [
            'public_path' => '/uploads/onboarding/tasks/' . $storedName,
            'stored_name' => $storedName,
            'original_name' => $originalName,
            'content_type' => $file->getClientMimeType() ?: $file->getMimeType() ?: 'application/octet-stream',
        ];
    }
}
