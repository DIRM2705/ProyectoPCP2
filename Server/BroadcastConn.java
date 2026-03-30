package Proyecto2.Server;
import java.io.IOException;
import java.net.*;

public class BroadcastConn implements AutoCloseable {
    private DatagramSocket socket = null;
    private int port = 0;
    private Inet4Address broadcastAddress = null;
    public BroadcastConn(String ip, int port)
    {
        try
        {
            socket = new DatagramSocket();
            this.port = port;
            socket.setBroadcast(true);
            broadcastAddress = (Inet4Address) InetAddress.getByName(ip);
        }
        catch (SocketException e)
        {
            System.out.println("Error al configurar el socket para broadcast: " + e.getMessage());
        }
        catch (UnknownHostException e)
        {
            System.out.println("Error al resolver la dirección de broadcast: " + e.getMessage());
        }
    }

    public void send(int code, byte[] data) throws IllegalArgumentException, IOException
    {
        //Unir el código del mensaje con los datos en un solo arreglo de bytes
        byte[] messageData = new byte[1 + data.length];
        messageData[0] = (byte) code;

        // Copiar los datos al arreglo del mensaje después del código
        System.arraycopy(data, 0, messageData, 1, data.length);
        DatagramPacket message = new DatagramPacket(messageData, messageData.length, broadcastAddress, port);
        socket.send(message);
    }

    @Override
    public void close()
    {
        if (socket != null && !socket.isClosed())
            socket.close();
    }
}

