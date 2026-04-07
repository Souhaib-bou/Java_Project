<?php

namespace App\Repository;

use App\Entity\Onboardingplan;
use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\QueryBuilder;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Onboardingplan>
 */
class OnboardingplanRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Onboardingplan::class);
    }

    /**
     * @return Onboardingplan[]
     */
    public function findVisibleFor(User $viewer, ?string $search = null, bool $caseSensitive = false, array $filters = []): array
    {
        $builder = $this->createQueryBuilder('plan')
            ->leftJoin('plan.user', 'user')
            ->addSelect('user')
            ->orderBy('plan.planId', 'DESC');

        if (1 === $viewer->getRole()?->getRoleId()) {
            $builder
                ->andWhere('plan.user = :viewer')
                ->setParameter('viewer', $viewer);
        }

        $this->applySearch($builder, $search, $caseSensitive);
        $this->applyFilters($builder, $filters);
        $this->applySorting($builder, (string) ($filters['sort'] ?? 'newest'));

        return $builder->getQuery()->getResult();
    }

    public function findOneByQrToken(string $token): ?Onboardingplan
    {
        return $this->createQueryBuilder('plan')
            ->leftJoin('plan.user', 'user')
            ->leftJoin('plan.onboardingtasks', 'task')
            ->addSelect('user', 'task')
            ->andWhere('plan.qr_token = :token')
            ->setParameter('token', trim($token))
            ->getQuery()
            ->getOneOrNullResult();
    }

    private function applySearch(QueryBuilder $builder, ?string $search, bool $caseSensitive): void
    {
        $search = null !== $search ? trim($search) : '';

        if ('' === $search) {
            return;
        }

        $tokens = preg_split('/\s+/', $search) ?: [];

        foreach ($tokens as $index => $token) {
            $parameterName = 'search_' . $index;
            $searchTerm = '%' . ($caseSensitive ? $token : mb_strtolower($token)) . '%';
            $conditions = [
                $this->buildLikeCondition('user.first_name', $caseSensitive, $parameterName),
                $this->buildLikeCondition('user.last_name', $caseSensitive, $parameterName),
                $this->buildLikeCondition('user.email', $caseSensitive, $parameterName),
                $this->buildLikeCondition('plan.status', $caseSensitive, $parameterName),
                $this->buildLikeCondition('plan.qr_token', $caseSensitive, $parameterName),
                $this->buildFullNameCondition($caseSensitive, $parameterName),
            ];

            if (ctype_digit($token)) {
                $conditions[] = 'plan.planId = :planId_' . $index;
                $builder->setParameter('planId_' . $index, (int) $token);
            }

            $builder
                ->andWhere('(' . implode(' OR ', $conditions) . ')')
                ->setParameter($parameterName, $searchTerm);
        }
    }

    private function buildLikeCondition(string $field, bool $caseSensitive, string $parameterName): string
    {
        if ($caseSensitive) {
            return sprintf('COALESCE(%s, \'\') LIKE :%s', $field, $parameterName);
        }

        return sprintf('LOWER(COALESCE(%s, \'\')) LIKE :%s', $field, $parameterName);
    }

    private function buildFullNameCondition(bool $caseSensitive, string $parameterName): string
    {
        $fullNameExpression = 'CONCAT(CONCAT(COALESCE(user.first_name, \'\'), \' \'), COALESCE(user.last_name, \'\'))';

        if ($caseSensitive) {
            return sprintf('%s LIKE :%s', $fullNameExpression, $parameterName);
        }

        return sprintf('LOWER(%s) LIKE :%s', $fullNameExpression, $parameterName);
    }

    private function applyFilters(QueryBuilder $builder, array $filters): void
    {
        $status = trim((string) ($filters['status'] ?? ''));
        if ('' !== $status) {
            $builder
                ->andWhere('plan.status = :status')
                ->setParameter('status', $status);
        }

        if (!empty($filters['overdue_only'])) {
            $builder
                ->andWhere('plan.deadline IS NOT NULL')
                ->andWhere('plan.deadline < :today')
                ->andWhere('plan.status != :completedStatus')
                ->setParameter('today', new \DateTimeImmutable('today'))
                ->setParameter('completedStatus', Onboardingplan::STATUS_COMPLETED);
        }
    }

    private function applySorting(QueryBuilder $builder, string $sort): void
    {
        switch ($sort) {
            case 'deadline':
                $builder
                    ->resetDQLPart('orderBy')
                    ->addOrderBy('CASE WHEN plan.deadline IS NULL THEN 1 ELSE 0 END', 'ASC')
                    ->addOrderBy('plan.deadline', 'ASC')
                    ->addOrderBy('plan.planId', 'DESC');
                break;

            case 'status':
                $builder
                    ->resetDQLPart('orderBy')
                    ->addOrderBy('plan.status', 'ASC')
                    ->addOrderBy('plan.deadline', 'ASC')
                    ->addOrderBy('plan.planId', 'DESC');
                break;

            case 'user':
                $builder
                    ->resetDQLPart('orderBy')
                    ->addOrderBy('user.first_name', 'ASC')
                    ->addOrderBy('user.last_name', 'ASC')
                    ->addOrderBy('plan.planId', 'DESC');
                break;

            case 'completion':
                $builder
                    ->resetDQLPart('orderBy')
                    ->addOrderBy('plan.status', 'DESC')
                    ->addOrderBy('plan.planId', 'DESC');
                break;

            case 'newest':
            default:
                $builder
                    ->resetDQLPart('orderBy')
                    ->addOrderBy('plan.planId', 'DESC');
                break;
        }
    }

    //    /**
    //     * @return Onboardingplan[] Returns an array of Onboardingplan objects
    //     */
    //    public function findByExampleField($value): array
    //    {
    //        return $this->createQueryBuilder('o')
    //            ->andWhere('o.exampleField = :val')
    //            ->setParameter('val', $value)
    //            ->orderBy('o.id', 'ASC')
    //            ->setMaxResults(10)
    //            ->getQuery()
    //            ->getResult()
    //        ;
    //    }

    //    public function findOneBySomeField($value): ?Onboardingplan
    //    {
    //        return $this->createQueryBuilder('o')
    //            ->andWhere('o.exampleField = :val')
    //            ->setParameter('val', $value)
    //            ->getQuery()
    //            ->getOneOrNullResult()
    //        ;
    //    }
}
