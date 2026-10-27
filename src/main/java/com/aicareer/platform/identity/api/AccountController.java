package com.aicareer.platform.identity.api;

import java.util.UUID;

import com.aicareer.platform.identity.application.AuthenticationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AuthenticationService authenticationService;

    public AccountController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt) {
        authenticationService.deleteAccount(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }
}
