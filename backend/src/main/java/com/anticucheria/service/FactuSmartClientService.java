package com.anticucheria.service;

import com.anticucheria.model.Boleta;
import com.anticucheria.model.PedidoItem;
import com.anticucheria.service.factusmart.ArchivoComprobante;
import com.anticucheria.service.factusmart.FactuSmartRespuesta;
import com.anticucheria.service.factusmart.TipoArchivo;

import java.util.List;

/** Unico punto de salida del sistema hacia FactuSmart. */
public interface FactuSmartClientService {

    /** Emite la boleta. Cuesta 1 credito, tambien en sandbox. */
    FactuSmartRespuesta emitir(Boleta boleta, List<PedidoItem> items);

    /** Reenvia a SUNAT un comprobante ya emitido. Gratis. */
    FactuSmartRespuesta reenviar(Boleta boleta);

    /** Pregunta en vivo a SUNAT por el estado real del comprobante. Gratis. */
    FactuSmartRespuesta consultarEnSunat(Boleta boleta);

    /** Descarga el PDF, XML o CDR. Gratis, y solo existe si SUNAT lo acepto. */
    ArchivoComprobante descargar(Boleta boleta, TipoArchivo tipo);

    /** Creditos que quedan en la cuenta. Gratis. */
    int creditosDisponibles();

    /** Comprueba que la URL y la API key sirven. Gratis. */
    boolean ping();
}
