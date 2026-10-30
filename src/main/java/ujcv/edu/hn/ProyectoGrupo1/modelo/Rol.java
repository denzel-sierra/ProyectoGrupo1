package ujcv.edu.hn.ProyectoGrupo1.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.openxava.annotations.*;

@Entity
@Getter @Setter
@Table(name = "roles")
@View(members =
        "id, nombre;" +
                "descripcion;" +
                "Acceso a pantallas [" +
                "accesoSeguridad;" +
                "accesoInventario;" +
                "accesoLotes;" +
                "accesoPrecios;" +
                "accesoVentas;" +
                "accesoClientes;" +
                "accesoAnalitica;" +
                "accesoBitacora" +
                "]"
)
public class Rol {

    @Id
    @Column(length = 20)
    @Required
    private String id;      // ADMIN, INVENTARIO, VENDEDOR

    @Column(length = 50, unique = true)
    @Required
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    @ColumnDefault("false")
    private boolean accesoSeguridad;      // Roles y usuarios

    @ColumnDefault("false")
    private boolean accesoInventario;     // Inventario y autopartes

    @ColumnDefault("false")
    private boolean accesoLotes;          // Vehiculos y lotes

    @ColumnDefault("false")
    private boolean accesoPrecios;        // Catalogo de precios

    @ColumnDefault("false")
    private boolean accesoVentas;         // Punto de venta y facturacion

    @ColumnDefault("false")
    private boolean accesoClientes;       // Clientes, garantias y reclamos

    @ColumnDefault("false")
    private boolean accesoAnalitica;      // Dashboard y reportes

    @ColumnDefault("false")
    private boolean accesoBitacora;       // Bitacora de auditoria
}