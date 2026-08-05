package com.anticucheria.service.impl;

import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.exception.ResourceNotFoundException;
import com.anticucheria.model.Boleta;
import com.anticucheria.model.enums.EstadoSunat;
import com.anticucheria.repository.BoletaRepository;
import com.anticucheria.service.BoletaService;
import com.anticucheria.service.CorreoBoletaService;
import com.anticucheria.service.FactuSmartClientService;
import com.anticucheria.service.factusmart.ArchivoComprobante;
import com.anticucheria.service.factusmart.TipoArchivo;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CorreoBoletaServiceImpl implements CorreoBoletaService {

    private final BoletaRepository boletaRepository;
    private final FactuSmartClientService factuSmartClientService;
    private final BoletaService boletaService;
    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;

    public CorreoBoletaServiceImpl(BoletaRepository boletaRepository,
                                   FactuSmartClientService factuSmartClientService,
                                   BoletaService boletaService,
                                   ObjectProvider<JavaMailSender> mailSender) {
        this.boletaRepository = boletaRepository;
        this.factuSmartClientService = factuSmartClientService;
        this.boletaService = boletaService;
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void enviarAsync(Long boletaId, String correo) {
        try {
            enviar(boletaId, correo);
            boletaService.marcarEnviadaCorreo(boletaId);
        } catch (Exception ex) {
            log.error("No se pudo enviar la boleta {} a {}: {}", boletaId, correo, ex.getMessage());
        }
    }

    private void enviar(Long boletaId, String correo) throws Exception {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!mailEnabled || sender == null) {
            throw new ReglaNegocioException("El envío por correo no está configurado");
        }
        Boleta boleta = boletaRepository.findById(boletaId)
                .orElseThrow(() -> new ResourceNotFoundException("Boleta no encontrada"));
        if (boleta.getEstadoSunat() != EstadoSunat.ACEPTADO || boleta.isSimulada()) {
            throw new ReglaNegocioException("Solo se puede enviar una boleta aceptada por SUNAT");
        }

        String serieCorr = (boleta.getSerie() == null ? "BA" : boleta.getSerie())
                + "-" + (boleta.getCorrelativo() == null ? boleta.getId() : boleta.getCorrelativo());
        String asunto = "Tu boleta " + serieCorr + " - Anticuchería Puro Norte";

        String enlace = null;
        if (boleta.getTokenPublico() != null) {
            String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
            enlace = base + "/api/publico/boletas/" + boleta.getTokenPublico() + "/pdf";
        }

        ArchivoComprobante pdf = factuSmartClientService.descargar(boleta, TipoArchivo.PDF);

        MimeMessage mensaje = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
        helper.setTo(correo);
        if (mailFrom != null && !mailFrom.isBlank()) {
            helper.setFrom(mailFrom);
        }
        helper.setSubject(asunto);
        String cuerpo = """
                ¡Gracias por tu visita!

                Anticuchería Puro Norte
                Boleta %s
                Total: S/ %s

                %s
                """.formatted(
                serieCorr,
                boleta.getMontoTotal(),
                enlace == null ? "Adjuntamos tu boleta en PDF." : "Descarga tu boleta aquí:\n" + enlace);
        helper.setText(cuerpo);
        helper.addAttachment(pdf.nombre(), new ByteArrayResource(pdf.contenido()));
        sender.send(mensaje);
    }
}
