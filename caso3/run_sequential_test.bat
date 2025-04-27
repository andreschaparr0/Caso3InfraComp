@echo off
echo Compiling the application...
javac -d . src\main\java\com\infracomp\caso3\security\*.java
javac -d . src\main\java\com\infracomp\caso3\server\*.java
javac -d . src\main\java\com\infracomp\caso3\client\*.java
javac -d . src\main\java\com\infracomp\caso3\test\SequentialTest.java

echo Creating sequential test class...
echo package com.infracomp.caso3.test; > src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo. >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo import com.infracomp.caso3.server.Server; >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo import java.util.concurrent.*; >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo import com.infracomp.caso3.test.ConcurrentTest; >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo. >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo public class SequentialTest { >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo     public static void main(String[] args) { >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo         Server server = null; >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo         ExecutorService serverExecutor = null; >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo. >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo         try { >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             serverExecutor = Executors.newSingleThreadExecutor(); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             final Server serverInstance = new Server(); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             server = serverInstance; >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo. >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             serverExecutor.submit(() -^> { >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo                 try { >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo                     serverInstance.start(); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo                 } catch (Exception e) { >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo                     System.err.println("Error iniciando servidor: " + e.getMessage()); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo                 } >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             }); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo. >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             Thread.sleep(2000); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo. >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             ConcurrentTest test = new ConcurrentTest(); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             System.out.println("\n=== Ejecutando prueba secuencial ==="); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             test.runSequentialTest(); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo. >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo         } catch (Exception e) { >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             System.err.println("Error en pruebas: " + e.getMessage()); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             e.printStackTrace(); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo         } finally { >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             if (server != null) { >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo                 server.shutdown(); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             } >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo. >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             if (serverExecutor != null) { >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo                 serverExecutor.shutdownNow(); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             } >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo. >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             System.out.println("Prueba secuencial completada."); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo             System.exit(0); >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo         } >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo     } >> src\main\java\com\infracomp\caso3\test\SequentialTest.java
echo } >> src\main\java\com\infracomp\caso3\test\SequentialTest.java

javac -d . src\main\java\com\infracomp\caso3\test\SequentialTest.java

echo Running the sequential test...
java com.infracomp.caso3.test.SequentialTest

echo Test completed.
pause 