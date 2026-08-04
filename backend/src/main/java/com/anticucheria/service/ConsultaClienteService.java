package com.anticucheria.service;

import com.anticucheria.dto.response.ConsultaClienteResponse;

public interface ConsultaClienteService {

    /** Codigo SUNAT del DNI, el mismo que viaja en la boleta. */
    String TIPO_DNI = "1";

    /**
     * Busca a quien pertenece un documento. Nunca lanza por un fallo del proveedor: si no
     * se pudo verificar, lo devuelve como resultado para que la caja siga funcionando.
     *
     * @param tipoDocumento codigo SUNAT del tipo de documento; hoy solo "1" (DNI)
     */
    ConsultaClienteResponse consultar(String tipoDocumento, String numero);
}
