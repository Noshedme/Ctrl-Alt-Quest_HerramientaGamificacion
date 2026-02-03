-- ====================================================
-- SCRIPT DE PRUEBA: Insertar Misiones de Ejemplo
-- ====================================================
-- ADVERTENCIA: Reemplaza <USER_ID> con tu ID de usuario real
-- Verifica primero: SELECT id, username FROM public.users;

-- ====================================================
-- 1. VERIFICAR TU USER_ID (Ejecuta primero)
-- ====================================================
SELECT id, username, email FROM public.users LIMIT 5;


-- ====================================================
-- 2. INSERTAR MISIONES DIARIAS (para prueba)
-- ====================================================
-- Asume que tu user_id es 1. Reemplaza si es diferente.

INSERT INTO public.missions 
(user_id, title, category, difficulty, xp_reward, coin_reward, is_daily, is_weekly, progress, completed, conditions, trigger_type, created_at) 
VALUES 
-- Misión Diaria #1
(1, 'Programa 1 Hora Sin Interrupciones', 'Productividad', 'Fácil', 100, 50, true, false, 
 50, false, '{"metric": "time_coding", "target": 3600}', 'activity', NOW()),

-- Misión Diaria #2
(1, 'Lee 3 Artículos Técnicos', 'Aprendizaje', 'Medio', 150, 75, true, false, 
 75, false, '{"metric": "articles_read", "target": 3}', 'activity', NOW()),

-- Misión Diaria #3 (completada)
(1, 'Escribe 500 Palabras', 'Escritura', 'Fácil', 80, 40, true, false, 
 100, true, '{"metric": "words_typed", "target": 500}', 'activity', NOW() - INTERVAL '2 days');


-- ====================================================
-- 3. INSERTAR MISIONES SEMANALES
-- ====================================================

INSERT INTO public.missions 
(user_id, title, category, difficulty, xp_reward, coin_reward, is_daily, is_weekly, progress, completed, conditions, trigger_type, created_at) 
VALUES 

-- Misión Semanal #1
(1, 'Alcanza 20 Horas de Código en la Semana', 'Productividad', 'Medio', 500, 250, false, true, 
 60, false, '{"metric": "weekly_coding_hours", "target": 20}', 'activity', NOW() - INTERVAL '3 days'),

-- Misión Semanal #2
(1, 'Completa 5 Ejercicios de Programación', 'Desafío', 'Difícil', 350, 180, false, true, 
 40, false, '{"metric": "exercises_completed", "target": 5}', 'activity', NOW() - INTERVAL '2 days'),

-- Misión Semanal #3 (completada)
(1, 'Asiste a 3 Sesiones de Estudio', 'Educación', 'Fácil', 200, 100, false, true, 
 100, true, '{"metric": "study_sessions", "target": 3}', 'activity', NOW() - INTERVAL '5 days');


-- ====================================================
-- 4. INSERTAR MISIONES DE HISTORIA DE CLASE
-- ====================================================
-- Nota: is_daily = false, is_weekly = false → Tipo = "CLASE"

INSERT INTO public.missions 
(user_id, title, category, difficulty, xp_reward, coin_reward, is_daily, is_weekly, progress, completed, conditions, trigger_type, created_at) 
VALUES 

-- Misión de Clase #1
(1, 'Capítulo 1: El Primer Código', 'Historia', 'Fácil', 200, 100, false, false, 
 0, false, '{"chapter": 1}', 'story', NOW()),

-- Misión de Clase #2
(1, 'Capítulo 2: Dominar los Bucles', 'Historia', 'Medio', 300, 150, false, false, 
 0, false, '{"chapter": 2}', 'story', NOW()),

-- Misión de Clase #3
(1, 'Capítulo 3: La Gran Base de Datos', 'Historia', 'Difícil', 500, 250, false, false, 
 30, false, '{"chapter": 3}', 'story', NOW());


-- ====================================================
-- 5. VERIFICAR DATOS INSERTADOS
-- ====================================================
SELECT 
    id, 
    user_id, 
    title, 
    category, 
    difficulty, 
    xp_reward, 
    coin_reward, 
    is_daily, 
    is_weekly, 
    progress, 
    completed,
    created_at
FROM public.missions 
WHERE user_id = 1
ORDER BY is_daily DESC, is_weekly DESC, created_at DESC;


-- ====================================================
-- 6. SI ALGO SALE MAL: LIMPIAR DATOS
-- ====================================================
-- Descomenta si necesitas eliminar las misiones de prueba:

-- DELETE FROM public.missions WHERE user_id = 1 AND created_at > NOW() - INTERVAL '1 day';

-- ====================================================
-- 7. SCRIPT ALTERNATIVO: INSERT MASIVO
-- ====================================================
-- Si prefieres insertar muchas misiones a la vez:

INSERT INTO public.missions (user_id, title, category, difficulty, xp_reward, coin_reward, is_daily, is_weekly, progress, completed, conditions, trigger_type, created_at) 
VALUES 
(1, 'Misión Rápida 1', 'Test', 'Fácil', 50, 25, true, false, 25, false, '{}', 'test', NOW()),
(1, 'Misión Rápida 2', 'Test', 'Fácil', 50, 25, true, false, 50, false, '{}', 'test', NOW()),
(1, 'Misión Rápida 3', 'Test', 'Fácil', 50, 25, true, false, 75, false, '{}', 'test', NOW()),
(1, 'Misión Rápida 4', 'Test', 'Fácil', 50, 25, true, false, 100, true, '{}', 'test', NOW()),
(1, 'Semanal Test 1', 'Test', 'Medio', 100, 50, false, true, 45, false, '{}', 'test', NOW()),
(1, 'Semanal Test 2', 'Test', 'Medio', 100, 50, false, true, 90, false, '{}', 'test', NOW()),
(1, 'Clase Test 1', 'Test', 'Difícil', 200, 100, false, false, 0, false, '{}', 'test', NOW()),
(1, 'Clase Test 2', 'Test', 'Difícil', 200, 100, false, false, 50, false, '{}', 'test', NOW());

-- ====================================================
-- NOTA IMPORTANTE:
-- ====================================================
-- Si después de ejecutar esto aún no ves las misiones:
-- 1. Verifica que user_id = 1 es correcto para TU usuario
-- 2. Ejecuta los SELECT para confirmar que los datos existen
-- 3. Ejecuta la aplicación y revisa el console para mensajes DEBUG
-- 4. Si ves "Total misiones encontradas: 0", el problema está en la BD
-- 5. Si ves "Total misiones encontradas: 5" pero no aparecen en UI, 
--    el problema está en el mapeo o visualización
