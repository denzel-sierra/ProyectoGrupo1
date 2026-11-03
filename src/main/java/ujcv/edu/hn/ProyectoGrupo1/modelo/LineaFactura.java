package ujcv.edu.hn.ProyectoGrupo1.modelo;

import java.math.BigDecimal;

import org.openxava.annotations.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import lombok.*;

// Linea de la tabla de productos de la factura.
// No es una entidad: no tiene modulo propio ni aparece en el menu.
@Embeddable
@Getter @Setter
@View(members = "autoparte; cantidad, precioUnitario, subtotal")
public class LineaFactura {

    // Se elige del inventario: se ve SKU, descripcion y precio base
    @ManyToOne(fetch = FetchType.LAZY)
    @Required
    @DescriptionsList(descriptionProperties = "sku, descripcion, precioBase")
    Autoparte autoparte;

    @Required
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    int cantidad = 1;

    // Precio "congelado" al guardar la factura (no se muestra directamente)
    @Column(name = "precio_vendido", precision = 10, scale = 2)
    @Hidden
    BigDecimal precioVendido;

    // Columna "SKU" de la tabla: el SKU de la autoparte elegida
    @Depends("autoparte")
    public String getSku() {
        return autoparte != null ? autoparte.getSku() : "";
    }

    // Columna "Nombre de la pieza": la descripcion que tiene en Autoparte
    @Depends("autoparte")
    public String getNombrePieza() {
        return autoparte != null ? autoparte.getDescripcion() : "";
    }

    // Antes de guardar muestra el precio actual de la autoparte;
    // despues de guardar devuelve el precio con el que se vendio.
    @Money
    @Depends("autoparte")
    public BigDecimal getPrecioUnitario() {
        if (precioVendido != null) {
            return precioVendido;
        }
        if (autoparte != null && autoparte.getPrecioBase() != null) {
            return autoparte.getPrecioBase();
        }
        return BigDecimal.ZERO;
    }

    // precio x cantidad
    @Money
    @Depends("autoparte, cantidad")
    public BigDecimal getSubtotal() {
        return getPrecioUnitario().multiply(BigDecimal.valueOf(cantidad));
    }

    // Validaciones: solo aplican a lineas nuevas (precioVendido aun vacio)

    @AssertTrue(message = "La autoparte seleccionada ya no esta disponible")
    public boolean isAutoparteDisponible() {
        return precioVendido != null || autoparte == null
                || autoparte.getEstado() == EstadoAutoparte.Disponible;
    }

    @AssertTrue(message = "La autoparte seleccionada no tiene precio base")
    public boolean isAutoparteConPrecio() {
        return precioVendido != null || autoparte == null
                || (autoparte.getPrecioBase() != null
                && autoparte.getPrecioBase().compareTo(BigDecimal.ZERO) > 0);
    }
}