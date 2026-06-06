# SupportDesk - Professionelles Ticket-System

Ein modernes Support-Ticket-System, entwickelt mit Spring Boot 3, JavaFX und PostgreSQL.

## Technologie-Stack

| Ebene        | Technologie |
|--------------|-------------|
| **Frontend** | Java 21, JavaFX, FXML, CSS |
| **Backend**  | Java 21, Spring Boot 3.2.3, Spring Security |
| **Datenbank**| PostgreSQL, Flyway, Spring Data JPA |
| **Auth**     | JWT (JSON Web Tokens) |
| **Build**    | Maven Multi-Module |
| **Docs**     | Swagger / OpenAPI |

## Voraussetzungen
- Java 21
- Maven
- PostgreSQL laufend auf `localhost:5432`

## Setup & Ausführung

1. **Datenbank erstellen**
   In PostgreSQL eine Datenbank namens `supportdesk` erstellen:
   ```sql
   CREATE DATABASE supportdesk;
   ```

2. **Backend konfigurieren**
   Prüfen Sie `backend/src/main/resources/application.yml` und passen Sie ggf. Passwort und User an.

3. **Anwendung bauen**
   Im Root-Verzeichnis:
   ```bash
   mvn clean install -DskipTests
   ```

4. **Backend starten**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   *Das Backend läuft auf http://localhost:8080*

5. **Frontend starten**
   In einem neuen Terminalfenster:
   ```bash
   cd frontend
   mvn javafx:run
   ```

## Standard-Logins
Die Datenbank wird beim ersten Start durch Flyway und DataSeeder automatisch migriert und befüllt.
- **Admin**: `admin` / `admin123`
- **Agent**: `agent` / `agent123`
- **Customer**: `customer` / `customer123`
(Oder Sie registrieren selbst einen neuen Customer).

## Swagger URL
http://localhost:8080/swagger-ui.html

## Architektur & Routing
Die App nutzt strenges, rollen-basiertes Routing über den JWT Token.
- **Admin**: Sieht Dashboard, alle Tickets, kann Benutzer aktivieren/deaktivieren und Rollen ändern.
- **Agent**: Sieht nur offene und zugewiesene Tickets, kann Status updaten.
- **Customer**: Kann nur eigene Tickets sehen und erstellen.

Auf allen Seiten steht ein Profil-Tab (`Profil`) zur Verfügung, in dem die eigenen Nutzerdaten inklusive Profilbild (als Base64/URL) angepasst werden können.

## Testausführung
```bash
cd backend
mvn test
```
