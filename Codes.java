package Proyecto2;

public class Codes
{
    public static final int NEW_DOC = 0;
    public static final int OPEN_DOC = 1;
    public static final int CLOSE_DOC = 2;
    public static final int DELETE_DOC = 3;
    public static final int UPDATE_DOC = 4;
    public static final int ERROR = 5;
    public static final int NEW_CLIENT = 6;

    // --- NUEVOS CÓDIGOS PARA P2P ---
    // El cliente avisa al servidor: "Guarde el fragmento N de este documento"
    public static final int REPORT_CHUNK = 7; 
    
    // El cliente pregunta: "¿Dónde están los fragmentos de este documento?"
    public static final int REQUEST_LOCATIONS = 8; 
    
    // El servidor responde con el mapa de ubicaciones
    public static final int LOCATIONS_RESPONSE = 9; 
    // Comando para el inventario en el servidor 
    public static final int INVENTORY =11;
    public static final int PURGE_FILE = 15; // O el número que tengas libre
}

