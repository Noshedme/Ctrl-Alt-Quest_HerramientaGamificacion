-- ==========================================
-- SISTEMA DE EVENTOS CONTEXTUALES
-- ==========================================
-- Este script configura:
-- 1. Bosses de ejemplo
-- 2. Datos de prueba para eventos
-- 3. Logros relacionados con eventos
-- ==========================================

-- ========== PARTE 1: BOSSES ==========
-- Los bosses son enemigos que aparecen como eventos

INSERT INTO public.bosses (name, description, mechanic, base_hp, spawn_reason, xp_reward, coin_reward, difficulty, config)
VALUES
('Lagbug', 'Un bug que ralentiza tu código', 'CLICK_BASED', 50, 'During coding sessions', 100, 75, 'EASY', '{"clicksNeeded": 50, "timeLimit": 120}'),
('The Compiler', 'Error messages personificados', 'TYPING_BASED', 75, 'While coding in IDE', 200, 150, 'MEDIUM', '{"wordsNeeded": 100, "timeLimit": 180}'),
('Procrastination Demon', 'La tentación de no trabajar', 'CLICK_BASED', 100, 'After long sessions', 300, 200, 'HARD', '{"clicksNeeded": 150, "timeLimit": 240}'),
('Typo Troll', 'Tramposo de escritura', 'TYPING_BASED', 60, 'Random during typing', 120, 90, 'MEDIUM', '{"wordsNeeded": 80, "timeLimit": 150}'),
('The Distractor', 'Las notificaciones que irrumpen', 'CLICK_BASED', 80, 'During work', 180, 120, 'MEDIUM', '{"clicksNeeded": 100, "timeLimit": 200}');

-- ========== PARTE 2: LOGROS RELACIONADOS CON EVENTOS ==========

INSERT INTO public.achievements (name, description, condition, xp_reward, coin_reward)
VALUES
-- Logros de eventos typing
('Escritor Veloz', 'Completar un reto de escritura', '{"type":"typing_challenge_complete","count":1}', 40, 60),
('Mecanógrafo Profesional', 'Completar 5 retos de escritura', '{"type":"typing_challenge_complete","count":5}', 150, 200),

-- Logros de eventos clicking
('Bombardero de Clicks', 'Completar un reto de clicks', '{"type":"click_rush_complete","count":1}', 35, 50),
('Máquina de Clicks', 'Completar 5 tormentas de clicks', '{"type":"click_rush_complete","count":5}', 140, 180),

-- Logros de boss fights
('Cazador de Bosses', 'Derrotar tu primer boss', '{"type":"boss_defeated","count":1}', 100, 150),
('Domador de Enemigos', 'Derrotar 5 bosses', '{"type":"boss_defeated","count":5}', 400, 500),
('Leyenda de Batalla', 'Derrotar 20 bosses', '{"type":"boss_defeated","count":20}', 1000, 1500),

-- Logros de descanso
('Cuidador de Salud', 'Completar un descanso forzado', '{"type":"break_taken","count":1}', 20, 30),
('Consistente', 'Tomar 10 descansos', '{"type":"break_taken","count":10}', 150, 200);

-- ========== PARTE 3: INSTRUCCIONES DE TESTING ==========
/*
CÓMO PROBAR EL SISTEMA DE EVENTOS:

1. INICIAR LA APLICACIÓN
   - Login como usuario
   - Comenzar a monitorear actividad (presionar botón Actividad)

2. EVENTO TYPING CHALLENGE (Reto de escritura)
   - Sistema detecta actividad en VSCode/IDE
   - Pop-up aparece: "¡Escribe 75 palabras para ganar XP!"
   - Usuario escribe en cualquier aplicación
   - Progreso se actualiza en tiempo real
   - Al completar: +40 XP, +60 coins

3. EVENTO CLICK RUSH (Tormenta de clicks)
   - Sistema detecta actividad en navegador o escritura
   - Pop-up: "¡Haz 50 clicks lo más rápido posible!"
   - Usuario hace clicks (mouse)
   - Progreso aumenta
   - Al completar: +35 XP, +50 coins

4. EVENTO BOSS ENCOUNTER (Encuentro con jefe)
   - Aparición aleatoria (baja probabilidad)
   - Pop-up: "¡Un Boss apareció! Derrótalo haciendo clicks"
   - Usuario debe hacer X clicks para derrotarlo
   - Recompensas altas: +100 XP, +150 coins

5. EVENTO BREAK TIME (Descanso forzado)
   - Muy raro (10% probabilidad)
   - Pop-up: "Es hora de descansar. Apartate de la pantalla por 30 seg"
   - Timer cuenta hacia atrás
   - Recompensas bajas pero importantes para salud: +20 XP, +30 coins

VERIFICACIÓN EN BD:
-- Ver eventos activos
SELECT id, user_id, type, description, handled FROM public.events WHERE user_id = 3 ORDER BY occurred_at DESC LIMIT 5;

-- Ver estadísticas de eventos
SELECT COUNT(*) as total, 
       SUM(CASE WHEN handled = true THEN 1 ELSE 0 END) as completados
FROM public.events WHERE user_id = 3;

-- Ver logros desbloqueados por eventos
SELECT a.name, a.description FROM public.achievements a 
JOIN public.user_achievements ua ON a.id = ua.achievement_id 
WHERE ua.user_id = 3 AND a.name LIKE '%reto%' OR a.name LIKE '%click%' OR a.name LIKE '%boss%';
*/

-- ========== PARTE 4: AUDITORÍA ==========

INSERT INTO public.audit_logs (user_id, action, description, ip_address, pc_name, os_info, created_at)
VALUES 
(1, 'EVENTOS_INICIALIZADOS', 'Sistema de eventos contextuales inicializado', '127.0.0.1', 'LOCAL-PC', 'Windows 10', NOW()),
(1, 'BOSSES_AGREGADOS', '5 bosses de ejemplo agregados', '127.0.0.1', 'LOCAL-PC', 'Windows 10', NOW()),
(1, 'EVENTOS_LOGROS_AGREGADOS', '13 logros relacionados con eventos agregados', '127.0.0.1', 'LOCAL-PC', 'Windows 10', NOW());

SELECT 'EVENTOS SETUP COMPLETADO' as status;
