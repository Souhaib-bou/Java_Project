<?php

namespace App\Tests\Entity;

use App\Entity\Onboardingplan;
use App\Entity\Onboardingtask;
use PHPUnit\Framework\TestCase;
use DateTimeImmutable;
use Symfony\Component\Validator\Validation;
use Symfony\Component\Validator\Validator\ValidatorInterface;

class OnboardingTaskValidationTest extends TestCase
{
    private ValidatorInterface $validator;

    protected function setUp(): void
    {
        $this->validator = Validation::createValidatorBuilder()
            ->enableAttributeMapping()
            ->getValidator();
    }

    public function testTaskRequiresPlanTitleAndValidStatus(): void
    {
        $task = new Onboardingtask();
        $task->setTitle(' ');
        $task->setDescription('short');
        $task->setStatus('pending');
        $task->setDeadline(new DateTimeImmutable('yesterday'));

        $violations = $this->validator->validate($task, null, ['Default', 'full_edit']);
        $properties = $this->getViolationProperties($violations);

        self::assertContains('plan', $properties);
        self::assertContains('title', $properties);
        self::assertContains('description', $properties);
        self::assertContains('status', $properties);
        self::assertContains('deadline', $properties);
    }

    public function testTaskAcceptsValidData(): void
    {
        $task = new Onboardingtask();
        $task->setPlan(new Onboardingplan());
        $task->setTitle('Prepare onboarding checklist');
        $task->setDescription('Prepare the checklist, documents, and first-day tasks.');
        $task->setStatus(Onboardingtask::STATUS_NOT_STARTED);
        $task->setDeadline(new DateTimeImmutable('tomorrow'));
        $task->setFilePath('https://res.cloudinary.com/hirely-demo/image/upload/onboarding-checklist.pdf');

        $violations = $this->validator->validate($task, null, ['Default', 'full_edit']);

        self::assertCount(0, $violations);
    }

    public function testCandidateValidationAllowsStatusOnlyUpdatesOnPastDeadline(): void
    {
        $task = new Onboardingtask();
        $task->setStatus(Onboardingtask::STATUS_COMPLETED);
        $task->setDeadline(new DateTimeImmutable('yesterday'));

        $violations = $this->validator->validate($task);
        $properties = $this->getViolationProperties($violations);

        self::assertNotContains('deadline', $properties);
        self::assertNotContains('title', $properties);
        self::assertNotContains('description', $properties);
        self::assertNotContains('plan', $properties);
    }

    private function getViolationProperties(iterable $violations): array
    {
        $properties = [];

        foreach ($violations as $violation) {
            $properties[] = $violation->getPropertyPath();
        }

        return array_values(array_unique($properties));
    }
}
