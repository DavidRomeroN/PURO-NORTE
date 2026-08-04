package com.anticucheria.service.factusmart;

public enum TipoArchivo {

    PDF("pdf", "application/pdf"),
    XML("xml", "application/xml"),
    CDR("cdr", "application/zip");

    private final String ruta;
    private final String contentType;

    TipoArchivo(String ruta, String contentType) {
        this.ruta = ruta;
        this.contentType = contentType;
    }

    public String ruta() {
        return ruta;
    }

    public String contentType() {
        return contentType;
    }
}
