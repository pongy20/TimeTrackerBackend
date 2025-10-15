# TimeTracker Backend

Spring Boot 3 Backend für eine einfache Time-Tracking App mit JWT-Auth, JPA/Hibernate und H2-SQL-Datenbank.

## Schnellstart

Voraussetzungen: Java 21, Maven

```bash
# Build und Tests
mvn clean test

# App starten
mvn spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui
- OpenAPI JSON: http://localhost:8080/api-docs
- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:timetracker`
  - User: `sa`, Password: leer

## Konfiguration

Wichtige Einstellungen in `src/main/resources/application.yml`:
- Datenbank via ENV: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- Server-Port via ENV: `SERVER_PORT`
- JWT via ENV: `JWT_SECRET`, `JWT_EXPIRATION_SECONDS`
- JPA DDL-Auto: `update`

Hinweis: Für Tests/H2 wird `src/test/resources/application-test.yml` verwendet.

## Authentifizierung

- Registrierung: `POST /api/auth/register`
- Login: `POST /api/auth/login`
- Auth-Header für geschützte Endpunkte: `Authorization: Bearer <JWT>`

### Beispiele

```bash
# Registrierung
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"secret"}'

# Login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"secret"}'
```

Antwort:
```json
{"token":"<JWT>"}
```

## Time Entries API

Alle Endpunkte erfordern `Authorization: Bearer <JWT>`

- `GET /api/time-entries` – Liste der eigenen Einträge
- `GET /api/time-entries/{id}` – Einzelner Eintrag
- `POST /api/time-entries` – Eintrag erstellen
- `PUT /api/time-entries/{id}` – Eintrag ändern
- `DELETE /api/time-entries/{id}` – Eintrag löschen

### Request-/Response-Modelle

Create/Update Request:
```json
{
  "subject": "Feature X",
  "description": "Implementierung",
  "dateWorked": "2025-10-15",
  "minutesWorked": 90
}
```

Response:
```json
{
  "id": 1,
  "subject": "Feature X",
  "description": "Implementierung",
  "dateWorked": "2025-10-15",
  "minutesWorked": 90,
  "createdAt": "2025-10-15T14:30:00Z",
  "updatedAt": "2025-10-15T14:45:00Z"
}
```

### Beispiele

```bash
TOKEN="$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"bob","password":"pw"}' | jq -r .token)"

# Anlegen
curl -s -X POST http://localhost:8080/api/time-entries \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"subject":"Coding","description":"API","dateWorked":"2025-10-15","minutesWorked":60}'

# Liste
curl -s http://localhost:8080/api/time-entries \
  -H "Authorization: Bearer $TOKEN"
```

## CSV-Import (bestehende Daten)

Es gibt einen sicheren Endpoint, um CSVs aus einem festen Ordner zu importieren.

- Import-Ordner im Container: `IMPORT_DIR` (Default: `/app/imports`)
- In docker-compose ist `./imports` read-only nach `/app/imports` gemountet.
- Lege deine Datei z. B. als `./imports/existing_data.csv` ab.

Endpoint:
- `POST /api/imports/time-entries?filename=existing_data.csv&username=<optional>&dryRun=<true|false>`
- Authentifizierung: Bearer JWT (wie bei den anderen Endpunkten)
- Parameter:
  - `filename` (Pflicht): Dateiname innerhalb des Import-Ordners (keine Pfade!)
  - `username` (optional): Fallback-User, wenn die CSV keine `username`-Spalte enthält
  - `dryRun` (optional, Default false): Testlauf ohne Inserts

Spalten-Mapping (tolerant bzgl. Namen):
- username: `username`, `user`, `email`
- subject: `subject`, `title`, `betreff`, `task`
- description: `description`, `desc`, `beschreibung`, `notes`, `note`
- dateWorked: `dateWorked`, `date`, `workDate`, `datum`, `day` (Formate: `yyyy-MM-dd`, `dd.MM.yyyy`, `MM/dd/yyyy`)
- minutesWorked: `minutesWorked`, `minutes`, `duration`, `dauer`, `mins`, `zeitMin`
- createdAt: `createdAt`, `created`, `erstelltAm` (ISO-Instant/-Offset/-LocalDateTime; sonst Tagesbeginn von `dateWorked`)
- lastUpdated/updatedAt: `updatedAt`, `lastUpdated`, `modified`, `geaendertAm` (falls fehlt: wird = `createdAt` gesetzt)

Deduplizierung: Einträge mit gleicher Kombination (user, subject, dateWorked, minutesWorked) werden übersprungen.

Beispiele (lokal, via cURL; ersetze Token):
```bash
# Dry-Run ohne Inserts
curl -X POST "http://localhost:8080/api/imports/time-entries?filename=existing_data.csv&dryRun=true" \
  -H "Authorization: Bearer <JWT>"

# Echter Import, Fallback-User angeben
curl -X POST "http://localhost:8080/api/imports/time-entries?filename=existing_data.csv&username=alice" \
  -H "Authorization: Bearer <JWT>"
```

Compose-Hinweis:
- `./imports` existiert lokal (ansonsten anlegen) und enthält deine CSVs.
- In Prod per `docker-compose.prod.yml` analog gemountet.

CommandLine-Import (optional beim Start):
- `IMPORT_CSV` auf die Datei setzen, optional `IMPORT_USERNAME` und `IMPORT_DRY_RUN=true`.

## Deployment auf Linux vServer (Docker)

Voraussetzungen auf dem Server:
- Docker Engine + Docker Compose Plugin

Schritte:
1) Repository auf den Server bringen (git clone oder rsync/scp).
2) `.env` erzeugen (siehe `.env.example`) und sensible Werte setzen:
   - `POSTGRES_PASSWORD` (starkes Passwort)
   - `JWT_SECRET` (mind. 32 zufällige Zeichen)
3) Container bauen und starten:

```bash
# im Projektverzeichnis
cp .env.example .env
# .env öffnen und Werte anpassen

docker compose -f docker-compose.prod.yml --env-file .env build
docker compose -f docker-compose.prod.yml --env-file .env up -d
```

- App: läuft unter Port `SERVER_PORT` (Default 8080)
- Swagger: `http://<server-ip>:8080/swagger-ui`
- Datenpersistenz: Postgres-Daten in Volume `pgdata`

Logs & Verwaltung:
```bash
docker compose -f docker-compose.prod.yml --env-file .env ps
docker compose -f docker-compose.prod.yml --env-file .env logs -f app
```

Stop/Update:
```bash
docker compose -f docker-compose.prod.yml --env-file .env pull   # neue Images ziehen (falls Tags statt Build)
docker compose -f docker-compose.prod.yml --env-file .env build  # neu bauen (bei Code-Änderungen)
docker compose -f docker-compose.prod.yml --env-file .env up -d  # ohne Downtime aktualisieren

docker compose -f docker-compose.prod.yml --env-file .env down   # Stoppen (Volume bleibt erhalten)
```

### Externe Datenbank nutzen (optional)
Wenn du eine externe/verwaltete Postgres-DB verwenden möchtest:
- Entferne oder kommentiere den `db`-Service in `docker-compose.prod.yml`.
- Setze in `.env` direkt `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` (die `app`-Service-Umgebung übernimmt diese Werte).

## Sicherheit

- JWT HS256; Secret/Gültigkeit über ENV steuerbar
- Keine Secrets im Repo: `.env` ist in `.gitignore`
- Bei Internetzugriff empfiehlt sich ein Reverse Proxy (z. B. Nginx/Traefik) mit HTTPS

## Troubleshooting

- App startet zu früh (DB noch nicht bereit): Compose hat Healthcheck für Postgres und `depends_on` mit `service_healthy`.
- Verbindung schlägt fehl: Prüfe ENV-Variablen (`DB_*`) und ob der Port offen ist (Firewall/Security Groups).
- Performance: Passe `JAVA_OPTS` in `Dockerfile` oder via ENV an.

## Lizenz

MIT
