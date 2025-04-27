package com.infracomp.caso3;

import com.infracomp.caso3.server.Server;
import com.infracomp.caso3.test.ConcurrentTest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        Server server = null;
        ExecutorService serverExecutor = null;
        
        try {
            // Iniciar el servidor en un thread separado
            serverExecutor = Executors.newSingleThreadExecutor();
            final Server serverInstance = new Server();
            server = serverInstance;
            
            serverExecutor.submit(() -> {
                try {
                    serverInstance.start();
                } catch (Exception e) {
                    System.err.println("Error iniciando servidor: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            // Esperar a que el servidor esté listo
            Thread.sleep(2000);

            // Ejecutar las pruebas
            ConcurrentTest test = new ConcurrentTest();
            
            // Prueba secuencial
            System.out.println("\n=== Ejecutando prueba secuencial ===");
            test.runSequentialTest();

            // Pruebas concurrentes
            System.out.println("\n=== Ejecutando pruebas concurrentes ===");
            test.runConcurrentTest(5);  // 5 clientes
            test.runConcurrentTest(10); // 10 clientes
            test.runConcurrentTest(20); // 20 clientes

        } catch (Exception e) {
            System.err.println("Error en pruebas: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cerrar el servidor
            if (server != null) {
                server.shutdown();
            }
            
            if (serverExecutor != null) {
                serverExecutor.shutdownNow();
                try {
                    if (!serverExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        System.err.println("El servidor no se cerró correctamente.");
                    }
                } catch (InterruptedException e) {
                    System.err.println("Interrupción al cerrar el executor: " + e.getMessage());
                }
            }
            
            System.out.println("Pruebas completadas.");
        }
    }
} 