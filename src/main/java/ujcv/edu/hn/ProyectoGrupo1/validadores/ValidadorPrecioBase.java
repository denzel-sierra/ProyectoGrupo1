package ujcv.edu.hn.ProyectoGrupo1.validadores;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.openxava.jpa.XPersistence;
import org.openxava.util.Messages;
import org.openxava.validators.IValidator;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Categoria;
import ujcv.edu.hn.ProyectoGrupo1.modelo.CondicionAutoparte;
import ujcv.edu.hn.ProyectoGrupo1.modelo.PrecioBase;

@Getter @Setter
public class ValidadorPrecioBase implements IValidator {

    Integer id;
    Categoria categoria;
    CondicionAutoparte condicion;
    BigDecimal precio;

    @Override
    public void validate(Messages errors) throws Exception {
        if (categoria == null || categoria.getId() == null || condicion == null || precio == null) return;
        EntityManager em = XPersistence.getManager();

        // RN-07: el precio no puede ser menor al costo prorrateado de las piezas afectadas
        BigDecimal costoMayor = em.createQuery(
                        "select max(a.costoProrrateado) from Autoparte a " +
                                "where a.categoria.id = :cat and a.condicion = :cond " +
                                "and a.estado in (:estados)", BigDecimal.class)
                .setParameter("cat", categoria.getId())
                .setParameter("cond", condicion)
                .setParameter("estados", PrecioBase.ESTADOS_AFECTADOS)
                .setFlushMode(FlushModeType.COMMIT)
                .getSingleResult();
        if (costoMayor != null && precio.compareTo(costoMayor) < 0) {
            errors.add("precio_menor_costo");
        }

        // Un solo precio por categoria y condicion
        int idActual = (id == null) ? -1 : id;
        Long repetidos = em.createQuery(
                        "select count(p) from PrecioBase p " +
                                "where p.categoria.id = :cat and p.condicion = :cond and p.id <> :id", Long.class)
                .setParameter("cat", categoria.getId())
                .setParameter("cond", condicion)
                .setParameter("id", idActual)
                .setFlushMode(FlushModeType.COMMIT)
                .getSingleResult();
        if (repetidos != null && repetidos > 0) {
            errors.add("precio_base_repetido");
        }
    }
}