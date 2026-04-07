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

        $violations = $this->validator->validate($task);
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

        $violations = $this->validator->validate($task);

        self::assertCount(0, $violations);
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
