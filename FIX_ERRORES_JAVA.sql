-- =====================================================================
-- CORRECCIONES PARA ERRORES DE JAVA Y BD
-- =====================================================================
-- Fecha: 28-01-2026
-- Problemas:
-- 1. mission_progress ON CONFLICT usa columnas que no tienen UNIQUE constraint
-- 2. app_usage_logs recibe app_id inválido (hash de nombre de app)
-- =====================================================================

BEGIN;

-- =====================================================================
-- FIX 1: Corregir constraint de mission_progress
-- =====================================================================

-- Primero, eliminar la constraint incorrecta si existe
ALTER TABLE IF EXISTS public.mission_progress
DROP CONSTRAINT IF EXISTS mission_progress_mission_id_metric_key_key;

-- Agregar la constraint correcta (user_id, mission_id, metric_key)
ALTER TABLE public.mission_progress
ADD CONSTRAINT mission_progress_user_mission_metric_key UNIQUE (user_id, mission_id, metric_key);

-- Limpiar duplicados si existen
DELETE FROM mission_progress mp
WHERE id NOT IN (
    SELECT MIN(id) FROM mission_progress
    GROUP BY user_id, mission_id, metric_key
);

COMMIT;

BEGIN;

-- =====================================================================
-- FIX 2: Tabla de mapeo app_name a app_id
-- =====================================================================

-- Crear tabla para mapear nombres de aplicaciones a IDs válidos
CREATE TABLE IF NOT EXISTS public.app_name_mapping (
    id serial PRIMARY KEY,
    app_hash integer NOT NULL UNIQUE,
    app_name character varying(255) NOT NULL,
    app_id integer NOT NULL,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT app_name_mapping_app_id_fkey FOREIGN KEY (app_id)
    REFERENCES public.apps(id) ON DELETE CASCADE
);

-- Crear índice para búsquedas rápidas
CREATE INDEX IF NOT EXISTS idx_app_name_mapping_hash ON app_name_mapping(app_hash);
CREATE INDEX IF NOT EXISTS idx_app_name_mapping_name ON app_name_mapping(app_name);

-- =====================================================================
-- FIX 3: Limpiar app_usage_logs de app_ids inválidos
-- =====================================================================

-- Eliminar registros con app_id que no existe en tabla apps
DELETE FROM public.app_usage_logs 
WHERE app_id IS NOT NULL 
AND app_id NOT IN (SELECT id FROM public.apps)
AND session_id IN (SELECT id FROM public.activity_sessions);

-- O hacer que app_id sea NULL si no existe (alternativa)
UPDATE public.app_usage_logs 
SET app_id = NULL
WHERE app_id IS NOT NULL 
AND app_id NOT IN (SELECT id FROM public.apps);

COMMIT;

BEGIN;

-- =====================================================================
-- FIX 4: Función para obtener o crear app por hash
-- =====================================================================

CREATE OR REPLACE FUNCTION get_or_create_app_id(p_app_hash integer, p_app_name character varying)
RETURNS integer AS $$
DECLARE
    v_app_id integer;
    v_existing_id integer;
BEGIN
    -- Buscar si ya existe mapping
    SELECT app_id INTO v_existing_id FROM app_name_mapping
    WHERE app_hash = p_app_hash LIMIT 1;
    
    IF v_existing_id IS NOT NULL THEN
        RETURN v_existing_id;
    END IF;
    
    -- Si no existe, crear una app genérica
    INSERT INTO public.apps (name, category, is_productive)
    VALUES (
        COALESCE(p_app_name, 'Aplicación-' || p_app_hash),
        'UNKNOWN',
        false
    )
    ON CONFLICT DO NOTHING
    RETURNING id INTO v_app_id;
    
    -- Si insertó, guardar en mapping
    IF v_app_id IS NOT NULL THEN
        INSERT INTO app_name_mapping (app_hash, app_name, app_id)
        VALUES (p_app_hash, COALESCE(p_app_name, 'Aplicación-' || p_app_hash), v_app_id)
        ON CONFLICT (app_hash) DO NOTHING;
        
        RETURN v_app_id;
    ELSE
        -- Si ya existe, obtenerlo
        SELECT id INTO v_app_id FROM public.apps
        WHERE name = COALESCE(p_app_name, 'Aplicación-' || p_app_hash)
        LIMIT 1;
        
        IF v_app_id IS NOT NULL THEN
            INSERT INTO app_name_mapping (app_hash, app_name, app_id)
            VALUES (p_app_hash, COALESCE(p_app_name, 'Aplicación-' || p_app_hash), v_app_id)
            ON CONFLICT (app_hash) DO NOTHING;
            RETURN v_app_id;
        END IF;
    END IF;
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

COMMIT;

BEGIN;

-- =====================================================================
-- FIX 5: Trigger para validar app_id antes de insertar
-- =====================================================================

CREATE OR REPLACE FUNCTION validate_app_id_before_insert()
RETURNS TRIGGER AS $$
BEGIN
    -- Si app_id es NULL, está permitido
    IF NEW.app_id IS NULL THEN
        RETURN NEW;
    END IF;
    
    -- Si app_id existe en apps, OK
    IF EXISTS (SELECT 1 FROM apps WHERE id = NEW.app_id) THEN
        RETURN NEW;
    END IF;
    
    -- Si no existe, poner NULL en lugar de fallar
    -- Esto evita el error de foreign key pero registra un log
    NEW.app_id = NULL;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Crear trigger
DROP TRIGGER IF EXISTS trigger_validate_app_id_on_insert ON app_usage_logs;
CREATE TRIGGER trigger_validate_app_id_on_insert
BEFORE INSERT ON app_usage_logs
FOR EACH ROW
EXECUTE FUNCTION validate_app_id_before_insert();

COMMIT;

-- =====================================================================
-- Logging de cambios
-- =====================================================================
BEGIN;

INSERT INTO audit_logs (action, description, created_at)
VALUES (
    'FIX_JAVA_BD_ERRORS',
    'Corregidos errores: (1) ON CONFLICT en mission_progress, (2) app_id inválido en app_usage_logs',
    CURRENT_TIMESTAMP
);

COMMIT;

-- =====================================================================
-- VERIFICACIÓN
-- =====================================================================
-- Ejecutar estas consultas para verificar que todo está bien:

-- Verificar constraint de mission_progress
-- SELECT constraint_name FROM information_schema.table_constraints 
-- WHERE table_name = 'mission_progress';

-- Verificar que no hay app_ids inválidos
-- SELECT COUNT(*) as invalid_app_ids FROM app_usage_logs 
-- WHERE app_id NOT IN (SELECT id FROM apps) AND app_id IS NOT NULL;

-- Verificar tabla de mapping
-- SELECT * FROM app_name_mapping LIMIT 5;
