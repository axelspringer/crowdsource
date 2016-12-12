package de.asideas.crowdsource.presentation.validator;

import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

@Component
public class ExistingEntitiesValidator implements ConstraintValidator<ExistingEntities, List> {

    @PersistenceContext
    private EntityManager entityManager;

    private Class<?> clazz;

    @Override
    public void initialize(ExistingEntities constraintAnnotation) {
        clazz = constraintAnnotation.entityType();
    }

    @Override
    @Transactional
    public boolean isValid(List value, ConstraintValidatorContext context) {
        final Long count = value.stream().filter(v -> v instanceof Long && entityManager.find(clazz, v) != null).count();

        return value.size() == count.intValue();
    }
}
