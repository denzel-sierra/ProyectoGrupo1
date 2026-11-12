package ujcv.edu.hn.ProyectoGrupo1.acciones;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.openxava.actions.SaveAction;
import org.openxava.jpa.XPersistence;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Descuento;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Usuario;
import ujcv.edu.hn.ProyectoGrupo1.servicios.AutorizacionDescuentos;
import ujcv.edu.hn.ProyectoGrupo1.servicios.RegistroBitacora;

public class AutorizarDescuento extends SaveAction {

    @Override
    public void execute() throws Exception {
        // Un descuento ya autorizado no se modifica (es un registro de auditoria)
        if (getView().getKeyValues().get("id") != null) {
            addError("descuento_no_editable");
            return;
        }

        // CA-01: sin clave de administrador valida, el descuento se bloquea
        String clave = (String) getView().getValue("claveAutorizacion");
        if (clave == null || clave.isBlank()) {
            addError("descuento_sin_permiso");
            return;
        }
        Usuario administrador = AutorizacionDescuentos.buscarAdministrador(clave);
        if (administrador == null) {
            addError("clave_autorizacion_incorrecta");
            return;
        }

        super.execute();                       // guarda con las validaciones normales
        if (getErrors().contains()) return;

        // El descuento recien guardado es el de id mas alto
        EntityManager em = XPersistence.getManager();
        Descuento d = em.createQuery("select d from Descuento d order by d.id desc", Descuento.class)
                .setMaxResults(1)
                .getSingleResult();

        d.setAutorizadoPor(administrador.getNombreUsuario());

        BigDecimal monto = null;
        if (d.getAutoparte() != null && d.getAutoparte().getPrecioBase() != null) {
            monto = d.getAutoparte().getPrecioBase().multiply(d.getPorcentaje())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        d.setMonto(monto);

        // CA-02: registrar en la bitacora (usuario y fecha los pone la bitacora)
        String detalle = String.format("Descuento %s%% autorizado por %s, solicitado por %s%s",
                d.getPorcentaje(), administrador.getNombreUsuario(), d.getSolicitadoPor(),
                d.getAutoparte() != null ? " | pieza " + d.getAutoparte().getSku() : "");
        RegistroBitacora.registrar("DESCUENTO", "Descuento", d.getId(), detalle, d.getMotivo(), monto);

        addMessage("descuento_autorizado");
    }
}