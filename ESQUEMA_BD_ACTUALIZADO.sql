-- =====================================================================
-- ESQUEMA ACTUALIZADO DE BASE DE DATOS - CTRL-ALT-QUEST
-- =====================================================================
-- Este es el archivo original actualizado con las correcciones
-- Contiene toda la estructura correcta de la BD
-- =====================================================================

-- ACTUALIZACIÓN: mission_progress - Agregada columna progress_percentage
CREATE TABLE IF NOT EXISTS public.mission_progress
(
    id serial NOT NULL,
    mission_id integer NOT NULL,
    user_id integer NOT NULL,
    metric_key character varying(50) COLLATE pg_catalog."default" NOT NULL,
    current_value bigint DEFAULT 0,
    target_value bigint DEFAULT 0,
    progress_percentage numeric(5, 2) DEFAULT 0.00,
    last_updated timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT mission_progress_pkey PRIMARY KEY (id),
    CONSTRAINT mission_progress_mission_id_metric_key_key UNIQUE (mission_id, metric_key)
);

-- =====================================================================
-- TRIGGER PARA MANTENER progress_percentage SINCRONIZADO
-- =====================================================================
CREATE OR REPLACE FUNCTION update_progress_percentage()
RETURNS TRIGGER AS $$
BEGIN
    -- Calcular automáticamente el progreso como porcentaje
    IF NEW.target_value > 0 THEN
        NEW.progress_percentage := (NEW.current_value::numeric / NEW.target_value::numeric) * 100;
    ELSE
        NEW.progress_percentage := 0.00;
    END IF;
    
    -- Actualizar last_updated
    NEW.last_updated := CURRENT_TIMESTAMP;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Crear trigger para INSERT
DROP TRIGGER IF EXISTS trigger_update_progress_percentage_insert ON mission_progress;
CREATE TRIGGER trigger_update_progress_percentage_insert
BEFORE INSERT ON mission_progress
FOR EACH ROW
EXECUTE FUNCTION update_progress_percentage();

-- Crear trigger para UPDATE
DROP TRIGGER IF EXISTS trigger_update_progress_percentage_update ON mission_progress;
CREATE TRIGGER trigger_update_progress_percentage_update
BEFORE UPDATE ON mission_progress
FOR EACH ROW
EXECUTE FUNCTION update_progress_percentage();

-- =====================================================================
-- TRIGGER PARA SINCRONIZAR app_usage_logs CON apps
-- =====================================================================
-- Este trigger asegura que si un app_id no existe, se rechace la inserción
-- en lugar de causar conflictos
CREATE OR REPLACE FUNCTION validate_app_id()
RETURNS TRIGGER AS $$
BEGIN
    -- Si app_id es NULL, está permitido (sin app asignada)
    IF NEW.app_id IS NOT NULL THEN
        -- Verificar que el app existe
        IF NOT EXISTS (SELECT 1 FROM apps WHERE id = NEW.app_id) THEN
            RAISE EXCEPTION 'El app_id % no existe en la tabla apps', NEW.app_id;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Crear trigger para validación
DROP TRIGGER IF EXISTS trigger_validate_app_id_insert ON app_usage_logs;
CREATE TRIGGER trigger_validate_app_id_insert
BEFORE INSERT ON app_usage_logs
FOR EACH ROW
EXECUTE FUNCTION validate_app_id();

DROP TRIGGER IF EXISTS trigger_validate_app_id_update ON app_usage_logs;
CREATE TRIGGER trigger_validate_app_id_update
BEFORE UPDATE ON app_usage_logs
FOR EACH ROW
EXECUTE FUNCTION validate_app_id();

-- =====================================================================
-- FUNCIÓN PARA LIMPIAR DATOS INVÁLIDOS (llamada periódicamente)
-- =====================================================================
CREATE OR REPLACE FUNCTION cleanup_orphaned_records()
RETURNS TABLE(deleted_count integer, action_name text) AS $$
DECLARE
    v_deleted_app_logs integer := 0;
    v_deleted_mission_items integer := 0;
    v_deleted_boss_items integer := 0;
    v_deleted_achievement_items integer := 0;
    v_deleted_payment_orders integer := 0;
BEGIN
    -- Limpiar app_usage_logs con app_id inválido
    DELETE FROM app_usage_logs 
    WHERE app_id IS NOT NULL 
      AND app_id NOT IN (SELECT id FROM apps);
    GET DIAGNOSTICS v_deleted_app_logs = ROW_COUNT;
    
    -- Limpiar mission_item_rewards con referencias inválidas
    DELETE FROM mission_item_rewards 
    WHERE mission_id NOT IN (SELECT id FROM missions)
       OR item_id NOT IN (SELECT id FROM items);
    GET DIAGNOSTICS v_deleted_mission_items = ROW_COUNT;
    
    -- Limpiar boss_item_rewards con referencias inválidas
    DELETE FROM boss_item_rewards 
    WHERE boss_id NOT IN (SELECT id FROM bosses)
       OR item_id NOT IN (SELECT id FROM items);
    GET DIAGNOSTICS v_deleted_boss_items = ROW_COUNT;
    
    -- Limpiar achievement_item_rewards con referencias inválidas
    DELETE FROM achievement_item_rewards 
    WHERE achievement_id NOT IN (SELECT id FROM achievements)
       OR item_id NOT IN (SELECT id FROM items);
    GET DIAGNOSTICS v_deleted_achievement_items = ROW_COUNT;
    
    -- Limpiar payment_orders con referencias inválidas
    DELETE FROM payment_orders 
    WHERE user_id NOT IN (SELECT id FROM users)
       OR product_id NOT IN (SELECT id FROM payment_products);
    GET DIAGNOSTICS v_deleted_payment_orders = ROW_COUNT;
    
    -- Registrar en audit_logs
    INSERT INTO audit_logs (action, description, created_at)
    VALUES (
        'CLEANUP',
        format('Limpieza ejecutada: app_logs=%s, mission_items=%s, boss_items=%s, achievement_items=%s, payment_orders=%s',
            v_deleted_app_logs, v_deleted_mission_items, v_deleted_boss_items, 
            v_deleted_achievement_items, v_deleted_payment_orders),
        CURRENT_TIMESTAMP
    );
    
    RETURN QUERY SELECT 
        (v_deleted_app_logs + v_deleted_mission_items + v_deleted_boss_items + 
         v_deleted_achievement_items + v_deleted_payment_orders)::integer,
        'Registros huérfanos eliminados'::text;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- ÍNDICES ADICIONALES PARA MEJOR RENDIMIENTO
-- =====================================================================
CREATE INDEX IF NOT EXISTS idx_mission_progress_mission_id 
    ON mission_progress(mission_id);

CREATE INDEX IF NOT EXISTS idx_mission_progress_user_id 
    ON mission_progress(user_id);

CREATE INDEX IF NOT EXISTS idx_mission_progress_percentage 
    ON mission_progress(progress_percentage)
    WHERE progress_percentage < 100;

CREATE INDEX IF NOT EXISTS idx_app_usage_logs_app_id 
    ON app_usage_logs(app_id)
    WHERE app_id IS NOT NULL;

-- =====================================================================
-- VISTAS ÚTILES PARA MONITOREO
-- =====================================================================

-- Vista: Progreso de misiones por usuario
CREATE OR REPLACE VIEW v_mission_progress_summary AS
SELECT 
    m.id as mission_id,
    m.title,
    u.username,
    mp.progress_percentage,
    mp.current_value,
    mp.target_value,
    CASE 
        WHEN mp.progress_percentage >= 100 THEN 'Completada'
        WHEN mp.progress_percentage >= 75 THEN 'Casi lista'
        WHEN mp.progress_percentage >= 50 THEN 'En progreso'
        ELSE 'Iniciada'
    END as estado,
    mp.last_updated
FROM missions m
JOIN users u ON m.user_id = u.id
LEFT JOIN mission_progress mp ON m.id = mp.mission_id
ORDER BY u.username, m.title;

-- Vista: Detección de registros huérfanos
CREATE OR REPLACE VIEW v_orphaned_records AS
SELECT 'app_usage_logs' as tabla, COUNT(*) as cantidad
FROM app_usage_logs 
WHERE app_id NOT IN (SELECT id FROM apps)
UNION ALL
SELECT 'mission_item_rewards', COUNT(*)
FROM mission_item_rewards 
WHERE mission_id NOT IN (SELECT id FROM missions)
   OR item_id NOT IN (SELECT id FROM items)
UNION ALL
SELECT 'payment_orders', COUNT(*)
FROM payment_orders 
WHERE user_id NOT IN (SELECT id FROM users)
   OR product_id NOT IN (SELECT id FROM payment_products);

-- Vista: Estado de sincronización
CREATE OR REPLACE VIEW v_sync_status AS
SELECT 
    'missions' as tabla,
    COUNT(m.id) as total_registros,
    (SELECT COUNT(*) FROM mission_progress mp WHERE mp.mission_id = m.id) as con_progreso,
    COUNT(m.id) - (SELECT COUNT(*) FROM mission_progress mp WHERE mp.mission_id = m.id) as sin_progreso
FROM missions m
GROUP BY 'missions'
UNION ALL
SELECT 
    'users',
    COUNT(*),
    (SELECT COUNT(*) FROM activity_sessions WHERE user_id IN (SELECT id FROM users)),
    0
FROM users;

-- =====================================================================
-- DOCUMENTACIÓN DE CAMBIOS
-- =====================================================================
/*
HISTORIAL DE CAMBIOS:

2026-01-28:
- Agregada columna progress_percentage a mission_progress
- Agregados triggers para sincronizar progress_percentage automáticamente
- Agregada función validate_app_id para validar foreign keys
- Agregada función cleanup_orphaned_records para limpiar datos huérfanos
- Agregados índices para mejorar performance
- Agregadas vistas para monitoreo y debugging

NOTAS:
- El progress_percentage se calcula automáticamente mediante trigger
- El app_id en app_usage_logs puede ser NULL pero si es válido, debe existir
- Se proporciona función cleanup_orphaned_records para mantenimiento periódico
*/
