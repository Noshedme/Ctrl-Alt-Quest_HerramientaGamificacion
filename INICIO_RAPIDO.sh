#!/bin/bash
# =====================================================
# SCRIPT DE INICIO RÁPIDO - Sistema Misiones en Tiempo Real
# =====================================================
# Este script ejecuta todos los pasos necesarios para
# compilar, setupear y testear el sistema.
# =====================================================

echo "🎮 CTRL-ALT-QUEST: Sistema de Misiones en Tiempo Real"
echo "=================================================="
echo ""

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# PASO 1: Compilar
echo -e "${BLUE}[PASO 1/3]${NC} Compilando proyecto Maven..."
echo "Ubicación: c:\Users\Usuario\Desktop\proyecto\Ctrl-Alt-Quest_HerramientaGamificacion\frontend"
echo ""

cd "c:\Users\Usuario\Desktop\proyecto\Ctrl-Alt-Quest_HerramientaGamificacion\frontend" || exit

if mvn clean compile; then
    echo -e "${GREEN}✅ Compilación exitosa${NC}"
else
    echo -e "\033[0;31m❌ Error en compilación${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}⏳ Espera a que termine Maven...${NC}"
sleep 2

# PASO 2: Instrucciones para SQL
echo ""
echo -e "${BLUE}[PASO 2/3]${NC} Configurar Base de Datos"
echo -e "${YELLOW}Instrucciones manuales:${NC}"
echo ""
echo "1. Abre pgAdmin 4"
echo "2. Query Tool → Nueva Query"
echo "3. Copiar contenido de: SISTEMA_MISIONES_TIEMPO_REAL.sql"
echo "4. Pegar en Query Tool"
echo "5. Ejecutar (Ctrl+Enter)"
echo "6. Esperar: 'SETUP COMPLETADO'"
echo ""
echo -e "${YELLOW}Ubicación del archivo:${NC}"
echo "c:\Users\Usuario\Desktop\proyecto\Ctrl-Alt-Quest_HerramientaGamificacion\SISTEMA_MISIONES_TIEMPO_REAL.sql"
echo ""

# PASO 3: Lanzar aplicación
echo -e "${BLUE}[PASO 3/3]${NC} Ejecutar Aplicación"
echo ""
echo "Para iniciar la aplicación:"
echo "1. IDE → Run (o F5)"
echo "2. O desde terminal: java -jar target/ctrlaltquest.jar"
echo ""

# Mostrar archivos de referencia
echo -e "${GREEN}📚 Documentación Disponible:${NC}"
echo ""
echo "├─ 📖 GUIA_IMPLEMENTACION_MISIONES_TIEMPO_REAL.md"
echo "│  └─ Guía completa paso a paso"
echo "│"
echo "├─ 🎯 EJEMPLOS_PRACTICOS_TIEMPO_REAL.md"
echo "│  └─ 5 escenarios con timeline de eventos"
echo "│"
echo "├─ 🧪 QUERIES_TESTING_RAPIDO.sql"
echo "│  └─ Queries para verificar en pgAdmin"
echo "│"
echo "├─ 📋 README_SISTEMA_TIEMPO_REAL.md"
echo "│  └─ Resumen ejecutivo"
echo "│"
echo "└─ ✅ VERIFICACION_FINAL_SISTEMA.md"
echo "   └─ Checklist de verificación"
echo ""

# Testing rápido
echo -e "${YELLOW}🧪 TESTING RÁPIDO${NC}"
echo ""
echo "Una vez compilado, puedes testear con estas queries:"
echo ""
echo "1. Ver progreso de misiones (ejecutar cada 5 seg):"
echo "   SELECT mp.current_value, mp.progress_percentage"
echo "   FROM mission_progress mp WHERE user_id = 1 LIMIT 1;"
echo ""
echo "2. Ver XP acumulado (ejecutar cada 10 seg):"
echo "   SELECT current_xp, total_xp, coins FROM users WHERE id = 1;"
echo ""
echo "3. Ver logros desbloqueados:"
echo "   SELECT COUNT(*) FROM user_achievements WHERE user_id = 1;"
echo ""

# Información de sistema
echo -e "${BLUE}ℹ️ INFORMACIÓN DEL SISTEMA${NC}"
echo ""
echo "Componentes creados:"
echo "├─ RewardsService.java (297 líneas)"
echo "├─ MissionProgressService.java (299 líneas)"
echo "├─ AchievementsDAO.java (316 líneas)"
echo ""
echo "Servicios modificados:"
echo "└─ ActivityMonitorService.java"
echo ""

# Datos de configuración
echo -e "${GREEN}🎮 DATOS DE CONFIGURACIÓN${NC}"
echo ""
echo "Misiones: 8 (diversas categorías)"
echo "Logros: 18 (variados y progresivos)"
echo "XP base: 1 por segundo (productivo)"
echo "Level-up: Cada (nivel+1)*100 XP"
echo "Bonus: 50 monedas por level-up"
echo ""

# Estados
echo -e "${GREEN}✅ ESTADO${NC}"
echo ""
echo "Compilación: ✅ COMPLETADA"
echo "SQL Setup: ⏳ MANUAL (pgAdmin)"
echo "Ejecución: ⏳ PRÓXIMO PASO"
echo "Testing: 🧪 VER ARRIBA"
echo ""

echo -e "${GREEN}=================================================="
echo "🚀 ¡Listo para usar!"
echo "================================================${NC}"
echo ""
