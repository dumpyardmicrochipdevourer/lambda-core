# lambda-core

File swap by code and personal storage for lambda. Accounts live in
`lambda-auth`; this service only checks its access tokens against the JWKS.

Spring Boot 4, Java 21, PostgreSQL, Flyway. Files are kept on disk under
`LAMBDA_STORAGE_ROOT`, metadata in the database.

## API

### Swap (anonymous)

| method | path | what |
|---|---|---|
| POST | `/api/share` | `{ttlSeconds}` — 900, 3600 or 86400 → `{code, expiresAt}` |
| PUT | `/api/share/{code}/files/{name}` | raw body → 201 `{id, name, size}` |
| GET | `/api/share/{code}` | `{code, expiresAt, files: [{id, name, size}]}` |
| GET | `/api/share/{code}/files/{id}` | download, `Range` supported |
| GET | `/api/share/{code}/archive` | all files as an uncompressed zip |

A text note is an ordinary file named `message.txt`; the page decides whether to
show it as text. Expired shares disappear within a minute. Unknown or expired
code → 404, taken name → 409, file over `LAMBDA_MAX_FILE_BYTES` → 413.

### Personal storage (signed in)

| method | path | what |
|---|---|---|
| GET | `/api/files` | `{quota, used, reserved, files: [{id, name, size, received, status, createdAt}]}` |
| POST | `/api/files` | `{name, size, contentType?}` → 201, reserves `size` bytes of the quota |
| PUT | `/api/files/{id}/content` | raw body appended at `Upload-Offset` (default 0) |
| GET | `/api/files/{id}/content` | download, `Range` supported |
| DELETE | `/api/files/{id}` | 204, gives the space back |

Uploads resume: whatever reached the disk stays, `received` tells where to
continue, a wrong `Upload-Offset` gets 409 with the right one in the same header.
The file turns `COMPLETE` once `received == size`. A taken name gets a suffix
(`report (1).pdf`). Not enough quota → 507. Uploads left unfinished for 7 days are
removed. Downloads also accept the token as `?access_token=` so a plain link works.

### Feedback

| method | path | who | what |
|---|---|---|---|
| POST | `/api/feedback` | anyone | `{message, contact?}`, up to 8192 chars, 5 per hour per IP |
| GET | `/api/feedback` | admin | latest 200 |
| POST | `/api/feedback/{id}/read` | admin | mark as read |

`GET /actuator/health` is public.

## Configuration

| variable | default | |
|---|---|---|
| `LAMBDA_DB_URL` | `jdbc:postgresql://localhost:5432/lambda` | |
| `LAMBDA_DB_USER` / `LAMBDA_DB_PASSWORD` | `lambda` | |
| `LAMBDA_PORT` | `8080` | |
| `LAMBDA_STORAGE_ROOT` | `./data` (`/data` in the image) | |
| `LAMBDA_MAX_FILE_BYTES` | 2 GiB | per file in the swap |
| `LAMBDA_QUOTA_DEFAULT_BYTES` | 2.5 GiB | personal quota, the same for everyone |
| `LAMBDA_AUTH_JWKS_URI` | `http://localhost:8081/.well-known/jwks.json` | |
| `LAMBDA_AUTH_ISSUER` | `http://localhost:8081` | must equal lambda-auth's issuer |

## Run

```sh
./mvnw test                       # needs Docker for Testcontainers
docker build -t lambda-core .
```

The whole stack is deployed by `lambda-deploy`.
