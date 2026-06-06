# Backend start fix

The backend now uses `spring.jpa.hibernate.ddl-auto=update` and disables Flyway for the local H2 demo database.
This prevents startup crashes caused by schema validation while the course project is still changing quickly.

Run backend:

```bash
cd backend
mvn spring-boot:run
```

If it still stops with exit code 1, the most common reason is that port 8080 is already used. On Windows run:

```bash
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

Then start backend again.

To see the real Spring error, run:

```bash
mvn spring-boot:run -e
```

and copy the lines around `APPLICATION FAILED TO START`.
