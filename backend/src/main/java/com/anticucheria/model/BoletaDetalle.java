package com.anticucheria.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "boleta_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoletaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boleta_id", nullable = false)
    private Boleta boleta;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private Integer cantidad = 1;

    @Column(name = "precio_unitario", nullable = false, precision = 6, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal subtotal;
}
