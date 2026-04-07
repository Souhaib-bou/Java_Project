<?php

namespace App\Repository;

use App\Entity\Onboardingplan;
use App\Entity\Onboardingtask;
use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\QueryBuilder;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Onboardingtask>
 */
class OnboardingtaskRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Onboardingtask::class);
    }

    /**
     * @return Onboardingtask[]
     */
    public function findByPlan(Onboardingplan $plan, ?string $search = null, bool $caseSensitive = false, array $filters = []): array
    {
        $builder = $this->createQueryBuilder('task')
            ->andWhere('task.plan = :plan')
            ->setParameter('plan', $plan)
            ->orderBy('task.taskId', 'DESC');

        $this->applySearch($builder, $search, $caseSensitive);
        $this->applyFilters($builder, $filters);
        $this->applySorting($builder, (string) ($filters['sort'] ?? 'newest'));

        return $builder->getQuery()->getResult();
    }

    /**
     * @return Onboardingtask[]
     */
    public function findVisibleFor(User $viewer, ?string $search = null, bool $caseSensitive = false, array $filters = []): array
    {
        $builder = $this->createQueryBuilder('task')
            ->leftJoin('task.plan', 'plan')
            ->leftJoin('plan.user', 'user')
            ->addSelect('plan', 'user')
            ->orderBy('task.taskId', 'DESC');

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
                $this->buildLikeCondition('task.title', $caseSensitive, $parameterName),
                $this->buildLikeCondition('task.status', $caseSensitive, $parameterName),
                $this->buildLikeCondition('task.original_file_name', $caseSensitive, $parameterName),
                $this->buildLikeCondition('task.content_type', $caseSensitive, $parameterName),
            ];

            if (ctype_digit($token)) {
                $conditions[] = 'task.taskId = :taskId_' . $index;
                $builder->setParameter('taskId_' . $index, (int) $token);
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

    private function applyFilters(QueryBuilder $builder, array $filters): void
    {
        $status = trim((string) ($filters['status'] ?? ''));
        if ('' !== $status) {
            $builder
                ->andWhere('task.status = :taskStatus')
                ->setParameter('taskStatus', $status);
        }

        if (!empty($filters['attachment_only'])) {
            $builder
                ->andWhere('task.filePath IS NOT NULL')
                ->andWhere('task.filePath != :emptyFilePath')
                ->setParameter('emptyFilePath', '');
        }
    }

    private function applySorting(QueryBuilder $builder, string $sort): void
    {
        switch ($sort) {
            case 'title':
                $builder
                    ->resetDQLPart('orderBy')
                    ->addOrderBy('task.title', 'ASC')
                    ->addOrderBy('task.taskId', 'DESC');
                break;

            case 'status':
                $builder
                    ->resetDQLPart('orderBy')
                    ->addOrderBy('task.status', 'ASC')
                    ->addOrderBy('task.taskId', 'DESC');
                break;

            case 'deadline':
                $builder
                    ->resetDQLPart('orderBy')
                    ->addOrderBy('CASE WHEN task.deadline IS NULL THEN 1 ELSE 0 END', 'ASC')
                    ->addOrderBy('task.deadline', 'ASC')
                    ->addOrderBy('task.taskId', 'DESC');
                break;

            case 'newest':
            default:
                $builder
                    ->resetDQLPart('orderBy')
                    ->addOrderBy('task.taskId', 'DESC');
                break;
        }
    }

    //    /**
    //     * @return Onboardingtask[] Returns an array of Onboardingtask objects
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

    //    public function findOneBySomeField($value): ?Onboardingtask
    //    {
    //        return $this->createQueryBuilder('o')
    //            ->andWhere('o.exampleField = :val')
    //            ->setParameter('val', $value)
    //            ->getQuery()
    //            ->getOneOrNullResult()
    //        ;
    //    }
}
