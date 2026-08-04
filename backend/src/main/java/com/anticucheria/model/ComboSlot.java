package com.anticucheria.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "combo_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComboSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false)
    private Combo combo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_base_default_id", nullable = false)
    private ProductoBase productoBaseDefault;

    @Column(name = "es_cortesia", nullable = false)
    @Builder.Default
    private Boolean esCortesia = false;

    @Column(name = "es_sustituible", nullable = false)
    @Builder.Default
    private Boolean esSustituible = true;

    @Column(nullable = false)
    private Integer orden;
}
