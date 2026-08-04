package com.anticucheria.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pedido_item_componentes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoItemComponente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_item_id", nullable = false)
    private PedidoItem pedidoItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_base_id", nullable = false)
    private ProductoBase productoBase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_slot_id")
    private ComboSlot comboSlot;

    @Column(name = "es_sustitucion", nullable = false)
    @Builder.Default
    private Boolean esSustitucion = false;

    @Column(name = "precio_unitario_snapshot", nullable = false, precision = 6, scale = 2)
    private BigDecimal precioUnitarioSnapshot;
}
