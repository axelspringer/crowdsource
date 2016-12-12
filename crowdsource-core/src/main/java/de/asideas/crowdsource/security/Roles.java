package de.asideas.crowdsource.security;

public interface Roles {
    /**
     * User that is logged in.
     */
    String ROLE_USER = "ROLE_USER";

    /**
     * Logged in user that has admin privileges.
     */
    String ROLE_ADMIN = "ROLE_ADMIN";
}
