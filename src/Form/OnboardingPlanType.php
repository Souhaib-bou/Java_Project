<?php

namespace App\Form;

use App\Entity\Onboardingplan;
use App\Entity\User;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\Form\FormInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class OnboardingPlanType extends AbstractType
{
    private const EDITOR_MODE_FULL = 'full';
    private const EDITOR_MODE_CANDIDATE = 'candidate';

    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        if (self::EDITOR_MODE_FULL === $options['editor_mode']) {
            $builder->add('user', EntityType::class, [
                'class' => User::class,
                'choice_label' => function (User $user) {
                    return $user->getFirstName() . ' ' . $user->getLastName();
                },
                'label' => 'User',
                'placeholder' => 'Select a user',
            ]);
        }

        $builder->add('status', ChoiceType::class, [
            'choices' => Onboardingplan::getStatusChoices(),
            'label' => 'Status',
            'required' => true,
            'placeholder' => false,
            'empty_data' => Onboardingplan::STATUS_PENDING,
        ]);

        if (self::EDITOR_MODE_FULL === $options['editor_mode']) {
            $builder->add('deadline', DateType::class, [
                'widget' => 'single_text',
                'required' => false,
                'label' => 'Deadline',
            ]);
        }
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Onboardingplan::class,
            'editor_mode' => self::EDITOR_MODE_FULL,
            'validation_groups' => static function (FormInterface $form): array {
                return self::EDITOR_MODE_FULL === $form->getConfig()->getOption('editor_mode')
                    ? ['Default', 'full_edit']
                    : ['Default'];
            },
        ]);

        $resolver->setAllowedValues('editor_mode', [
            self::EDITOR_MODE_FULL,
            self::EDITOR_MODE_CANDIDATE,
        ]);
    }
}
