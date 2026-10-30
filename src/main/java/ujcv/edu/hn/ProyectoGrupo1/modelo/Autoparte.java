package ujcv.edu.hn.ProyectoGrupo1.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.*;
import org.openxava.jpa.XPersistence;
import ujcv.edu.hn.ProyectoGrupo1.validadores.ValidadorAutoparte;

@Entity
@Getter @Setter
@Table(name = "autopartes")
@EntityValidator(
        value = ValidadorAutoparte.class,
        properties = {
                @PropertyValue(name = "id"),
                @PropertyValue(name = "descripcion"),
                @PropertyValue(name = "anio"),
                @PropertyValue(name = "ubicacion")
        }
)
@View(members =
        "Identificacion [ sku, estado, creadoEn ];" +
                "Datos de la Autoparte [" +
                "   descripcion;" +
                "   marca, modelo, anio;" +
                "   categoria, condicion;" +
                "];" +
                "Almacenamiento [" +
                "   ubicacion;" +
                "   costoProrrateado;" +
                "]"
)
@Tab(properties = "sku, descripcion, marca, modelo, anio, condicion, estado, categoria.nombre, ubicacion.zona, ubicacion.pasillo, ubicacion.estante, costoProrrateado")
public class Autoparte {

    @Id
    @Hidden
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(length = 30, nullable = false, unique = true)
    @ReadOnly
    String sku;

    @Column(length = 200, nullable = false)
    @Required(message = "La descripcion debe tener al menos 5 caracteres")
    String descripcion;

    @Column(length = 50, nullable = false)
    @Required(message = "Ingrese una marca valida")
    @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ .-]+$", message = "Ingrese una marca valida")
    String marca;

    @Column(length = 50, nullable = false)
    @Required(message = "Ingrese un modelo valido")
    @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ ./-]+$", message = "Ingrese un modelo valido")
    String modelo;

    @Column(nullable = false)
    @Required(message = "Ingrese un anio valido")
    Integer anio;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @Required(message = "Seleccione una condicion")
    CondicionAutoparte condicion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @ReadOnly
    EstadoAutoparte estado = EstadoAutoparte.Disponible;

    @Column(name = "costo_prorrateado", precision = 10, scale = 2)
    @Required(message = "El costo debe ser un numero positivo")
    @DecimalMin(value = "0.00", message = "El costo debe ser un numero positivo")
    @Money
    BigDecimal costoProrrateado = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    @DescriptionsList(descriptionProperties = "nombre")
    @Required(message = "Seleccione una categoria")
    Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    @DescriptionsList(descriptionProperties = "ubicacionCompleta")
    @Required(message = "Ingrese al menos la zona")
    Ubicacion ubicacion;

    @Column(name = "creado_en", updatable = false)
    @ReadOnly
    LocalDateTime creadoEn;

    @PrePersist
    void antesDeCrear() {
        creadoEn = LocalDateTime.now();
        if (estado == null) {
            estado = EstadoAutoparte.Disponible;
        }
        if (sku == null || sku.isBlank()) {
            String prefijo = "AUT";
            if (categoria != null && categoria.getNombre() != null && categoria.getNombre().trim().length() >= 3) {
                prefijo = categoria.getNombre().trim().substring(0, 3).toUpperCase();
            }
            Long conteo = XPersistence.getManager()
                    .createQuery("select count(a) from Autoparte a", Long.class)
                    .getSingleResult();
            long consecutivo = (conteo != null ? conteo : 0L) + 1L;
            sku = String.format("%s-%d-%04d", prefijo, (anio != null ? anio : 0), consecutivo);
        }
    }
}