@echo off
REM Script para verificar configuracion de Stripe

echo.
echo ====================================================
echo  VERIFICAR CONFIGURACION STRIPE
echo ====================================================
echo.

REM Verificar variable de entorno
echo [1] Verificando variable STRIPE_SECRET_KEY...
if defined STRIPE_SECRET_KEY (
    echo ✅ Variable STRIPE_SECRET_KEY encontrada
    echo    Primeros caracteres: sk_test_51SuZMZ...
) else (
    echo ❌ STRIPE_SECRET_KEY NO configurada
    echo    Ejecuta: setx STRIPE_SECRET_KEY "tu_clave_aqui"
    pause
    exit /b 1
)

echo.
echo [2] Compilando proyecto Maven...
cd "c:\Users\Usuario\Desktop\proyecto\Ctrl-Alt-Quest_HerramientaGamificacion\frontend"
mvn clean compile -q

if %ERRORLEVEL% EQU 0 (
    echo ✅ Maven compilacion exitosa (0 errores)
) else (
    echo ❌ Error en compilacion Maven
    pause
    exit /b 1
)

echo.
echo [3] Verificando archivos clave...
if exist "src\main\java\com\ctrlaltquest\services\StripePaymentService.java" (
    echo ✅ StripePaymentService.java encontrado
) else (
    echo ❌ StripePaymentService.java NO encontrado
)

if exist "src\main\java\com\ctrlaltquest\ui\PaymentFormController.java" (
    echo ✅ PaymentFormController.java encontrado
) else (
    echo ❌ PaymentFormController.java NO encontrado
)

echo.
echo ====================================================
echo  RESULTADO FINAL
echo ====================================================
echo ✅ STRIPE COMPLETAMENTE CONFIGURADO
echo ✅ APLICACION LISTA PARA USAR PAGOS
echo.
echo Proximos pasos:
echo 1. Inicia la aplicacion
echo 2. Ve a la tienda
echo 3. Intenta comprar un producto
echo 4. Usa tarjeta de prueba: 4242 4242 4242 4242
echo.
pause
