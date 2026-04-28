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
        try (DatagramSocket socket = new DatagramSocket(puertoUDP)) {
            byte[] buffer = new byte[1024];
            System.out.println("[UDP] Escuchando notificaciones del servidor en el puerto " + puertoUDP + "...");
            
            while (true) {
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                socket.receive(paquete);
                
                // Extraer el primer byte que es el código
                int codigo = paquete.getData()[0];
                // Extraer el resto como String (el ID o datos del documento)
                String mensaje = new String(paquete.getData(), 1, paquete.getLength() - 1);
              
                if (codigo == Codes.NEW_CLIENT) {
                    vecinos.add(mensaje); // Guardamos la IP del nuevo vecino
                    System.out.println("\n[P2P] Nodo vecino registrado: " + mensaje);
                }
                
                procesarNotificacion(codigo, mensaje);
            }
        } catch (IOException e) {
            System.err.println("Error en el receptor broadcast: " + e.getMessage());
        }
    }

    private void procesarNotificacion(int codigo, String datos) {
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
            default:
                System.out.println("\n[Alerta Global] Mensaje recibido (Código " + codigo + "): " + datos);
        }
        System.out.print("> ");
    }
}
