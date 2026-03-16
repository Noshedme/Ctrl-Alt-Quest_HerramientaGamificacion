Una app desktop gamificada que convierte el uso de tu PC en una aventura RPG. Registra actividades, gana XP, completa misiones y derrota jefes.

•	Java 17 (compilación configurada con `maven-compiler-plugin` / `release=17`).
•	Maven instalado (usa `javafx-maven-plugin` para ejecutar
•	empaquetado/ejecución de JavaFX).
•	Dependencias de runtime (p. ej. acceso a la BD PostgreSQL si se usa en
•	desarrollo).
Estructura general (resumen por carpeta relevante)
•	`frontend/` — módulo principal JavaFX (maven). Contiene `pom.xml`, scripts src/`.
•	`frontend/src/main/java/com/ctrlaltquest/dao/` — capa de acceso a datos
•	(DAOs para usuarios, misiones, pagos, inventario, etc.).
•	`frontend/src/main/java/com/ctrlaltquest/db/` — scripts y utilidades de conexión a BD (`CtrlAltQuestDB.sql`, `DatabaseConnection.java`).
•	`frontend/src/main/java/com/ctrlaltquest/models/` — POJOs que representan
•	entidades del dominio (User, Character, Mission, Item, Achievement...).
•	`frontend/src/main/java/com/ctrlaltquest/services/` — lógica de negocio y orquestación (pagos, misiones, recompensas, email, sesiones, etc.).
•	`frontend/src/main/java/com/ctrlaltquest/ui/` — código de la UI: `app`
•	(launcher), `controllers` (controladores JavaFX), `fxml` (vistas) y assets asociados.
•	`frontend/src/main/resources/assets/` — fuentes, imágenes (sprites), sonidos y vídeos.
•	`frontend/src/main/resources/fxml/` — vistas FXML y sub-vistas reutilizables (`views/`).
•	`frontend/src/main/resources/styles/` — hojas CSS para JavaFX.

Uso y comandos básicos
- Compilar: `mvn -f frontend/ clean package`.
- Ejecutar (modo desarrollo con plugin JavaFX):
- `mvn -f frontend/ javafx:run`
- Empaquetar JAR/ejecutable: dependerá de configuración adicional; `mvn
package` crea el artefacto en `frontend/target/`.
- Tests: `mvn -f frontend/ test` (JUnit 5 configurado).
- Script auxiliar: `frontend/verificar-stripe.bat` (verificación/uso local de
integración Stripe — revisar contenido antes de ejecutar).

Recursos clave
- Script de BD: `frontend/src/main/java/com/ctrlaltquest/db/CtrlAltQuestDB.sql`
— inicialización del esquema.
- Assets para UI: `frontend/src/main/resources/assets/` (fuentes, sprites,
sonidos).
- Vistas: `frontend/src/main/resources/fxml/` (pantallas principales y
componentes embebidos).

Extensiones / librerías usadas (listado desde `frontend/pom.xml`) — breve
descripción y valoración subjetiva (1-5)
- `org.openjfx:javafx-controls, javafx-fxml, javafx-media` (v21) — JavaFX para
UI/medios. Valoración: 5/5 (estándar para UI Java moderna).
- `org.postgresql:postgresql` (42.7.2) — driver JDBC PostgreSQL. Valoración: 4/5
(robusto y mantenido).
- `com.google.code.gson:gson` (2.10.1) — JSON <-> POJO. Valoración: 4/5 (ligero
y simple).
- `org.mindrot:jbcrypt` (0.4) — hashing bcrypt para contraseñas. Valoración: 4/5
(buena práctica para password hashing).

- `com.sun.mail:javax.mail` (1.6.2) — envío de email SMTP. Valoración: 3/5
(funcional, pero anticuado; considerar Jakarta Mail si se actualiza).
- `io.github.cdimascio:java-dotenv` (5.2.2) — carga de variables de entorno desde
`.env`. Valoración: 4/5 (útil en entornos locales).
- `net.java.dev.jna:jna` y `jna-platform` (5.13.0) — acceso nativo (posibles
utilidades de plataforma). Valoración: 3/5 (potente, usar con cuidado).
- `com.stripe:stripe-java` (24.8.0) — integración con Stripe. Valoración: 4/5
(buena integración; requiere manejo seguro de claves).
- `org.junit.jupiter:junit-jupiter-api` (5.10.0) — testing. Valoración: 5/5 (estándar
moderno para tests).

Seguridad y consideraciones operativas
- No dejar claves/secretos en repositorio; usar `java-dotenv` o variables de
entorno (`DB_URL`, `DB_USER`, `DB_PASS`, `STRIPE_KEY`).
- Validar firmas de webhooks (Stripe) y aplicar idempotencia en handlers de
webhooks.
- Evitar ejecutar envíos de email de forma sincrónica en UI thread.

Buenas prácticas y recomendaciones rápidas
- Mantener la lógica de negocio en `services/`, DAOs para persistencia y
controladores solo para UI y binding.
- Añadir migraciones (Flyway/Liquibase) en lugar de usar sólo el SQL de arranque
para evolución de esquema.
- Añadir pruebas de integración usando una BD en memoria (H2) y mocks para
servicios externos (Stripe, SMTP).
- Extraer variables CSS comunes a `variables.css` y centralizar estilos.
- Usar un pool de conexiones (HikariCP) si la app abre muchas conexiones
simultáneas.

Posibles siguientes pasos (opciones)
- Generar un README detallado por módulo (ya existen READMEs en `dao`,
`models`, `services`, `fxml`, `assets`, `styles`, `db`, `controllers`, `app`).

- Crear una guía de instalación local (instalar PostgreSQL/H2, cargar
`CtrlAltQuestDB.sql`, configurar `.env`).
- Añadir CI básico que compile, ejecute tests y valide estilo.

Dónde encontrar la documentación ya generada por carpeta (en el repo)
- `frontend/src/main/java/com/ctrlaltquest/dao/README.md` — DAOs
- `frontend/src/main/java/com/ctrlaltquest/db/README.md` — DB
- `frontend/src/main/java/com/ctrlaltquest/models/README.md` — Models
- `frontend/src/main/java/com/ctrlaltquest/services/README.md` — Services
- `frontend/src/main/java/com/ctrlaltquest/ui/app/README.md` — App launcher
- `frontend/src/main/java/com/ctrlaltquest/ui/controllers/README.md` —
Controllers
- `frontend/src/main/resources/fxml/README.md` — FXML views
- `frontend/src/main/resources/assets/README.md` — Assets
- `frontend/src/main/resources/styles/README.md` — Styles
