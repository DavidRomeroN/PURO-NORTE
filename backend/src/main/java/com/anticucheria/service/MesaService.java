package com.anticucheria.service;

import com.anticucheria.dto.request.MesaRequest;
import com.anticucheria.dto.response.MesaResponse;

import java.util.List;

public interface MesaService {

    List<MesaResponse> listar();

    MesaResponse obtener(Long id);

    MesaResponse crear(MesaRequest request);

    MesaResponse actualizar(Long id, MesaRequest request);

    void eliminar(Long id);
}
