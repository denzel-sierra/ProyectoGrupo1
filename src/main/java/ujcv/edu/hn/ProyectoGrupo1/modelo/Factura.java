package ujcv.edu.hn.ProyectoGrupo1.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import org.openxava.annotations.*;
import org.openxava.util.Users;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

@Entity
@Getter @Setter
@View(members =
        "Datos de la Factura [" +
                "   id, numeroFactura, fechaHora;" +
                "   vendedor, cliente" +
                "];" +
                "Autopartes a Facturar [" +
                "   autopartes" +
                "];" +
                "Totales [" +
                "   porcentajeImpuesto;" +
                "   subtotal, impuesto, total" +
                "]"
)
@Tab(
        properties = "id, numeroFactura, fechaHora, cliente.nombre, vendedor, subtotal, impuesto, total",
        defaultOrder = "${id} desc"
)
public class Factura {

    // ISV de Honduras (15%), usado si el porcentaje viene vacio
    private static final BigDecimal IMPUESTO_POR_DEFECTO = new BigDecimal("15.00");

    // Id de la factura: lo asigna la base de datos al guardar
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ReadOnly
    Integer id;

    // Se genera al guardar (necesita el id), por ejemplo FAC-000015
    @Column(length = 30, unique = true)
    @ReadOnly
    String numeroFactura;

    @ReadOnly
    LocalDateTime fechaHora;

    // Se llena con el usuario actual al guardar
    @Column(length = 100)
    @ReadOnly
    String vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @Required
    Cliente cliente;

    // Autopartes de la factura. El boton "Anadir" abre la lista de autopartes
    // con casillas para elegir varias de una vez.
    // Los datos (SKU, descripcion, marca, modelo, precio) salen siempre de Autoparte.
    @ManyToMany
    @JoinTable(name = "factura_autopartes",
            joinColumns = @JoinColumn(name = "factura_id"),
            inverseJoinColumns = @JoinColumn(name = "autoparte_id"))
    @ListProperties("sku, descripcion, marca, modelo, precioBase+")
    Collection<Autoparte> autopartes = new ArrayList<>();

    // Porcentaje de impuesto aplicado a esta factura (se guarda con ella)
    @Column(name = "porcentaje_impuesto", precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "El impuesto no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El impuesto no puede pasar de 100%")
    BigDecimal porcentajeImpuesto = IMPUESTO_POR_DEFECTO;

    // --- TOTALES ---
    // Se calculan en pantalla al instante con @Calculation (suma de los precios
    // de la tabla) y se vuelven a calcular al guardar para que queden correctos
    // en la base de datos.

    // Suma de los precios de todas las autopartes de la tabla
    @Column(precision = 12, scale = 2)
    @Money
    @ReadOnly
    @Calculation("sum(autopartes.precioBase)")
    BigDecimal subtotal = BigDecimal.ZERO;

    // Impuesto = suma de precios x porcentaje / 100
    @Column(precision = 12, scale = 2)
    @Money
    @ReadOnly
    @Calculation("sum(autopartes.precioBase) * porcentajeImpuesto / 100")
    BigDecimal impuesto = BigDecimal.ZERO;

    // TOTAL A PAGAR = suma de precios + impuesto
    @Column(precision = 12, scale = 2)
    @Money
    @ReadOnly
    @Calculation("subtotal + impuesto")
    BigDecimal total = BigDecimal.ZERO;

    private void recalcularTotales() {
        BigDecimal suma = BigDecimal.ZERO;
        if (autopartes != null) {
            for (Autoparte a : autopartes) {
                if (a.getPrecioBase() != null) {
                    suma = suma.add(a.getPrecioBase());
                }
            }
        }
        BigDecimal porcentaje = porcentajeImpuesto != null ? porcentajeImpuesto : IMPUESTO_POR_DEFECTO;
        subtotal = suma.setScale(2, RoundingMode.HALF_UP);
        impuesto = suma.multiply(porcentaje)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        total = subtotal.add(impuesto);
    }

    @PrePersist @PreUpdate
    void antesDeGuardar() {
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
        // DESACTIVADO TEMPORALMENTE: modificar otra entidad (Autoparte) dentro del
        // guardado puede colgar la transaccion. Se reactiva de forma segura despues.
        // marcarAutopartesVendidas();
    }

    // Las autopartes de la factura pasan de Disponible a Vendido
    private void marcarAutopartesVendidas() {
        if (autopartes == null) {
            return;
        }
        for (Autoparte a : autopartes) {
            if (a.getEstado() == EstadoAutoparte.Disponible) {
                a.setEstado(EstadoAutoparte.Vendida);
            }
        }
    }

    // Si se elimina la factura, las autopartes vuelven a estar disponibles
    @PreRemove
    void alAnular() {
        if (autopartes == null) {
            return;
        }
        for (Autoparte a : autopartes) {
            a.setEstado(EstadoAutoparte.Disponible);
        }
    }

    // Ya con el id asignado por la BD, se arma el numero de factura
    @PostPersist
    void asignarNumero() {
        if (numeroFactura == null) {
            numeroFactura = String.format("FAC-%06d", id);
        }
    }
}