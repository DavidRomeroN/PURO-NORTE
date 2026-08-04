package com.anticucheria.model;

import com.anticucheria.model.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boletas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    /**
     * Identificador que devuelve FactuSmart y el unico con el que se puede consultar
     * estado, reenviar o descargar archivos. Si se pierde, el comprobante queda huerfano.
     */
    @Column(name = "external_id", length = 64)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoBoleta tipo;

    @Column(length = 10)
    private String serie;

    @Column(length = 15)
    private String correlativo;

    @Column(name = "monto_total", nullable = false, precision = 8, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false)
    @Builder.Default
    private FormaPago formaPago = FormaPago.CONTADO;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false)
    private MedioPago medioPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_sunat", nullable = false)
    @Builder.Default
    private EstadoSunat estadoSunat = EstadoSunat.PENDIENTE;

    /**
     * Emitida en modo simulado: figura como ACEPTADO para que el sistema sea usable de
     * punta a punta, pero no existe en FactuSmart ni ante SUNAT y no tiene validez
     * fiscal. Solo puede ocurrir en el perfil de desarrollo.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean simulada = false;

    @Column(name = "sunat_codigo", length = 10)
    private String sunatCodigo;

    @Column(name = "sunat_descripcion", length = 500)
    private String sunatDescripcion;

    /** DNI del cliente cuando lo da. SUNAT lo exige en boletas de S/700 o mas. */
    @Column(name = "cliente_documento", length = 15)
    private String clienteDocumento;

    @Column(name = "intentos_envio", nullable = false)
    @Builder.Default
    private Integer intentosEnvio = 0;

    @Column(name = "ultimo_intento_en")
    private LocalDateTime ultimoIntentoEn;

    @Column(name = "xml_url")
    private String xmlUrl;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "cdr_url")
    private String cdrUrl;

    @Column(name = "emitido_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime emitidoEn = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id", nullable = false)
    private Usuario cajero;

    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoletaDetalle> detalles = new ArrayList<>();
}
