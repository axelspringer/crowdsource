package de.asideas.crowdsource.presentation.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExistingEntitiesValidator.class)
@Documented
public @interface ExistingEntities {

    String message() default "Entity does not exist";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    Class<?> entityType();
}
