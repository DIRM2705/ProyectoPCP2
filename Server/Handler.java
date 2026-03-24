package Proyecto2.Server;

import Proyecto2.Codes;
import java.util.ArrayList;

public class Handler
{
    private TablaDocs tablaDocs;
    private BroadcastConn broadcastMessaging;
    public Handler()
    {
        tablaDocs = new TablaDocs();
        broadcastMessaging = new BroadcastConn(1234);
    }

    public void addDoc(String doc, int origen, ArrayList<Integer> particiones)
    {
        //TODO: Verificar espacio disponible en los clientes para almacenar las particiones del documento
        String title = doc + "_og" + origen; //Construye el título del documento con su origen
        try
        {
            tablaDocs.insertarDoc(title, origen, particiones);
            try
            {
                //Envía un mensaje de broadcast para indicar que se ha agregado un nuevo documento a la tabla
                broadcastMessaging.send(Codes.NEW_DOC, title.getBytes());
            }
            catch (java.io.IOException e)
            {
                System.out.println("Error al enviar el mensaje de broadcast: " + e.getMessage());
            }
            catch (IllegalArgumentException e)
            {
                System.out.println("Error al enviar el mensaje de broadcast: " + e.getMessage());
            }
            System.out.println("Documento agregado: " + title);
        }
        catch (IllegalArgumentException e) //El documento ya existe en la tabla
        {
            //TODO: Mandar mensaje al cliente origen actualizar o crear una copia del documento
        }
    }

    public void abrirDoc(String doc, int origen) {
        try
        {
            Documento documento = tablaDocs.get(doc, origen);
            documento.abrir();
            System.out.println("Documento abierto: " + doc);
        }
        catch (IllegalArgumentException e) //El documento no existe en la tabla
        {
            //TODO: Mandar mensaje de error al cliente origen
        }
    }

    public void cerrarDoc(String doc, int origen)
    {
        try {
            Documento documento = tablaDocs.get(doc, origen);
            documento.cerrar();
            System.out.println("Documento cerrado: " + doc);
        }
        catch (IllegalArgumentException e) //El documento no existe en la tabla
        {

        }
    }

    public void eliminarDoc(String doc, int origen)
    {
        try {
            tablaDocs.eliminarDoc(doc, origen);

            System.out.println("Documento eliminado: " + doc);
        }
        catch (IllegalArgumentException e) //El documento no existe en la tabla
        {
            //TODO: Mandar mensaje de error al cliente origen
        }
        catch (IllegalStateException e) //El documento está abierto
        {
            //TODO: Mandar mensaje de error al cliente origen
        }
    }
}
