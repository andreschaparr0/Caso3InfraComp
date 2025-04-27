package com.infracomp.caso3.test;

import com.infracomp.caso3.security.KeyManager;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.PrivateKey;

public class KeyManagerTest {
    public static void main(String[] args) {
        try {
            System.out.println("Directorio actual: " + System.getProperty("user.dir"));
            
            // Generar un par de llaves
            System.out.println("Generando par de llaves...");
            KeyPair keyPair = KeyManager.generateKeyPair();
            
            // Guardar las llaves
            System.out.println("Guardando par de llaves...");
            KeyManager.saveKeyPair(keyPair);
            
            // Cargar la llave pública
            System.out.println("Cargando llave pública...");
            PublicKey publicKey = KeyManager.loadPublicKey();
            
            // Cargar la llave privada
            System.out.println("Cargando llave privada...");
            PrivateKey privateKey = KeyManager.loadPrivateKey();
            
            // Verificar que las llaves cargadas sean iguales a las originales
            boolean publicKeyMatches = keyPair.getPublic().getEncoded().equals(publicKey.getEncoded());
            boolean privateKeyMatches = keyPair.getPrivate().getEncoded().equals(privateKey.getEncoded());
            
            System.out.println("Llave pública cargada correctamente: " + publicKeyMatches);
            System.out.println("Llave privada cargada correctamente: " + privateKeyMatches);
            
            System.out.println("Prueba completada exitosamente.");
            
        } catch (Exception e) {
            System.err.println("Error en prueba: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 