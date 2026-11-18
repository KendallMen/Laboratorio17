# Bitácora pericial de remediación

## Alcance y cadena de custodia

La revisión se realizó sobre el backend Spring Boot y el frontend Angular de InsecurePay.
Las pruebas dinámicas se ejecutaron únicamente contra `localhost:8080`, usando cuentas de
prueba y sin conservar tokens ni datos sensibles en el proyecto.

## Hallazgos

### HALLAZGO-01 — Inyección SQL en búsqueda

- **OWASP/CWE:** A03:2021 Injection / CWE-89.
- **Severidad:** Alta (CVSS 8.1).
- **Antes:** `GET /api/transacciones/buscar?termino=' OR 1=1 --` alteraba la consulta nativa.
- **Después:** `TransaccionRepository.buscarPorTerminoSeguro` usa JPQL con `@Param`; el mismo
  payload se trata como texto literal y la respuesta validada fue `[]`.

### HALLAZGO-02 — IDOR/BOLA en cuentas y transferencias

- **OWASP/CWE:** A01:2021 Broken Access Control / CWE-639.
- **Severidad:** Crítica (CVSS 9.1).
- **Antes:** un usuario podía cambiar el ID de cuenta o el `cuentaOrigenId`.
- **Después:** el servicio valida el `sub` y el rol del JWT. La consulta de saldo de una
  cuenta ajena y la transferencia desde ella respondieron `403 Forbidden`.

### HALLAZGO-03 — XSS almacenado en comprobantes

- **OWASP/CWE:** A03:2021 Injection / CWE-79.
- **Severidad:** Alta (CVSS 8.2).
- **Antes:** `bypassSecurityTrustHtml` ejecutaba HTML recibido en el comprobante.
- **Después:** el backend escapa `&`, `<`, `>`, comillas y apóstrofes antes de persistir;
  Angular muestra el concepto mediante interpolación `{{ }}`. Un payload `<img ...>` se
  devolvió como `&lt;img ...&gt;`.

### HALLAZGO-04 — Contraseñas en texto plano

- **OWASP/CWE:** A02:2021 Cryptographic Failures / CWE-256.
- **Severidad:** Crítica (CVSS 9.1).
- **Antes:** `data.sql` y `AuthService` comparaban contraseñas sin hash.
- **Después:** se agregó `BCryptPasswordEncoder` con factor 12, los datos iniciales
  contienen únicamente hashes `$2a$12$...` y el login usa `matches`.

### HALLAZGO-05 — Fuga de trazas y configuración insegura

- **OWASP/CWE:** A05:2021 Security Misconfiguration / CWE-209.
- **Severidad:** Media (CVSS 6.5).
- **Antes:** se exponían mensajes, trazas y la consola H2.
- **Después:** `ProblemDetail` estandariza errores sin detalles internos, las opciones
  `server.error.*` están en `never`, H2 está deshabilitado y `JWT_SECRET` se obtiene del
  entorno con una longitud mínima recomendada de 32 bytes.

## Verificación

```text
Backend: mvnw.cmd test
Tests run: 1, Failures: 0, Errors: 0
BUILD SUCCESS

GET /api/cuentas/3/saldo con token de otro usuario -> 403
POST /api/transacciones/transferir con cuenta ajena -> 403
Payload SQLi -> [] (status 200)
Payload XSS persistido -> &lt;img src=x onerror=alert(1)&gt;
```
