# Personal data protection runbook

## Required deployment secret

Set `DATA_ENCRYPTION_KEY_BASE64` to a randomly generated Base64 value that decodes to exactly 32 bytes. Keep it in the deployment secret manager; do not commit it, print it, or put it in an issue or log.

Example generation:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

The local Compose fallback is for development only. Production must supply its own key before the first startup that applies migration `V22` or later encryption migrations. A missing variable must not be passed through literally as `${DATA_ENCRYPTION_KEY_BASE64}`; startup should fail with a missing-key configuration error.

## Protected data

- `users.email` and `users.display_name`
- Experience and Project text, URLs, descriptions, career periods, and employment status
- Resume names and immutable ResumeVersion content
- Job posting text and source URLs
- Resume application questions and answers
- AI analysis, match, and resume draft JSON
- Retrieval embedding source text

Passwords, refresh tokens, verification codes, extension authorization codes, and tool approval tokens remain one-way hashed because the service never needs to recover their original values.

## Operational rules

- Back up the encryption key separately from the database backup.
- Never change the key casually. A key change requires a controlled re-encryption migration.
- Do not inspect protected columns by copying database output into tickets or chat.
- The authenticated owner may receive decrypted content through the API; cross-user access must still return `404`.
- The API must not log request bodies, resume content, job descriptions, email addresses, tokens, or provider payloads.
