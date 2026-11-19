package ujcv.edu.hn.ProyectoGrupo1.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.*;

@Entity
@Getter @Setter
@Table(name = "lotes")
@View(members =
        "DatosDelLote [" +
                "       codigo, vin, fechaAdquisicion;" +
                "       subastaOrigen, costoTotal;" +
                "       observaciones" +
                "];" +
                "AutopartesVinculadas [" +
                "       autopartes" +
                "];" +
                "ResumenDeRentabilidad [" +
                "       ingresosGenerados, margenNeto, inventarioPendiente" +
                "]"
)
@Tab(properties = "codigo, vin, fechaAdquisicion, subastaOrigen, costoTotal, ingresosGenerados, margenNeto, inventarioPendiente")
public class Lote {

    @Id
    @Hidden
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    // Pantalla E-10: Código de lote (Obligatorio, texto 50, único en el sistema)
    @Column(length = 50, nullable = false, unique = true)
    @Required(message = "El codigo de lote ya existe o es obligatorio")
    String codigo;

    // Módulo 3 / HU-07: VIN del vehículo importado (validación de formato de 17 caracteres alfanuméricos)
    @Column(length = 17)
    @DisplaySize(17)
    @Pattern(regexp = "^$|^[A-Za-z0-9]{17}$", message = "El VIN debe tener exactamente 17 caracteres alfanumericos")
    String vin;

    // Pantalla E-10: Fecha de adquisición (Obligatorio, no puede ser futura)
    @Column(name = "fecha_adquisicion", nullable = false)
    @Required(message = "Ingrese una fecha valida")
    @PastOrPresent(message = "Ingrese una fecha valida")
    LocalDate fechaAdquisicion = LocalDate.now();

    // Pantalla E-10: Subasta de origen (Opcional, solo letras y espacios)
    @Column(name = "subasta_origen", length = 100)
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]*$", message = "Ingrese una subasta valida")
    String subastaOrigen;

    // Pantalla E-10: Costo total (Obligatorio, decimal 12,2, mayor a 0)
    @Column(name = "costo_total", precision = 12, scale = 2, nullable = false)
    @Required(message = "El costo debe ser mayor a cero")
    @DecimalMin(value = "0.01", message = "El costo debe ser mayor a cero")
    @Money
    BigDecimal costoTotal;

    // Pantalla E-10: Observaciones (Opcional, área de texto 500)
    @Column(length = 500)
    @Stereotype("MEMO")
    String observaciones;

    // HU-07: Vincular cada autoparte a su contenedor/lote de origen y calcular costo prorrateado
    @OneToMany(mappedBy = "lote")
    @ListProperties("sku, descripcion, marca, modelo, anio, categoria.nombre, condicion, estado, costoProrrateado, precioBase")
    @ListAction("Lote.calcularCostoProrrateado")
    Collection<Autoparte> autopartes;

    @PrePersist
    @PreUpdate
    void normalizarVin() {
        if (vin != null) {
            vin = vin.trim().toUpperCase();
            if (vin.isEmpty()) {
                vin = null;
            }
        }
    }

    // --- Propiedades calculadas para Trazabilidad y Rentabilidad (HU-07 / HU-17 / S-03) ---

    @Money
    @Depends("costoTotal")
    public BigDecimal getIngresosGenerados() {
        if (autopartes == null || autopartes.isEmpty()) return BigDecimal.ZERO;
        BigDecimal ingresos = BigDecimal.ZERO;
        for (Autoparte a : autopartes) {
            if (a.getEstado() == EstadoAutoparte.Vendida && a.getPrecioBase() != null) {
                ingresos = ingresos.add(a.getPrecioBase());
            }
        }
        return ingresos.setScale(2, RoundingMode.HALF_UP);
    }

    @Money
    @Depends("costoTotal")
    public BigDecimal getMargenNeto() {
        BigDecimal costo = costoTotal != null ? costoTotal : BigDecimal.ZERO;
        return getIngresosGenerados().subtract(costo).setScale(2, RoundingMode.HALF_UP);
    }

    public int getInventarioPendiente() {
        if (autopartes == null || autopartes.isEmpty()) return 0;
        int pendientes = 0;
        for (Autoparte a : autopartes) {
            if (a.getEstado() != EstadoAutoparte.Vendida) {
                pendientes++;
            }
        }
        return pendientes;
    }
}