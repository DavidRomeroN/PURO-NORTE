package com.anticucheria.model;

import com.anticucheria.model.enums.EstadoPedido;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nula en los pedidos para llevar: el cliente no ocupa mesa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;

    /**
     * Mesas que la cuenta ocupa ademas de la principal. Un grupo grande junta la 12 con
     * la 13 y se sienta en las dos, pero paga una sola cuenta.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pedido_mesas_unidas",
            joinColumns = @JoinColumn(name = "pedido_id"),
            inverseJoinColumns = @JoinColumn(name = "mesa_id"))
    @OrderBy("numero")
    @Builder.Default
    private List<Mesa> mesasUnidas = new ArrayList<>();

    /** Solo en los pedidos para llevar. Se reinicia cada día para que no crezca sin fin. */
    @Column(name = "numero_llevar")
    private Integer numeroLlevar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mozo_id", nullable = false)
    private Usuario mozo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoPedido estado = EstadoPedido.ABIERTO;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "cerrado_en")
    private LocalDateTime cerradoEn;

    @Column(name = "anulado_en")
    private LocalDateTime anuladoEn;

    /**
     * Quien descarto la cuenta. Anular es la via mas comoda para que una mesa que si
     * consumio desaparezca sin pasar por caja, asi que queda registrado con nombre.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anulado_por")
    private Usuario anuladoPor;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;
}
