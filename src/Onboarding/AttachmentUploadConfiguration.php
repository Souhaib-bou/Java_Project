<?php

namespace App\Onboarding;

final class AttachmentUploadConfiguration
{
    public function __construct(
        ?string $cloudName,
        ?string $unsignedPreset,
    ) {
        $this->cloudName = $cloudName ?? '';
        $this->unsignedPreset = $unsignedPreset ?? '';
    }

    private readonly string $cloudName;
    private readonly string $unsignedPreset;

    public function isEnabled(): bool
    {
        return '' !== trim($this->cloudName) && '' !== trim($this->unsignedPreset);
    }

    public function getCloudName(): string
    {
        return $this->cloudName;
    }

    public function getUnsignedPreset(): string
    {
        return $this->unsignedPreset;
    }

    /**
     * @return array{enabled: bool, cloud_name: string, unsigned_preset: string}
     */
    public function toViewData(): array
    {
        return [
            'enabled' => $this->isEnabled(),
            'cloud_name' => $this->cloudName,
            'unsigned_preset' => $this->unsignedPreset,
        ];
    }
}
