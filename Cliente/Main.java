package Proyecto2.Cliente;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class Main {

    public static void main(String[] args) throws Exception {
        DatagramSocket broad_sock = new DatagramSocket(1234);
        broad_sock.setBroadcast(true);
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        // Crear un socket para conectar al servidor en localhost puerto 9090
        Socket socket = new Socket("localhost", 1235);

        // Configurar stream de salida para enviar datos al servidor
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Configurar stream de entrada para recibir datos del servidor
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );

        broad_sock.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Mensaje recibido: " + received);

        // Cerrar el socket
        socket.close();
    }

}
