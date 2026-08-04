package com.anticucheria.service;

import com.anticucheria.dto.request.ActualizarUsuarioRequest;
import com.anticucheria.dto.request.CambiarPasswordRequest;
import com.anticucheria.dto.request.CrearUsuarioRequest;
import com.anticucheria.dto.response.UsuarioResponse;

import java.util.List;

public interface UsuarioService {

    List<UsuarioResponse> listar(boolean incluirInactivos);

    UsuarioResponse obtener(Long id);

    UsuarioResponse obtenerPorUsername(String username);

    UsuarioResponse crear(CrearUsuarioRequest request);

    UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest request);

    void cambiarPassword(Long id, CambiarPasswordRequest request);

    UsuarioResponse cambiarEstado(Long id, boolean activo, String usernameActual);
}
