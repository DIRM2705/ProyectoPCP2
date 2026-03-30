package Proyecto2.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionManager implements Runnable
{
    private final int port;
    public ConnectionManager(int port)
    {
        this.port = port;
    }

    @Override
    public void run()
    {
        try(ServerSocket socket = new ServerSocket(port))
        {
            System.out.println("Servidor escuchando en el puerto " + port);
            while (true)
            {
                Socket sock = socket.accept();
                System.out.println("Nueva conexión aceptada desde " + sock.getInetAddress().getHostAddress());
                Handler handler = new Handler(sock);
                handler.announce();
                handler.processMessage();
            }
        }
        catch (IOException e)
        {
            System.out.println("Error al aceptar una conexión: " + e.getMessage());
        }
    }
}
