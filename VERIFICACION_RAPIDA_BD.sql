-- =====================================================================
-- SCRIPT DE VERIFICACIÓN RÁPIDA - CTRL-ALT-QUEST BD
-- =====================================================================
-- Ejecuta este script para verificar si tu BD está correcta
-- Después de ejecutar CORRECCION_BD_ERRORES.sql
-- =====================================================================

-- ✅ VERIFICACIÓN 1: Foreign Keys Válidas
SELECT 
    'FOREIGN KEY VALIDATION' as check_name,
    COUNT(*) as invalid_app_logs
FROM app_usage_logs 
WHERE app_id IS NOT NULL 
  AND app_id NOT IN (SELECT id FROM apps)
UNION ALL

-- ✅ VERIFICACIÓN 2: Columna progress_percentage Existe
SELECT 
    'PROGRESS_PERCENTAGE COLUMN' as check_name,
    COUNT(*) as column_records
FROM information_schema.columns 
WHERE table_name = 'mission_progress' 
  AND column_name = 'progress_percentage'
UNION ALL

-- ✅ VERIFICACIÓN 3: Misiones Sincronizadas
SELECT 
    'MISSION PROGRESS SYNC' as check_name,
    COUNT(*) as missions_with_progress
FROM missions m
WHERE EXISTS (
    SELECT 1 FROM mission_progress mp 
    WHERE mp.mission_id = m.id 
    AND mp.metric_key = 'completion_progress'
)
UNION ALL

-- ✅ VERIFICACIÓN 4: Triggers Creados
SELECT 
    'TRIGGERS' as check_name,
    COUNT(*) as active_triggers
FROM information_schema.triggers 
WHERE event_object_table IN ('mission_progress', 'app_usage_logs')
  AND trigger_schema = 'public'
UNION ALL

-- ✅ VERIFICACIÓN 5: Registros Huérfanos
SELECT 
    'ORPHANED RECORDS' as check_name,
    COUNT(*) as orphaned_count
FROM mission_progress mp
WHERE mp.mission_id NOT IN (SELECT id FROM missions)
   OR mp.user_id NOT IN (SELECT id FROM users);

-- =====================================================================
-- REPORTE DETALLADO DE ISSUES (Si hay problemas)
-- =====================================================================

-- Si VERIFICACIÓN 1 retorna > 0:
-- SELECT 'Invalid app_usage_logs records' as issue, COUNT(*) as count
-- FROM app_usage_logs WHERE app_id NOT IN (SELECT id FROM apps);

-- Si VERIFICACIÓN 2 retorna 0:
-- Significa que la columna progress_percentage NO EXISTE
-- Necesitas ejecutar: ALTER TABLE mission_progress ADD COLUMN progress_percentage numeric(5,2);

-- Si VERIFICACIÓN 3 retorna < total de misiones:
-- Significa que hay misiones sin progreso
-- Necesitas ejecutar el INSERT para crear registros de progreso

-- Si VERIFICACIÓN 4 retorna 0:
-- Significa que los triggers no se crearon
-- Necesitas ejecutar ESQUEMA_BD_ACTUALIZADO.sql

-- =====================================================================
-- ESTADÍSTICAS GENERALES
-- =====================================================================

SELECT '=== ESTADÍSTICAS DE BASE DE DATOS ===' as stat;

SELECT 
    'Total Users' as stat_type,
    COUNT(*) as count
FROM users

UNION ALL

SELECT 'Total Missions', COUNT(*)
FROM missions

UNION ALL

SELECT 'Total Mission Progress', COUNT(*)
FROM mission_progress

UNION ALL

SELECT 'Total App Usage Logs', COUNT(*)
FROM app_usage_logs

UNION ALL

SELECT 'Total Activity Sessions', COUNT(*)
FROM activity_sessions

UNION ALL

SELECT 'Progress With Valid Percentage', COUNT(*)
FROM mission_progress
WHERE progress_percentage >= 0 AND progress_percentage <= 100;

-- =====================================================================
-- VISTA: Resumen de Salud de Base de Datos
-- =====================================================================

CREATE OR REPLACE VIEW v_database_health AS
SELECT 
    'app_usage_logs Foreign Key' as health_check,
    (SELECT COUNT(*) FROM app_usage_logs 
     WHERE app_id IS NOT NULL AND app_id NOT IN (SELECT id FROM apps)) as issues_found,
    CASE 
        WHEN (SELECT COUNT(*) FROM app_usage_logs 
              WHERE app_id IS NOT NULL AND app_id NOT IN (SELECT id FROM apps)) = 0 
        THEN '✅ BIEN'
        ELSE '❌ PROBLEMA'
    END as status

UNION ALL

SELECT 
    'mission_progress Column' as health_check,
    (SELECT COUNT(*) FROM information_schema.columns 
     WHERE table_name = 'mission_progress' AND column_name = 'progress_percentage'),
    CASE 
        WHEN (SELECT COUNT(*) FROM information_schema.columns 
              WHERE table_name = 'mission_progress' AND column_name = 'progress_percentage') > 0
        THEN '✅ BIEN'
        ELSE '❌ PROBLEMA'
    END as status

UNION ALL

SELECT 
    'Triggers Active' as health_check,
    (SELECT COUNT(*) FROM information_schema.triggers 
     WHERE event_object_table IN ('mission_progress', 'app_usage_logs')),
    CASE 
        WHEN (SELECT COUNT(*) FROM information_schema.triggers 
              WHERE event_object_table IN ('mission_progress', 'app_usage_logs')) >= 4
        THEN '✅ BIEN'
        ELSE '⚠️ PARCIAL'
    END as status

UNION ALL

SELECT 
    'Orphaned Mission Progress' as health_check,
    (SELECT COUNT(*) FROM mission_progress mp
     WHERE mp.mission_id NOT IN (SELECT id FROM missions)),
    CASE 
        WHEN (SELECT COUNT(*) FROM mission_progress mp
              WHERE mp.mission_id NOT IN (SELECT id FROM missions)) = 0
        THEN '✅ BIEN'
        ELSE '❌ PROBLEMA'
    END as status

UNION ALL

SELECT 
    'Active Sessions Count' as health_check,
    (SELECT COUNT(*) FROM activity_sessions WHERE session_end IS NULL),
    '✅ INFO'
    
UNION ALL

SELECT 
    'Missions Completeness' as health_check,
    (SELECT COUNT(*) FROM missions m 
     WHERE NOT EXISTS (SELECT 1 FROM mission_progress mp WHERE mp.mission_id = m.id)),
    CASE 
        WHEN (SELECT COUNT(*) FROM missions m 
              WHERE NOT EXISTS (SELECT 1 FROM mission_progress mp WHERE mp.mission_id = m.id)) = 0
        THEN '✅ BIEN'
        ELSE '⚠️ INCOMPLETE'
    END as status;

-- Para ver el reporte de salud:
-- SELECT * FROM v_database_health;

-- =====================================================================
-- EXPORTAR REPORTE DE SALUD
-- =====================================================================

-- Crear tabla temporal para reporte
CREATE TEMP TABLE temp_health_report AS
SELECT 
    NOW()::timestamp as check_timestamp,
    health_check,
    issues_found,
    status
FROM v_database_health
ORDER BY 
    CASE 
        WHEN status = '❌ PROBLEMA' THEN 1
        WHEN status = '⚠️ INCOMPLETE' THEN 2
        WHEN status = '⚠️ PARCIAL' THEN 3
        ELSE 4
    END;

SELECT 
    '=== HEALTH REPORT ===' as header,
    check_timestamp::text as timestamp
FROM temp_health_report
LIMIT 1;

SELECT * FROM temp_health_report;

-- =====================================================================
-- NOTAS FINALES
-- =====================================================================

/*
INTERPRETACIÓN DE RESULTADOS:

✅ BIEN
- No hay problemas identificados
- Puedes usar la aplicación normalmente

⚠️ PARCIAL / INCOMPLETE
- Hay algunos registros que pueden causar problemas
- Ejecutar CORRECCION_BD_ERRORES.sql para limpiar

❌ PROBLEMA
- La base de datos está dañada
- CRÍTICO: Ejecutar CORRECCION_BD_ERRORES.sql inmediatamente
- Considerar restaurar backup si es necesario

Para más información:
1. Ver RESUMEN_ERRORES_Y_SOLUCIONES.md
2. Ver GUIA_CORRECCION_BD.md
3. Ver RECOMENDACIONES_CODIGO_JAVA.md
*/
