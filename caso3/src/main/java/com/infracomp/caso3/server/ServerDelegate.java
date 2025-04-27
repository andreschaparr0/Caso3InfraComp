package com.infracomp.caso3.server;

import java.io.*;
import java.net.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Base64;
import com.infracomp.caso3.security.CryptoManager;
import com.infracomp.caso3.security.SecurityUtils;
import java.math.BigInteger;
import java.util.Map;
import java.security.spec.X509EncodedKeySpec;

public class ServerDelegate extends Thread {
    private Socket clientSocket;
    private KeyPair serverKeyPair;
    private ServiceTable serviceTable;
    private SecretKey[] sessionKeys; // [0] = encryption key, [1] = hmac key
    private DataInputStream in;
    private DataOutputStream out;
    private KeyAgreement keyAgreement;
    private byte[] sharedSecret;
    private PublicKey clientPublicKey;

    // Métricas de tiempo
    private long signTime;
    private long encryptTime;
    private long verifyTime;

    public long getSignTime() { return signTime; }
    public long getEncryptTime() { return encryptTime; }
    public long getVerifyTime() { return verifyTime; }

    public ServerDelegate(Socket socket, KeyPair serverKeyPair, ServiceTable serviceTable) {
        this.clientSocket = socket;
        this.serverKeyPair = serverKeyPair;
        this.serviceTable = serviceTable;
        try {
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Error inicializando streams: " + e.getMessage());
            e.printStackTrace();
            // Close the socket in case of error
            try {
                socket.close();
            } catch (IOException ex) {
                System.err.println("Error cerrando socket después de error en streams: " + ex.getMessage());
            }
        }
    }

    @Override
    public void run() {
        try {
            // 1. Recibir "HELLO"
            String hello = readMessage();
            if (!"HELLO".equals(hello)) {
                throw new Exception("Protocolo incorrecto: esperaba HELLO");
            }

            // 2. Enviar "RETO"
            long startTime = System.nanoTime();
            String reto = generateChallenge();
            signTime = System.nanoTime() - startTime;

            sendMessage(reto);

            // 3-6. Proceso Diffie-Hellman
            setupDiffieHellman();

            // 7-9. Recibir y verificar G, P, G^r
            startTime = System.nanoTime();
            receiveAndVerifyDHParameters();
            verifyTime = System.nanoTime() - startTime;

            // 10-12. Generar y enviar llaves simétricas
            startTime = System.nanoTime();
            generateAndSendSymmetricKeys();
            encryptTime = System.nanoTime() - startTime;

            // 13-15. Verificar HMAC
            if (!verifyHMAC()) {
                throw new Exception("Error en verificación HMAC");
            }

            // 16-18. Enviar tabla de servicios y terminar
            sendServiceTable();

            // Imprimir métricas
            System.out.printf("Thread %d - Tiempos (ns): Firma=%d, Cifrado=%d, Verificación=%d%n",
                Thread.currentThread().getId(), signTime, encryptTime, verifyTime);

        } catch (Exception e) {
            System.err.println("Error en protocolo: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error cerrando socket: " + e.getMessage());
            }
        }
    }

    private String readMessage() throws IOException {
        int length = in.readInt();
        byte[] message = new byte[length];
        in.readFully(message);
        return new String(message);
    }

    private void sendMessage(String message) throws IOException {
        byte[] messageBytes = message.getBytes();
        out.writeInt(messageBytes.length);
        out.write(messageBytes);
        out.flush();
    }

    private String generateChallenge() {
        // Generar un reto aleatorio
        byte[] challenge = new byte[16];
        new SecureRandom().nextBytes(challenge);
        return Base64.getEncoder().encodeToString(challenge);
    }

    private void setupDiffieHellman() throws Exception {
        // Inicializar el acuerdo de llaves
        keyAgreement = KeyAgreement.getInstance("DH");
    }

    private void receiveAndVerifyDHParameters() throws Exception {
        // Recibir G, P y G^r del cliente
        BigInteger g = new BigInteger(readMessage());
        BigInteger p = new BigInteger(readMessage());
        byte[] clientPublicKeyBytes = Base64.getDecoder().decode(readMessage());

        // Crear los parámetros DH
        DHParameterSpec dhParams = new DHParameterSpec(p, g);
        
        // Generar el par de llaves del servidor usando los parámetros recibidos
        KeyPairGenerator serverKpairGen = KeyPairGenerator.getInstance("DH");
        serverKpairGen.initialize(dhParams);
        KeyPair serverPair = serverKpairGen.generateKeyPair();
        
        // Inicializar el intercambio de llaves con la llave privada del servidor
        keyAgreement.init(serverPair.getPrivate());
        
        // Reconstruir la llave pública del cliente
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(clientPublicKeyBytes);
        clientPublicKey = keyFactory.generatePublic(x509KeySpec);
        
        // Realizar la última fase del acuerdo
        keyAgreement.doPhase(clientPublicKey, true);
        
        // Generar el secreto compartido
        sharedSecret = keyAgreement.generateSecret();
        
        // Generar las llaves de sesión usando el secreto compartido
        sessionKeys = SecurityUtils.deriveKeys(sharedSecret);
    }

    private void generateAndSendSymmetricKeys() throws Exception {
        if (sessionKeys == null || sessionKeys.length != 2) {
            throw new IllegalStateException("Las llaves de sesión no han sido generadas correctamente");
        }

        // Obtener los bytes de las llaves
        byte[] encryptionKeyBytes = sessionKeys[0].getEncoded();
        byte[] hmacKeyBytes = sessionKeys[1].getEncoded();

        // Concatenar las llaves
        byte[] combinedKeys = new byte[encryptionKeyBytes.length + hmacKeyBytes.length];
        System.arraycopy(encryptionKeyBytes, 0, combinedKeys, 0, encryptionKeyBytes.length);
        System.arraycopy(hmacKeyBytes, 0, combinedKeys, encryptionKeyBytes.length, hmacKeyBytes.length);

        // Convertir a Base64 antes de cifrar
        String combinedKeysStr = Base64.getEncoder().encodeToString(combinedKeys);
        
        // Cifrar con la llave pública del cliente
        byte[] encryptedKeys = CryptoManager.encryptRSA(clientPublicKey, combinedKeysStr);
        if (encryptedKeys == null) {
            throw new Exception("Error al cifrar las llaves simétricas");
        }

        // Enviar las llaves cifradas en Base64
        String encryptedKeysStr = Base64.getEncoder().encodeToString(encryptedKeys);
        
        sendMessage(encryptedKeysStr);
    }

    private boolean verifyHMAC() throws Exception {
        // Recibir el HMAC del cliente
        String receivedHmacStr = readMessage();
        byte[] receivedHmac = Base64.getDecoder().decode(receivedHmacStr);

        // Generar el mensaje para verificar (concatenación de mensajes anteriores)
        String verificationMessage = "HELLO" + sharedSecret;
        
        // Verificar el HMAC usando la llave HMAC (sessionKeys[1])
        return SecurityUtils.verifyHMAC(sessionKeys[1], verificationMessage.getBytes(), receivedHmac);
    }

    private void sendServiceTable() throws Exception {
        // Convertir la tabla de servicios a formato transmisible
        Map<String, String> services = serviceTable.getServiceList();
        StringBuilder serviceData = new StringBuilder();
        for (Map.Entry<String, String> entry : services.entrySet()) {
            serviceData.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
        }

        // Generar IV para el cifrado AES
        byte[] iv = CryptoManager.generateIV();

        // Cifrar la tabla de servicios usando AES
        byte[] encryptedServices = CryptoManager.encryptAES(
            sessionKeys[0], 
            serviceData.toString(),
            iv
        );

        // Generar HMAC de la tabla cifrada
        byte[] serviceHmac = SecurityUtils.generateHMAC(sessionKeys[1], encryptedServices);

        // Enviar métricas de tiempo
        sendMessage(String.format("%d,%d,%d", signTime, encryptTime, verifyTime));

        // Enviar IV
        sendMessage(Base64.getEncoder().encodeToString(iv));

        // Enviar datos cifrados
        sendMessage(Base64.getEncoder().encodeToString(encryptedServices));

        // Enviar HMAC
        sendMessage(Base64.getEncoder().encodeToString(serviceHmac));
    }
} 