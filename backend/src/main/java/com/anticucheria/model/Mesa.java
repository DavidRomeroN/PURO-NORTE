package com.anticucheria.model;

import com.anticucheria.model.enums.EstadoMesa;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mesas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoMesa estado = EstadoMesa.LIBRE;
}
