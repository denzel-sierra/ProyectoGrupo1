package ujcv.edu.hn.ProyectoGrupo1.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.*;
import org.openxava.util.Users;

@Entity
@Getter @Setter
@Table(name = "descuentos")
@View(members = "porcentaje, autoparte; motivo; claveAutorizacion; solicitadoPor, autorizadoPor, monto")
@Tab(properties = "fechaHora, solicitadoPor, autorizadoPor, porcentaje, monto, motivo",
        defaultOrder = "${fechaHora} desc")
public class Descuento {

    // Tope maximo de descuento, en porcentaje. Para cambiarlo, modificar solo esta constante.
    public static final String TOPE_MAXIMO = "20.00";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Hidden
    Integer id;

    @Column(precision = 5, scale = 2, nullable = false, updatable = false)
    @Required(message = "Ingrese el descuento")
    @DecimalMin(value = "0.01", message = "El descuento debe ser mayor que cero")
    @DecimalMax(value = TOPE_MAXIMO, message = "El descuento excede el tope permitido")
    BigDecimal porcentaje;

    // Opcional: si se elige una pieza, se calcula el monto del descuento
    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties = "sku, descripcion")
    Autoparte autoparte;

    @Column(length = 200, nullable = false, updatable = false)
    @Required(message = "Ingrese el motivo")
    @Size(min = 10, max = 200, message = "El motivo debe tener al menos 10 caracteres")
    String motivo;

    // No se guarda: es la contrasena de un administrador, que se verifica al guardar
    @Transient
    @Stereotype("PASSWORD")
    String claveAutorizacion;

    @Column(name = "solicitado_por", length = 100, updatable = false)
    @ReadOnly
    String solicitadoPor;

    @Column(name = "autorizado_por", length = 100)
    @ReadOnly
    String autorizadoPor;

    @Column(precision = 12, scale = 2)
    @Money
    @ReadOnly
    BigDecimal monto;

    @Column(name = "fecha_hora", updatable = false)
    @Hidden
    LocalDateTime fechaHora;

    @PrePersist
    void antesDeCrear() {
        fechaHora = LocalDateTime.now();
        String actual = Users.getCurrent();
        solicitadoPor = actual != null ? actual : "sistema";
    }
}