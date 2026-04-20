package com.itesm.application.usecase;

import com.itesm.application.dto.LoginDto;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.security.TokenService;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.User;
import com.itesm.domain.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class LoginUseCase {

    @Inject
    UserRepository userRepository;

    @Inject
    TokenService tokenService;

    public String execute(LoginDto loginDto) {
        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Set<String> roles = user.getRoles().stream().map(r -> r.getCode()).collect(Collectors.toSet());
        Set<String> privileges = user.getRoles().stream()
                .flatMap(r -> r.getPrivileges().stream())
                .map(p -> p.getCode())
                .collect(Collectors.toSet());

        CurrentUser currentUser = new CurrentUser(
                user.getId(),
                user.getExternalAuthId(),
                user.getEmail(),
                user.getFullName(),
                roles,
                privileges
        );

        return tokenService.issueToken(currentUser);
    }
}
