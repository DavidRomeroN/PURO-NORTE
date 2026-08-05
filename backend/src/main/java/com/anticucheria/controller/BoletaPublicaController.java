package com.anticucheria.controller;

import com.anticucheria.service.BoletaPublicaService;
import com.anticucheria.service.factusmart.ArchivoComprobante;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publico/boletas")
@RequiredArgsConstructor
public class BoletaPublicaController {

    private final BoletaPublicaService boletaPublicaService;

    @GetMapping("/{token}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String token) {
        ArchivoComprobante archivo = boletaPublicaService.descargarPdfPublico(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + archivo.nombre() + "\"")
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .body(archivo.contenido());
    }
}
