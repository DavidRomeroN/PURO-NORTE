package com.anticucheria.service.factusmart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Corta el envio a SUNAT cuando aparece un error de configuracion del RUC.
 *
 * Existe por un caso real que cuenta la documentacion del proveedor: un integrador paso
 * un dia entero emitiendo comprobantes que fallaban todos por lo mismo. Si la clave SOL
 * esta mal, la emision numero cien fallara igual que la primera.
 *
 * Solo se suspende el envio: los pedidos se siguen registrando y cobrando con normalidad.
 */
@Component
@Slf4j
public class InterruptorDeEmision {

    private final AtomicReference<String> motivo = new AtomicReference<>(null);

    public void bloquear(String codigo, String descripcion) {
        String texto = "SUNAT " + codigo + ": " + descripcion;
        if (motivo.compareAndSet(null, texto)) {
            log.error("EMISION SUSPENDIDA. {}. Es configuracion del RUC, no un problema puntual: "
                    + "revisar el usuario secundario SOL y su permiso de facturacion electronica "
                    + "en el panel de FactuSmart. Las ventas se siguen registrando sin comprobante.", texto);
        }
    }

    public boolean estaBloqueado() {
        return motivo.get() != null;
    }

    public String motivo() {
        return motivo.get();
    }

    /** Para usar cuando ya se corrigio la configuracion, sin tener que reiniciar la app. */
    public void reactivar() {
        String anterior = motivo.getAndSet(null);
        if (anterior != null) {
            log.warn("Emision reactivada manualmente. Estaba suspendida por: {}", anterior);
        }
    }
}
