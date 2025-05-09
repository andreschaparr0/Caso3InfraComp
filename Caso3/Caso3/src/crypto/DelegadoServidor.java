package src.crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import javax.crypto.KeyAgreement;
import java.util.Base64;
import javax.crypto.spec.DHParameterSpec;
import java.util.Locale;

public class DelegadoServidor implements Runnable {

    private Socket cliente;
    private PrivateKey llavePrivada;
    private PublicKey llavePublica;
    private ObjectInputStream entrada;
    private ObjectOutputStream salida;
    private MedidorTiempo medidorFirmar = new MedidorTiempo();
    private MedidorTiempo medidorCifrar = new MedidorTiempo();
    private MedidorTiempo medidorVerificar = new MedidorTiempo();
    private MedidorTiempo medidorCifrarAsimetrico = new MedidorTiempo();
    private String escenario = "Secuencial";
    private int numClientes = 1;
    private int clienteId = 1;

    public DelegadoServidor(Socket cliente, PrivateKey llavePrivada, PublicKey llavePublica) {
        this.cliente = cliente;
        this.llavePrivada = llavePrivada;
        this.llavePublica = llavePublica;
    }
    
    public DelegadoServidor(Socket cliente, PrivateKey llavePrivada, PublicKey llavePublica, 
                          String escenario, int numClientes, int clienteId) {
        this.cliente = cliente;
        this.llavePrivada = llavePrivada;
        this.llavePublica = llavePublica;
        this.escenario = escenario;
        this.numClientes = numClientes;
        this.clienteId = clienteId;
    }

    @Override
    public void run() {
        try {
            entrada = new ObjectInputStream(cliente.getInputStream());
            salida = new ObjectOutputStream(cliente.getOutputStream());

            // 1. Intercambio Diffie-Hellman
            // Generate DH parameters
            AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH");
            paramGen.init(1024);
            AlgorithmParameters params = paramGen.generateParameters();
            DHParameterSpec dhSpec = params.getParameterSpec(DHParameterSpec.class);

            // Send DH parameters to client
            salida.writeObject(dhSpec.getP());
            salida.writeObject(dhSpec.getG());
            salida.flush();

            // Create server key pair and agreement using same parameters
            KeyPair parLlaves = DiffieHellman.crearLlavesDH(dhSpec);
            KeyAgreement acuerdo = DiffieHellman.acuerdoLlaves(parLlaves.getPrivate());

            // Send server public key
            salida.writeObject(parLlaves.getPublic().getEncoded());
            salida.flush();

            // Receive client's public key and compute shared secret
            byte[] llavePublicaClienteBytes = (byte[]) entrada.readObject();
            PublicKey llavePublicaCliente = DiffieHellman.reconstruirLlavePublica(llavePublicaClienteBytes);
            byte[] secretoCompartido = DiffieHellman.crearSecretoCompartido(acuerdo, llavePublicaCliente);

            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            byte[] hash = sha512.digest(secretoCompartido);

            byte[] llaveCifradoBytes = new byte[32];
            byte[] llaveHMACBytes = new byte[32];
            System.arraycopy(hash, 0, llaveCifradoBytes, 0, 32);
            System.arraycopy(hash, 32, llaveHMACBytes, 0, 32);

            SecretKey llaveCifrado = Cifrado.crearLlaveAES(llaveCifradoBytes);
            SecretKey llaveHMAC = Cifrado.crearLlaveHMAC(llaveHMACBytes);

            // 2. Enviar tabla de servicios cifrada y firmada
            enviarTablaServicios(llaveCifrado, llaveHMAC);

            // 3. Recibir solicitud de servicio
            recibirYResponderSolicitud(llaveCifrado, llaveHMAC);

            cliente.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Print measured times
        System.out.println("Tiempo de firma: " + medidorFirmar.tiempoMilisegundos() + " ms");
        System.out.println("Tiempo de cifrado: " + medidorCifrar.tiempoMilisegundos() + " ms");
        // Print measured time for verification
        System.out.println("Tiempo de verificación: " + medidorVerificar.tiempoMilisegundos() + " ms");
    }

    // Método estático para guardar la firma directamente en el CSV
    public static void guardarFirmaEnCSV(double tiempoFirma, String escenario, int numClientes, int clienteId) {
        String linea = String.format(Locale.US, "%s,%d,%d,%s,%.4f\n", 
            escenario, numClientes, clienteId, "firma", tiempoFirma);
        MedicionesMain.escribirLineaCSV(linea);
        System.out.println("Tiempo de firma guardado en CSV: " + tiempoFirma + " ms para " + escenario + " con " + numClientes + " clientes, ID " + clienteId);
    }

    private void enviarTablaServicios(SecretKey llaveCifrado, SecretKey llaveHMAC) throws Exception {
        Map<Integer, String> servicios = Servidor.obtenerServicios();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, String> entry : servicios.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }
        byte[] datosTabla = sb.toString().getBytes();

        // Medir tiempo de firma
        medidorFirmar.reset();
        medidorFirmar.comenzar();
        byte[] firma = Cifrado.firmarDatos(datosTabla, llavePrivada);
        medidorFirmar.parar();
        
        double tiempoFirma = medidorFirmar.tiempoMilisegundos();
        System.out.println("Tiempo de firma: " + tiempoFirma + " ms");
        
        // Guardar medición de firma en CSV (evitamos duplicación usando solo un método)
        guardarMedicionEnCSV("firma", tiempoFirma);
        // Ya no llamamos a guardarFirmaEnCSV para evitar duplicación

        // Medir tiempo de cifrado
        medidorCifrar.reset();
        medidorCifrar.comenzar();
        IvParameterSpec iv = Cifrado.generarIV();
        byte[] tablaCifrada = Cifrado.cifrarAES(datosTabla, llaveCifrado, iv);
        medidorCifrar.parar();
        System.out.println("Tiempo de cifrado: " + medidorCifrar.tiempoMilisegundos() + " ms");

        byte[] hmac = Cifrado.HMAC(tablaCifrada, llaveHMAC);

        salida.writeObject(iv.getIV());
        salida.writeObject(tablaCifrada);
        salida.writeObject(firma);
        salida.writeObject(hmac);
        salida.flush();
    }

    private void recibirYResponderSolicitud(SecretKey llaveCifrado, SecretKey llaveHMAC) throws Exception {
        // Recibir solicitud cifrada
        byte[] ivBytes = (byte[]) entrada.readObject();
        byte[] solicitudCifrada = (byte[]) entrada.readObject();
        byte[] hmacRecibido = (byte[]) entrada.readObject();

        // Verificar HMAC
        medidorVerificar.reset();
        medidorVerificar.comenzar();
        byte[] hmacCalculado = Cifrado.HMAC(solicitudCifrada, llaveHMAC);
        boolean hmacValido = MessageDigest.isEqual(hmacRecibido, hmacCalculado);
        medidorVerificar.parar();
        
        double tiempoVerificacion = medidorVerificar.tiempoMilisegundos();
        System.out.println("Tiempo de verificación: " + tiempoVerificacion + " ms");
        guardarMedicionEnCSV("verificacion_servidor", tiempoVerificacion);

        if (!hmacValido) {
            System.out.println("Error en la consulta (HMAC inválido). Terminando conexión.");
            cliente.close();
            return;
        }

        // Descifrar solicitud
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        byte[] solicitudDescifrada = Cifrado.descifrarAES(solicitudCifrada, llaveCifrado, iv);
        String solicitudStr = new String(solicitudDescifrada);
        int idServicio = Integer.parseInt(solicitudStr.trim());

        // Buscar dirección
        Map<Integer, String> direcciones = Servidor.obtenerDirecciones();
        String direccion = direcciones.getOrDefault(idServicio, "-1:-1");

        // Enviar dirección cifrada con HMAC
        byte[] direccionBytes = direccion.getBytes();

        // Medir cifrado simétrico
        medidorCifrar.reset();
        medidorCifrar.comenzar();
        IvParameterSpec ivRespuesta = Cifrado.generarIV();
        byte[] direccionCifrada = Cifrado.cifrarAES(direccionBytes, llaveCifrado, ivRespuesta);
        medidorCifrar.parar();
        
        double tiempoCifrado = medidorCifrar.tiempoMilisegundos();
        System.out.println("Tiempo de cifrado simétrico: " + tiempoCifrado + " ms");
        guardarMedicionEnCSV("cifrado_simetrico_servidor", tiempoCifrado);

        // Medir cifrado asimétrico
        medidorCifrarAsimetrico.reset();
        medidorCifrarAsimetrico.comenzar();
        byte[] direccionCifradaAsimetrica = Cifrado.cifrarRSA(direccionBytes, llavePublica);
        medidorCifrarAsimetrico.parar();
        
        double tiempoCifradoAsimetrico = medidorCifrarAsimetrico.tiempoMilisegundos();
        System.out.println("Tiempo de cifrado asimétrico: " + tiempoCifradoAsimetrico + " ms");
        guardarMedicionEnCSV("cifrado_asimetrico_servidor", tiempoCifradoAsimetrico);

        byte[] hmacDireccion = Cifrado.HMAC(direccionCifrada, llaveHMAC);

        salida.writeObject(ivRespuesta.getIV());
        salida.writeObject(direccionCifrada);
        salida.writeObject(hmacDireccion);
        salida.flush();
    }
    
    private void guardarMedicionEnCSV(String operacion, double tiempo) {
        String linea = String.format(Locale.US, "%s,%d,%d,%s,%.4f\n", 
            escenario, numClientes, clienteId, operacion, tiempo);
        MedicionesMain.escribirLineaCSV(linea);
    }
}


