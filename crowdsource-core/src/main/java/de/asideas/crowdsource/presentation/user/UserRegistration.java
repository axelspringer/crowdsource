package de.asideas.crowdsource.presentation.user;

import de.asideas.crowdsource.util.validation.email.EligibleEmail;
import de.asideas.crowdsource.util.validation.email.NotActivated;
import lombok.Data;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.AssertTrue;

@Data
public class UserRegistration {

    @NotEmpty
    @Email
    @NotActivated
    @EligibleEmail
    private String email;
    @NotBlank
    private String firstname;
    @NotBlank
    private String lastname;

    @AssertTrue
    private boolean termsOfServiceAccepted;

    public UserRegistration() {
    }
}
