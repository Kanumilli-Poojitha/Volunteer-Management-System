package com.volunteerhub.model;

public enum Role {
    ADMIN, ORGANIZER, VOLUNTEER;

    /** Spring Security authority name, e.g. ROLE_ADMIN. */
    public String authority() {
        return "ROLE_" + name();
    }
}
