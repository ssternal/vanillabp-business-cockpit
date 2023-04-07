package io.vanillabp.cockpit.commons.utils;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import io.vanillabp.cockpit.commons.exceptions.BcForbiddenException;
import reactor.core.publisher.Mono;

@Component
public class UserContext {
    
    public String getUserLoggedIn() {
        
        final var authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        
        if (authentication == null) {
            throw new BcForbiddenException("No security context");
        }
        if (!authentication.isAuthenticated()) {
            throw new BcForbiddenException("User anonymous");
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        
        return ((org.springframework.security.core.userdetails.User) authentication
                    .getPrincipal())
                    .getUsername();
        
    }
    
    public Mono<String> getUserLoggedInAsMono() {
        
        return ReactiveSecurityContextHolder
                .getContext()
                .map(c -> c.getAuthentication())
                .map(authentication -> {
                    if (authentication == null) {
                        throw new BcForbiddenException("No security context");
                    }
                    if (!authentication.isAuthenticated()) {
                        throw new BcForbiddenException("User anonymous");
                    }
                    if (authentication instanceof AnonymousAuthenticationToken) {
                        return null;
                    }
                    return ((org.springframework.security.core.userdetails.User) authentication
                            .getPrincipal())
                            .getUsername();
                });
        
    }
    
}