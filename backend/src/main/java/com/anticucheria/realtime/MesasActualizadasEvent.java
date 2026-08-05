package com.anticucheria.realtime;

import com.anticucheria.dto.response.MesaResponse;

import java.util.List;

public record MesasActualizadasEvent(List<MesaResponse> mesas) {
}
