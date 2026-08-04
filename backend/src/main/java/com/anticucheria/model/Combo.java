package com.anticucheria.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "combos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Combo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(name = "precio_base", nullable = false, precision = 6, scale = 2)
    private BigDecimal precioBase;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
