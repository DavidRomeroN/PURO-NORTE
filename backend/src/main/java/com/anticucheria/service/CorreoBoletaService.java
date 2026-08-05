package com.anticucheria.service;

public interface CorreoBoletaService {

    void enviarAsync(Long boletaId, String correo);
}
