package Proyecto2.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionManager
{
    private final int port;
    public ConnectionManager(int port)
    {
        this.port = port;
    }

    public void manageConnections()
    {
        try (ServerSocket serverSocket = new ServerSocket(port))
        {
            System.out.println("Servidor escuchando en el puerto " + port);
            while (true)
            {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress());
                Handler handler = new Handler(clientSocket);
                handler.announce();
                new Thread(handler).start();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
