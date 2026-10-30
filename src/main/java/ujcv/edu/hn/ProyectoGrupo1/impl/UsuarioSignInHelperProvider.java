package ujcv.edu.hn.ProyectoGrupo1.impl;

import com.openxava.naviox.impl.SignInHelperProvider;
import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletRequest;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;
import org.openxava.jpa.XPersistence;
import org.openxava.util.Messages;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Usuario;

public class UsuarioSignInHelperProvider extends SignInHelperProvider {

    @Override
    public boolean isAuthorized(ServletRequest request, String userName,
                                String password, Messages errors, String unauthorizedMessage) {

        EntityManager em = XPersistence.createManager();
        try {
            List<Usuario> lista = em.createQuery(
                            "select u from Usuario u where u.nombreUsuario = :usuario", Usuario.class)
                    .setParameter("usuario", userName.trim().toLowerCase())
                    .getResultList();

            if (!lista.isEmpty()) {
                Usuario u = lista.get(0);
                if (!u.isActivo()) {
                    errors.add("usuario_desactivado");
                    return false;
                }
                return u.getContrasena() != null
                        && BCrypt.checkpw(password, u.getContrasena());
            }
        } finally {
            em.close();
        }
        // Si no existe en la tabla, se usa el login por defecto (admin del archivo de propiedades)
        return super.isAuthorized(request, userName, password, errors, unauthorizedMessage);
    }
}