package Proyecto2.Server;

import Proyecto2.Codes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static Proyecto2.Server.Main.*;

public class TablaDocs
{
    private HashMap<String, Documento> tabla;

    public TablaDocs()
    {
        tabla = new HashMap<>();
    }

    public boolean existeDoc(String name)
    {
        return tabla.containsKey(name);
    }

    public void incrementarSemaforos()
    {
        /*
        Incrementa el número de permisos disponibles en los semáforos de eliminación de cada documento, esto se hace
        cada vez que un nuevo cliente se conecta al servidor para permitir que el nuevo cliente pueda abrir
        los documentos existentes
         */

        for (Documento doc : tabla.values())
        {
            doc.eliminar.release();
        }
    }

    public synchronized void insertarDoc(String titulo) throws IOException
    {
        Documento doc = new Documento(""+(docCounter++)); //Crea un nuevo documento con un ID único
        tabla.put(titulo, doc); //Agrega el documento a la tabla con su lista de particiones
        //Anuncia a los clientes que se ha creado un nuevo documento
        broadcastMessaging.send(Codes.NEW_DOC, doc.getID());
    }

    public void abrirDoc(String name, byte[] origen) throws IOException
    {
        Documento doc = tabla.get(name); //Devuelve la info del documento
        if(doc.eliminar.tryAcquire())
        {
            doc.abrir();
            byte[] data = new byte[doc.getID().length + origen.length];
            System.arraycopy(doc.getID(), 0, data, 0, doc.getID().length);
            System.arraycopy(origen, 0, data, doc.getID().length, origen.length);
            broadcastMessaging.send(Codes.OPEN_DOC, data);
        }
        else
        {
            throw new IllegalStateException("El documento no se puede abrir porque está siendo eliminado");
        }
    }

    public void cerrarDoc(String name) throws IOException
    {
        Documento doc = tabla.get(name); //Devuelve la info del documento
        doc.cerrar();
        broadcastMessaging.send(Codes.CLOSE_DOC, doc.getID());
    }

    public void eliminarDoc(String name) throws IllegalStateException, IOException
    {
        Documento doc = tabla.get(name); //Devuelve la info del documento
        //Intenta adquirir el semáforo para eliminar el documento,
        //si no se puede adquirir es porque hay clientes con el documento abierto
        if(!doc.eliminar.tryAcquire(connections))
        {
            throw new IllegalStateException("El documento no se puede eliminar porque hay clientes con el documento abierto");
        }
        byte[] id = tabla.get(name).getID(); //Obtiene el ID del documento antes de eliminarlo
        tabla.remove(name); //Elimina el documento de la tabla
        broadcastMessaging.send(Codes.DELETE_DOC, id); //Anuncia a los clientes que se ha eliminado un documento
    }
}
