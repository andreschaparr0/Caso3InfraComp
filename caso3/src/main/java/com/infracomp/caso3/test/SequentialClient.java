package com.infracomp.caso3.test;

import com.infracomp.caso3.client.Client;
import java.util.concurrent.CountDownLatch;

public class SequentialClient extends Thread {
    private final int numRequests;
    private final TimeMetrics metrics;
    private final CountDownLatch latch;

    public SequentialClient(int numRequests, CountDownLatch latch) {
        this.numRequests = numRequests;
        this.metrics = new TimeMetrics();
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < numRequests; i++) {
                Client client = new Client(i + 1, new CountDownLatch(1));
                client.start();
                client.join(); // Esperar a que cada cliente termine
                metrics.addMetrics(
                    client.getSignTime(),
                    client.getEncryptTime(),
                    client.getVerifyTime()
                );
                System.out.println("Cliente secuencial - Consulta " + (i + 1) + " completada");
            }
        } catch (Exception e) {
            System.err.println("Error en cliente secuencial: " + e.getMessage());
        } finally {
            latch.countDown(); // Notificar que este cliente secuencial ha terminado
        }
    }

    public TimeMetrics getMetrics() {
        return metrics;
    }
} 