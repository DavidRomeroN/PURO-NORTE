package com.anticucheria.config;

import com.anticucheria.model.Combo;
import com.anticucheria.model.ComboSlot;
import com.anticucheria.model.Mesa;
import com.anticucheria.model.ProductoBase;
import com.anticucheria.model.Usuario;
import com.anticucheria.model.enums.EstadoMesa;
import com.anticucheria.model.enums.Rol;
import com.anticucheria.model.enums.TipoProducto;
import com.anticucheria.repository.ComboRepository;
import com.anticucheria.repository.ComboSlotRepository;
import com.anticucheria.repository.MesaRepository;
import com.anticucheria.repository.ProductoBaseRepository;
import com.anticucheria.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final int TOTAL_MESAS = 13;

    private static final String CORAZON = "Corazón";
    private static final String CARNE = "Carne";
    private static final String SALCHICHA = "Salchicha";
    private static final String POLLO = "Pollo";
    private static final String MOLLEJA = "Molleja";
    private static final String CHORIZO = "Chorizo";

    private static final String MIXTO_SIMPLE = "Mixto Simple";
    private static final String MIXTO_ESPECIAL = "Mixto Especial";

    private final ProductoBaseRepository productoBaseRepository;
    private final ComboRepository comboRepository;
    private final ComboSlotRepository comboSlotRepository;
    private final MesaRepository mesaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-usuario:admin}")
    private String adminUsuario;

    /** Vacío = no crea admin. En prod debe venir de ADMIN_PASSWORD y ser fuerte. */
    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.catalogo:true}")
    private boolean seedCatalogo;

    @Value("${app.seed.admin:true}")
    private boolean seedAdmin;

    @Override
    @Transactional
    public void run(String... args) {
        if (seedCatalogo) {
            seedProductos();
            seedCombos();
            seedMesas();
        }
        if (seedAdmin) {
            seedAdmin();
        }
    }

    private void seedProductos() {
        crearProductoSiFalta(CORAZON, TipoProducto.ANTICUCHO, "6.00");
        crearProductoSiFalta(CARNE, TipoProducto.ANTICUCHO, "7.00");
        // El precio de la salchicha corresponde a un par; se usa tal cual como componente.
        crearProductoSiFalta(SALCHICHA, TipoProducto.ANTICUCHO, "4.00");
        crearProductoSiFalta(POLLO, TipoProducto.ANTICUCHO, "6.00");
        crearProductoSiFalta(MOLLEJA, TipoProducto.ANTICUCHO, "6.00");
        crearProductoSiFalta(CHORIZO, TipoProducto.ANTICUCHO, "6.00");

        crearProductoSiFalta("Taper", TipoProducto.EXTRA, "1.00");
        crearProductoSiFalta("Bandeja", TipoProducto.EXTRA, "0.50");
        // TODO: definir precio de la porción de papa con el dueño.
        crearProductoSiFalta("Porción de papa", TipoProducto.EXTRA, "0.00");

        // TODO: cargar los precios de las bebidas desde el panel de admin.
        crearProductoSiFalta("Gaseosa", TipoProducto.BEBIDA, "0.00");
        crearProductoSiFalta("Mate", TipoProducto.BEBIDA, "0.00");
        crearProductoSiFalta("Emoliente", TipoProducto.BEBIDA, "0.00");
    }

    private void seedCombos() {
        Combo mixtoSimple = crearComboSiFalta(MIXTO_SIMPLE, "17.00");
        crearSlotsSiFaltan(mixtoSimple, List.of(
                new SlotSeed(1, POLLO, false, true),
                new SlotSeed(2, CARNE, false, true),
                new SlotSeed(3, CORAZON, false, true),
                new SlotSeed(4, SALCHICHA, true, false)));

        Combo mixtoEspecial = crearComboSiFalta(MIXTO_ESPECIAL, "23.00");
        crearSlotsSiFaltan(mixtoEspecial, List.of(
                new SlotSeed(1, POLLO, false, true),
                new SlotSeed(2, CARNE, false, true),
                new SlotSeed(3, CORAZON, false, true),
                new SlotSeed(4, CHORIZO, false, true),
                new SlotSeed(5, SALCHICHA, true, false)));
    }

    private void seedMesas() {
        for (int numero = 1; numero <= TOTAL_MESAS; numero++) {
            int actual = numero;
            mesaRepository.findByNumero(numero).orElseGet(() -> {
                log.info("Seed: creando mesa {}", actual);
                return mesaRepository.save(Mesa.builder()
                        .numero(actual)
                        .estado(EstadoMesa.LIBRE)
                        .build());
            });
        }
    }

    private void seedAdmin() {
        if (usuarioRepository.existsByUsuario(adminUsuario)) {
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("Seed: no hay ADMIN_PASSWORD; no se crea el usuario '{}'.", adminUsuario);
            return;
        }
        if (esContrasenaDebil(adminPassword)) {
            throw new ConfiguracionInseguraException(
                    "La contraseña del admin del seed es demasiado débil o es la de ejemplo.",
                    """
                    Define ADMIN_PASSWORD con al menos 12 caracteres (letras y números) \
                    y vuelve a arrancar. No uses admin123 ni palabras obvias.

                    En producción, después del primer arranque pon app.seed.enabled=false \
                    (o SEED_ENABLED=false) para que no se vuelva a crear nada.""");
        }
        usuarioRepository.save(Usuario.builder()
                .nombre("Administrador")
                .usuario(adminUsuario)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .rol(Rol.ADMIN)
                .activo(true)
                .build());
        log.warn("Seed: usuario '{}' creado. Cambia o guarda esa contraseña con cuidado.", adminUsuario);
    }

    private boolean esContrasenaDebil(String password) {
        if (password.length() < 12) {
            return true;
        }
        String baja = password.toLowerCase();
        return baja.equals("admin123")
                || baja.equals("password")
                || baja.equals("123456789012")
                || baja.contains("anticucho");
    }

    private ProductoBase crearProductoSiFalta(String nombre, TipoProducto tipo, String precio) {
        return productoBaseRepository.findByNombre(nombre).orElseGet(() -> {
            log.info("Seed: creando producto {}", nombre);
            return productoBaseRepository.save(ProductoBase.builder()
                    .nombre(nombre)
                    .tipo(tipo)
                    .precioUnitario(new BigDecimal(precio))
                    .activo(true)
                    .build());
        });
    }

    private Combo crearComboSiFalta(String nombre, String precioBase) {
        return comboRepository.findByNombre(nombre).orElseGet(() -> {
            log.info("Seed: creando combo {}", nombre);
            return comboRepository.save(Combo.builder()
                    .nombre(nombre)
                    .precioBase(new BigDecimal(precioBase))
                    .activo(true)
                    .build());
        });
    }

    private void crearSlotsSiFaltan(Combo combo, List<SlotSeed> slots) {
        if (!comboSlotRepository.findByComboIdOrderByOrdenAsc(combo.getId()).isEmpty()) {
            return;
        }
        for (SlotSeed seed : slots) {
            ProductoBase porDefecto = productoBaseRepository.findByNombre(seed.productoDefault())
                    .orElseThrow(() -> new IllegalStateException(
                            "Seed inconsistente: falta el producto " + seed.productoDefault()));
            comboSlotRepository.save(ComboSlot.builder()
                    .combo(combo)
                    .productoBaseDefault(porDefecto)
                    .esCortesia(seed.esCortesia())
                    .esSustituible(seed.esSustituible())
                    .orden(seed.orden())
                    .build());
        }
        log.info("Seed: creados {} slots para el combo {}", slots.size(), combo.getNombre());
    }

    private record SlotSeed(int orden, String productoDefault, boolean esCortesia, boolean esSustituible) {
    }
}
