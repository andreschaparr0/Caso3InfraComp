package com.infracomp.caso3.client;

import java.io.*;
import java.net.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Base64;
import com.infracomp.caso3.security.CryptoManager;
import com.infracomp.caso3.security.SecurityUtils;
import com.infracomp.caso3.security.KeyManager;
import java.math.BigInteger;
import java.util.concurrent.CountDownLatch;

public class Client extends Thread {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 3000;
    
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private SecretKey[] sessionKeys; // [0] = encryption key, [1] = hmac key
    private PublicKey serverPublicKey;
    private DHParameterSpec dhParams;
    private KeyPair dhKeyPair;
    private KeyAgreement keyAgreement;
    private long signTime;
    private long encryptTime;
    private long verifyTime;
    private final int id;
    private final CountDownLatch latch;
    private SecretKey encryptionKey;
    private SecretKey hmacKey;
    private KeyPair clientKeyPair;

    public Client(int id, CountDownLatch latch) {
        this.id = id;
        this.latch = latch;
        try {
            // Inicializar el par de llaves del cliente
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            this.clientKeyPair = keyGen.generateKeyPair();
            
            loadServerPublicKey();
            System.out.println("Cliente " + id + ": Llave pública del servidor cargada exitosamente");
        } catch (Exception e) {
            System.err.println("Cliente " + id + ": Error inicializando llaves: " + e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void run() {
        try {
            connect();
            System.out.println("Cliente " + id + " completado");
        } catch (Exception e) {
            System.err.println("Error en cliente " + id + ": " + e.getMessage());
        } finally {
            latch.countDown();
        }
    }

    private void loadServerPublicKey() throws Exception {
        try {
            serverPublicKey = KeyManager.loadPublicKey();
            System.out.println("Cliente " + id + ": Llave pública del servidor cargada exitosamente");
        } catch (Exception e) {
            System.err.println("Cliente " + id + ": Error cargando llave pública del servidor: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public void connect() throws Exception {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            // 1. Enviar "HELLO"
            sendMessage("HELLO");

            // 2. Enviar llave pública del cliente
            byte[] publicKeyBytes = clientKeyPair.getPublic().getEncoded();
            String publicKeyStr = Base64.getEncoder().encodeToString(publicKeyBytes);
            sendMessage(publicKeyStr);

            // 3. Recibir "RETO"
            String reto = readMessage();

            // 4-7. Proceso Diffie-Hellman
            setupDiffieHellman();

            // 8-10. Enviar G, P, G^r
            sendDHParameters();

            // 11-13. Recibir y verificar llaves simétricas
            receiveAndVerifySymmetricKeys();

            // 14-16. Generar y enviar HMAC
            sendHMAC();

            // 17-19. Recibir tabla de servicios
            receiveServiceTable();

        } catch (Exception e) {
            throw new Exception("Error en protocolo: " + e.getMessage());
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

    private void setupDiffieHellman() throws Exception {
        // Generar parámetros DH (G, P)
        dhParams = SecurityUtils.generateDHParams();
        
        // Generar par de llaves DH (incluye el valor aleatorio r)
        dhKeyPair = SecurityUtils.generateDHKeyPair(dhParams);
        
        // Inicializar el acuerdo de llaves
        keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(dhKeyPair.getPrivate());
    }

    private void sendDHParameters() throws Exception {
        // Obtener los valores G, P del dhParams
        BigInteger p = dhParams.getP();
        BigInteger g = dhParams.getG();
        
        // Obtener G^r (la llave pública DH)
        byte[] publicKeyBytes = dhKeyPair.getPublic().getEncoded();
        
        // Enviar G
        sendMessage(g.toString());
        
        // Enviar P
        sendMessage(p.toString());
        
        // Enviar G^r
        sendMessage(Base64.getEncoder().encodeToString(publicKeyBytes));
    }

    private void receiveAndVerifySymmetricKeys() throws Exception {
        // Recibir las llaves cifradas
        String encryptedKeysStr = readMessage();
        if (encryptedKeysStr == null) {
            throw new Exception("No se recibieron las llaves simétricas");
        }

        byte[] encryptedKeys = Base64.getDecoder().decode(encryptedKeysStr);
        
        // Descifrar con la llave privada del cliente
        String decryptedKeysStr = CryptoManager.decryptRSA(clientKeyPair.getPrivate(), encryptedKeys);
        if (decryptedKeysStr == null) {
            throw new Exception("Error al descifrar las llaves simétricas");
        }

        byte[] decryptedKeys = Base64.getDecoder().decode(decryptedKeysStr);
        
        // Separar las llaves
        int halfLength = decryptedKeys.length / 2;
        byte[] encryptionKeyBytes = new byte[halfLength];
        byte[] hmacKeyBytes = new byte[halfLength];
        
        System.arraycopy(decryptedKeys, 0, encryptionKeyBytes, 0, halfLength);
        System.arraycopy(decryptedKeys, halfLength, hmacKeyBytes, 0, halfLength);

        // Crear las llaves SecretKey
        this.encryptionKey = new SecretKeySpec(encryptionKeyBytes, "AES");
        this.hmacKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA256");
        
        // También almacenar en el array para compatibilidad
        this.sessionKeys = new SecretKey[]{this.encryptionKey, this.hmacKey};
        
        System.out.println("Cliente " + id + ": Llaves simétricas recibidas y procesadas correctamente");
    }

    private void sendHMAC() throws Exception {
        if (sessionKeys == null || sessionKeys.length != 2) {
            throw new IllegalStateException("Las llaves de sesión no están disponibles");
        }

        // Generar el mensaje para el HMAC (concatenación de mensajes anteriores)
        String message = "HELLO" + keyAgreement.generateSecret();
        
        // Generar el HMAC usando la llave HMAC (sessionKeys[1])
        byte[] hmac = SecurityUtils.generateHMAC(sessionKeys[1], message.getBytes());

        // Enviar el HMAC
        sendMessage(Base64.getEncoder().encodeToString(hmac));
    }

    public long getSignTime() { return signTime; }
    public long getEncryptTime() { return encryptTime; }
    public long getVerifyTime() { return verifyTime; }

    private void receiveServiceTable() throws Exception {
        if (sessionKeys == null || sessionKeys.length != 2) {
            throw new IllegalStateException("Las llaves de sesión no están disponibles");
        }

        // Recibir métricas de tiempo
        String[] timeMetrics = readMessage().split(",");
        signTime = Long.parseLong(timeMetrics[0]);
        encryptTime = Long.parseLong(timeMetrics[1]);
        verifyTime = Long.parseLong(timeMetrics[2]);

        // Recibir IV
        String ivStr = readMessage();
        byte[] iv = Base64.getDecoder().decode(ivStr);

        // Recibir datos cifrados
        String encryptedDataStr = readMessage();
        byte[] encryptedData = Base64.getDecoder().decode(encryptedDataStr);

        // Recibir HMAC
        String receivedHmacStr = readMessage();
        byte[] receivedHmac = Base64.getDecoder().decode(receivedHmacStr);

        // Verificar HMAC de los datos cifrados
        if (!SecurityUtils.verifyHMAC(sessionKeys[1], encryptedData, receivedHmac)) {
            throw new SecurityException("HMAC inválido para la tabla de servicios");
        }

        // Descifrar la tabla de servicios
        String serviceData = CryptoManager.decryptAES(
            sessionKeys[0],
            encryptedData,
            iv
        );

        // Procesar la tabla de servicios
        System.out.println("\nTabla de servicios recibida:");
        String[] services = serviceData.split(";");
        for (String service : services) {
            if (!service.isEmpty()) {
                String[] parts = service.split(":");
                System.out.printf("Servicio: %s, Descripción: %s%n", parts[0], parts[1]);
            }
        }
    }
} 