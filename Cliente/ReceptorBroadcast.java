package Proyecto2.Cliente;

import java.net.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.io.IOException;
import Proyecto2.Codes;

public class ReceptorBroadcast implements Runnable {
    private final int puertoUDP = 1234;
    public static final CopyOnWriteArraySet<String> vecinos = new CopyOnWriteArraySet<>();

    @Override
    public void run() {
       InetSocketAddress direccionOido = new InetSocketAddress(puertoUDP);

    // 2. Creamos el socket sin asignarle puerto todavía (null)
    try (DatagramSocket socket = new DatagramSocket(null)) {
        
        // 3. ¡LA MAGIA! Le decimos al SO que permita que otros usen este mismo puerto
        socket.setReuseAddress(true);
        
        // 4. Ahora sí, lo amarramos al puerto 1234
        socket.bind(direccionOido);

        byte[] buffer = new byte[1024];
        System.out.println("[UDP] Escuchando (Multicliente) en el puerto " + puertoUDP + "...");
        
        while (true) {
            DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
            socket.receive(paquete);
            
            // ... resto de tu lógica para extraer código y mensaje ...
            int codigo = paquete.getData()[0];
            String mensaje = new String(paquete.getData(), 1, paquete.getLength() - 1);
            
            if (codigo == Codes.NEW_CLIENT) {
                vecinos.add(mensaje);
                System.out.println("\n[P2P] Nodo vecino registrado: " + mensaje);
            }
            procesarNotificacion(codigo, mensaje);
        }
    } catch (IOException e) {
        System.err.println("Error en el receptor broadcast multicliente: " + e.getMessage());
    }
}

    private void procesarNotificacion(int codigo, String datos) {
        // Usamos los mismos códigos de tu archivo Codes.java
        switch (codigo) {
            case Codes.NEW_DOC:
                System.out.println("\n[Alerta Global] Se ha creado un nuevo documento con ID: " + datos);
                break;
            case Codes.DELETE_DOC:
                System.out.println("\n[Alerta Global] Se ha eliminado el documento con ID: " + datos);
                break;
            case Codes.NEW_CLIENT:
                System.out.println("\n[Alerta Global] Nuevo cliente conectado desde IP: " + datos);
                break;
             case Codes.PURGE_FILE:
                // =========================================================
                // CORRECCIÓN: Simplemente usamos "datos", que ya trae el nombre del archivo
                // =========================================================
                String archivoABorrar = datos.trim();
                System.out.println("[Alerta Global] Orden de purga recibida. Destruyendo fragmentos de: " + archivoABorrar);
                
                // Ejecutamos la destrucción masiva local
                ServidorP2P.purgarFragmentosLocales(archivoABorrar);
                break;
            // Aquí puedes agregar OPEN_DOC y CLOSE_DOC
            default:
                System.out.println("\n[Alerta Global] Mensaje recibido (Código " + codigo + "): " + datos);
        }
        System.out.print("> "); // Para no arruinar la línea de comandos
    }
}