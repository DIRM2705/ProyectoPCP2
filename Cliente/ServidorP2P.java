package Proyecto2.Cliente;

import java.io.*;
import java.net.*;
import java.util.HashMap;

public class ServidorP2P implements Runnable {
    private final int puertoP2P = 1236;
    public static final HashMap<String, Fragmento> misFragmentos = new HashMap<>();
    
    // Necesitamos la conexión al tracker para reportar cuando recibimos algo
    private ConexionServidor servidorTracker;

    public ServidorP2P(ConexionServidor servidorTracker) {
        this.servidorTracker = servidorTracker;
    }

    @Override
    public void run() {
        try (ServerSocket serverP2P = new ServerSocket(puertoP2P)) {
            while (true) {
                Socket clientePedidor = serverP2P.accept();
                new Thread(() -> manejarPeticion(clientePedidor)).start();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void manejarPeticion(Socket socket) {
        // Usamos ObjectStreams para poder enviar/recibir el objeto Fragmento completo
        try (ObjectOutputStream salidaObj = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream entradaObj = new ObjectInputStream(socket.getInputStream())) {
            
            // Leemos la acción que el otro nodo quiere hacer
            String accion = entradaObj.readUTF(); 
            
            if (accion.startsWith("GET:")) {
                // Lógica de DESCARGA (Ejemplo: "GET:mi_archivo.zip:2")
                String[] partes = accion.split(":");
                String clave = partes[1] + ":" + partes[2];
                
                if (misFragmentos.containsKey(clave)) {
                    salidaObj.writeObject(misFragmentos.get(clave));
                    System.out.println("\n[P2P] Fragmento enviado al vecino: " + clave);
                }
                
            } else if (accion.equals("STORE")) {
                // Lógica de SUBIDA (Un vecino nos está regalando un pedazo)
                Fragmento fragNuevo = (Fragmento) entradaObj.readObject();
                String clave = fragNuevo.getIdDocumento() + ":" + fragNuevo.getNumeroSecuencia();
                
                // 1. Guardamos el fragmento en nuestra memoria local
                misFragmentos.put(clave, fragNuevo);
                System.out.println("\n[P2P] Fragmento " + clave + " recibido de un vecino y guardado.");
                
                // 2. ¡EL REPORTE AUTOMÁTICO! Le avisamos al servidor central
                servidorTracker.reportarFragmento(fragNuevo.getIdDocumento(), fragNuevo.getNumeroSecuencia());
            }
            
        } catch (Exception e) { 
            System.err.println("Error en comunicación P2P: " + e.getMessage());
        }
    }
}