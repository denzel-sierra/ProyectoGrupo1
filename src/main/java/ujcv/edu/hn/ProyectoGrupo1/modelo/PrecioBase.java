package ujcv.edu.hn.ProyectoGrupo1.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.*;
import org.openxava.calculators.CurrentLocalDateCalculator;
import ujcv.edu.hn.ProyectoGrupo1.validadores.ValidadorPrecioBase;

@Entity
@Getter @Setter
@Table(name = "precios_base",
        uniqueConstraints = @UniqueConstraint(columnNames = {"categoria_id", "condicion"}))
@EntityValidator(value = ValidadorPrecioBase.class, properties = {
        @PropertyValue(name = "id"),
        @PropertyValue(name = "categoria"),
        @PropertyValue(name = "condicion"),
        @PropertyValue(name = "precio")
})
@View(members = "categoria, condicion; precio, vigenteDesde; precioAplicado, aplicado")
@Tab(properties = "categoria.nombre, condicion, precio, vigenteDesde, precioAplicado, aplicado",
        defaultOrder = "${categoria.nombre} asc")
public class PrecioBase {

    // Estados de autoparte a los que se aplica el precio (las vendidas no cambian)
    public static final List<EstadoAutoparte> ESTADOS_AFECTADOS =
            List.of(EstadoAutoparte.Disponible, EstadoAutoparte.Reservada);

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Hidden
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    @DescriptionsList(descriptionProperties = "nombre")
    @Required(message = "Seleccione una categoria")
    Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @Required(message = "Seleccione una condicion")
    CondicionAutoparte condicion;

    @Column(precision = 10, scale = 2, nullable = false)
    @Required(message = "Ingrese un precio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
    @Money
    BigDecimal precio;

    @Column(name = "vigente_desde", nullable = false)
    @Required(message = "Seleccione una fecha valida")
    @FutureOrPresent(message = "Seleccione una fecha valida")
    @DefaultValueCalculator(CurrentLocalDateCalculator.class)
    LocalDate vigenteDesde;

    // Ultimo precio que se aplico a las autopartes (lo llena la accion de guardado)
    @Column(name = "precio_aplicado", precision = 10, scale = 2)
    @Money
    @ReadOnly
    BigDecimal precioAplicado;

    // true cuando el precio actual ya esta aplicado a las autopartes
    @ReadOnly
    boolean aplicado;
}