package ujcv.edu.hn.ProyectoGrupo1.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.*;

@Entity
@Getter @Setter
@Table(name = "categorias")
@View(members = "nombre; descripcion; tipoGarantia")
@Tab(properties = "id, nombre, descripcion, tipoGarantia")
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

    // RN-04 (HU-12): tipo de garantia de las piezas de esta categoria.
    // Si se deja vacio, se deduce del nombre (motor, transmision, electrica).
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_garantia", length = 30)
    TipoGarantia tipoGarantia;

    // Horas de garantia: 24 en piezas electricas, 720 (30 dias) en motores y transmisiones
    @Hidden
    public int getHorasGarantia() {
        if (tipoGarantia != null) {
            switch (tipoGarantia) {
                case Electrica: return 24;
                case MotorTransmision: return 720;
                default: return 0;
            }
        }
        if (nombre == null) return 0;
        String n = nombre.trim().toLowerCase();
        if (n.contains("motor") || n.contains("transmision")) return 720;
        if (n.contains("electric") || n.contains("el\u00e9ctric")) return 24;
        return 0;
    }

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