package ujcv.edu.hn.ProyectoGrupo1.acciones;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.openxava.actions.SaveAction;
import org.openxava.jpa.XPersistence;
import ujcv.edu.hn.ProyectoGrupo1.modelo.PrecioBase;
import ujcv.edu.hn.ProyectoGrupo1.servicios.RegistroBitacora;

public class GuardarPrecioBase extends SaveAction {

    @Override
    public void execute() throws Exception {
        super.execute();                       // guarda con las validaciones normales
        if (getErrors().contains()) return;    // si hubo errores (por ejemplo RN-07), no se hace mas

        EntityManager em = XPersistence.getManager();
        List<PrecioBase> precios = em.createQuery("select p from PrecioBase p", PrecioBase.class)
                .getResultList();

        LocalDate hoy = LocalDate.now();
        int preciosAplicados = 0;
        int autopartesActualizadas = 0;
        int futuros = 0;

        for (PrecioBase p : precios) {
            boolean vigente = !p.getVigenteDesde().isAfter(hoy);
            boolean cambio = p.getPrecioAplicado() == null
                    || p.getPrecioAplicado().compareTo(p.getPrecio()) != 0;

            if (vigente && cambio) {
                // CA-01: aplicar a las autopartes de esa categoria y condicion
                int afectadas = em.createQuery(
                                "update Autoparte a set a.precioBase = :precio " +
                                        "where a.categoria.id = :cat and a.condicion = :cond " +
                                        "and a.estado in (:estados)")
                        .setParameter("precio", p.getPrecio())
                        .setParameter("cat", p.getCategoria().getId())
                        .setParameter("cond", p.getCondicion())
                        .setParameter("estados", PrecioBase.ESTADOS_AFECTADOS)
                        .executeUpdate();

                // CA-02: registrar en la bitacora (fecha y usuario los pone la bitacora sola)
                String detalle = String.format(
                        "%s / %s: %s -> %s | vigente desde %s | autopartes actualizadas: %d",
                        p.getCategoria().getNombre(), p.getCondicion(),
                        p.getPrecioAplicado() != null ? p.getPrecioAplicado() : "nuevo",
                        p.getPrecio(), p.getVigenteDesde(), afectadas);
                RegistroBitacora.registrar("PRECIO_BASE", "PrecioBase", p.getId(),
                        detalle, null, p.getPrecio());

                p.setPrecioAplicado(p.getPrecio());
                preciosAplicados++;
                autopartesActualizadas += afectadas;
            } else if (!vigente && cambio) {
                futuros++;
            }

            p.setAplicado(p.getPrecioAplicado() != null
                    && p.getPrecioAplicado().compareTo(p.getPrecio()) == 0);
        }

        if (preciosAplicados > 0) {
            addMessage("precio_base_aplicado", autopartesActualizadas);
        }
        if (futuros > 0) {
            addMessage("precio_base_pendiente", futuros);
        }
    }
}