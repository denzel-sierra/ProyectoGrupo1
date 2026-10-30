package ujcv.edu.hn.ProyectoGrupo1.impl;

import com.openxava.naviox.impl.ModulesHelperProvider;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.openxava.application.meta.MetaModule;
import org.openxava.jpa.XPersistence;
import org.openxava.util.Users;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Rol;

public class RolesModulesHelperProvider extends ModulesHelperProvider {

    // Modulo (nombre de la entidad) -> casilla del rol que lo habilita.
    // A medida que se creen mas modulos, se agregan aqui.
    private static final Map<String, String> ACCESO = Map.of(
            "Usuario", "accesoSeguridad",
            "Rol", "accesoSeguridad"
    );

    @Override
    public List<MetaModule> getAll(HttpServletRequest request) {
        List<MetaModule> todos = super.getAll(request);
        String actual = Users.getCurrent();
        if (actual == null) return todos;

        Set<String> permitidas = casillasDe(actual);
        if (permitidas == null) return todos;   // usuario del archivo de propiedades (admin): ve todo

        List<MetaModule> resultado = new ArrayList<>();
        for (MetaModule modulo : todos) {
            String casilla = ACCESO.get(modulo.getName());
            if (casilla != null && permitidas.contains(casilla)) {
                resultado.add(modulo);
            }
        }
        return resultado;
    }

    /** @return las casillas marcadas en el rol del usuario, o null si el usuario no esta en la tabla */
    private Set<String> casillasDe(String nombreUsuario) {
        EntityManager em = XPersistence.createManager();
        try {
            List<Rol> roles = em.createQuery(
                            "select u.rol from Usuario u where u.nombreUsuario = :n", Rol.class)
                    .setParameter("n", nombreUsuario.trim().toLowerCase())
                    .getResultList();
            if (roles.isEmpty()) return null;

            Rol r = roles.get(0);
            Set<String> s = new HashSet<>();
            if (r.isAccesoSeguridad())  s.add("accesoSeguridad");
            if (r.isAccesoInventario()) s.add("accesoInventario");
            if (r.isAccesoLotes())      s.add("accesoLotes");
            if (r.isAccesoPrecios())    s.add("accesoPrecios");
            if (r.isAccesoVentas())     s.add("accesoVentas");
            if (r.isAccesoClientes())   s.add("accesoClientes");
            if (r.isAccesoAnalitica())  s.add("accesoAnalitica");
            if (r.isAccesoBitacora())   s.add("accesoBitacora");
            return s;
        } finally {
            em.close();
        }
    }
}