package ujcv.edu.hn.ProyectoGrupo1.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.openxava.annotations.*;
import org.openxava.util.Users;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

/**
 * Cotizacion en mostrador: precio de unas autopartes para un cliente, con
 * fecha de vencimiento. No tiene valor fiscal y no reserva las piezas.
 * Si el cliente acepta, el boton "Convertir en factura" crea el borrador.
 */
@Entity
@Getter @Setter
@Table(name = "cotizaciones")
@View(members =
        "Cotizacion [" +
                "   numeroCotizacion, estado;" +
                "   fechaHora, fechaVencimiento;" +
                "   vendedor" +
                "];" +
                "Cliente [" +
                "   cliente;" +
                "   rtnCliente, tipoCliente" +
                "];" +
                "Detalle de la Cotizacion [" +
                "   autopartes" +
                "];" +
                "Totales [" +
                "   porcentajeImpuesto;" +
                "   subtotal, impuesto, total" +
                "];" +
                "Observaciones [" +
                "   observaciones" +
                "];" +
                "Factura generada [" +
                "   facturaGenerada" +
                "]"
)
@Tab(
        properties = "numeroCotizacion, fechaHora, fechaVencimiento, cliente.nombre, estado, subtotal, impuesto, total",
        defaultOrder = "${id} desc"
)
public class Cotizacion {

    // 1. IMPUESTO FIJO EN 12%
    private static final BigDecimal IMPUESTO_POR_DEFECTO = new BigDecimal("12.00");
    private static final int DIAS_DE_VIGENCIA = 15;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ReadOnly
    @Hidden
    Integer id;

    // COT-000001, se genera al guardar
    @Column(name = "numero_cotizacion", length = 20, unique = true)
    @ReadOnly
    String numeroCotizacion;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    @ReadOnly
    EstadoCotizacion estado = EstadoCotizacion.Vigente;

    @ReadOnly
    LocalDateTime fechaHora;

    // Hasta cuando es valido el precio cotizado
    @Column(name = "fecha_vencimiento")
    @Required
    LocalDate fechaVencimiento = LocalDate.now().plusDays(DIAS_DE_VIGENCIA);

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

    // 2. SE QUITÓ @CollectionView("Simple") para poder seleccionar piezas sin fallos
    @SearchListCondition("${estado} = 'Disponible'")
    @ManyToMany
    @JoinTable(name = "cotizacion_autopartes",
            joinColumns = @JoinColumn(name = "cotizacion_id"),
            inverseJoinColumns = @JoinColumn(name = "autoparte_id"))
    @ListProperties("sku, descripcion, marca, modelo, precioBase+")
    Collection<Autoparte> autopartes = new ArrayList<>();

    // 3. IMPUESTO FIJO: Con @ReadOnly ya no es editable y queda congelado en 12%
    @Column(name = "porcentaje_impuesto", precision = 5, scale = 2)
    @ReadOnly
    BigDecimal porcentajeImpuesto = IMPUESTO_POR_DEFECTO;

    // 4. TOTALES EN VIVO CON @Calculation (12% DIRECTO)
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

    @Column(length = 500)
    @Stereotype("MEMO")
    String observaciones;

    // Se llena cuando la cotizacion se convierte en factura
    @ManyToOne(fetch = FetchType.LAZY)
    @ReadOnly
    @DescriptionsList(descriptionProperties = "id")
    Factura facturaGenerada;

    // ------------------------------------------------------------------

    /**
     * Crea una factura en borrador con el cliente, el impuesto y las
     * autopartes de esta cotizacion. Lo llama la accion "Convertir en factura".
     */
    public Factura convertirEnFactura(EntityManager em) {
        if (estado == EstadoCotizacion.Convertida) {
            throw new IllegalStateException("Esta cotizacion ya fue convertida en factura.");
        }
        if (fechaVencimiento != null && LocalDate.now().isAfter(fechaVencimiento)) {
            throw new IllegalStateException("La cotizacion vencio el " + fechaVencimiento
                    + ". Cree una cotizacion nueva con los precios actuales.");
        }
        if (autopartes == null || autopartes.isEmpty()) {
            throw new IllegalStateException("La cotizacion no tiene autopartes.");
        }
        for (Autoparte a : autopartes) {
            if (a.getEstado() != EstadoAutoparte.Disponible) {
                throw new IllegalStateException("La autoparte " + a.getSku() + " ya no esta disponible.");
            }
        }

        Factura factura = new Factura();
        factura.setCliente(cliente);
        factura.setPorcentajeImpuesto(porcentajeImpuesto);
        factura.setAutopartes(new ArrayList<>(autopartes));
        em.persist(factura);

        facturaGenerada = factura;
        estado = EstadoCotizacion.Convertida;
        return factura;
    }

    // ------------------------------------------------------------------

    @PrePersist @PreUpdate
    void antesDeGuardar() {
        if (estado == null) {
            estado = EstadoCotizacion.Vigente;
        }
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
        if (vendedor == null) {
            String usuario = Users.getCurrent();
            vendedor = usuario != null ? usuario : "sistema";
        }
        if (porcentajeImpuesto == null) {
            porcentajeImpuesto = IMPUESTO_POR_DEFECTO;
        }
        recalcularTotales();
    }

    private void recalcularTotales() {
        BigDecimal suma = BigDecimal.ZERO;
        if (autopartes != null) {
            for (Autoparte a : autopartes) {
                if (a != null && a.getPrecioBase() != null) {
                    suma = suma.add(a.getPrecioBase());
                }
            }
        }
        subtotal = suma.setScale(2, RoundingMode.HALF_UP);
        impuesto = subtotal.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);
        total = subtotal.add(impuesto);
    }

    // Con el id ya asignado por la base de datos se arma el numero COT-000001
    @PostPersist
    void asignarNumero() {
        if (numeroCotizacion == null) {
            numeroCotizacion = String.format("COT-%06d", id);
        }
    }
}