package com.infracomp.caso3.test;

import com.infracomp.caso3.client.Client;
import com.infracomp.caso3.server.Server;
import java.util.concurrent.*;

public class SimpleTest {
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
            System.out.println("Esperando 2 segundos para que el servidor se inicie...");
            Thread.sleep(2000);

            // Probar un solo cliente
            System.out.println("Iniciando cliente de prueba...");
            CountDownLatch latch = new CountDownLatch(1);
            Client client = new Client(1, latch);
            client.start();
            
            // Esperar a que el cliente termine
            latch.await(10, TimeUnit.SECONDS);
            
            System.out.println("Prueba completada, cerrando servidor...");
            
        } catch (Exception e) {
            System.err.println("Error en la prueba: " + e.getMessage());
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
            
            System.out.println("Prueba simple completada.");
            System.exit(0);  // Asegurar que la aplicación termine
        }
    }
} 