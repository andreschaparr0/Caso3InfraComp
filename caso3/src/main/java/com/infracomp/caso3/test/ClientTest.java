package com.infracomp.caso3.test;

import java.util.concurrent.CountDownLatch;
import com.infracomp.caso3.client.Client;

public class ClientTest {
    public static void main(String[] args) {
        try {
            System.out.println("Iniciando test para verificar el método readMessage() en Client");
            
            // Crear un cliente de prueba
            CountDownLatch latch = new CountDownLatch(1);
            Client client = new Client(999, latch);
            
            System.out.println("Cliente creado exitosamente, lo que indica que la llave pública se cargó correctamente");
            System.out.println("Esto verifica que el KeyManager está funcionando bien con la ruta relativa");
            
            System.out.println("La corrección del método readMessage() en lugar de receiveMessage() no se puede probar");
            System.out.println("sin un servidor en ejecución, pero la compilación exitosa confirma la corrección sintáctica.");
            
            System.out.println("Prueba completada.");
            
        } catch (Exception e) {
            System.err.println("Error en prueba: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 