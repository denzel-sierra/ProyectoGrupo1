package ujcv.edu.hn.ProyectoGrupo1.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.*;

@Entity
@Getter @Setter
@Table(name = "ubicaciones")
@View(members =
        "zona, pasillo;" +
                "estante, nivel;" +
                "descripcion"
)
@Tab(properties = "zona, pasillo, estante, nivel, descripcion")
public class Ubicacion {

    @Id
    @Hidden
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(length = 50, nullable = false)
    @Required(message = "Ingrese al menos la zona")
    String zona;

    @Column(length = 20)
    String pasillo;

    @Column(length = 20)
    String estante;

    @Column(length = 20)
    String nivel;

    @Column(length = 150)
    String descripcion;

    @Hidden
    public String getUbicacionCompleta() {
        StringBuilder sb = new StringBuilder(zona != null ? zona : "");
        if (pasillo != null && !pasillo.isBlank()) sb.append(" - Pasillo ").append(pasillo);
        if (estante != null && !estante.isBlank()) sb.append(" - Estante ").append(estante);
        if (nivel != null && !nivel.isBlank()) sb.append(" - Nivel ").append(nivel);
        return sb.toString();
    }
}