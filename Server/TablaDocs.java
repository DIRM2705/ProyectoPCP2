package Proyecto2.Server;

import Proyecto2.Codes;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import static Proyecto2.Server.Main.broadcastMessaging;

public class TablaDocs
{
    private HashMap<String, Documento> tabla;

    public TablaDocs()
    {
        tabla = new HashMap<>();
    }

    public synchronized void insertarDoc(String name, int origen, ArrayList<Integer> particiones) throws IllegalArgumentException, IOException
    {
        String titulo = name + "_og" + origen; //Construye el título del documento con su origen
        if (tabla.containsKey(titulo))
            throw new IllegalArgumentException("El documento ya existe en la tabla");

        Documento doc = new Documento(titulo, origen, particiones);
        tabla.put(titulo, doc); //Agrega el documento a la tabla con su lista de particiones
        //Anuncia a los clientes que se ha creado un nuevo documento
        broadcastMessaging.send(Codes.NEW_DOC, titulo.getBytes());
    }

    public void get(String name) throws IllegalArgumentException, IOException
    {
        if (!tabla.containsKey(name))
            throw new IllegalArgumentException("El documento no existe en la tabla");
        Documento doc = tabla.get(name); //Devuelve la lista de particiones asociada al documento
        doc.abrir(); //Marca el documento como abierto por el cliente
        broadcastMessaging.send(Codes.OPEN_DOC, name.getBytes());
    }

    public synchronized void eliminarDoc(String name) throws IllegalArgumentException, IllegalStateException, IOException
    {
        if (!tabla.containsKey(name))
            throw new IllegalArgumentException("El documento no existe en la tabla");
        else if (tabla.get(name).estaAbierto())
            throw new IllegalStateException("El documento no se puede eliminar porque está abierto");
        tabla.remove(name); //Elimina el documento de la tabla
        broadcastMessaging.send(Codes.DELETE_DOC, name.getBytes()); //Anuncia a los clientes que se ha eliminado un documento
    }
}
