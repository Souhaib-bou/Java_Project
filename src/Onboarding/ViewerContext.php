<?php

namespace App\Onboarding;

use App\Entity\Onboardingplan;
use App\Entity\Onboardingtask;
use App\Entity\User;
use App\Repository\UserRepository;
use Symfony\Component\HttpFoundation\RequestStack;

final class ViewerContext
{
    public const ROLE_CANDIDATE = 1;
    public const ROLE_RECRUITER = 2;
    public const ROLE_ADMIN = 3;

    private const SESSION_KEY = 'onboarding_viewer_user_id';

    public function __construct(
        private readonly RequestStack $requestStack,
        private readonly UserRepository $userRepository,
    ) {
    }

    public function getCurrentUser(): ?User
    {
        $request = $this->requestStack->getCurrentRequest();
        $session = $request?->getSession();

        $requestedViewerId = $request?->query->getInt('viewer', 0);
        if ($requestedViewerId > 0) {
            $user = $this->findSelectableUser($requestedViewerId);
            if ($user) {
                $session?->set(self::SESSION_KEY, $user->getUserId());

                return $user;
            }
        }

        $storedViewerId = $session?->get(self::SESSION_KEY);
        if (\is_int($storedViewerId) || ctype_digit((string) $storedViewerId)) {
            $user = $this->findSelectableUser((int) $storedViewerId);
            if ($user) {
                return $user;
            }
        }

        $fallback = $this->getFallbackUser();
        if ($fallback) {
            $session?->set(self::SESSION_KEY, $fallback->getUserId());
        }

        return $fallback;
    }

    /**
     * @return User[]
     */
    public function getAvailableUsers(): array
    {
        return $this->userRepository->createQueryBuilder('user')
            ->leftJoin('user.role', 'role')
            ->addSelect('role')
            ->andWhere('role.role_id IN (:roles)')
            ->setParameter('roles', [
                self::ROLE_ADMIN,
                self::ROLE_RECRUITER,
                self::ROLE_CANDIDATE,
            ])
            ->addOrderBy('role.role_id', 'DESC')
            ->addOrderBy('user.user_id', 'ASC')
            ->getQuery()
            ->getResult();
    }

    public function setViewerUserId(?int $viewerUserId): bool
    {
        $session = $this->requestStack->getCurrentRequest()?->getSession();
        if (!$session) {
            return false;
        }

        if (!$viewerUserId) {
            $session->remove(self::SESSION_KEY);

            return true;
        }

        $user = $this->findSelectableUser($viewerUserId);
        if (!$user) {
            return false;
        }

        $session->set(self::SESSION_KEY, $user->getUserId());

        return true;
    }

    public function getRoleId(): ?int
    {
        return $this->getCurrentUser()?->getRole()?->getRoleId();
    }

    public function getRoleName(): string
    {
        $roleName = $this->getCurrentUser()?->getRole()?->getName();

        return $roleName ? ucfirst(strtolower($roleName)) : 'Guest';
    }

    public function getRoleBadgeClass(): string
    {
        return match ($this->getRoleId()) {
            self::ROLE_ADMIN => 'viewer-badge admin',
            self::ROLE_RECRUITER => 'viewer-badge recruiter',
            self::ROLE_CANDIDATE => 'viewer-badge candidate',
            default => 'viewer-badge guest',
        };
    }

    public function getWorkspaceTitle(): string
    {
        return $this->isCandidate() ? 'My Onboarding Space' : 'Onboarding Coordination Workspace';
    }

    public function getSidebarTitle(): string
    {
        return $this->isCandidate() ? 'Candidate Workspace' : 'Admin Panel';
    }

    public function getCurrentUserDisplayName(): string
    {
        $user = $this->getCurrentUser();
        if (!$user) {
            return 'No active user';
        }

        return trim(($user->getFirstName() ?? '') . ' ' . ($user->getLastName() ?? ''));
    }

    public function isAdminOrRecruiter(): bool
    {
        return \in_array($this->getRoleId(), [self::ROLE_ADMIN, self::ROLE_RECRUITER], true);
    }

    public function isAdmin(): bool
    {
        return self::ROLE_ADMIN === $this->getRoleId();
    }

    public function isRecruiter(): bool
    {
        return self::ROLE_RECRUITER === $this->getRoleId();
    }

    public function isCandidate(): bool
    {
        return self::ROLE_CANDIDATE === $this->getRoleId();
    }

    public function canCreatePlans(): bool
    {
        return $this->isAdminOrRecruiter();
    }

    public function canDeletePlan(Onboardingplan $plan): bool
    {
        return $this->isAdminOrRecruiter() && $this->canViewPlan($plan);
    }

    public function canViewPlan(?Onboardingplan $plan): bool
    {
        if (!$plan) {
            return false;
        }

        if ($this->isAdminOrRecruiter()) {
            return true;
        }

        return $this->isCandidate() && $this->belongsToCurrentUser($plan->getUser());
    }

    public function canEditPlan(Onboardingplan $plan): bool
    {
        return $this->isAdminOrRecruiter() || ($this->isCandidate() && $this->canViewPlan($plan));
    }

    public function canFullyEditPlan(Onboardingplan $plan): bool
    {
        return $this->isAdminOrRecruiter() && $this->canViewPlan($plan);
    }

    public function canCreateTasks(): bool
    {
        return $this->isAdminOrRecruiter();
    }

    public function canViewTask(Onboardingtask $task): bool
    {
        return $this->canViewPlan($task->getPlan());
    }

    public function canEditTask(Onboardingtask $task): bool
    {
        return $this->isAdminOrRecruiter() || ($this->isCandidate() && $this->canViewTask($task));
    }

    public function canFullyEditTask(Onboardingtask $task): bool
    {
        return $this->isAdminOrRecruiter() && $this->canViewTask($task);
    }

    public function canDeleteTask(Onboardingtask $task): bool
    {
        return $this->isAdminOrRecruiter() && $this->canViewTask($task);
    }

    public function canViewQr(Onboardingplan $plan): bool
    {
        return $this->canViewPlan($plan);
    }

    private function getFallbackUser(): ?User
    {
        $users = $this->getAvailableUsers();

        return $users[0] ?? null;
    }

    private function findSelectableUser(int $userId): ?User
    {
        $user = $this->userRepository->find($userId);
        if (!$user) {
            return null;
        }

        $roleId = $user->getRole()?->getRoleId();
        if (!\in_array($roleId, [self::ROLE_ADMIN, self::ROLE_RECRUITER, self::ROLE_CANDIDATE], true)) {
            return null;
        }

        return $user;
    }

    private function belongsToCurrentUser(?User $user): bool
    {
        return $user && $this->getCurrentUser() && $user->getUserId() === $this->getCurrentUser()?->getUserId();
    }
}
