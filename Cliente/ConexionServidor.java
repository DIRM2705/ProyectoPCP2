package Proyecto2.Cliente;

import java.io.*;
import java.net.Socket;
import Proyecto2.Codes;
import java.util.*;

public class ConexionServidor {
    private Socket socket;
    private DataOutputStream salida;
    private BufferedReader entrada;

    public ConexionServidor(String ip, int puerto) throws IOException {
        this.socket = new Socket(ip, puerto);
        this.salida = new DataOutputStream(socket.getOutputStream());
        this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public synchronized void enviarComando(int codigo, String nombreDoc) throws IOException {
        // Es vital agregar el salto de línea para que reader.readLine() en el servidor funcione
        String payload = nombreDoc + "\n";
        byte[] bytesString = payload.getBytes();
        
        // El arreglo final tiene 1 byte extra para el código de operación
        byte[] mensaje = new byte[1 + bytesString.length];
        mensaje[0] = (byte) codigo;
        
        // Copiamos el texto después del código
        System.arraycopy(bytesString, 0, mensaje, 1, bytesString.length);
        
        // Enviamos el mensaje en bruto
        salida.write(mensaje);
        salida.flush();
    }

    public String recibirRespuesta() throws IOException {
        // En caso de que el servidor envíe errores u otras respuestas directas
        return entrada.readLine();
    }

    // Avisa al servidor que este cliente tiene un pedazo específico
    public void reportarFragmento(String nombreDoc, int numFrag) throws IOException {
        // Formato que espera el servidor: "nombreDoc:num"
        enviarComando(7, nombreDoc + ":" + numFrag); 
    }

    // Pide al servidor la lista de quién tiene qué
    public void pedirMapaUbicaciones(String nombreDoc) throws IOException {
        enviarComando(8, nombreDoc);
    }
    public HashMap<Integer, String> recibirMapaUbicaciones() throws IOException {
        HashMap<Integer, String> mapa = new HashMap<>();
        
        // Esperamos la respuesta del servidor
        String respuesta = entrada.readLine();
        
        // Si el archivo no existe o hubo un error, el servidor podría responder "ERROR" o vacío
        if (respuesta == null || respuesta.trim().isEmpty() || respuesta.equals("ERROR")) {
            return null; 
        }

        try {
            // 1. Separar el total de fragmentos
            String[] separacionPrincipal = respuesta.split("\\|");
            if(separacionPrincipal.length < 2) return null; 
            
            String totalLimpio = separacionPrincipal[0].replaceAll("[^0-9]", "");
            int totalFragmentos = Integer.parseInt(totalLimpio);
            
            String datosUbicaciones = separacionPrincipal[1];

            // 2. Cortamos por el punto y coma ";" para tener los pares
            String[] pares = datosUbicaciones.split(";");
            
            for (String par : pares) {
                if (par.trim().isEmpty()) continue; 
                
                // =========================================================
                // CORRECCIÓN AQUÍ: split(":", 2) corta SOLO el primer ":"
                // Dejando partes[0] = "0" y partes[1] = "192.168.1.69:63182"
                // =========================================================
                String[] partes = par.split(":", 2); 
                
                if (partes.length == 2) {
                    
                    String numLimpio = partes[0].replaceAll("[^0-9]", "");
                    int numFrag = Integer.parseInt(numLimpio);
                    
                    // A la IP:Puerto le quitamos espacios raros
                    String ipDestino = partes[1].replaceAll("\\s+", ""); 
                    
                    mapa.put(numFrag, ipDestino);
                }
            }
            
            if (mapa.size() < totalFragmentos) {
                System.out.println("Advertencia: El mapa está incompleto. Faltan fragmentos reportados.");
            }
            
        } catch (Exception e) {
            System.err.println("Error procesando el mapa de ubicaciones: " + e.getMessage());
            return null;
        }
        
        return mapa;
    }
    public List<String> pedirListaArchivos() throws IOException {

    enviarComando(11, "LIST"); 
    
    String respuesta = entrada.readLine();
    
    if (respuesta == null || respuesta.equals("VACIO") || respuesta.trim().isEmpty()) {
        return new ArrayList<>(); // Retorna una lista vacía si no hay nada
    }
    
    // Convertimos el texto "doc1.txt,foto.png" en una Lista de Java
    return Arrays.asList(respuesta.split(","));
}
    public void cerrar() throws IOException {
        socket.close();
    }
}