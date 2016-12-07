package de.asideas.crowdsource.util.validation.email;

import de.asideas.crowdsource.testutil.ValidatorTestUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

public class EligibleEmailValidatorTest {

    private EligibleEmailValidator eligibleEmailValidator;

    @Before
    public void beforeMethod() throws Exception {
        eligibleEmailValidator = spy(new EligibleEmailValidator());
        Whitebox.setInternalState(eligibleEmailValidator, "emailBlacklistPatterns", Arrays.asList("_extern"));
    }

    @Test
    public void testIsValidValidEmail() throws Exception {

        assertTrue(eligibleEmailValidator.isValid("test@crowd.source.de", ValidatorTestUtil.constraintValidatorContext()));
    }

    @Test
    public void testIsValidConsultantEmail() throws Exception {

        assertFalse(eligibleEmailValidator.isValid("test_extern@crowd.source.de", ValidatorTestUtil.constraintValidatorContext()));
    }

}