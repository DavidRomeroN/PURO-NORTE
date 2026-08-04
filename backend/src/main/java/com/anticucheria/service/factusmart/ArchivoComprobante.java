package com.anticucheria.service.factusmart;

/** Un PDF, XML o CDR descargado del proveedor, listo para reenviar al navegador. */
public record ArchivoComprobante(byte[] contenido, String contentType, String nombre) {
}
