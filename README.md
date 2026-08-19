# SupportDesk – Ticket Support System

Ein modernes Ticket-Support-System mit JavaFX-Frontend und Spring-Boot-Backend. Das System bietet rollenbasierte Benutzerverwaltung, Ticketverwaltung, Benachrichtigungen und eine REST-API mit JWT-Authentifizierung.

## Technologie-Stack

| Ebene                 | Technologie                                 |
| --------------------- | ------------------------------------------- |
| **Frontend**          | Java 21, JavaFX 21.0.6, FXML, CSS           |
| **Backend**           | Java 21, Spring Boot 3.2.3, Spring Security |
| **Datenbank**         | H2 In-Memory, Spring Data JPA, Hibernate    |
| **Authentifizierung** | JWT (JSON Web Tokens)                       |
| **Kommunikation**     | REST API, WebSocket / STOMP                 |
| **Build**             | Maven Multi-Module                          |
| **API-Dokumentation** | Swagger / OpenAPI                           |

> Flyway ist als Dependency vorhanden und SQL-Migrationsdateien befinden sich unter `backend/src/main/resources/db/migration`. In der aktuellen Konfiguration ist Flyway jedoch deaktiviert.

## Voraussetzungen

* Java JDK 21
* Maven
* Windows für den automatischen Start über die `.bat`-Datei

Eine separate PostgreSQL-Installation ist nicht erforderlich.

## Anwendung starten

### Automatischer Start unter Windows

Die einfachste Möglichkeit ist:

`start-ticket-system.bat`

Die Datei:

1. baut das Backend,
2. startet das Spring-Boot-Backend,
3. wartet kurz auf den Backend-Start,
4. startet anschließend das JavaFX-Frontend.

Zusätzlich steht folgende Datei zur Verfügung:

`start-ticket-system.bat`

Diese führt vor dem Start zusätzlich ein `git pull` aus.

### Manueller Start

Falls der automatische Start nicht funktioniert, kann das System manuell gestartet werden.

#### 1. Projekt bauen

Im Root-Verzeichnis:

```bash
mvn clean install -DskipTests
```

#### 2. Backend starten

```bash
cd backend
mvn spring-boot:run
```

Das Backend läuft standardmäßig auf:

`http://localhost:8080`

#### 3. Frontend starten

In einem neuen Terminal im Projektverzeichnis:

```bash
cd frontend
mvn javafx:run
```

## Datenbank

Das Projekt verwendet aktuell eine **H2 In-Memory-Datenbank**.

Die Konfiguration befindet sich in:

`backend/src/main/resources/application.yml`

Aktuelle JDBC-URL:

```text
jdbc:h2:mem:supportdesk;DB_CLOSE_DELAY=-1
```

Das Datenbankschema wird über Hibernate automatisch verwaltet:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

Da es sich um eine In-Memory-Datenbank handelt, werden die gespeicherten Daten beim Beenden des Backends verworfen und beim nächsten Start neu erstellt.

## Standard-Logins

Beim Start erstellt der `DataSeeder` automatisch Standardbenutzer und weitere Beispieldaten.

| Rolle        | Benutzername | Passwort      |
| ------------ | ------------ | ------------- |
| **Admin**    | `admin`      | `admin123`    |
| **Agent**    | `agent`      | `agent123`    |
| **Customer** | `customer`   | `customer123` |

Neue Customer-Benutzer können außerdem über die Registrierung erstellt werden.

## Rollen und Berechtigungen

Das System verwendet JWT-basierte Authentifizierung und Spring Security.

### Admin

Administratoren können unter anderem:

* alle Tickets verwalten,
* Benutzer anzeigen,
* Benutzer aktivieren oder deaktivieren,
* Benutzerrollen ändern,
* Agent-Spezialisierungen verwalten,
* Dashboard-Statistiken anzeigen,
* Kategorien und Workflow-Optionen verwalten,
* Audit-Logs einsehen.

### Agent

Agenten können unter anderem:

* Tickets anzeigen,
* zugewiesene Tickets anzeigen,
* Tickets übernehmen und zuweisen,
* Ticketstatus und Ticketinformationen bearbeiten,
* Kommentare hinzufügen.

### Customer

Customer können:

* eigene Tickets erstellen,
* eigene Tickets anzeigen,
* Kommentare und Feedback hinzufügen,
* abgeschlossene Tickets wieder öffnen,
* das eigene Profil bearbeiten.

## Profil

Angemeldete Benutzer können ihr eigenes Profil bearbeiten.

Unter anderem können folgende Informationen geändert werden:

* E-Mail-Adresse
* Geburtsdatum
* Profilbild
* Passwort

Für Profilbilder kann eine URL oder eine lokale Bilddatei ausgewählt werden.

## Weitere Funktionen

Das Projekt enthält außerdem Funktionen wie:

* Ticket-Suche und Filter
* Kategorien und Prioritäten
* Ticket-Zuweisung
* SLA- und Eskalationslogik
* kritische Tickets
* Kommentare
* Benachrichtigungen
* WebSocket-basierte Echtzeit-Events
* Audit-Logs
* System-Aktivitätsprotokoll
* Knowledge Base
* ähnliche Tickets und Duplikat-Erkennung
* CSV- und PDF-Export
* Dashboard-Statistiken

## Swagger / OpenAPI

Die REST-API kann über Swagger eingesehen und getestet werden:

`http://localhost:8080/swagger-ui.html`

## Tests

Backend-Tests können mit folgendem Befehl ausgeführt werden:

```bash
cd backend
mvn test
```

## Projektstruktur

```text
Ticket-Support-System/
├── backend/                      # Spring-Boot-Backend
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── pom.xml
├── frontend/                     # JavaFX-Frontend
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── pom.xml
├── pom.xml                       # Maven Multi-Module Build
├── start-ticket-system.bat
