<?php

namespace App\Tests\Entity;

use App\Entity\Onboardingplan;
use App\Entity\User;
use PHPUnit\Framework\TestCase;
use DateTimeImmutable;
use Symfony\Component\Validator\Validation;
use Symfony\Component\Validator\Validator\ValidatorInterface;

class OnboardingPlanValidationTest extends TestCase
{
    private ValidatorInterface $validator;

    protected function setUp(): void
    {
        $this->validator = Validation::createValidatorBuilder()
            ->enableAttributeMapping()
            ->getValidator();
    }

    public function testPlanRequiresUserAndValidStatus(): void
    {
        $plan = new Onboardingplan();
        $plan->setStatus('invalid_status');
        $plan->setDeadline(new DateTimeImmutable('yesterday'));

        $violations = $this->validator->validate($plan, null, ['Default', 'full_edit']);
        $properties = $this->getViolationProperties($violations);

        self::assertContains('user', $properties);
        self::assertContains('status', $properties);
        self::assertContains('deadline', $properties);
    }

    public function testPlanAcceptsValidData(): void
    {
        $plan = new Onboardingplan();
        $plan->setUser(new User());
        $plan->setStatus(Onboardingplan::STATUS_PENDING);
        $plan->setDeadline(new DateTimeImmutable('today'));

        $violations = $this->validator->validate($plan, null, ['Default', 'full_edit']);

        self::assertCount(0, $violations);
    }

    public function testCandidateValidationAllowsStatusOnlyUpdatesOnPastDeadline(): void
    {
        $plan = new Onboardingplan();
        $plan->setStatus(Onboardingplan::STATUS_COMPLETED);
        $plan->setDeadline(new DateTimeImmutable('yesterday'));

        $violations = $this->validator->validate($plan);
        $properties = $this->getViolationProperties($violations);

        self::assertNotContains('deadline', $properties);
        self::assertNotContains('user', $properties);
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
