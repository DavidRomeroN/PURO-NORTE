package com.anticucheria.controller;

import com.anticucheria.dto.response.ConsultaClienteResponse;
import com.anticucheria.service.ConsultaClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ConsultaClienteService consultaClienteService;

    /**
     * Devuelve a quien pertenece un documento para que el cajero confirme el numero antes
     * de emitir. Solo caja y administracion: es una consulta a un servicio de pago y no
     * hace falta en el salon.
     */
    @GetMapping("/consultar")
    @PreAuthorize("hasAnyRole('CAJA','ADMIN')")
    public ConsultaClienteResponse consultar(@RequestParam(defaultValue = "1") String tipo,
                                             @RequestParam String numero) {
        return consultaClienteService.consultar(tipo, numero);
    }
}
