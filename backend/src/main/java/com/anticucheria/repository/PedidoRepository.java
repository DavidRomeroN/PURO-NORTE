package com.anticucheria.repository;

import com.anticucheria.model.Pedido;
import com.anticucheria.model.enums.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByEstadoOrderByCreadoEnDesc(EstadoPedido estado);

    List<Pedido> findByEstadoInOrderByCreadoEnDesc(List<EstadoPedido> estados);

    List<Pedido> findAllByOrderByCreadoEnDesc();

    /**
     * Pedidos vivos para la parrilla, del más viejo al más nuevo: el que pidió primero
     * se cocina primero. Incluye cerrados sin pagar porque la comida puede seguir saliendo.
     */
    @Query("""
            select p from Pedido p
            where p.estado in (com.anticucheria.model.enums.EstadoPedido.ABIERTO,
                               com.anticucheria.model.enums.EstadoPedido.CERRADO)
            order by p.creadoEn asc
            """)
    List<Pedido> findParaParrilla();

    Optional<Pedido> findByMesaIdAndEstado(Long mesaId, EstadoPedido estado);

    /**
     * Cuentas vivas que ocupan una mesa, sea como principal o unida. Una cuenta cerrada
     * pero sin pagar sigue ocupando la mesa: el grupo está ahí esperando para pagar. Las
     * pagadas y las anuladas ya no ocupan nada, aunque sus filas de mesas unidas queden
     * como historia.
     */
    @Query("""
            select distinct p from Pedido p
            left join p.mesasUnidas unida
            where p.estado in (com.anticucheria.model.enums.EstadoPedido.ABIERTO,
                               com.anticucheria.model.enums.EstadoPedido.CERRADO)
              and (p.mesa.id = :mesaId or unida.id = :mesaId)
            """)
    List<Pedido> cuentasVivasEnMesa(@Param("mesaId") Long mesaId);

    /** Último número de "para llevar" usado en el rango, para seguir la cuenta del día. */
    @Query("""
            select coalesce(max(p.numeroLlevar), 0) from Pedido p
            where p.mesa is null and p.creadoEn between :inicio and :fin
            """)
    int ultimoNumeroLlevar(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
