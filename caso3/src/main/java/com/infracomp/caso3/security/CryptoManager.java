package com.infracomp.caso3.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoManager {
    // Constantes según requerimientos
    private static final String AES_PADDING = "AES/CBC/PKCS5Padding";
    private static final String RSA_PADDING = "RSA/ECB/PKCS1Padding";
    private static final int IV_LENGTH = 16; // 16 bytes para AES

    // Método para generar IV aleatorio
    public static byte[] generateIV() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }

    // Métodos para cifrado simétrico (AES)
    public static byte[] encryptAES(SecretKey key, String text, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_PADDING);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            return cipher.doFinal(text.getBytes("UTF-8"));
        } catch (Exception e) {
            System.err.println("Error en cifrado AES: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static String decryptAES(SecretKey key, byte[] encryptedText, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_PADDING);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            byte[] decrypted = cipher.doFinal(encryptedText);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            System.err.println("Error en descifrado AES: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Métodos para cifrado asimétrico (RSA)
    public static byte[] encryptRSA(Key key, String text) throws Exception {
        if (text == null) throw new IllegalArgumentException("El texto a cifrar no puede ser null");
        if (key == null) throw new IllegalArgumentException("La llave no puede ser null");
        
        Cipher cipher = Cipher.getInstance(RSA_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] textBytes = text.getBytes("UTF-8");
        
        return cipher.doFinal(textBytes);
    }

    public static String decryptRSA(Key key, byte[] encryptedText) throws Exception {
        if (encryptedText == null) throw new IllegalArgumentException("El texto cifrado no puede ser null");
        if (key == null) throw new IllegalArgumentException("La llave no puede ser null");
        
        Cipher cipher = Cipher.getInstance(RSA_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(encryptedText);
        
        return new String(decrypted, "UTF-8");
    }

    // Método utilitario para imprimir bytes
    public static void printBytes(byte[] content) {
        for (int i = 0; i < content.length; i++) {
            System.out.print((content[i] & 0xFF) + (i < content.length - 1 ? " " : "\n"));
        }
    }
} 