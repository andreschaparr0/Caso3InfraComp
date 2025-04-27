@echo off
setlocal

REM Create build directory if it doesn't exist
if not exist build\classes\java\main mkdir build\classes\java\main

REM Compile all Java files
javac -d build\classes\java\main src\main\java\com\infracomp\caso3\security\*.java
javac -d build\classes\java\main src\main\java\com\infracomp\caso3\server\*.java
javac -d build\classes\java\main src\main\java\com\infracomp\caso3\client\*.java
javac -d build\classes\java\main src\main\java\com\infracomp\caso3\test\*.java

REM Start server in background
start /B java -cp build\classes\java\main com.infracomp.caso3.server.Server

REM Wait for server to start
timeout /t 2

REM Run tests
java -cp build\classes\java\main com.infracomp.caso3.test.ConcurrentTest

endlocal 