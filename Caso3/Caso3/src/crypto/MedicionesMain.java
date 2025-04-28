package src.crypto;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class MedicionesMain {
    private static Servidor servidorActual = null;
    private static FileWriter csvWriter;

    public static void main(String[] args) throws Exception {
        // Crear archivo CSV
        try {
            csvWriter = new FileWriter("mediciones.csv");
            csvWriter.write("Escenario,NumClientes,Cliente,Operacion,Tiempo(ms)\n");
            csvWriter.flush();
            System.out.println("Archivo CSV creado correctamente");
        } catch (IOException e) {
            System.err.println("Error al crear el archivo CSV:");
            e.printStackTrace();
            return;
        }

        System.out.println("=== Iniciando mediciones ===\n");

        // Escenario 1: Un servidor y un cliente iterativo (32 consultas secuenciales)
        System.out.println("Escenario 1: Un servidor y un cliente iterativo (32 consultas secuenciales)");
        System.out.println("----------------------------------------------------------------");
        
        // Iniciar servidor
        Thread servidorThread = new Thread(() -> {
            try {
                servidorActual = new Servidor();
                servidorActual.iniciar();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        servidorThread.start();

        // Esperar a que el servidor esté listo
        Thread.sleep(1000);

        // Cliente iterativo
        System.out.println("\nEjecutando 32 consultas secuenciales...\n");
        for (int i = 0; i < 32; i++) {
            System.out.println("\nConsulta " + (i + 1) + " de 32:");
            ClienteMedicion cliente = new ClienteMedicion(i + 1);
            cliente.iniciar();
            // Guardar tiempos en CSV
            guardarTiemposEnCSV("Secuencial", 1, i + 1, cliente.getTiempos());
        }

        // Cerrar servidor actual
        if (servidorActual != null) {
            servidorActual.cerrar();
            servidorThread.join();
        }

        // Escenario 2: Servidor y clientes concurrentes
        int[] numClientes = {4, 16, 32, 64};
        
        for (int num : numClientes) {
            System.out.println("\n\n========================================================");
            System.out.println("Escenario 2: Servidor con " + num + " clientes concurrentes");
            System.out.println("========================================================\n");
            
            // Esperar 5 segundos para asegurar que el puerto esté liberado
            Thread.sleep(5000);
            
            // Iniciar nuevo servidor
            servidorThread = new Thread(() -> {
                try {
                    servidorActual = new Servidor();
                    servidorActual.iniciar();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            servidorThread.start();

            // Esperar a que el servidor esté listo
            Thread.sleep(1000);

            // Crear y ejecutar clientes concurrentes
            Thread[] hilos = new Thread[num];
            for (int i = 0; i < num; i++) {
                final int clienteId = i + 1;
                hilos[i] = new Thread(() -> {
                    try {
                        System.out.println("\n[Cliente " + clienteId + "] Iniciando consulta");
                        ClienteMedicion clienteConcurrente = new ClienteMedicion(clienteId);
                        clienteConcurrente.iniciar();
                        System.out.println("[Cliente " + clienteId + "] Consulta completada");
                        // Guardar tiempos en CSV
                        guardarTiemposEnCSV("Concurrente", num, clienteId, clienteConcurrente.getTiempos());
                    } catch (Exception e) {
                        System.out.println("[Cliente " + clienteId + "] Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                hilos[i].start();
            }

            // Esperar a que todos los clientes terminen
            for (Thread hilo : hilos) {
                hilo.join();
            }
            
            // Cerrar servidor actual
            if (servidorActual != null) {
                servidorActual.cerrar();
                servidorThread.join();
            }
        }

        // Cerrar archivo CSV
        try {
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("\n=== Mediciones completadas ===");
        System.out.println("Los datos se han guardado en mediciones.csv");
    }

    private static void guardarTiemposEnCSV(String escenario, int numClientes, int clienteId, Map<String, Double> tiempos) {
        try {
            if (tiempos == null || tiempos.isEmpty()) {
                return;
            }

            for (Map.Entry<String, Double> entry : tiempos.entrySet()) {
                String linea = String.format("%s,%d,%d,%s,%.4f\n",
                    escenario, numClientes, clienteId, entry.getKey(), entry.getValue());
                csvWriter.write(linea);
                csvWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("Error al escribir en el archivo CSV:");
            e.printStackTrace();
        }
    }
} 