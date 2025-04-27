package com.infracomp.caso3.test;

import com.infracomp.caso3.client.Client;
import com.infracomp.caso3.server.Server;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.io.IOException;

public class ConcurrentTest {
    private static final int SEQUENTIAL_REQUESTS = 32;
    private static final int[] CONCURRENT_CLIENTS = {4, 16, 32, 64};

    // Clase para métricas de tiempo
    private static class TimeMetrics {
        private final List<Long> signTimes = new ArrayList<>();
        private final List<Long> encryptTimes = new ArrayList<>();
        private final List<Long> verifyTimes = new ArrayList<>();

        public void addMetrics(long sign, long encrypt, long verify) {
            signTimes.add(sign);
            encryptTimes.add(encrypt);
            verifyTimes.add(verify);
        }

        public void printStatistics(String testName) {
            System.out.println("\nEstadísticas para " + testName + ":");
            System.out.println("Tiempo de Firma:");
            printStats(signTimes);
            System.out.println("Tiempo de Cifrado:");
            printStats(encryptTimes);
            System.out.println("Tiempo de Verificación:");
            printStats(verifyTimes);
        }

        private void printStats(List<Long> times) {
            if (times.isEmpty()) return;
            
            long min = times.stream().min(Long::compare).get();
            long max = times.stream().max(Long::compare).get();
            double avg = times.stream().mapToLong(Long::longValue).average().getAsDouble();
            
            System.out.printf("  Mínimo: %d ns\n", min);
            System.out.printf("  Máximo: %d ns\n", max);
            System.out.printf("  Promedio: %.2f ns\n", avg);
        }
    }

    // Cliente que ejecuta múltiples consultas secuenciales
    private static class SequentialClient implements Runnable {
        private final int numRequests;
        private final TimeMetrics metrics;

        public SequentialClient(int numRequests) {
            this.numRequests = numRequests;
            this.metrics = new TimeMetrics();
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < numRequests; i++) {
                    CountDownLatch latch = new CountDownLatch(1);
                    Client client = new Client(i + 1, latch);
                    client.start();
                    latch.await();
                    metrics.addMetrics(
                        client.getSignTime(),
                        client.getEncryptTime(),
                        client.getVerifyTime()
                    );
                    System.out.println("Cliente secuencial - Consulta " + (i + 1) + " completada");
                }
            } catch (Exception e) {
                System.err.println("Error en cliente secuencial: " + e.getMessage());
            }
        }

        public TimeMetrics getMetrics() {
            return metrics;
        }
    }

    // Cliente para pruebas concurrentes
    private static class ConcurrentClient implements Runnable {
        private final int clientId;
        private final TimeMetrics metrics;

        public ConcurrentClient(int id) {
            this.clientId = id;
            this.metrics = new TimeMetrics();
        }

        @Override
        public void run() {
            try {
                CountDownLatch latch = new CountDownLatch(1);
                Client client = new Client(clientId, latch);
                client.start();
                latch.await();
                metrics.addMetrics(
                    client.getSignTime(),
                    client.getEncryptTime(),
                    client.getVerifyTime()
                );
                System.out.println("Cliente concurrente " + clientId + " completado");
            } catch (Exception e) {
                System.err.println("Error en cliente concurrente " + clientId + ": " + e.getMessage());
            }
        }

        public TimeMetrics getMetrics() {
            return metrics;
        }
    }

    public void runSequentialTest() {
        System.out.println("\nIniciando prueba secuencial (32 consultas)...");
        SequentialClient client = new SequentialClient(SEQUENTIAL_REQUESTS);
        Thread clientThread = new Thread(client);
        clientThread.start();
        try {
            clientThread.join();
            client.getMetrics().printStatistics("Prueba Secuencial");
        } catch (InterruptedException e) {
            System.err.println("Prueba secuencial interrumpida");
        }
    }

    public void runConcurrentTest(int numClients) {
        System.out.println("\nIniciando prueba concurrente con " + numClients + " clientes...");
        ExecutorService executor = Executors.newFixedThreadPool(numClients);
        List<Future<?>> futures = new ArrayList<>();
        List<ConcurrentClient> clients = new ArrayList<>();

        // Iniciar todos los clientes concurrentes
        for (int i = 0; i < numClients; i++) {
            ConcurrentClient client = new ConcurrentClient(i + 1);
            clients.add(client);
            futures.add(executor.submit(client));
        }

        // Esperar a que todos los clientes terminen
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                System.err.println("Error esperando cliente: " + e.getMessage());
            }
        }

        // Recolectar y combinar métricas
        TimeMetrics combinedMetrics = new TimeMetrics();
        for (ConcurrentClient client : clients) {
            TimeMetrics clientMetrics = client.getMetrics();
            // Combinar métricas de cada cliente
            for (int i = 0; i < clientMetrics.signTimes.size(); i++) {
                combinedMetrics.addMetrics(
                    clientMetrics.signTimes.get(i),
                    clientMetrics.encryptTimes.get(i),
                    clientMetrics.verifyTimes.get(i)
                );
            }
        }

        combinedMetrics.printStatistics("Prueba Concurrente (" + numClients + " clientes)");

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    public static void main(String[] args) {
        ConcurrentTest test = new ConcurrentTest();

        // Iniciar el servidor en un thread separado
        new Thread(() -> {
            Server server = new Server();
            try {
                server.start();
            } catch (IOException e) {
                System.err.println("Error iniciando servidor: " + e.getMessage());
            }
        }).start();

        // Esperar a que el servidor esté listo
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            System.err.println("Error esperando inicio del servidor");
            return;
        }

        // Ejecutar prueba secuencial
        test.runSequentialTest();

        // Ejecutar pruebas concurrentes
        for (int numClients : CONCURRENT_CLIENTS) {
            test.runConcurrentTest(numClients);
        }
    }
} 