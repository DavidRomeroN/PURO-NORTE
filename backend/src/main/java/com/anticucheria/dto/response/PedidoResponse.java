package com.anticucheria.dto.response;

import com.anticucheria.model.enums.EstadoPedido;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PedidoResponse {

    private Long id;
    private Long mesaId;
    private Integer mesaNumero;
    /** Mesas que la cuenta ocupa además de la principal, cuando el grupo juntó mesas. */
    private List<MesaResumenResponse> mesasUnidas;
    /** Solo en los pedidos para llevar, donde reemplaza al número de mesa. */
    private Integer numeroLlevar;
    private Long mozoId;
    private String mozoNombre;
    private EstadoPedido estado;
    private LocalDateTime creadoEn;
    private LocalDateTime cerradoEn;
    private LocalDateTime anuladoEn;
    private String anuladoPorNombre;
    private String motivoAnulacion;
    private BigDecimal total;
    /** Cuántos platos todavía no salieron. La parrilla ordena y filtra con esto. */
    private Integer pendientesDespacho;
    private List<PedidoItemResponse> items;
}
