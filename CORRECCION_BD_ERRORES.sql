-- =====================================================================
-- SCRIPT DE CORRECCIÓN DE ERRORES EN BASE DE DATOS
-- =====================================================================
-- Fecha: 2026-01-28
-- Problemas a corregir:
-- 1. Foreign key mal configurada en app_usage_logs (app_id puede ser NULL)
-- 2. Columna progress_percentage faltante en mission_progress
-- 3. Sincronización de datos entre tablas
-- =====================================================================

BEGIN;

-- =====================================================================
-- PASO 1: LIMPIAR DATOS INVÁLIDOS EN app_usage_logs
-- =====================================================================
-- Antes de aplicar restricciones, eliminar registros con app_id inválidos
DELETE FROM app_usage_logs 
WHERE app_id IS NOT NULL 
  AND app_id NOT IN (SELECT id FROM apps);

-- Registrar en log
INSERT INTO audit_logs (action, description, created_at)
VALUES ('MAINTENANCE', 'Limpieza de registros inválidos en app_usage_logs', CURRENT_TIMESTAMP);

-- =====================================================================
-- PASO 2: ACTUALIZAR ESTRUCTURA DE mission_progress
-- =====================================================================
-- Agregar columna progress_percentage si no existe
ALTER TABLE mission_progress 
ADD COLUMN IF NOT EXISTS progress_percentage numeric(5, 2) DEFAULT 0.00;

-- Actualizar valores basados en current_value y target_value
UPDATE mission_progress 
SET progress_percentage = CASE 
    WHEN target_value > 0 THEN (current_value::numeric / target_value::numeric) * 100
    ELSE 0.00
END
WHERE target_value > 0;

-- Registrar en log
INSERT INTO audit_logs (action, description, created_at)
VALUES ('MAINTENANCE', 'Agregada columna progress_percentage a mission_progress', CURRENT_TIMESTAMP);

-- =====================================================================
-- PASO 3: SINCRONIZAR MISIONES CON PROGRESO
-- =====================================================================
-- Asegurar que todas las misiones activas tengan registro de progreso
INSERT INTO mission_progress (mission_id, user_id, metric_key, current_value, target_value, progress_percentage)
SELECT DISTINCT 
    m.id,
    m.user_id,
    'completion_progress',
    0,
    100,
    0.00
FROM missions m
WHERE NOT EXISTS (
    SELECT 1 FROM mission_progress mp 
    WHERE mp.mission_id = m.id 
    AND mp.metric_key = 'completion_progress'
);

-- Registrar en log
INSERT INTO audit_logs (action, description, created_at)
VALUES ('MAINTENANCE', 'Sincronización de misiones con mission_progress', CURRENT_TIMESTAMP);

-- =====================================================================
-- PASO 4: SINCRONIZAR TABLAS DE RECOMPENSAS
-- =====================================================================
-- Asegurar integridad referencial en mission_item_rewards
DELETE FROM mission_item_rewards 
WHERE mission_id NOT IN (SELECT id FROM missions)
   OR item_id NOT IN (SELECT id FROM items);

-- Asegurar integridad referencial en boss_item_rewards
DELETE FROM boss_item_rewards 
WHERE boss_id NOT IN (SELECT id FROM bosses)
   OR item_id NOT IN (SELECT id FROM items);

-- Asegurar integridad referencial en achievement_item_rewards
DELETE FROM achievement_item_rewards 
WHERE achievement_id NOT IN (SELECT id FROM achievements)
   OR item_id NOT IN (SELECT id FROM items);

-- Registrar en log
INSERT INTO audit_logs (action, description, created_at)
VALUES ('MAINTENANCE', 'Sincronización de tablas de recompensas', CURRENT_TIMESTAMP);

-- =====================================================================
-- PASO 5: LIMPIAR REGISTROS HUÉRFANOS EN PAYMENT ORDERS
-- =====================================================================
DELETE FROM payment_orders 
WHERE user_id NOT IN (SELECT id FROM users)
   OR product_id NOT IN (SELECT id FROM payment_products);

-- Registrar en log
INSERT INTO audit_logs (action, description, created_at)
VALUES ('MAINTENANCE', 'Limpieza de payment_orders huérfanos', CURRENT_TIMESTAMP);

-- =====================================================================
-- PASO 6: VALIDAR INTEGRIDAD DE SESSION TOKENS
-- =====================================================================
DELETE FROM session_tokens 
WHERE user_id NOT IN (SELECT id FROM users)
   OR revoked = true 
   AND expires_at < CURRENT_TIMESTAMP;

-- Registrar en log
INSERT INTO audit_logs (action, description, created_at)
VALUES ('MAINTENANCE', 'Limpieza de session_tokens inválidos', CURRENT_TIMESTAMP);

-- =====================================================================
-- PASO 7: VERIFICACIÓN FINAL Y RESUMEN
-- =====================================================================
-- Mostrar resumen de cambios realizados
SELECT 
    'VERIFICACIÓN COMPLETADA' as status,
    (SELECT COUNT(*) FROM missions) as total_missions,
    (SELECT COUNT(*) FROM mission_progress) as total_mission_progress,
    (SELECT COUNT(*) FROM app_usage_logs) as total_app_logs,
    (SELECT COUNT(*) FROM apps) as total_apps;

COMMIT;

-- =====================================================================
-- CONSULTAS DE VERIFICACIÓN (ejecutar después del COMMIT)
-- =====================================================================
-- Verificar que no hay app_usage_logs con app_id inválidos
-- SELECT * FROM app_usage_logs WHERE app_id NOT IN (SELECT id FROM apps);

-- Verificar que todas las misiones tienen progreso
-- SELECT m.id, m.title, COUNT(mp.id) as progress_records 
-- FROM missions m 
-- LEFT JOIN mission_progress mp ON m.id = mp.mission_id 
-- GROUP BY m.id, m.title;

-- Verificar que la columna progress_percentage existe y tiene datos
-- SELECT COUNT(*), AVG(progress_percentage) as promedio 
-- FROM mission_progress 
-- WHERE progress_percentage > 0;
