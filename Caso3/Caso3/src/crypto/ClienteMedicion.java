package src.crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.DHParameterSpec;
import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.KeyAgreement;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class ClienteMedicion {
    private static final String SERVIDOR_IP = "127.0.0.1";
    private static final int SERVIDOR_PUERTO = 5000;
    private static final String ID_SERVICIO_AUTOMATICO = "1"; // Siempre consultará el servicio 1

    private PublicKey llavePublicaServidor;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private SecretKey llaveCifrado;
    private SecretKey llaveHMAC;
    private final Map<String, Double> tiempos;
    private final int id; // Agregar ID para identificar cada cliente

    public ClienteMedicion(int id) throws Exception {
        this.id = id;
        cargarLlavePublicaServidor();
        tiempos = new HashMap<>();
        System.out.println(String.format("Cliente %d creado con mapa de tiempos inicializado", id));
    }

    private void cargarLlavePublicaServidor() throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("keys/public.key"))) {
            llavePublicaServidor = (PublicKey) ois.readObject();
        }
    }

    protected void imprimirTiempo(String operacion, double tiempo) {
        String operacionKey = operacion.toLowerCase().replace(" ", "_");
        tiempos.put(operacionKey, tiempo);
        System.out.println(String.format("%s: %.4f ms", operacion, tiempo));
    }

    public Map<String, Double> getTiempos() {
        return new HashMap<>(tiempos);
    }

    public void iniciar() throws Exception {
        System.out.println(String.format("\nCliente %d iniciando...", id));
        Socket socket = new Socket(SERVIDOR_IP, SERVIDOR_PUERTO);
        salida = new ObjectOutputStream(socket.getOutputStream());
        entrada = new ObjectInputStream(socket.getInputStream());

        // 1. Intercambio Diffie-Hellman
        // Receive DH parameters from server
        BigInteger p = (BigInteger) entrada.readObject();
        BigInteger g = (BigInteger) entrada.readObject();
        DHParameterSpec dhSpec = new DHParameterSpec(p, g);

        // Generate client key pair and agreement using server parameters
        KeyPair parLlaves = DiffieHellman.crearLlavesDH(dhSpec);
        KeyAgreement acuerdo = DiffieHellman.acuerdoLlaves(parLlaves.getPrivate());

        // Receive server public key
        byte[] llavePublicaServidorBytes = (byte[]) entrada.readObject();
        PublicKey llavePublicaServidorDH = DiffieHellman.reconstruirLlavePublica(llavePublicaServidorBytes);

        // Send client public key
        salida.writeObject(parLlaves.getPublic().getEncoded());
        salida.flush();

        // Compute shared secret
        byte[] secretoCompartido = DiffieHellman.crearSecretoCompartido(acuerdo, llavePublicaServidorDH);

        MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
        byte[] hash = sha512.digest(secretoCompartido);

        byte[] llaveCifradoBytes = new byte[32];
        byte[] llaveHMACBytes = new byte[32];
        System.arraycopy(hash, 0, llaveCifradoBytes, 0, 32);
        System.arraycopy(hash, 32, llaveHMACBytes, 0, 32);

        llaveCifrado = Cifrado.crearLlaveAES(llaveCifradoBytes);
        llaveHMAC = Cifrado.crearLlaveHMAC(llaveHMACBytes);

        recibirTablaServicios();
        enviarSolicitudServicio();

        socket.close();
    }

    private void recibirTablaServicios() throws Exception {
        byte[] ivBytes = (byte[]) entrada.readObject();
        byte[] tablaCifrada = (byte[]) entrada.readObject();
        byte[] firma = (byte[]) entrada.readObject();
        byte[] hmacRecibido = (byte[]) entrada.readObject();

        // Verificar HMAC
        long inicioHMAC = System.nanoTime();
        byte[] hmacCalculado = Cifrado.HMAC(tablaCifrada, llaveHMAC);
        double tiempoHMAC = (System.nanoTime() - inicioHMAC) / 1_000_000.0;
        imprimirTiempo("HMAC", tiempoHMAC);

        if (!MessageDigest.isEqual(hmacRecibido, hmacCalculado)) {
            System.out.println("Error en la consulta (HMAC inválido). Terminando.");
            System.exit(1);
        }

        // Descifrar tabla
        long inicioDescifrado = System.nanoTime();
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        byte[] datosDescifrados = Cifrado.descifrarAES(tablaCifrada, llaveCifrado, iv);
        double tiempoDescifrado = (System.nanoTime() - inicioDescifrado) / 1_000_000.0;
        imprimirTiempo("Descifrado", tiempoDescifrado);

        // Verificar firma
        long inicioVerificacion = System.nanoTime();
        boolean firmaValida = Cifrado.verificarFirma(datosDescifrados, firma, llavePublicaServidor);
        double tiempoVerificacion = (System.nanoTime() - inicioVerificacion) / 1_000_000.0;
        imprimirTiempo("Verificacion", tiempoVerificacion);

        if (!firmaValida) {
            System.out.println("Error: firma de la tabla invalida. Terminando.");
            System.exit(1);
        }
    }

    private void enviarSolicitudServicio() throws Exception {
        // Usar ID predefinido en lugar de pedirlo al usuario
        byte[] datos = ID_SERVICIO_AUTOMATICO.getBytes();
        
        // Generar IV
        long inicioIV = System.nanoTime();
        IvParameterSpec iv = Cifrado.generarIV();
        double tiempoIV = (System.nanoTime() - inicioIV) / 1_000_000.0;
        imprimirTiempo("Generacion IV", tiempoIV);

        // Cifrar solicitud
        long inicioCifrado = System.nanoTime();
        byte[] solicitudCifrada = Cifrado.cifrarAES(datos, llaveCifrado, iv);
        double tiempoCifrado = (System.nanoTime() - inicioCifrado) / 1_000_000.0;
        imprimirTiempo("Cifrado", tiempoCifrado);

        // HMAC
        long inicioHMAC = System.nanoTime();
        byte[] hmacSolicitud = Cifrado.HMAC(solicitudCifrada, llaveHMAC);
        double tiempoHMAC = (System.nanoTime() - inicioHMAC) / 1_000_000.0;
        imprimirTiempo("HMAC Solicitud", tiempoHMAC);

        salida.writeObject(iv.getIV());
        salida.writeObject(solicitudCifrada);
        salida.writeObject(hmacSolicitud);
        salida.flush();

        recibirDireccion();
    }

    private void recibirDireccion() throws Exception {
        byte[] ivBytes = (byte[]) entrada.readObject();
        byte[] direccionCifrada = (byte[]) entrada.readObject();
        byte[] hmacRecibido = (byte[]) entrada.readObject();

        // Verificar HMAC
        long inicioHMAC = System.nanoTime();
        byte[] hmacCalculado = Cifrado.HMAC(direccionCifrada, llaveHMAC);
        double tiempoHMAC = (System.nanoTime() - inicioHMAC) / 1_000_000.0;
        imprimirTiempo("HMAC Direccion", tiempoHMAC);

        if (!MessageDigest.isEqual(hmacRecibido, hmacCalculado)) {
            System.out.println("Error en la respuesta (HMAC inválido). Terminando.");
            System.exit(1);
        }

        // Descifrar dirección
        long inicioDescifrado = System.nanoTime();
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        byte[] direccionBytes = Cifrado.descifrarAES(direccionCifrada, llaveCifrado, iv);
        double tiempoDescifrado = (System.nanoTime() - inicioDescifrado) / 1_000_000.0;
        imprimirTiempo("Descifrado Direccion", tiempoDescifrado);
    }
} 