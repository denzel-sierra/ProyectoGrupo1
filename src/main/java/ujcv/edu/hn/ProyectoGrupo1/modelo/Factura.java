package ujcv.edu.hn.ProyectoGrupo1.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.openxava.annotations.*;
import org.openxava.util.Users;
import org.openxava.jpa.XPersistence;

import jakarta.persistence.*;
import jakarta.validation.ValidationException;

import lombok.*;

/**
 * Entidad Factura adaptada para OpenXava y normativa fiscal SAR (Honduras).
 * Totales calculados en tiempo real con @Calculation, desstock a 'Vendida' e impuesto fijo al 12%.
 */
@Entity
@Getter @Setter
@RemoveValidator(ujcv.edu.hn.ProyectoGrupo1.validadores.ValidadorBorradoFactura.class)

@View(members =
        "DatosFiscales [" +
                "   cai;" +
                "   rangoSar;" +
                "   fechaLimiteEmision" +
                "];" +
                "Factura [" +
                "   estado, numeroFactura;" +
                "   fechaHora, fechaEmision;" +
                "   vendedor" +
                "];" +
                "Cliente [" +
                "   cliente;" +
                "   rtnCliente, tipoCliente" +
                "];" +
                "DetalleDeLaFactura [" +
                "   autopartes" +
                "];" +
                "Pagos [" +
                "   pagos" +
                "];" +
                "Totales [" +
                "   porcentajeImpuesto;" +
                "   subtotal, impuesto, total;" +
                "   totalPagado, saldoPendiente" +
                "]"
)

@Tab(
        properties = "numeroFactura, estado, fechaHora, cliente.nombre, cliente.rtnDni, vendedor, subtotal, impuesto, total",
        defaultOrder = "${id} desc"
)

public class Factura {

    private static final BigDecimal IMPUESTO_POR_DEFECTO =
            new BigDecimal("12.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ReadOnly
    @Hidden
    Integer id;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    @ReadOnly
    EstadoFactura estado = EstadoFactura.Borrador;

    @Column(length = 30, unique = true)
    @ReadOnly
    String numeroFactura;

    @Column(length = 50)
    @ReadOnly
    String cai;

    @Column(length = 100)
    @ReadOnly
    String rangoSar;

    @Column(name = "fecha_limite_emision")
    @ReadOnly
    LocalDate fechaLimiteEmision;

    @ReadOnly
    LocalDateTime fechaHora;

    @Column(name = "fecha_emision")
    @ReadOnly
    LocalDateTime fechaEmision;

    @Column(length = 100)
    @ReadOnly
    String vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @Required
    @DescriptionsList(descriptionProperties = "nombre, rtnDni")
    Cliente cliente;

    @Depends("cliente")
    @ReadOnly
    public String getRtnCliente() {
        return cliente != null ? cliente.getRtnDni() : "";
    }

    @Depends("cliente")
    @ReadOnly
    public String getTipoCliente() {
        return cliente != null ? cliente.getTipo() : "";
    }

    @SearchListCondition("${estado} = 'Disponible'")
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "factura_autopartes",
            joinColumns = @JoinColumn(name = "factura_id"),
            inverseJoinColumns = @JoinColumn(name = "autoparte_id")
    )
    @ListProperties("sku, descripcion, marca, modelo, precioBase+")
    Collection<Autoparte> autopartes = new ArrayList<>();

    @OneToMany(
            mappedBy = "factura",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @ListProperties("metodo, monto+, referencia")
    Collection<PagoFactura> pagos = new ArrayList<>();

    // 1. IMPUESTO FIJO: Con @ReadOnly queda establecido en 12.00% y no editable
    @Column(name = "porcentaje_impuesto", precision = 5, scale = 2)
    @ReadOnly
    BigDecimal porcentajeImpuesto = IMPUESTO_POR_DEFECTO;

    // 2. TOTALES EN VIVO: Fórmulas que OpenXava calcula en tiempo real en la pantalla
    @Column(precision = 12, scale = 2)
    @Money
    @ReadOnly
    @Calculation("sum(autopartes.precioBase)")
    BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Money
    @ReadOnly
    @Calculation("subtotal * 0.12")
    BigDecimal impuesto = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Money
    @ReadOnly
    @Calculation("subtotal + impuesto")
    BigDecimal total = BigDecimal.ZERO;

    @Column(name = "total_pagado", precision = 12, scale = 2)
    @Money
    @ReadOnly
    @Calculation("sum(pagos.monto)")
    BigDecimal totalPagado = BigDecimal.ZERO;

    @Column(name = "saldo_pendiente", precision = 12, scale = 2)
    @Money
    @ReadOnly
    @Calculation("total - totalPagado")
    BigDecimal saldoPendiente = BigDecimal.ZERO;

    @Transient
    @Hidden
    boolean operacionInterna;

    @Transient
    private Map<Integer, Autoparte> autopartesAlCargar = new HashMap<>();

    public boolean esBorrador() {
        return estado == null || estado == EstadoFactura.Borrador;
    }

    // Guarda las piezas originales al cargar la factura
    @PostLoad
    private void cargarAutopartesAnteriores() {
        autopartesAlCargar = new HashMap<>();
        if (autopartes != null) {
            for (Autoparte a : autopartes) {
                if (a != null && a.getId() != null) {
                    autopartesAlCargar.put(a.getId(), a);
                }
            }
        }
    }

    // DESSTOCK INMEDIATO SEGURO: Cambia a Vendida sin queries manuales que provoquen StackOverflow
    private void actualizarStockAutopartes() {
        if (autopartes == null) return;
        Set<Integer> actualesIds = new HashSet<>();

        for (Autoparte a : autopartes) {
            if (a == null || a.getId() == null) continue;
            actualesIds.add(a.getId());

            // Al tener CascadeType.MERGE, Hibernate actualiza la autoparte automáticamente
            a.setEstado(EstadoAutoparte.Vendida);
        }

        // Si se retiró alguna pieza que estaba antes en la factura, regresarla a 'Disponible'
        if (autopartesAlCargar != null) {
            for (Map.Entry<Integer, Autoparte> entrada : autopartesAlCargar.entrySet()) {
                if (!actualesIds.contains(entrada.getKey())) {
                    Autoparte anterior = entrada.getValue();
                    if (anterior != null) {
                        anterior.setEstado(EstadoAutoparte.Disponible);
                    }
                }
            }
        }
    }

    // Emisión de factura con datos SAR de Honduras
    public void emitir(EntityManager em) {
        if (!esBorrador()) {
            throw new IllegalStateException("Solo se puede emitir una factura en borrador.");
        }
        if (cliente == null) {
            throw new IllegalStateException("Seleccione el cliente de la factura.");
        }
        if (autopartes == null || autopartes.isEmpty()) {
            throw new IllegalStateException("Agregue al menos una autoparte.");
        }

        recalcularTotales();

        if (totalPagado.compareTo(total) != 0) {
            throw new IllegalStateException(
                    "Los pagos (" + totalPagado + ") deben sumar exactamente el total de la factura (" + total + ")."
            );
        }

        for (Autoparte a : autopartes) {
            if (a.getEstado() != EstadoAutoparte.Vendida && a.getEstado() != EstadoAutoparte.Reservada) {
                throw new IllegalStateException("La autoparte " + a.getSku() + " no está asignada.");
            }
            BigDecimal precio = (a.getPrecioBase() != null && a.getPrecioBase().signum() > 0)
                    ? a.getPrecioBase()
                    : a.getCostoProrrateado();
            if (precio == null || precio.signum() <= 0) {
                throw new IllegalStateException("La autoparte " + a.getSku() + " no tiene precio o costo asignado.");
            }
        }

        ConfiguracionFiscal config = ConfiguracionFiscal.activaConBloqueo(em);
        if (config == null) {
            throw new IllegalStateException("No hay una configuración fiscal activa.");
        }

        numeroFactura = config.reservarNumero();
        cai = config.getCai();
        rangoSar = config.getRangoAutorizado();
        fechaLimiteEmision = config.getFechaLimiteEmision();
        fechaEmision = LocalDateTime.now();

        for (Autoparte a : autopartes) {
            a.setEstado(EstadoAutoparte.Vendida);
        }

        estado = EstadoFactura.Emitida;
        operacionInterna = true;
    }

    // Anular factura y liberar autopartes a Disponible
    public void anular() {
        if (estado != EstadoFactura.Emitida) {
            throw new IllegalStateException("Solo se puede anular una factura emitida.");
        }
        if (autopartes != null) {
            for (Autoparte a : autopartes) {
                if (a != null) {
                    a.setEstado(EstadoAutoparte.Disponible);
                }
            }
        }
        estado = EstadoFactura.Anulada;
        operacionInterna = true;
    }

    @PrePersist
    void antesDeCrear() {
        completarDatos();
        actualizarStockAutopartes();
        recalcularTotales();
    }

    @PreUpdate
    void antesDeActualizar() {
        if (!esBorrador() && !operacionInterna) {
            throw new ValidationException(
                    "No se puede modificar una factura " + estado +
                            ". Las facturas emitidas son definitivas."
            );
        }
        if (esBorrador()) {
            actualizarStockAutopartes();
            completarDatos();
            recalcularTotales();
        }
    }

    @PreRemove
    private void antesDeEliminar() {
        if (!esBorrador()) {
            throw new ValidationException("No se puede eliminar una factura emitida o anulada.");
        }
        if (autopartes != null) {
            for (Autoparte a : autopartes) {
                if (a != null) {
                    a.setEstado(EstadoAutoparte.Disponible);
                }
            }
        }
    }

    private void completarDatos() {
        if (estado == null) {
            estado = EstadoFactura.Borrador;
        }
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
        if (vendedor == null) {
            String usuario = Users.getCurrent();
            vendedor = (usuario != null && !usuario.isBlank()) ? usuario : "sistema";
        }
        if (porcentajeImpuesto == null) {
            porcentajeImpuesto = IMPUESTO_POR_DEFECTO;
        }
    }

    public void recalcularTotales() {
        BigDecimal suma = BigDecimal.ZERO;
        if (autopartes != null) {
            for (Autoparte a : autopartes) {
                if (a != null && a.getPrecioBase() != null) {
                    suma = suma.add(a.getPrecioBase());
                }
            }
        }

        BigDecimal pagado = BigDecimal.ZERO;
        if (pagos != null) {
            for (PagoFactura p : pagos) {
                if (p != null && p.getMonto() != null) {
                    pagado = pagado.add(p.getMonto());
                }
            }
        }

        subtotal = suma.setScale(2, RoundingMode.HALF_UP);
        impuesto = subtotal.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);
        total = subtotal.add(impuesto);
        totalPagado = pagado.setScale(2, RoundingMode.HALF_UP);
        saldoPendiente = total.subtract(totalPagado);
    }
}