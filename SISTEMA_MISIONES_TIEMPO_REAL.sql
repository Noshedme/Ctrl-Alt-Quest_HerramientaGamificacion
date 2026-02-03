-- ==========================================
-- SISTEMA COMPLETO DE MISIONES CON PROGRESO EN TIEMPO REAL
-- ==========================================
-- Este script configura:
-- 1. Misiones de ejemplo categorizadas
-- 2. Condiciones de logros
-- 3. Datos de prueba para validar el sistema de recompensas en tiempo real
-- ==========================================

-- ========== PARTE 1: MISIONES DE EJEMPLO ==========

-- Limpiar misiones previas si existen (SOLO para testing)
-- DELETE FROM public.missions WHERE user_id IN (SELECT id FROM public.users LIMIT 5);

-- Insertar MISIONES DE TIEMPO (se completan automáticamente mientras están en VSCode, etc.)

INSERT INTO public.missions 
(user_id, title, category, difficulty, xp_reward, coin_reward, trigger_type, conditions, is_manual, is_daily, is_weekly, progress, completed)
VALUES 
-- Misiones de programación
(1, 'Coder por 1 Hora', 'CODING', 'EASY', 50, 100, 'ACTIVITY', '{"metric":"time_coding","target":3600}', false, true, false, 0, false),
(1, 'Coder por 5 Horas', 'CODING', 'MEDIUM', 200, 300, 'ACTIVITY', '{"metric":"time_coding","target":18000}', false, false, true, 0, false),
(1, 'Maratón de Código', 'CODING', 'HARD', 500, 1000, 'ACTIVITY', '{"metric":"time_coding","target":86400}', false, false, false, 0, false),

-- Misiones de productividad general
(1, '1 Hora Productiva', 'PRODUCTIVITY', 'EASY', 30, 75, 'ACTIVITY', '{"metric":"time_productivity","target":3600}', false, true, false, 0, false),
(1, 'Día de Productividad', 'PRODUCTIVITY', 'MEDIUM', 150, 250, 'ACTIVITY', '{"metric":"time_productivity","target":28800}', false, false, true, 0, false),

-- Misiones de navegación
(1, 'Explorador Web', 'BROWSING', 'EASY', 25, 50, 'ACTIVITY', '{"metric":"time_browsing","target":3600}', false, true, false, 0, false),

-- Misiones de contador (apps únicas, etc.)
(1, 'Multitarea Experto', 'GENERAL', 'MEDIUM', 100, 200, 'ACTIVITY', '{"metric":"apps_used","target":5}', false, false, true, 0, false),
(1, 'Explorador Versátil', 'GENERAL', 'EASY', 40, 100, 'ACTIVITY', '{"metric":"categories_used","target":3}', false, true, false, 0, false);

-- ========== PARTE 2: LOGROS DE EJEMPLO ==========
-- Estos se desbloquean automáticamente cuando se cumplen las condiciones

INSERT INTO public.achievements (name, description, condition, xp_reward, coin_reward)
VALUES
-- Logros de programación
('Primer Código', 'Pasar 1 hora programando', '{"type":"time_coding","seconds":3600}', 50, 100),
('Programador Dedicado', 'Acumular 10 horas programando', '{"type":"total_time_coding","hours":10}', 200, 500),
('Marathonista de Código', 'Sesión de 8 horas programando consecutivas', '{"type":"consecutive_time_coding","seconds":28800}', 500, 1500),

-- Logros de misiones
('Primer Éxito', 'Completar tu primera misión', '{"type":"missions_completed","count":1}', 30, 75),
('Cazador de Misiones', 'Completar 10 misiones', '{"type":"missions_completed","count":10}', 150, 300),
('Leyenda de Misiones', 'Completar 50 misiones', '{"type":"missions_completed","count":50}', 500, 1000),

-- Logros de progresión
('Novato', 'Alcanzar nivel 5', '{"type":"level_reached","level":5}', 100, 200),
('Aventurero', 'Alcanzar nivel 10', '{"type":"level_reached","level":10}', 300, 500),
('Campeón', 'Alcanzar nivel 20', '{"type":"level_reached","level":20}', 1000, 2000),

-- Logros de consistencia
('Primer Día', 'Actividad en 1 día', '{"type":"consecutive_days","days":1}', 25, 50),
('Habitual', 'Actividad 7 días consecutivos', '{"type":"consecutive_days","days":7}', 200, 400),
('Adecuado', 'Actividad 30 días consecutivos', '{"type":"consecutive_days","days":30}', 1000, 2000),

-- Logros de XP
('Aprendiz', 'Acumular 100 XP', '{"type":"total_xp","amount":100}', 0, 100),
('Erudito', 'Acumular 1000 XP', '{"type":"total_xp","amount":1000}', 0, 500),
('Sabio', 'Acumular 10000 XP', '{"type":"total_xp","amount":10000}', 0, 2000);

-- ========== PARTE 3: VERIFICACIÓN ==========
-- Ejecutar estas queries para verificar que todo se creó correctamente:

SELECT 'MISIONES CREADAS:' as info;
SELECT id, title, category, xp_reward, coin_reward FROM public.missions WHERE user_id = 1 ORDER BY category;

SELECT 'LOGROS CREADOS:' as info;
SELECT id, name, xp_reward, coin_reward FROM public.achievements ORDER BY name LIMIT 10;

SELECT 'ESTATUS DEL USUARIO:' as info;
SELECT id, username, level, current_xp, total_xp, coins FROM public.users WHERE id = 1;

-- ========== PARTE 4: INSTRUCCIONES PARA TESTING ==========
/*
CÓMO PROBAR EL SISTEMA EN TIEMPO REAL:

1. INICIAR LA APLICACIÓN
   - Login con usuario existente
   - La aplicación iniciará a monitorear automáticamente

2. ABRIR VSCode (o la aplicación deseada)
   - El sistema detectará "Visual Studio Code" 
   - Comenzará a registrar tiempo en la categoría "CODING"
   - Cada segundo: mission_progress se actualiza +1 segundo
   - Cada 10 segundos: el usuario gana +1 XP

3. VERIFICAR PROGRESO EN LA UI
   - El panel derecho (Activity Panel) mostrará la app actual
   - Las misiones mostrarán progreso en tiempo real
   - Si completa una misión: popup de notificación + recompensas

4. VERIFICAR EN LA BD
   - Ejecutar cada 10 segundos:
     SELECT current_value, progress_percentage FROM mission_progress 
     WHERE user_id = 1 AND metric_key LIKE 'time_coding%'
     ORDER BY mission_id DESC LIMIT 1;
   
   - Ver progreso de XP:
     SELECT current_xp, total_xp FROM users WHERE id = 1;
   
   - Ver monedas obtenidas:
     SELECT SUM(amount) as total_earned FROM coin_transactions WHERE user_id = 1;

5. COMPLETAR MISIÓN RÁPIDAMENTE (para testing)
   - Abrir pgAdmin y ejecutar:
     UPDATE mission_progress SET current_value = target_value WHERE user_id = 1 AND metric_key LIKE 'time_coding%' LIMIT 1;
   
   - El siguiente tick del monitor (~1 segundo) completará la misión
   - Verá XP y monedas otorgadas automáticamente

6. DESBLOQUEAR LOGRO MANUAL (para testing)
   - INSERT INTO user_achievements (user_id, achievement_id, unlocked_at) VALUES (1, 1, NOW());
   - El sistema otorgará las recompensas (XP + monedas del logro)
*/

-- ========== PARTE 5: DATOS DE AUDIT PARA TESTING ==========

INSERT INTO public.audit_logs (user_id, action, description, ip_address, pc_name, os_info, created_at)
VALUES 
(1, 'SISTEMA_INICIADO', 'Sistema de misiones en tiempo real activado', '127.0.0.1', 'LOCAL-PC', 'Windows 10', NOW()),
(1, 'MONITOREO_INICIADO', 'Monitoreo de actividad iniciado para usuario 1', '127.0.0.1', 'LOCAL-PC', 'Windows 10', NOW());

SELECT 'SETUP COMPLETADO' as status;
