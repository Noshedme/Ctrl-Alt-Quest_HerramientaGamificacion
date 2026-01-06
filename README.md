 (cd "$(git rev-parse --show-toplevel)" && git apply --3way <<'EOF' 
diff --git a/README.md b/README.md
index 6b58734525cc762108e5108a0159d3a62b601cf7..7d1c580106ddd9281a5fb56e92eaaa24ebf665a3 100644
--- a/README.md
+++ b/README.md
@@ -1,22 +1,29 @@
 # Ctrl+Alt+Quest
 
 Una app desktop gamificada que convierte el uso de tu PC en una aventura RPG. Registra actividades, gana XP, completa misiones y derrota jefes.
 
 ## Tecnologías
 - Backend: Spring Boot, JPA/Hibernate, H2 DB (local)
 - Frontend: JavaFX (para UI interactiva y visualmente atractiva)
 - Monitoreo: JNA para procesos de sistema
 - Seguridad: Spring Security (login local)
 
 ## Setup
 1. Clona el repo.
 2. Abre en VS Code o IntelliJ.
 3. Ejecuta `mvn clean install` para dependencias.
-4. Lanza la app con `mvn spring-boot:run` o desde IDE.
+4. Exporta las credenciales de la base de datos:
+   ```bash
+   export DB_USER=tu_usuario
+   export DB_PASSWORD=tu_password
+   ```
+5. (Opcional) Si usas perfiles de Spring, crea `src/main/resources/application-local.properties`
+   con tus valores locales y activa el perfil `local` al ejecutar la app.
+6. Lanza la app con `mvn spring-boot:run` o desde IDE.
 
 ## Pendiente
 - Implementar monitoreo dinámico.
 - UI con animaciones (e.g., progress bars animadas, pop-ups para misiones).
 - Integración completa con BDD.
 
-Para más, ve el documento original.
\ No newline at end of file
+Para más, ve el documento original.
 
EOF
)