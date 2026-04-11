package Proyecto2.Cliente;

import java.io.Serializable;

// Implementamos Serializable por si decides enviarlos usando ObjectOutputStream más adelante
public class Fragmento implements Serializable {
    private String idDocumento;
    private int numeroSecuencia;
    private boolean esUltimo;
    private byte[] datos;

    public Fragmento(String idDocumento, int numeroSecuencia, boolean esUltimo, byte[] datos) {
        this.idDocumento = idDocumento;
        this.numeroSecuencia = numeroSecuencia;
        this.esUltimo = esUltimo;
        this.datos = datos;
    }

    // Getters
    public String getIdDocumento() { return idDocumento; }
    public int getNumeroSecuencia() { return numeroSecuencia; }
    public boolean isEsUltimo() { return esUltimo; }
    public byte[] getDatos() { return datos; }
}