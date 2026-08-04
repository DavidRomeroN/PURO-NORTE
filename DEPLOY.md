# Despliegue en la nube (GCP)

## Antes de subir

1. **Rota** las API keys de FactuSmart y apiperu si alguna vez estuvieron en el código o en el chat.
2. Genera un `JWT_SECRET` largo (≥ 32 caracteres).
3. Elige una `ADMIN_PASSWORD` fuerte (≥ 12 caracteres) solo para el **primer** arranque.
4. Crea la base MySQL 8 (Cloud SQL) con charset `utf8mb4`.

## Variables obligatorias en producción

| Variable | Uso |
|----------|-----|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | JDBC completo (con SSL o socket Cloud SQL) |
| `DB_USER` / `DB_PASSWORD` | Usuario de la app |
| `JWT_SECRET` | Firma de sesiones |
| `CORS_ORIGINS` | Origen HTTPS del frontend (sin `*`) |
| `FACTUSMART_API_KEY` | Emisión real |
| `FACTUSMART_RUC` | RUC emisor |
| `ADMIN_PASSWORD` | Solo si `SEED_ENABLED=true` la primera vez |
| `VITE_API_URL` | URL pública del API al **compilar** el frontend |

Opcional: `APIPERU_TOKEN` (si no está, la caja emite igual sin verificar el nombre).

## Probar en local como en la nube

```bash
cp .env.example .env
# edita .env
docker compose up --build
```

- Frontend: http://localhost:8088  
- Health: http://localhost:8080/actuator/health  

## Cloud Run + Cloud SQL (resumen)

1. Cloud SQL MySQL 8, usuario y base `puro_norte`.
2. Secret Manager: `JWT_SECRET`, `DB_PASSWORD`, `FACTUSMART_API_KEY`, `APIPERU_TOKEN`, etc.
3. Build y push de la imagen del backend:

```bash
gcloud builds submit --tag REGION-docker.pkg.dev/PROYECTO/REPO/anticucheria-api ./backend
```

4. Deploy Cloud Run con:
   - `SPRING_PROFILES_ACTIVE=prod`
   - Cloud SQL connection (socket) o VPC + IP privada
   - Secrets montados como variables de entorno
   - Primer deploy: `SEED_ENABLED=true` + `ADMIN_PASSWORD=...`
   - Segundo deploy: `SEED_ENABLED=false` (quitar la contraseña del seed)

5. Frontend:
   - Build con `VITE_API_URL=https://TU-API.run.app/api`
   - Hosting: Cloud Storage + CDN, Firebase Hosting, o Cloud Run con la imagen de `frontend/`
   - `CORS_ORIGINS` debe ser exactamente la URL HTTPS del frontend

## Migraciones

Flyway aplica `backend/src/main/resources/db/migration/V1__schema_inicial.sql` en bases vacías.

Si la base **ya** tiene tablas (local antiguo), Flyway hace baseline en la versión 1 y no recrea nada. Los cambios futuros van como `V2__....sql`.

Los archivos `migracion-*.sql` de la raíz son el histórico manual; en instalaciones nuevas no hacen falta.

## Checklist post-deploy

- [ ] `GET /actuator/health` → `UP`
- [ ] Login con el admin del seed
- [ ] Cambiar contraseña del admin desde el panel (o crear otro admin y desactivar el seed)
- [ ] Emitir una boleta de prueba (sandbox FactuSmart si aún no es producción SUNAT)
- [ ] `SEED_ENABLED=false` en el servicio
- [ ] HTTPS en frontend y API
