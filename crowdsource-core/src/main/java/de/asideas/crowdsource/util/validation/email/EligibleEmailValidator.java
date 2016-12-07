package de.asideas.crowdsource.util.validation.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

/**
 * Validator for {@link de.asideas.crowdsource.util.validation.email.EligibleEmail}
 */
@Service
public class EligibleEmailValidator implements ConstraintValidator<EligibleEmail, String> {

    @Value("#{'${de.asideas.crowdsource.content.email.blacklist.patterns}'.split(',')}")
    private List<String> emailBlacklistPatterns;

    @Override
    public void initialize(EligibleEmail constraintAnnotation) {
        // no-op
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {

        email = email.toLowerCase();

        if (doesNotMatchBlacklistPatterns(email)) {
            return true;
        }

        context.buildConstraintViolationWithTemplate("eligible").addConstraintViolation();

        return false;
    }

    private boolean doesNotMatchBlacklistPatterns(String email) {
        for (String emailBlacklistPattern : emailBlacklistPatterns) {
            if (email.contains(emailBlacklistPattern)) {
                return false;
            }
        }
        return true;
    }
}
