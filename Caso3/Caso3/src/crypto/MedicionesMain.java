package src.crypto;

public class MedicionesMain {
    private static Servidor servidorActual = null;

    public static void main(String[] args) throws Exception {
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
        ClienteMedicion cliente = new ClienteMedicion();
        for (int i = 0; i < 32; i++) {
            System.out.println("\nConsulta " + (i + 1) + " de 32:");
            cliente.iniciar();
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
                        ClienteMedicion clienteConcurrente = new ClienteMedicion() {
                            @Override
                            protected void imprimirTiempo(String operacion, double tiempo) {
                                System.out.println(String.format("[Cliente %d] %s: %.4f ms", 
                                    clienteId, operacion, tiempo));
                            }
                        };
                        clienteConcurrente.iniciar();
                        System.out.println("[Cliente " + clienteId + "] Consulta completada");
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

        System.out.println("\n=== Mediciones completadas ===");
    }
} 