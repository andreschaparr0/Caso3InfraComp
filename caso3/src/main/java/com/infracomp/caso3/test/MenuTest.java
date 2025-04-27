package com.infracomp.caso3.test;

import com.infracomp.caso3.server.Server;
import com.infracomp.caso3.Main;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MenuTest {
    
    public static void main(String[] args) {
        // Si hay argumentos, ejecutar modo directo
        if (args.length > 0) {
            try {
                int option = Integer.parseInt(args[0]);
                switch (option) {
                    case 1:
                        runSequentialTest();
                        break;
                    case 2:
                        int numClients = args.length > 1 ? Integer.parseInt(args[1]) : 4;
                        runConcurrentTest(numClients);
                        break;
                    case 6:
                        runAllTests();
                        break;
                    default:
                        System.out.println("Opción inválida. Use 1 para secuencial, 2 para concurrente, o 6 para todas las pruebas.");
                        break;
                }
                return;
            } catch (NumberFormatException e) {
                System.out.println("Argumento inválido. Iniciando modo interactivo.");
            }
        }
        
        // Modo interactivo
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            printMenu();
            int option = getOption(scanner);
            
            switch (option) {
                case 1:
                    runSequentialTest();
                    break;
                case 2:
                    runConcurrentTest(4);
                    break;
                case 3:
                    runConcurrentTest(16);
                    break;
                case 4:
                    runConcurrentTest(32);
                    break;
                case 5:
                    runConcurrentTest(64);
                    break;
                case 6:
                    runAllTests();
                    break;
                case 7:
                    running = false;
                    System.out.println("Saliendo del programa...");
                    break;
                default:
                    System.out.println("Opción inválida. Intente de nuevo.");
                    break;
            }
            
            if (running && option >= 1 && option <= 6) {
                System.out.println("\nPresione ENTER para volver al menú...");
                scanner.nextLine();
            }
        }
        
        scanner.close();
    }
    
    private static void printMenu() {
        System.out.println("\n======================================================");
        System.out.println("       CASO 3 - INFRAESTRUCTURA COMPUTACIONAL");
        System.out.println("======================================================");
        System.out.println();
        System.out.println("Seleccione una opción:");
        System.out.println();
        System.out.println("[1] Prueba secuencial (32 consultas)");
        System.out.println("[2] Prueba concurrente con 4 clientes");
        System.out.println("[3] Prueba concurrente con 16 clientes");
        System.out.println("[4] Prueba concurrente con 32 clientes");
        System.out.println("[5] Prueba concurrente con 64 clientes");
        System.out.println("[6] Ejecutar todas las pruebas");
        System.out.println("[7] Salir");
        System.out.println();
    }
    
    private static int getOption(Scanner scanner) {
        System.out.print("Ingrese su opción: ");
        while (!scanner.hasNextInt()) {
            System.out.println("Por favor ingrese un número válido.");
            System.out.print("Ingrese su opción: ");
            scanner.next();
        }
        int option = scanner.nextInt();
        scanner.nextLine(); // Consume the remaining newline
        return option;
    }
    
    private static void runSequentialTest() {
        Server server = null;
        ExecutorService serverExecutor = null;
        
        try {
            System.out.println("\n=== Iniciando prueba secuencial (32 consultas) ===");
            
            // Iniciar servidor
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
            
            // Ejecutar prueba secuencial
            ConcurrentTest test = new ConcurrentTest();
            test.runSequentialTest();
            
        } catch (Exception e) {
            System.err.println("Error en prueba secuencial: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cerrar servidor
            shutdownServer(server, serverExecutor);
        }
    }
    
    private static void runConcurrentTest(int numClients) {
        Server server = null;
        ExecutorService serverExecutor = null;
        
        try {
            System.out.println("\n=== Iniciando prueba concurrente con " + numClients + " clientes ===");
            
            // Iniciar servidor
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
            
            // Ejecutar prueba concurrente
            ConcurrentTest test = new ConcurrentTest();
            test.runConcurrentTest(numClients);
            
        } catch (Exception e) {
            System.err.println("Error en prueba concurrente: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cerrar servidor
            shutdownServer(server, serverExecutor);
        }
    }
    
    private static void runAllTests() {
        try {
            System.out.println("\n=== Ejecutando todas las pruebas ===");
            Main.main(new String[0]);
        } catch (Exception e) {
            System.err.println("Error ejecutando todas las pruebas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void shutdownServer(Server server, ExecutorService serverExecutor) {
        System.out.println("\nCerrando servidor...");
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
        
        System.out.println("Prueba completada.");
    }
} 