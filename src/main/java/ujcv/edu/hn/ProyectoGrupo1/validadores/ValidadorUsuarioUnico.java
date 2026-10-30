package ujcv.edu.hn.ProyectoGrupo1.validadores;

import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.Setter;
import org.openxava.jpa.XPersistence;
import org.openxava.util.Messages;
import org.openxava.validators.IValidator;

@Getter @Setter
public class ValidadorUsuarioUnico implements IValidator {

    private Integer id;
    private String correo;
    private String nombreUsuario;

    @Override
    public void validate(Messages errors) throws Exception {
        EntityManager em = XPersistence.getManager();
        if (existe(em, "correo", correo)) {
            errors.add("correo_ya_registrado");
        }
        if (existe(em, "nombreUsuario", nombreUsuario)) {
            errors.add("nombre_usuario_ya_registrado");
        }
    }

    private boolean existe(EntityManager em, String campo, String valor) {
        if (valor == null || valor.isBlank()) return false;
        int idActual = (id == null) ? -1 : id;   // al crear no hay id; al editar se excluye a si mismo
        Long cantidad = em.createQuery(
                        "select count(u) from Usuario u where lower(u." + campo + ") = :valor and u.id <> :id",
                        Long.class)
                .setParameter("valor", valor.trim().toLowerCase())
                .setParameter("id", idActual)
                .getSingleResult();
        return cantidad > 0;
    }
}