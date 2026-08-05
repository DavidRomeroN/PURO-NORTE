package com.anticucheria.service;

import com.anticucheria.dto.request.GenerarBoletaRequest;
import com.anticucheria.dto.response.BoletaResponse;
import com.anticucheria.dto.response.CreditosResponse;
import com.anticucheria.model.enums.EstadoSunat;
import com.anticucheria.service.factusmart.ArchivoComprobante;
import com.anticucheria.service.factusmart.TipoArchivo;

import java.time.LocalDate;
import java.util.List;

public interface BoletaService {

    BoletaResponse generar(GenerarBoletaRequest request, String cajeroUsername);

    List<BoletaResponse> listar(EstadoSunat estadoSunat, LocalDate desde, LocalDate hasta);

    BoletaResponse obtener(Long id);

    /** Vuelve a mandar a SUNAT una boleta que quedo pendiente. */
    BoletaResponse reintentar(Long id);

    /** Pregunta a SUNAT en vivo en que quedo la boleta y actualiza el estado local. */
    BoletaResponse sincronizar(Long id);

    ArchivoComprobante descargar(Long id, TipoArchivo tipo);

    CreditosResponse creditos();

    /** Reactiva la emision despues de corregir la configuracion del RUC. */
    void reactivarEmision();

    BoletaResponse marcarEnviadaWhatsapp(Long id);

    void marcarEnviadaCorreo(Long id);
}
