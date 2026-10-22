package com.aicareer.platform.identity.api;

import com.aicareer.platform.identity.application.AuthenticationService;
import com.aicareer.platform.identity.application.AuthenticationResult;
import com.aicareer.platform.identity.application.SignupPolicyProperties;
import com.aicareer.platform.identity.application.EmailVerificationProperties;
import com.aicareer.platform.identity.application.EmailVerificationRateLimiter;
import com.aicareer.platform.identity.application.EmailVerificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.ACCEPTED;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final SignupPolicyProperties signupPolicyProperties;
    private final EmailVerificationProperties emailVerificationProperties;
    private final EmailVerificationService emailVerificationService;
    private final EmailVerificationRateLimiter emailVerificationRateLimiter;

    public AuthController(
            AuthenticationService authenticationService,
            SignupPolicyProperties signupPolicyProperties,
            EmailVerificationProperties emailVerificationProperties,
            EmailVerificationService emailVerificationService,
            EmailVerificationRateLimiter emailVerificationRateLimiter
    ) {
        this.authenticationService = authenticationService;
        this.signupPolicyProperties = signupPolicyProperties;
        this.emailVerificationProperties = emailVerificationProperties;
        this.emailVerificationService = emailVerificationService;
        this.emailVerificationRateLimiter = emailVerificationRateLimiter;
    }

    @GetMapping("/signup-policy")
    public SignupPolicyResponse signupPolicy() {
        return new SignupPolicyResponse(
                signupPolicyProperties.termsVersion(),
                signupPolicyProperties.privacyVersion(),
                emailVerificationProperties.enabled()
        );
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest
    ) {
        if (emailVerificationProperties.enabled()) {
            emailVerificationRateLimiter.checkClientRequest(httpRequest.getRemoteAddr());
        }
        SignupResponse response = SignupResponse.from(authenticationService.register(
                request.email(),
                request.password(),
                request.displayName()
        ));
        return ResponseEntity.status(response.verificationRequired() ? ACCEPTED : CREATED).body(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        emailVerificationService.verify(request.email(), request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(
            @Valid @RequestBody EmailVerificationResendRequest request,
            HttpServletRequest httpRequest
    ) {
        emailVerificationRateLimiter.checkClientRequest(httpRequest.getRemoteAddr());
        emailVerificationService.resend(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/login")
    public AuthenticationResponse login(@Valid @RequestBody LoginRequest request) {
        return AuthenticationResponse.from(authenticationService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public AuthenticationResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthenticationResult result = authenticationService.refresh(request.refreshToken());
        return AuthenticationResponse.from(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
