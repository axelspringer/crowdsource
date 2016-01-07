package de.asideas.crowdsource.domain.shared;

public enum ProjectStatus {

    // just saved
    PROPOSED,
    // accepted by admin
    PUBLISHED,
    // rejected by admin
    REJECTED,
    // deferred by admin
    DEFERRED,
    // by admin
    PUBLISHED_DEFERRED,
    // fully pledged / all money that is needed
    FULLY_PLEDGED
}
