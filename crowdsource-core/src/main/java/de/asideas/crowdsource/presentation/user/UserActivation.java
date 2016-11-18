package de.asideas.crowdsource.presentation.user;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Pattern;

@Data
public class UserActivation {

    @NotEmpty
    private String activationToken;

    @NotEmpty
    // (at least one non-word-character in password)(no whitespaces anywhere in password).{min 8 chars long}
    @Pattern(regexp = "(?=.*\\W)(?=\\S+$).{8,}", message = "insecure_password")
    private String password;

    @java.beans.ConstructorProperties({"activationToken", "password"})
    public UserActivation(String activationToken, String password) {
        this.activationToken = activationToken;
        this.password = password;
    }

    public UserActivation() {
    }
}
