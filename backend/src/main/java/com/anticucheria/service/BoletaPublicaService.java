package com.anticucheria.service;

import com.anticucheria.service.factusmart.ArchivoComprobante;

public interface BoletaPublicaService {

    ArchivoComprobante descargarPdfPublico(String token);
}
