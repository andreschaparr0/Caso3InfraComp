package com.infracomp.caso3.test;

import com.infracomp.caso3.client.Client;
import com.infracomp.caso3.server.Server;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

public class TestController {
    private final Server server;
    private final ExecutorService serverExecutor;
    private final List<TestResult> results;
    private static final int SEQUENTIAL_REQUESTS = 32;
    private static final int[] CONCURRENT_CLIENTS = {4, 16, 32, 64};

    public TestController() {
        this.server = new Server();
        this.serverExecutor = Executors.newSingleThreadExecutor();
        this.results = new ArrayList<>();
    }

    public void startServer() {
        serverExecutor.submit(() -> {
            try {
                server.start();
            } catch (Exception e) {
                System.err.println("Error en servidor: " + e.getMessage());
            }
        });
    }

    public void runSequentialTest() throws InterruptedException {
        System.out.println("\nIniciando prueba secuencial (32 consultas)...");
        CountDownLatch latch = new CountDownLatch(1);
        SequentialClient client = new SequentialClient(SEQUENTIAL_REQUESTS, latch);
        client.start();
        latch.await();
        results.add(new TestResult("Secuencial", 1, client.getMetrics()));
    }

    public void runConcurrentTest(int numClients) throws InterruptedException {
        System.out.println("\nIniciando prueba concurrente con " + numClients + " clientes...");
        CountDownLatch latch = new CountDownLatch(numClients);
        List<Client> clients = new ArrayList<>();
        TimeMetrics combinedMetrics = new TimeMetrics();

        // Crear y empezar todos los clientes
        for (int i = 0; i < numClients; i++) {
            Client client = new Client(i + 1, latch);
            clients.add(client);
            client.start();
        }

        // Esperar a que todos los clientes terminen
        latch.await();

        // Recolectar métricas
        for (Client client : clients) {
            combinedMetrics.addMetrics(
                client.getSignTime(),
                client.getEncryptTime(),
                client.getVerifyTime()
            );
        }

        results.add(new TestResult("Concurrente", numClients, combinedMetrics));
    }

    public void runAllTests() throws InterruptedException {
        startServer();
        Thread.sleep(2000); // Esperar a que el servidor inicie

        runSequentialTest();

        for (int numClients : CONCURRENT_CLIENTS) {
            runConcurrentTest(numClients);
        }

        printResults();
        shutdown();
    }

    private void printResults() {
        System.out.println("\n=== Resultados de las Pruebas ===");
        for (TestResult result : results) {
            System.out.println("\n" + result.getTestName() + " (" + result.getNumClients() + " clientes):");
            result.getMetrics().printStatistics(result.getTestName());
        }
    }

    public void shutdown() {
        serverExecutor.shutdown();
        try {
            if (!serverExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                serverExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            serverExecutor.shutdownNow();
        }
    }

    private static class TestResult {
        private final String testName;
        private final int numClients;
        private final TimeMetrics metrics;

        public TestResult(String testName, int numClients, TimeMetrics metrics) {
            this.testName = testName;
            this.numClients = numClients;
            this.metrics = metrics;
        }

        public String getTestName() { return testName; }
        public int getNumClients() { return numClients; }
        public TimeMetrics getMetrics() { return metrics; }
    }

    public static void main(String[] args) {
        try {
            TestController controller = new TestController();
            controller.runAllTests();
        } catch (Exception e) {
            System.err.println("Error en pruebas: " + e.getMessage());
        }
    }
} 