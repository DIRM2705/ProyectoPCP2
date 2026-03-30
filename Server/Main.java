package Proyecto2.Server;

import Proyecto2.Codes;

import java.net.SocketException;
import java.net.UnknownHostException;

public class Main
{
    public static final BroadcastConn broadcastMessaging = new BroadcastConn("127.255.255.255", 1234);
    public static void main(String[] args)
    {
        ConnectionManager manager = new ConnectionManager( 1235);
        new Thread(manager).start();
    }
}
