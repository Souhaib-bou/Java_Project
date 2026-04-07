<?php

namespace App\Form;

use App\Entity\Onboardingtask;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\HiddenType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class OnboardingTaskType extends AbstractType
{
    private const EDITOR_MODE_FULL = 'full';
    private const EDITOR_MODE_CANDIDATE = 'candidate';

    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        if (self::EDITOR_MODE_FULL === $options['editor_mode']) {
            $builder->add('title', TextType::class, [
                'label' => 'Title',
                'required' => true,
            ]);

            $builder->add('description', TextareaType::class, [
                'label' => 'Description',
                'required' => true,
            ]);
        }

        $builder->add('status', ChoiceType::class, [
            'choices' => Onboardingtask::getStatusChoices(),
            'label' => 'Status',
            'required' => true,
            'placeholder' => false,
            'empty_data' => Onboardingtask::STATUS_NOT_STARTED,
        ]);

        if (self::EDITOR_MODE_FULL === $options['editor_mode']) {
            $builder->add('deadline', DateType::class, [
                'widget' => 'single_text',
                'required' => false,
                'label' => 'Deadline',
            ]);
        }

        $builder->add('filePath', HiddenType::class, [
            'required' => false,
        ]);

        $builder->add('cloudinaryPublicId', HiddenType::class, [
            'required' => false,
        ]);

        $builder->add('originalFileName', HiddenType::class, [
            'required' => false,
        ]);

        $builder->add('contentType', HiddenType::class, [
            'required' => false,
        ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Onboardingtask::class,
            'editor_mode' => self::EDITOR_MODE_FULL,
        ]);

        $resolver->setAllowedValues('editor_mode', [
            self::EDITOR_MODE_FULL,
            self::EDITOR_MODE_CANDIDATE,
        ]);
    }
}
