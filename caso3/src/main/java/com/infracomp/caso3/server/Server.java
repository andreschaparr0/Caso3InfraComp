package com.infracomp.caso3.server;

import java.io.*;
import java.net.*;
import java.security.*;
import javax.crypto.SecretKey;
import com.infracomp.caso3.security.CryptoManager;
import com.infracomp.caso3.security.SecurityUtils;
import com.infracomp.caso3.security.KeyManager;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 3000;
    private ServerSocket serverSocket;
    private KeyPair serverKeyPair;
    private ServiceTable serviceTable;
    private ExecutorService threadPool;

    public Server() {
        try {
            // Solo generar el par de llaves
            this.serverKeyPair = KeyManager.generateKeyPair();
            // Guardar las llaves para que los clientes puedan usarlas
            KeyManager.saveKeyPair(this.serverKeyPair);
            System.out.println("Llaves del servidor generadas y guardadas exitosamente");
            
            this.serviceTable = new ServiceTable();
            this.threadPool = Executors.newCachedThreadPool();
        } catch (Exception e) {
            System.err.println("Error inicializando servidor: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor iniciado en puerto " + PORT);

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nueva conexión aceptada de " + clientSocket.getInetAddress());

                // Crear y empezar el delegado como thread
                ServerDelegate delegate = new ServerDelegate(clientSocket, serverKeyPair, serviceTable);
                delegate.start();
            } catch (Exception e) {
                System.err.println("Error aceptando conexión: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        try {
            System.out.println("Cerrando servidor...");
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Socket del servidor cerrado");
            }
            threadPool.shutdown();
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
                System.out.println("Thread pool cerrado forzosamente");
            } else {
                System.out.println("Thread pool cerrado correctamente");
            }
        } catch (Exception e) {
            System.err.println("Error cerrando servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        try {
            // Agregar un hook de apagado para cerrar correctamente
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Recibida señal de apagado. Cerrando servidor...");
                server.shutdown();
            }));
            
            server.start();
        } catch (Exception e) {
            System.err.println("Error iniciando servidor: " + e.getMessage());
            e.printStackTrace();
            server.shutdown();
            System.exit(1);
        }
    }
} 