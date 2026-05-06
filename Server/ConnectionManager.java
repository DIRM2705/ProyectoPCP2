package Proyecto2.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import static Proyecto2.Server.Main.connections;
import static Proyecto2.Server.Main.tablaDocs;

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
                
                // Extraemos la IP y el Puerto de la conexión entrante
                String ipCliente = clientSocket.getInetAddress().getHostAddress();
                int puertoConexion = clientSocket.getPort();
                
                System.out.println("[Tracker] Conexión TCP entrante desde: " + ipCliente + ":" + puertoConexion);
                
                connections++;
                tablaDocs.incrementarSemaforos();
                Handler handler = new Handler(clientSocket);
                new Thread(handler).start();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}