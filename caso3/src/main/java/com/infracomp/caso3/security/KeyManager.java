package com.infracomp.caso3.security;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

public class KeyManager {
    private static final String KEY_DIRECTORY = System.getProperty("user.dir") + "/src/main/resources/keys/";
    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String PRIVATE_KEY_FILE = "private.key";

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    public static void saveKeyPair(KeyPair keyPair) throws Exception {
        // Crear directorio si no existe
        Files.createDirectories(Paths.get(KEY_DIRECTORY));

        // Guardar llave pública
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        String publicKeyStr = Base64.getEncoder().encodeToString(publicKeyBytes);
        Files.write(Paths.get(KEY_DIRECTORY + PUBLIC_KEY_FILE), publicKeyStr.getBytes());

        // Guardar llave privada
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
        String privateKeyStr = Base64.getEncoder().encodeToString(privateKeyBytes);
        Files.write(Paths.get(KEY_DIRECTORY + PRIVATE_KEY_FILE), privateKeyStr.getBytes());
    }

    public static PublicKey loadPublicKey() throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(KEY_DIRECTORY + PUBLIC_KEY_FILE));
        byte[] decodedKey = Base64.getDecoder().decode(new String(keyBytes));
        
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static PrivateKey loadPrivateKey() throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(KEY_DIRECTORY + PRIVATE_KEY_FILE));
        byte[] decodedKey = Base64.getDecoder().decode(new String(keyBytes));
        
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public static KeyPair loadKeyPair() throws Exception {
        PublicKey publicKey = loadPublicKey();
        PrivateKey privateKey = loadPrivateKey();
        return new KeyPair(publicKey, privateKey);
    }
} 