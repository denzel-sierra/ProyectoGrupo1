package ujcv.edu.hn.ProyectoGrupo1.modelo;

import jakarta.persistence.*;
import org.openxava.annotations.*;
import lombok.*;

@Entity @Getter @Setter
@View(members =
        "Identificacion [ id ];" +
                "Datos del Cliente [" +
                "   nombre;" +
                "   rtnDni, tipo;" +
                "   telefono, correo" +
                "]"
)
@Tab(properties = "id, nombre, rtnDni, tipo, telefono, correo")
public class Cliente {

    // Id del cliente: lo asigna la base de datos al guardar
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @ReadOnly
    Integer id;

    @Column(length=150) @Required
    String nombre;

    @Column(length=20) @Required
    String rtnDni;

    @Column(length=20)
    String telefono;

    @Column(length=150)
    String correo;

    @Column(length=20)
    String tipo = "Particular"; // Particular o Taller
}