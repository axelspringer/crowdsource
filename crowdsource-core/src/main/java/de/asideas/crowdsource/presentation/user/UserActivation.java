package de.asideas.crowdsource.presentation.user;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Pattern;

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

    public String getActivationToken() {
        return this.activationToken;
    }

    public String getPassword() {
        return this.password;
    }

    public void setActivationToken(String activationToken) {
        this.activationToken = activationToken;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
