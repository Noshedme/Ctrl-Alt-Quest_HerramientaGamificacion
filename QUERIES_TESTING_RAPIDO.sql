-- =====================================================
-- QUERIES RÁPIDAS PARA TESTING DEL SISTEMA EN TIEMPO REAL
-- =====================================================
-- Copiar/pegar estas queries en pgAdmin para verificar
-- que el sistema está funcionando correctamente
-- =====================================================

-- ========== ANTES DE INICIAR =========

-- 1. Verificar que el usuario existe
SELECT id, username, level, current_xp, total_xp, coins 
FROM users WHERE username = 'usuario_test' LIMIT 1;
-- Si no existe, usar SELECT * FROM users LIMIT 1; para obtener un ID válido

-- 2. Verificar que existen misiones
SELECT COUNT(*) as total_misiones FROM missions WHERE user_id = 1;

-- 3. Verificar que existen logros
SELECT COUNT(*) as total_logros FROM achievements;

-- ========== MIENTRAS LA APLICACIÓN ESTÁ CORRIENDO =========

-- QUERY 1: Ver progreso de misiones EN TIEMPO REAL (ejecutar cada 5 seg)
SELECT 
    mp.mission_id,
    m.title,
    mp.metric_key,
    mp.current_value as valor_actual,
    mp.target_value as valor_objetivo,
    ROUND(mp.progress_percentage, 2) as progreso_pct,
    CASE 
        WHEN mp.progress_percentage >= 100 THEN '✅ COMPLETA'
        WHEN mp.progress_percentage >= 50 THEN '⏳ 50%'
        WHEN mp.progress_percentage >= 25 THEN '🔄 25%'
        ELSE '⭐ Iniciada'
    END as estado,
    m.completed
FROM mission_progress mp
JOIN missions m ON mp.mission_id = m.id
WHERE mp.user_id = 1 AND m.completed = false
ORDER BY mp.progress_percentage DESC;

-- QUERY 2: Ver XP y monedas EN TIEMPO REAL (ejecutar cada 10 seg)
SELECT 
    username,
    level,
    current_xp as xp_actual,
    total_xp as xp_total,
    CASE 
        WHEN current_xp >= (level+1)*100 THEN '⬆️ LISTO PARA SUBIR'
        ELSE CAST((current_xp * 100) / ((level+1)*100) AS INTEGER) || '%'
    END as progreso_nivel,
    coins as monedas_totales,
    '🎯' as estado
FROM users WHERE id = 1;

-- QUERY 3: Ver últimas transacciones (ejecutar cada 20 seg)
SELECT 
    reason,
    amount as monedas,
    created_at,
    CASE 
        WHEN reason LIKE '%Misión%' THEN '🎯'
        WHEN reason LIKE '%Logro%' THEN '🏆'
        WHEN reason LIKE '%Level%' THEN '⬆️'
        ELSE '💰'
    END as tipo
FROM coin_transactions
WHERE user_id = 1
ORDER BY created_at DESC
LIMIT 10;

-- QUERY 4: Ver logros desbloqueados
SELECT 
    a.name,
    a.xp_reward || ' XP + ' || a.coin_reward || ' 💰' as recompensa,
    ua.unlocked_at,
    EXTRACT(EPOCH FROM (NOW() - ua.unlocked_at))::INT as hace_segundos
FROM user_achievements ua
JOIN achievements a ON ua.achievement_id = a.id
WHERE ua.user_id = 1
ORDER BY ua.unlocked_at DESC;

-- QUERY 5: Ver sesiones activas y actividad
SELECT 
    s.id as session_id,
    s.session_start,
    EXTRACT(EPOCH FROM (NOW() - s.session_start))::INT as segundos_activo,
    COUNT(DISTINCT aul.app_id) as apps_diferentes,
    COUNT(*) as total_logs
FROM activity_sessions s
LEFT JOIN app_usage_logs aul ON s.id = aul.session_id
WHERE s.user_id = 1 AND s.session_end IS NULL
GROUP BY s.id, s.session_start;

-- ========== PARA FORZAR COMPLETACIÓN (DEBUG TESTING) =========

-- PASO 1: Ver qué misión es más cercana a completarse
SELECT 
    mp.mission_id,
    m.title,
    mp.current_value,
    mp.target_value,
    mp.target_value - mp.current_value as falta
FROM mission_progress mp
JOIN missions m ON mp.mission_id = m.id
WHERE mp.user_id = 1 AND m.completed = false
ORDER BY falta ASC
LIMIT 1;

-- PASO 2: Completar la misión (reemplazar 1 con ID real de la query anterior)
UPDATE mission_progress 
SET current_value = target_value, progress_percentage = 100
WHERE user_id = 1 AND mission_id = 1;

-- PASO 3: En el siguiente tick (1-2 seg), verificar que se completó
SELECT title, completed, progress FROM missions WHERE id = 1;

-- PASO 4: Verificar que se otorgaron recompensas
SELECT total_xp, coins FROM users WHERE id = 1;

-- ========== PARA DESBLOQUEAR LOGROS (DEBUG TESTING) =========

-- Desbloquear un logro manualmente (para testing)
-- Reemplazar 1 con ID del logro deseado
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
VALUES (1, 1, NOW())
ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- Verificar que se desbloqueó
SELECT * FROM user_achievements WHERE user_id = 1 ORDER BY unlocked_at DESC LIMIT 1;

-- ========== DASHBOARD COMPLETO =========

-- Copiar esta query para ver todo en una sola pantalla
WITH stats_usuario AS (
    SELECT 
        id, username, level, current_xp, total_xp, coins
    FROM users WHERE id = 1
),
stats_misiones AS (
    SELECT 
        COUNT(*) as total,
        SUM(CASE WHEN completed THEN 1 ELSE 0 END) as completadas
    FROM missions WHERE user_id = 1
),
stats_logros AS (
    SELECT COUNT(*) as total FROM user_achievements WHERE user_id = 1
),
stats_actividad AS (
    SELECT 
        COUNT(*) as logs,
        SUM(EXTRACT(EPOCH FROM duration))::INT as segundos
    FROM app_usage_logs aul
    JOIN activity_sessions act ON aul.session_id = act.id
    WHERE act.user_id = 1 AND DATE(aul.start_time) = CURRENT_DATE
)
SELECT 
    (SELECT username FROM stats_usuario) as usuario,
    (SELECT level FROM stats_usuario) as nivel,
    (SELECT total_xp FROM stats_usuario) as xp_total,
    (SELECT coins FROM stats_usuario) as monedas,
    (SELECT completadas || '/' || total FROM stats_misiones) as misiones,
    (SELECT total FROM stats_logros) as logros_obtenidos,
    (SELECT logs FROM stats_actividad) as registros_hoy,
    (SELECT segundos FROM stats_actividad) as segundos_activo;

-- ========== PARA MONITOREO CONTINUO =========

-- Crear una función para monitor rápido
-- (Ejecutar una sola vez, luego llamar con SELECT * FROM monitor_usuario(1))

CREATE OR REPLACE FUNCTION monitor_usuario(p_user_id INT) RETURNS TABLE (
    usuario VARCHAR,
    nivel INT,
    xp_actual BIGINT,
    xp_total BIGINT,
    monedas BIGINT,
    misiones_activas TEXT,
    logros_desbloqueados BIGINT,
    app_en_foco VARCHAR,
    tiempo_app INT
) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT
        u.username::VARCHAR,
        u.level,
        u.current_xp,
        u.total_xp,
        u.coins,
        (
            SELECT COUNT(*) || ' activas, ' || SUM(CASE WHEN completed THEN 1 ELSE 0 END) || ' completadas'
            FROM missions WHERE user_id = p_user_id
        )::VARCHAR,
        (SELECT COUNT(*) FROM user_achievements WHERE user_id = p_user_id),
        (
            SELECT a.name 
            FROM app_usage_logs aul
            LEFT JOIN apps a ON aul.app_id = a.id
            ORDER BY aul.start_time DESC LIMIT 1
        )::VARCHAR,
        (
            SELECT EXTRACT(EPOCH FROM (NOW() - MAX(aul.start_time)))::INT
            FROM app_usage_logs aul
            WHERE aul.session_id IN (
                SELECT id FROM activity_sessions WHERE user_id = p_user_id AND session_end IS NULL
            )
        )::INT
    FROM users u
    WHERE u.id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- Usar: SELECT * FROM monitor_usuario(1);

-- ========== LIMPIAR DATOS DE PRUEBA =========

-- Si necesitas resetear todo para empezar de nuevo:

-- 1. Borrar transacciones
DELETE FROM coin_transactions WHERE user_id = 1;

-- 2. Borrar logros desbloqueados
DELETE FROM user_achievements WHERE user_id = 1;

-- 3. Borrar misiones completadas (o todas)
-- DELETE FROM missions WHERE user_id = 1;

-- 4. Reset del usuario
UPDATE users 
SET 
    level = 1,
    current_xp = 0,
    total_xp = 0,
    coins = 500
WHERE id = 1;

-- 5. Reset de progreso de misiones
DELETE FROM mission_progress WHERE user_id = 1;

-- 6. Borrar logs de actividad
DELETE FROM app_usage_logs 
WHERE session_id IN (
    SELECT id FROM activity_sessions WHERE user_id = 1
);

-- 7. Borrar sesiones
DELETE FROM activity_sessions WHERE user_id = 1;

-- ========== VERIFICACIÓN FINAL =========

-- Ejecutar esto para confirmar que todo está limpio
SELECT 
    'Usuarios' as tabla, COUNT(*) as registros FROM users
    UNION ALL
    SELECT 'Misiones', COUNT(*) FROM missions WHERE user_id = 1
    UNION ALL
    SELECT 'Progreso Misiones', COUNT(*) FROM mission_progress WHERE user_id = 1
    UNION ALL
    SELECT 'Logros Desbloqueados', COUNT(*) FROM user_achievements WHERE user_id = 1
    UNION ALL
    SELECT 'Transacciones', COUNT(*) FROM coin_transactions WHERE user_id = 1;

-- =====================================================
-- FIN DE QUERIES DE TESTING
-- =====================================================
