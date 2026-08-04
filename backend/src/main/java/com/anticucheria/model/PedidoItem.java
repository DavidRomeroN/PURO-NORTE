package com.anticucheria.model;

import com.anticucheria.model.enums.EstadoDespacho;
import com.anticucheria.model.enums.TipoItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_item", nullable = false)
    private TipoItem tipoItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id")
    private Combo combo;

    @Column(nullable = false)
    @Builder.Default
    private Integer cantidad = 1;

    @Column(name = "precio_calculado", nullable = false, precision = 6, scale = 2)
    private BigDecimal precioCalculado;

    @Column(name = "precio_final", nullable = false, precision = 6, scale = 2)
    private BigDecimal precioFinal;

    @Column(name = "editado_manualmente", nullable = false)
    @Builder.Default
    private Boolean editadoManualmente = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editado_por")
    private Usuario editadoPor;

    @Column(name = "motivo_edicion")
    private String motivoEdicion;

    @Column(name = "para_llevar", nullable = false)
    @Builder.Default
    private Boolean paraLlevar = false;

    /**
     * Empieza pendiente. El parrillero lo marca cuando el plato sale; si se equivoca,
     * puede volverlo a pendiente sin tocar el resto del pedido.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_despacho", nullable = false)
    @Builder.Default
    private EstadoDespacho estadoDespacho = EstadoDespacho.PENDIENTE;

    @Column(name = "despachado_en")
    private LocalDateTime despachadoEn;

    private String observaciones;

    @OneToMany(mappedBy = "pedidoItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PedidoItemComponente> componentes = new ArrayList<>();
}
