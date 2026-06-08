# Private (self-hosted) Signal deployment runbook

This fork runs the Signal Android client against a self-hosted `Signal-Server` (`server-private`
branch) reached over WireGuard on an AWS host. It removes Signal's SMS/CDS/SVR/push dependencies and
adds a private registration + directory flow.

The application **code** changes for this are already in the repo (see "Code changes" at the bottom).
The items below are the **per-deployment configuration** you must still perform — they cannot be baked
into source because they depend on keys/certs you generate for your own server.

---

## 1. zkgroup / generic / backup params (CRITICAL — group & profile crypto)

The client verifies server-issued credentials (group auth, expiring profile credentials, backup
credentials) against three public params. They must be the public halves of the **secrets your server
holds**. Signal's production secrets are unobtainable, so the stock values will never verify.

On the AWS server host, generate one keypair per config and capture Public/Private:

```sh
java -jar server.jar zkparams        # prints Public: ... / Private: ...
# repeat / use the relevant command for callingZkConfig (generic) and backupsZkConfig
```

Server side (`service/config/sample.yml` + secrets bundle):
- `zkConfig.serverSecret`        <- Private (zkgroup)      ; `zkConfig.serverPublic` <- Public
- `callingZkConfig.serverSecret` <- Private (generic/calling)
- `backupsZkConfig.serverSecret` <- Private (backups)

Client side — set these as gradle properties so they bake into the APK (no code edit needed; defaults
are Signal-prod and will NOT work):

```properties
# ~/.gradle/gradle.properties or project gradle.properties, or pass with -P
PRIVATE_ZKGROUP_SERVER_PUBLIC_PARAMS=<zkConfig Public>
PRIVATE_GENERIC_SERVER_PUBLIC_PARAMS=<callingZkConfig Public>
PRIVATE_BACKUP_SERVER_PUBLIC_PARAMS=<backupsZkConfig Public>
```

These map to `ZKGROUP/GENERIC/BACKUP_SERVER_PUBLIC_PARAMS` in `app/build.gradle.kts`.
> Note: only the **prod** flavor is wired to these properties. The private build must use a
> `*PlayProd*` variant (the build env type is `Prod`). If you switch to staging, wire the staging
> block the same way.

---

## 2. TLS termination + trust (CRITICAL — nothing connects without this)

The client mandates TLS (`RESTRICTED_TLS`) for every connection; the server speaks plaintext HTTP
:8080. Put a TLS terminator in front and make the client trust the cert.

1. **Terminator:** nginx/Caddy/ALB on :443 -> `http://127.0.0.1:8080`, with **WebSocket upgrade**
   enabled for `/v1/websocket` (and `/v1/websocket/provisioning`). Forward the CDN/attachment paths
   too (see section 4).
2. **Cert SAN must match exactly what the user types** into the server-setup screen. If users connect
   to the WireGuard IP (e.g. `https://10.x.x.x`), the cert needs that **IP in a SAN**; if a hostname,
   that hostname.
3. **Trust:** the client now trusts (a) the bundled `whisper.store` CA **and** (b) the Android system
   CA store (code fix #2). So either:
   - use a **publicly-trusted cert** (e.g. Let's Encrypt on a real DNS name) — works out of the box; or
   - use a **private CA / self-signed** cert and add that CA to `app/src/main/res/raw/whisper.store`
     (BKS keystore, password `whisper`) at build time.

> The Rust `libsignal-net` path keeps its own embedded trust roots. It is only used for CDSI/SVR2
> (disabled here via code fix #6) and is not on the messaging path in this fork, so the OkHttp trust
> change above is sufficient for chat/registration/attachments. Re-verify if you re-enable those.

---

## 3. Durable storage (CRITICAL for a real deployment)

`run-local.sh` -> `LocalWhisperServerService` uses **DynamoDB-Local + Redis in testcontainers**, which
are **discarded on restart** (README "Storage is ephemeral"). Accounts and messages will be lost on
every reboot. For AWS, run the production `WhisperServerService` against durable DynamoDB + Redis (and
the production message store), not the local test entrypoint.

---

## 4. Attachments / CDN routing

The client sends all CDN/attachment traffic to the **base URL** (it rewrites signed upload URLs to the
configured host). Your reverse proxy must forward the CDN paths (`/attachments/...`, the GCS resumable
upload path, and the TUS `/upload` path) to the actual storage backend (S3/GCS/local). Otherwise
uploads/downloads 404.

---

## 5. WebSocket keepalive vs load-balancer idle timeout

Background keepalive cadence is 60s. AWS ALB default idle timeout is also 60s -> reconnect churn. Set
the LB/proxy idle timeout to **>=120s** for the websocket listener.

```sh
aws elbv2 modify-load-balancer-attributes \
  --attribute Key=idle_timeout.timeout_seconds,Value=180 ...
```

---

## 6. Group calls / Call Links (optional)

Group calling needs a separate **Signal-Calling-Service (SFU)**; the chat server does not host it.
If you deploy one reachable over WireGuard, set its URL via `customServer.sfuUrl` (code fix #7 wires
`Environment.Calling.defaultSfuUrl()` + the group-calling fallback to it). If you do **not** deploy an
SFU, leave it blank and avoid group calls (1:1 calls work via the chat server's TURN).

---

## 7. Network hardening

Restrict the server's `:8080`/`:8081` (admin) to the WireGuard interface via the AWS security group.
gRPC `:50051` binds loopback by default. Admin `:8081` must not be publicly reachable.

---

## Code changes already applied in this fork (for reference)

| # | Area | Files |
|---|------|-------|
| 1 | zkparams overridable via gradle props | `app/build.gradle.kts` |
| 2 | TLS trust falls back to system CA store | `lib/libsignal-service/.../BlacklistingTrustManager.java` |
| 4 | Don't store display name as e164; tolerate non-E164 account id | `LocalRegistrationMetadataUtil.kt`, `SignalE164Util.kt` |
| 5 | Force websocket delivery (null FCM token) | `RegistrationViewModel.kt` |
| 6 | Skip CDS/CDSI when custom server configured | `contacts/sync/ContactDiscovery.kt` |
| 7 | Configurable SFU URL | `CustomServerValues.kt`, `util/Environment.kt`, `keyvalue/InternalValues.kt` |
| 9 | Reject http://, make server URL re-editable | `registration/ui/server/ServerSetupFragment.kt` |

---

## ⚠️ Before production (NOT yet done — the live test runs on disposable test secrets)

The current VM deployment runs `run-local.sh`, which loads `service/src/test/resources/config/test.yml`
+ `test-secrets-bundle.yml`. **Those are upstream TEST fixtures and are publicly known** — so the live
server's zkgroup secret, sealed-sender (unidentifiedDelivery) key, TUS auth secret, and storage/SVR
shared secrets are NOT private. For a real deployment you MUST:

1. **Regenerate every secret** in a private secrets bundle (do not reuse `test-secrets-bundle.yml`):
   zkgroup/calling/backups `serverSecret` (via `ZkParamsCommand`), `unidentifiedDelivery` cert+key,
   `tus.userAuthenticationTokenSharedSecret`, storageService/SVR shared secrets, DynamoDB/Redis creds.
2. **Re-derive and re-pin** the clients' public values from the new secrets: Android `PRIVATE_*` gradle
   props, Desktop `SIGNAL_*` env (`serverPublicParams`, `genericServerPublicParams`,
   `backupServerPublicParams`, `serverTrustRoots`).
3. Run the production `WhisperServerService` against **durable** DynamoDB + Redis + object storage
   (run-local's testcontainers are ephemeral — data is wiped on restart).
4. Replace the self-signed `private-tls/` cert with one whose CA you control; keep `private-tls/`,
   `ca.key`, `server.key` OFF git (now gitignored).
5. Remove debug/test affordances (already reverted: Desktop provisioning-URL log; the emoji-validator
   relaxation in `scripts/generate-emoji-data.mjs` is test-build only).
