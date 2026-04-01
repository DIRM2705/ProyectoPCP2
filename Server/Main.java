package Proyecto2.Server;

public class Main
{
    public static final BroadcastConn broadcastMessaging = new BroadcastConn("127.255.255.255", 1234);
    public static final TablaDocs tablaDocs = new TablaDocs();
    public static void main(String[] args)
    {
        ConnectionManager manager = new ConnectionManager( 1235);
        manager.manageConnections();
    }
}
