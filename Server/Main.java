package Proyecto2.Server;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.io.File;

public class Main
{
    public static final Set<String> clientesConectados = new CopyOnWriteArraySet<>();
    public static int connections = 0; //Número máximo de usuarios que pueden abrir un documento, se asigna a 255 para que el número de usuarios pueda ser representado en un byte
    public static final BroadcastConn broadcastMessaging = new BroadcastConn("172.31.15.255", 1234);
    public static TablaDocs tablaDocs = null;
    public static int docCounter = 0; //Contador para asignar un número único a cada documento creado, se incrementa cada vez que se crea un nuevo documento
    public static void main(String[] args)
    {
        File file = new java.io.File("tablaDocs.dat");
        tablaDocs = file.exists() ? new TablaDocs("tablaDocs.dat") : new TablaDocs();
        ConnectionManager manager = new ConnectionManager( 1235);
        manager.manageConnections();
    }
}