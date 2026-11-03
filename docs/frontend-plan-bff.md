# Plan für jsonplaceholder-ui: BFF-Auth, vollständige Auswahllisten, saubere API-Typen

Dieser Plan beschreibt die Frontend-Umsetzung zu drei Backend-Änderungen in `jsonplaceholder-crud`. Arbeite ihn in der angegebenen Reihenfolge ab. Jeder Schritt endet mit Prüfkriterien; ein Schritt gilt erst als fertig, wenn sie erfüllt sind und `npm test` grün ist.

**Voraussetzung:** Das Backend läuft mit dem neuen Stand lokal auf `http://localhost:8080` (im Backend-Repo `scripts/local-app.sh start`; Prüfung mit `scripts/smoke-test.sh`).

## 1. Was sich im Backend geändert hat

### 1.1 OpenAPI-Vertrag

- Alle Felder aller `…Response`-Schemas sind jetzt `required` (`PostResponse`, `UserResponse`, `AuthTokenResponse`, `AuthAccountResponse` usw.).
- PATCH-Bodies haben eigene Schemas: `PostRequestPatch`, `CommentRequestPatch`, `AlbumRequestPatch`, `PhotoRequestPatch`, `TodoRequestPatch`, `UserRequestPatch` mit verschachtelten `AddressPatch`, `GeoPatch`, `CompanyPatch`. Alle Felder optional, ohne `null`. Das bisherige Schema `JsonNode` entfällt.
- Antworten sind als `application/json` dokumentiert statt `*/*`. In `paths[…]` erzeugten Typen ändert sich dadurch der Schlüssel unter `content`.

### 1.2 Keyset-Paging

- Jeder Listen-Endpunkt (`GET /posts`, `/comments`, `/albums`, `/photos`, `/todos`, `/users` sowie `/posts/{id}/comments`, `/albums/{id}/photos`, `/users/{id}/posts|albums|todos`) akzeptiert neu `_after=<id>`: Es kommen nur Einträge mit größerer ID, aufsteigend sortiert, höchstens `_limit` (Standard 100, Maximum 500).
- `_after` und `_start` zusammen ergeben `400` (`"_start and _after cannot be combined"`).
- Ende der Liste: Die Antwort hat weniger als `_limit` Einträge.

### 1.3 Auth

| Endpunkt | Body | Antwort |
|---|---|---|
| `POST /auth/login` | `{email, password}` | `200` `AuthTokenResponse` |
| `POST /auth/refresh` | `{refreshToken}` | `200` `AuthTokenResponse` (neues Paar) oder `401` |
| `POST /auth/logout` | `{refreshToken}` | `204`, idempotent |
| `POST /auth/register` | unverändert | unverändert |

`AuthTokenResponse` = `{accessToken, tokenType: "Bearer", expiresIn: 900, refreshToken, refreshExpiresIn: 604800}` (Sekunden).

- **Refresh-Tokens sind Einmal-Tokens.** Jeder Aufruf von `/auth/refresh` liefert ein neues Refresh-Token und entwertet das alte. Wird ein bereits benutztes Refresh-Token erneut geschickt, widerruft das Backend die gesamte Sitzung (alle Folgetokens dieses Logins) und antwortet `401`. Das gilt auch, wenn zwei Requests dasselbe Token gleichzeitig verwenden.
- **Rate-Limit:** alle Auth-Endpunkte 10 Anfragen pro Minute und Client-Adresse, Login zusätzlich 10 pro Minute und E-Mail. Überschreitung: `429` mit `Retry-After: 60`.
- **Client-Adresse hinter dem BFF:** Das Backend übernimmt die Adresse aus `X-Forwarded-For`, wenn der Aufruf von einem vertrauenswürdigen Proxy kommt (lokal: Loopback; sonst Backend-Variable `TRUSTED_PROXIES`). Ohne den Header teilen sich alle Nutzer des BFF ein Rate-Limit.
- Fehler sind `application/problem+json` mit `detail`.

## 2. Schritt A: Typen aktualisieren und Workarounds entfernen

1. `npm run api:update` gegen das laufende Backend.
2. `ResponseEntity<T>` und dessen Verwendungen in `src/features/*/store/*-types.ts` entfernen; die generierten Response-Typen direkt verwenden.
3. `MergePatch<T>` in `src/lib/api/shared-types.ts` durch die generierten Typen ersetzen, z. B. `components["schemas"]["PostRequestPatch"]`.
4. Stellen anpassen, die Antworttypen über `paths[…]["responses"][…]["content"]["*/*"]` beziehen (jetzt `"application/json"`).
5. Fake-API (`src/test/fake-api/`) an die neue `openapi.json` anpassen.

**Prüfkriterien:** `npm run api:check` ist sauber, `tsc` ohne Fehler, keine `Required<…>`-Krücken mehr für API-Typen, `npm test` grün.

## 3. Schritt B: Auswahllisten vollständig laden

`use-post-options`, `use-album-options` und `use-user-options` laden heute ohne Parameter, also höchstens 100 Einträge. JSONPlaceholder liefert genau 100 Posts und 100 Alben; jeder neu angelegte Eintrag fehlt in der Auswahl.

1. Einen gemeinsamen Loader für Optionen bauen, der per Keyset-Paging alles lädt: erste Seite mit `_limit=500`, danach `_after=<letzte id>`, bis eine Seite weniger als 500 Einträge hat. Kein `_start` verwenden (teuer und auf 10000 begrenzt).
2. Die drei Hooks darauf umstellen; Lade- und Fehlerzustand wie bisher liefern.
3. Fake-API um `_after` erweitern (gleiche Semantik wie oben, inkl. `400` bei Kombination mit `_start`).

**Prüfkriterien:** Test mit 501 Posts in der Fake-API zeigt alle 501 Optionen; Test, dass `_start` nicht verwendet wird.

## 4. Schritt C: Next.js als BFF

Ziel: Der Browser sieht keine Tokens mehr. Next hält Access- und Refresh-Token in httpOnly-Cookies und ruft das Backend serverseitig mit `Authorization: Bearer` auf. Das Backend bleibt unverändert und braucht kein CORS mehr für den Browser.

### 4.1 Konfiguration

- Backend-URL nur noch serverseitig: `JSONPLACEHOLDER_API_URL` (nicht `NEXT_PUBLIC_…`). Der Browser ruft nur noch Next auf.

### 4.2 Route Handler für Auth

Unter `src/app/api/auth/`:

- `login/route.ts`: ruft `POST /auth/login`, setzt Cookies, antwortet dem Browser nur mit `{expiresAt}` (keine Tokens).
- `register/route.ts`: reicht an `POST /auth/register` durch.
- `logout/route.ts`: ruft `POST /auth/logout` mit dem Refresh-Token aus dem Cookie, löscht beide Cookies, antwortet `204`, auch wenn das Backend nicht erreichbar ist.
- `session/route.ts` (`GET`): `{authenticated, expiresAt}` für die UI, ohne Tokens.

Cookies:

| Cookie | Inhalt | `maxAge` |
|---|---|---|
| `access_token` | `accessToken` | `expiresIn` |
| `refresh_token` | `refreshToken` | `refreshExpiresIn` |

Beide `httpOnly`, `sameSite: "lax"`, `path: "/"`, `secure` außerhalb von `localhost`. In Produktion den Präfix `__Host-` verwenden.

### 4.3 API-Proxy

`src/app/api/backend/[...path]/route.ts` für `GET`, `POST`, `PUT`, `PATCH`, `DELETE`:

- Pfad und Query unverändert an `JSONPLACEHOLDER_API_URL` weiterreichen, `Content-Type` und Body durchreichen (inklusive `application/merge-patch+json`), Status, `Content-Type` und Body der Antwort unverändert zurückgeben (inklusive `problem+json`).
- `Authorization: Bearer <access_token-Cookie>` setzen. Keine Browser-Cookies und keinen `Authorization`-Header aus dem Browser an das Backend weitergeben.
- `X-Forwarded-For` mit der Adresse des Browsers setzen (aus dem eingehenden `x-forwarded-for`/`x-real-ip` des Hostings; lokal ohne Header weglassen).
- Fehlt das Access-Token oder ist es abgelaufen und ein Refresh-Token ist da: zuerst erneuern (4.4), dann den Request ausführen. Antwortet das Backend trotzdem `401`, Cookies löschen und `401` zurückgeben.

### 4.4 Refresh: nur ein Aufruf gleichzeitig

**Wichtigste Falle:** Schickt der Browser mehrere Requests gleichzeitig mit abgelaufenem Access-Token und der BFF erneuert für jeden einzeln, sieht das Backend dasselbe Refresh-Token mehrfach, wertet das als Diebstahl und beendet die Sitzung.

- Refresh als Single-Flight implementieren: eine Map `refreshToken → Promise<Tokens>` im Serverprozess; gleichzeitige Requests warten auf denselben Aufruf und verwenden dessen Ergebnis. Eintrag nach Abschluss entfernen.
- Proaktiv erneuern, wenn das Access-Token in weniger als 60 Sekunden abläuft (Ablaufzeit beim Setzen zusätzlich als eigenes Cookie oder im Cookie-Wert mitführen).
- Hinweis: Single-Flight wirkt pro Next-Instanz. Für mehrere Instanzen bräuchte es Sticky Sessions oder eine Änderung im Backend; das ist bewusst noch nicht gelöst.

### 4.5 CSRF

Die Route Handler sind cookie-authentifiziert und brauchen deshalb CSRF-Schutz:

- Für `POST`, `PUT`, `PATCH`, `DELETE` den `Origin`-Header prüfen; nur der eigene Origin ist erlaubt, sonst `403`.
- Nur JSON-Bodies annehmen (`application/json`, `application/merge-patch+json`), sonst `415`. Fremde Seiten können solche Requests nicht ohne CORS-Preflight senden.
- `sameSite: "lax"` bleibt zusätzlich gesetzt.

### 4.6 Client umstellen

- `baseApi`: `baseUrl` auf `/api/backend`, `prepareHeaders` ohne Token, `credentials: "same-origin"`.
- Token aus Redux-State und `redux-persist`/`localStorage` entfernen. Der Auth-Slice hält nur noch `{authenticated, expiresAt}` aus `/api/auth/session` bzw. der Login-Antwort.
- Login, Register und Logout rufen die Route Handler aus 4.2 auf.
- `session-expiry-watcher` anpassen: Ablauf bedeutet jetzt nur das Ende des Refresh-Tokens (7 Tage) oder eine `401` vom Proxy; dann auf `/login` umleiten.

### 4.7 Routen schützen

- `src/proxy.ts` (in Next 16 Nachfolger von `middleware.ts`): Für die Routen unter `(protected)` ohne `refresh_token`-Cookie auf `/login` umleiten. Nur auf das Vorhandensein prüfen, die Gültigkeit prüft das Backend.
- Server Components dürfen das Backend direkt aufrufen (Token aus `cookies()`), müssen dann aber dieselbe Refresh-Logik aus 4.4 verwenden. Im Zweifel bei clientseitigem Laden über den Proxy bleiben.

**Prüfkriterien für Schritt C:**

- Nach dem Login enthalten weder `localStorage` noch JavaScript-zugängliche Cookies ein Token (`document.cookie` zeigt keine Tokens).
- Alle CRUD-Seiten funktionieren über `/api/backend`.
- E2E: Access-Token künstlich ablaufen lassen (Cookie löschen), fünf parallele Requests auslösen → genau ein `POST /auth/refresh` am Backend, alle Requests erfolgreich, Sitzung besteht weiter.
- E2E: Logout → `refresh_token` gelöscht, geschützte Route leitet auf `/login`.
- `POST /api/backend/posts` mit fremdem `Origin` → `403`.
- Unit-Tests für Proxy (Header-Weitergabe, kein Cookie an Backend, `problem+json` durchgereicht) und Single-Flight.

## 5. Nicht Teil dieses Plans

- Keine Änderungen am Backend nötig. Falls beim Umsetzen ein Backend-Problem auffällt, nicht im Frontend umgehen, sondern melden.
- Suche oder Autovervollständigung in Auswahllisten; bei mehreren tausend Einträgen sinnvoll, braucht aber Backend-Unterstützung.
