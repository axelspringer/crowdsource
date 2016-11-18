package de.asideas.crowdsource.util.validation.financinground;

import de.asideas.crowdsource.presentation.FinancingRound;
import de.asideas.crowdsource.service.FinancingRoundService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class FinancingRoundNotCollidingValidator implements ConstraintValidator<FinancingRoundNotColliding, FinancingRound> {

    @Autowired
    private FinancingRoundService financingRoundService;

    @Override
    public void initialize(FinancingRoundNotColliding constraintAnnotation) {
    }

    @Override
    public boolean isValid(final FinancingRound financingRound, ConstraintValidatorContext context) {

        for (FinancingRound fr : financingRoundService.allFinancingRounds()) {

            // if the other financing round is not starting after financingrounds end
            // AND it is not ending before financingrounds start we have a collision
            if (!fr.getStartDate().isAfter(financingRound.getEndDate())
                    && !fr.getEndDate().isBefore(financingRound.getStartDate())) {
                context.buildConstraintViolationWithTemplate("non-colliding").addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
