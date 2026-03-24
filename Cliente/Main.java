package Proyecto2.Cliente;

import java.net.*;

public class Main {

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(1234);
        socket.setBroadcast(true);
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Mensaje recibido: " + received);
        socket.close();
    }

}
