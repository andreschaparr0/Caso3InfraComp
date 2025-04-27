package com.infracomp.caso3.test;

import java.util.ArrayList;
import java.util.List;

public class TimeMetrics {
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

    public List<Long> getSignTimes() { return signTimes; }
    public List<Long> getEncryptTimes() { return encryptTimes; }
    public List<Long> getVerifyTimes() { return verifyTimes; }
} 