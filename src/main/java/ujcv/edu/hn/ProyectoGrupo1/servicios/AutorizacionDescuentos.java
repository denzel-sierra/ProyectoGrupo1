package ujcv.edu.hn.ProyectoGrupo1.servicios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;
import org.openxava.jpa.XPersistence;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Usuario;

public class AutorizacionDescuentos {

    /** @return el administrador activo cuya contrasena coincide con la clave, o null si ninguno coincide */
    public static Usuario buscarAdministrador(String clave) {
        if (clave == null || clave.isBlank()) return null;
        EntityManager em = XPersistence.getManager();
        List<Usuario> administradores = em.createQuery(
                        "select u from Usuario u where u.rol.id = 'ADMIN' and u.activo = true", Usuario.class)
                .setFlushMode(FlushModeType.COMMIT)
                .getResultList();
        for (Usuario u : administradores) {
            if (u.getContrasena() != null && BCrypt.checkpw(clave, u.getContrasena())) {
                return u;
            }
        }
        return null;
    }
}