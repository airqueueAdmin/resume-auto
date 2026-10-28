# ADR 0022: Use an HTTPS Email API for Render Free Email Verification

## Status

Accepted

## Context

Render Free Web Services block outbound SMTP traffic on ports 25, 465, and 587. The email-verification
flow originally used Gmail SMTP submission on port 587, so valid SMTP credentials still produced a
delivery-unavailable response from the deployed service.

## Decision

Keep email delivery behind the existing `EmailVerificationMessageSender` application port and support
two infrastructure providers selected by `EMAIL_PROVIDER`:

- `smtp` keeps the existing Gmail/SMTP implementation for local or paid-hosting deployments.
- `resend` calls the Resend HTTPS email endpoint with a server-side API key and is the Render Free
  deployment provider.

The Resend API key is injected only into the backend environment. The sender address must be configured
and verified with the selected provider. Provider failures remain mapped to the existing generic
`EMAIL_DELIVERY_UNAVAILABLE` response, while logs contain only the provider and HTTP status, never the
API key, email body, verification code, or recipient address.

## Consequences

- Render Free can deliver verification mail without changing the hosting plan.
- The service now depends on Resend for the Render Free deployment and its free-plan limits.
- A provider-specific adapter and configuration are tested without calling a live provider in CI.
- Existing SMTP deployments do not need to change provider settings.
