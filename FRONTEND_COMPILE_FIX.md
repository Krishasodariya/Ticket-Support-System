# Frontend compile fix

The frontend compile error came from calling new DTO setter methods while Maven was still resolving an older backend dependency.

Fixes applied:
- `TicketApiService` now accepts generic JSON payload objects for create/update requests.
- Attachment and solution updates are sent as `Map<String,Object>` from the frontend.
- `CreateTicketRequest` and `UpdateTicketRequest` now include explicit Java getters/setters for new fields, so they do not rely only on Lombok-generated methods.

Recommended build from the project root:

```bash
mvn clean install -DskipTests
```

Then start backend and frontend separately.
