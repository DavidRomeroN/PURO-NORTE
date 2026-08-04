package com.anticucheria.model;

import com.anticucheria.model.enums.TipoProducto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "productos_base")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoProducto tipo;

    @Column(name = "precio_unitario", nullable = false, precision = 6, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
