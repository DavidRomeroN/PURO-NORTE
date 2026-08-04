package com.anticucheria.controller;

import com.anticucheria.dto.request.GenerarBoletaRequest;
import com.anticucheria.dto.response.BoletaResponse;
import com.anticucheria.dto.response.CreditosResponse;
import com.anticucheria.model.enums.EstadoSunat;
import com.anticucheria.service.BoletaService;
import com.anticucheria.service.factusmart.ArchivoComprobante;
import com.anticucheria.service.factusmart.TipoArchivo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/boletas")
@RequiredArgsConstructor
public class BoletaController {

    private final BoletaService boletaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CAJA','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BoletaResponse generar(@Valid @RequestBody GenerarBoletaRequest request,
                                  @AuthenticationPrincipal UserDetails user) {
        return boletaService.generar(request, user.getUsername());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CAJA','ADMIN')")
    public List<BoletaResponse> listar(
            @RequestParam(required = false) EstadoSunat estadoSunat,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return boletaService.listar(estadoSunat, desde, hasta);
    }

    /** Antes de /{id} para que "creditos" no se lea como un id. */
    @GetMapping("/creditos")
    @PreAuthorize("hasRole('ADMIN')")
    public CreditosResponse creditos() {
        return boletaService.creditos();
    }

    @PostMapping("/reactivar-emision")
    @PreAuthorize("hasRole('ADMIN')")
    public CreditosResponse reactivarEmision() {
        boletaService.reactivarEmision();
        return boletaService.creditos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CAJA','ADMIN')")
    public BoletaResponse obtener(@PathVariable Long id) {
        return boletaService.obtener(id);
    }

    @PostMapping("/{id}/reintentar")
    @PreAuthorize("hasAnyRole('CAJA','ADMIN')")
    public BoletaResponse reintentar(@PathVariable Long id) {
        return boletaService.reintentar(id);
    }

    @PostMapping("/{id}/sincronizar")
    @PreAuthorize("hasAnyRole('CAJA','ADMIN')")
    public BoletaResponse sincronizar(@PathVariable Long id) {
        return boletaService.sincronizar(id);
    }

    /** Lo unico que se necesita en el 99% de los casos: es lo que se le da al cliente. */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('CAJA','ADMIN')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        return archivo(boletaService.descargar(id, TipoArchivo.PDF));
    }

    @GetMapping("/{id}/xml")
    @PreAuthorize("hasAnyRole('CAJA','ADMIN')")
    public ResponseEntity<byte[]> xml(@PathVariable Long id) {
        return archivo(boletaService.descargar(id, TipoArchivo.XML));
    }

    @GetMapping("/{id}/cdr")
    @PreAuthorize("hasAnyRole('CAJA','ADMIN')")
    public ResponseEntity<byte[]> cdr(@PathVariable Long id) {
        return archivo(boletaService.descargar(id, TipoArchivo.CDR));
    }

    private ResponseEntity<byte[]> archivo(ArchivoComprobante archivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(archivo.nombre()).toString())
                .body(archivo.contenido());
    }
}
