package de.asideas.crowdsource.presentation.validator;

import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
public class ExistingEntityValidator implements ConstraintValidator<ExistingEntity, Long> {

    @PersistenceContext
    private EntityManager entityManager;

    private Class<?> clazz;

    @Override
    public void initialize(ExistingEntity constraintAnnotation) {
        clazz = constraintAnnotation.entityType();
    }

    @Override
    @Transactional
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        return entityManager.find(clazz, value) != null;
    }
}
