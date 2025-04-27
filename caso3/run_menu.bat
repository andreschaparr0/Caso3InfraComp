@echo off
echo ======================================================
echo       CASO 3 - INFRAESTRUCTURA COMPUTACIONAL
echo ======================================================
echo.
echo Compilando las clases...

REM Crear directorios si no existen
md com\infracomp\caso3\security 2>nul
md com\infracomp\caso3\server 2>nul
md com\infracomp\caso3\client 2>nul
md com\infracomp\caso3\test 2>nul

REM Compilar todas las clases
javac -d . src\main\java\com\infracomp\caso3\security\*.java
if %errorlevel% neq 0 (
    echo Error al compilar las clases de seguridad.
    goto error
)

javac -d . src\main\java\com\infracomp\caso3\server\*.java
if %errorlevel% neq 0 (
    echo Error al compilar las clases del servidor.
    goto error
)

javac -d . src\main\java\com\infracomp\caso3\client\*.java
if %errorlevel% neq 0 (
    echo Error al compilar las clases del cliente.
    goto error
)

javac -d . src\main\java\com\infracomp\caso3\Main.java
if %errorlevel% neq 0 (
    echo Error al compilar la clase principal.
    goto error
)

javac -d . src\main\java\com\infracomp\caso3\test\ConcurrentTest.java
if %errorlevel% neq 0 (
    echo Error al compilar la clase ConcurrentTest.
    goto error
)

javac -d . src\main\java\com\infracomp\caso3\test\MenuTest.java
if %errorlevel% neq 0 (
    echo Error al compilar la clase MenuTest.
    goto error
)

echo.
echo Compilación exitosa!
echo.
echo Ejecutando el menu de pruebas...
echo.

REM Ejecutar el menú
java com.infracomp.caso3.test.MenuTest
if %errorlevel% neq 0 (
    echo Error al ejecutar el menú de pruebas.
    goto error
)

goto end

:error
echo.
echo Ha ocurrido un error. Por favor revise los mensajes anteriores.
pause
exit /b 1

:end
exit /b 0 