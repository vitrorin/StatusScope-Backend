package com.itesm.application.security;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AuthenticatedUserContext {
    private CurrentUser currentUser;

    public CurrentUser getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }
}
