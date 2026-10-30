package ujcv.edu.hn.ProyectoGrupo1.validadores;

import jakarta.persistence.EntityManager;
import java.time.Year;
import lombok.Getter;
import lombok.Setter;
import org.openxava.jpa.XPersistence;
import org.openxava.util.Messages;
import org.openxava.validators.IValidator;
import ujcv.edu.hn.ProyectoGrupo1.modelo.EstadoAutoparte;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Ubicacion;

@Getter @Setter
public class ValidadorAutoparte implements IValidator {

    Integer id;
    String descripcion;
    Integer anio;
    Ubicacion ubicacion;

    @Override
    public void validate(Messages errors) throws Exception {
        // Validación E-03: Descripción mínimo 5 caracteres
        if (descripcion != null && descripcion.trim().length() < 5) {
            errors.add("descripcion_corta");
        }

        // Validación E-03: Año entre 1980 y el año actual
        int anioActual = Year.now().getValue();
        if (anio != null && (anio < 1980 || anio > anioActual)) {
            errors.add("anio_invalido");
        }

        // Validación HU-02 (CA-02): Advertir si la ubicación ya está ocupada por otra pieza disponible
        if (ubicacion != null && ubicacion.getId() != null) {
            EntityManager em = XPersistence.getManager();
            int idActual = (id == null) ? -1 : id;
            Long ocupadas = em.createQuery(
                            "select count(a) from Autoparte a " +
                                    "where a.ubicacion.id = :ubicacionId " +
                                    "and a.estado = :estadoDisponible " +
                                    "and a.id <> :id",
                            Long.class)
                    .setParameter("ubicacionId", ubicacion.getId())
                    .setParameter("estadoDisponible", EstadoAutoparte.Disponible)
                    .setParameter("id", idActual)
                    .getSingleResult();

            if (ocupadas != null && ocupadas > 0) {
                errors.add("ubicacion_ocupada");
            }
        }
    }
}
