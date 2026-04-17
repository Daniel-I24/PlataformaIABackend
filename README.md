# AI Proxy Backend

Backend de la plataforma de consumo de IA con Patrón Proxy para rate limiting y control de cuotas.

## Tecnologías
- Java 21
- Spring Boot 3.2.5
- Maven

## Arquitectura - Patrón Proxy

La cadena de proxies funciona así:

```
Request → RateLimitProxyService → QuotaProxyService → MockAIGenerationService → Response
```

- **RateLimitProxyService**: Verifica que el usuario no supere N requests/minuto según su plan
- **QuotaProxyService**: Verifica que el usuario no haya agotado su cuota mensual de tokens
- **MockAIGenerationService**: Servicio real que simula la generación de texto con IA

## Planes disponibles

| Plan       | Requests/min | Tokens/mes |
|------------|-------------|------------|
| FREE       | 10          | 50,000     |
| PRO        | 60          | 500,000    |
| ENTERPRISE | Ilimitado   | Ilimitado  |

## Endpoints

| Método | Endpoint              | Descripción                          |
|--------|-----------------------|--------------------------------------|
| POST   | /api/ai/generate      | Genera texto (pasa por proxy chain)  |
| GET    | /api/quota/status     | Estado de cuota del usuario          |
| GET    | /api/quota/history    | Historial de uso (últimos 7 días)    |
| POST   | /api/quota/upgrade    | Upgrade de plan                      |

## Usuarios de prueba

- `user-free` → Plan FREE
- `user-pro` → Plan PRO
- `user-enterprise` → Plan ENTERPRISE

## Ejecutar localmente

```bash
./mvnw spring-boot:run
```

El servidor inicia en `http://localhost:8080`

## Variables de entorno

| Variable              | Descripción                    | Default                    |
|-----------------------|--------------------------------|----------------------------|
| PORT                  | Puerto del servidor            | 8080                       |
| CORS_ALLOWED_ORIGINS  | Orígenes permitidos para CORS  | http://localhost:5173       |

## Despliegue en Railway

1. Conectar el repositorio en Railway
2. Railway detecta automáticamente el proyecto Maven
3. Configurar la variable `CORS_ALLOWED_ORIGINS` con la URL de Vercel
4. Railway usa el `Procfile` o el jar generado automáticamente
