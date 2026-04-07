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
    public function findByPlan(Onboardingplan $plan, ?string $search = null, bool $caseSensitive = false): array
    {
        $builder = $this->createQueryBuilder('task')
            ->andWhere('task.plan = :plan')
            ->setParameter('plan', $plan)
            ->orderBy('task.taskId', 'DESC');

        $this->applySearch($builder, $search, $caseSensitive);

        return $builder->getQuery()->getResult();
    }

    /**
     * @return Onboardingtask[]
     */
    public function findVisibleFor(User $viewer, ?string $search = null, bool $caseSensitive = false): array
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
