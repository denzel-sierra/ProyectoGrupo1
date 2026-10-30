package ujcv.edu.hn.ProyectoGrupo1.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.*;

@Entity
@Getter @Setter
@Table(name = "categorias")
@View(members = "nombre; descripcion")
@Tab(properties = "id, nombre, descripcion")
public class Categoria {

    @Id
    @Hidden
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(length = 80, nullable = false, unique = true)
    @Required(message = "Seleccione una categoria")
    String nombre;

    @Column(length = 200)
    @Stereotype("MEMO")
    String descripcion;

    @Hidden
    public int getPesoRelativo() {
        if (nombre == null) return 1;
        String n = nombre.trim().toLowerCase();
        if (n.contains("motor")) return 10;
        if (n.contains("transmision")) return 8;
        if (n.contains("electric") || n.contains("eléctric")) return 3;
        if (n.contains("carroceria")) return 2;
        return 1;
    }
}