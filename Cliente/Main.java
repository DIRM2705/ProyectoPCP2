package Proyecto2.Cliente;

import Proyecto2.Codes;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.*;

public class Main {

    public static void main(String[] args) throws Exception {
        DatagramSocket broad_sock = new DatagramSocket(1234);
        broad_sock.setBroadcast(true);
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        // Crear un socket para conectar al servidor
        Socket socket = new Socket("localhost", 1235);

        // Configurar stream de salida para enviar datos al servidor
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // Configurar stream de entrada para recibir datos del servidor
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );

        broad_sock.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Mensaje recibido: " + received);

        byte[] data = {Codes.NEW_DOC, 'h', 'o', 'l', 'a', '\n'}; // El primer byte es el código de la operación, el resto es el mensaje
        out.write(data);
        System.out.println("Mensaje enviado al servidor: " + new String(data, 1, data.length - 1));

        broad_sock.receive(packet);
        received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Mensaje recibido: " + received);

        data = new byte[]{Codes.NEW_DOC, 'h', 'o', 'l', 'a', '\n'}; // El primer byte es el código de la operación, el resto es el mensaje
        out.write(data);
        System.out.println("Mensaje enviado al servidor: " + new String(data, 1, data.length - 1));
        in.readLine(); // Leer la respuesta del servidor (si es necesario)

        // Cerrar el socket
        socket.close();
    }

}
