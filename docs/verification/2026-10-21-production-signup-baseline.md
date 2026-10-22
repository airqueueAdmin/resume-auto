# Production signup baseline — 2026-10-21

This is a feature verification record, not a phase completion record.

## Objective

Harden signup beyond a development-only name/email/password form while preserving the existing login,
JWT, refresh-session, ownership, and rate-limit architecture.

## Implemented

- Added browser-side password confirmation, length feedback, field-level errors, and duplicate-account
  guidance.
- Added required terms and privacy acknowledgements with separate policy pages.
- Added a public signup-policy endpoint exposing server-controlled policy versions.
- Added server-side consent validation and immutable acceptance records containing the user, policy
  versions, and acceptance time.
- Added Flyway migration `V17__create_user_consents.sql`.
- Added database-race handling so concurrent duplicate signups return the same stable `409` contract.
- Kept signup and policy routes under the existing IP-based authentication rate-limit bucket.
- Added provider-neutral SMTP delivery, configured for dedicated Gmail submission over STARTTLS port
  587 by default in the Oracle VM deployment.
- Added `PENDING_EMAIL_VERIFICATION` accounts, six-digit code verification, resend, and activation.
- Stored only HMAC-SHA256 code digests with a five-minute expiry, five-attempt limit, and one-minute
  resend cooldown.
- Added a dedicated verification screen and blocked login until email ownership is confirmed.
- Added Flyway migration `V18__add_email_verification.sql`.

## Security and privacy decisions

- Password confirmation is not transmitted or stored.
- The server chooses the policy versions; the browser only sends explicit acceptance booleans.
- Passwords remain one-way encoded through Spring Security.
- Production delivery uses a dedicated Gmail account and Google app password; credentials remain
  backend-only. Tests replace the outbound port with a capture implementation and never send mail.
- The code HMAC secret is separate from the JWT secret so either credential can be rotated independently.
- Resend returns the same accepted response for unknown, active, and pending accounts to reduce account
  enumeration. Authentication endpoints remain protected by the existing IP rate-limit bucket.
- Signup and resend are additionally limited to 20 requests per client per hour, while actual messages
  are capped globally at 400 per day to preserve headroom under Gmail's free-account limit. These
  counters are process-local and reset after a backend restart.
- Operator identity, legal contact details, and final policy wording must be reviewed before public
  launch; application code cannot determine those business facts.

## Verification

- `npm run build`
- `./mvnw test`
