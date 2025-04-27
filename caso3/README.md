# Caso 3 - Infraestructura Computacional

Este proyecto implementa la solución al Caso 3 sobre canales seguros para consulta de servicios en una aerolínea.

## Requisitos

- Java 8 o superior
- Windows (para ejecutar los scripts .bat)

## Instrucciones de Uso

### Opción 1: Usando el Menú Java (Recomendado)

1. Ejecuta el archivo `run_menu.bat` haciendo doble clic sobre él o desde la línea de comandos:
   ```
   .\run_menu.bat
   ```

2. Se abrirá un menú interactivo con las siguientes opciones:
   - Prueba secuencial (32 consultas)
   - Prueba concurrente con 4 clientes
   - Prueba concurrente con 16 clientes
   - Prueba concurrente con 32 clientes
   - Prueba concurrente con 64 clientes
   - Ejecutar todas las pruebas
   - Salir

3. Selecciona la opción deseada ingresando el número correspondiente y presionando Enter.

### Opción 2: Ejecutar Directamente desde la Línea de Comandos

Si prefieres ejecutar directamente desde la línea de comandos, puedes usar los siguientes comandos después de compilar las clases:

1. Compilar todas las clases:
   ```
   javac -d . src\main\java\com\infracomp\caso3\security\*.java
   javac -d . src\main\java\com\infracomp\caso3\server\*.java
   javac -d . src\main\java\com\infracomp\caso3\client\*.java
   javac -d . src\main\java\com\infracomp\caso3\Main.java
   javac -d . src\main\java\com\infracomp\caso3\test\*.java
   ```

2. Ejecutar la prueba secuencial:
   ```
   java com.infracomp.caso3.test.MenuTest 1
   ```

3. Ejecutar prueba concurrente con X clientes (donde X es 4, 16, 32 o 64):
   ```
   java com.infracomp.caso3.test.MenuTest 2 X
   ```

4. Ejecutar todas las pruebas:
   ```
   java com.infracomp.caso3.Main
   ```

## Estructura del Proyecto

- `src/main/java/com/infracomp/caso3/security/`: Clases para manejo de seguridad y criptografía
- `src/main/java/com/infracomp/caso3/server/`: Implementación del servidor
- `src/main/java/com/infracomp/caso3/client/`: Implementación del cliente
- `src/main/java/com/infracomp/caso3/test/`: Clases para pruebas
- `src/main/java/com/infracomp/caso3/Main.java`: Clase principal para ejecutar todas las pruebas

## Notas

- Cada prueba iniciará automáticamente un servidor en segundo plano y luego lo cerrará al finalizar.
- Las pruebas recolectan datos sobre tiempos de firma, cifrado y verificación.
- Los resultados de las pruebas se muestran en la consola. 