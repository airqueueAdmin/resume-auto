package com.aicareer.platform.identity.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_consents")
public class UserConsent {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "terms_version", nullable = false, length = 50)
    private String termsVersion;

    @Column(name = "privacy_version", nullable = false, length = 50)
    private String privacyVersion;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    protected UserConsent() {
    }

    private UserConsent(User user, String termsVersion, String privacyVersion, Instant acceptedAt) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.termsVersion = termsVersion;
        this.privacyVersion = privacyVersion;
        this.acceptedAt = acceptedAt;
    }

    public static UserConsent accept(
            User user,
            String termsVersion,
            String privacyVersion,
            Instant acceptedAt
    ) {
        return new UserConsent(user, termsVersion, privacyVersion, acceptedAt);
    }
}
