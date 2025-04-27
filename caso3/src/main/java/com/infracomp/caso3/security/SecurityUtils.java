package com.infracomp.caso3.security;

import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.math.BigInteger;
import javax.crypto.spec.DHParameterSpec;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;

public class SecurityUtils {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int DH_KEY_SIZE = 1024;
    
    // Generación de parámetros Diffie-Hellman
    public static DHParameterSpec generateDHParams() throws Exception {
        AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH");
        paramGen.init(DH_KEY_SIZE);
        AlgorithmParameters params = paramGen.generateParameters();
        return params.getParameterSpec(DHParameterSpec.class);
    }

    // Generación de par de llaves DH
    public static KeyPair generateDHKeyPair(DHParameterSpec dhParams) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
        keyGen.initialize(dhParams);
        return keyGen.generateKeyPair();
    }

    // Generación de llave secreta compartida usando DH
    public static byte[] generateSharedSecret(KeyAgreement keyAgreement, PublicKey publicKey) throws Exception {
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret();
    }

    // Derivación de llaves a partir de la llave maestra
    public static SecretKey[] deriveKeys(byte[] masterKey) throws Exception {
        MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
        byte[] digest = sha512.digest(masterKey);
        
        // Primera mitad para cifrado (256 bits)
        byte[] encryptionKeyBytes = new byte[32];
        System.arraycopy(digest, 0, encryptionKeyBytes, 0, 32);
        
        // Segunda mitad para HMAC (256 bits)
        byte[] hmacKeyBytes = new byte[32];
        System.arraycopy(digest, 32, hmacKeyBytes, 0, 32);
        
        SecretKey encryptionKey = new SecretKeySpec(encryptionKeyBytes, "AES");
        SecretKey hmacKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA256");
        
        return new SecretKey[]{encryptionKey, hmacKey};
    }

    // Generación de HMAC
    public static byte[] generateHMAC(SecretKey key, byte[] data) throws Exception {
        Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
        hmac.init(key);
        return hmac.doFinal(data);
    }

    // Verificación de HMAC
    public static boolean verifyHMAC(SecretKey key, byte[] data, byte[] receivedHMAC) throws Exception {
        byte[] calculatedHMAC = generateHMAC(key, data);
        return MessageDigest.isEqual(calculatedHMAC, receivedHMAC);
    }

    // Generación de par de llaves RSA
    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }
} 