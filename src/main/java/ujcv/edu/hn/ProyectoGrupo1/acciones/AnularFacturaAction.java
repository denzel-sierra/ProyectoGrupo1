package ujcv.edu.hn.ProyectoGrupo1.acciones;

import java.util.HashMap;
import java.util.Map;

import org.openxava.actions.ViewBaseAction;
import org.openxava.jpa.XPersistence;

import jakarta.persistence.EntityManager;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Factura;

// Boton "Anular": deja la factura como anulada y devuelve las autopartes al inventario.
public class AnularFacturaAction extends ViewBaseAction {

    @Override
    public void execute() throws Exception {
        Object id = getView().getValue("id");
        if (id == null) {
            addError("error_factura", "Abra primero una factura emitida.");
            return;
        }

        EntityManager em = XPersistence.getManager();
        Factura factura = em.find(Factura.class, id);
        if (factura == null) {
            addError("error_factura", "No se encontro la factura.");
            return;
        }

        try {
            factura.anular();
        } catch (IllegalStateException e) {
            addError("error_factura", e.getMessage());
            return;
        }

        addMessage("info_factura", "Factura " + factura.getNumeroFactura()
                + " anulada. Las autopartes volvieron a estar disponibles.");

        Map<String, Object> clave = new HashMap<>();
        clave.put("id", factura.getId());
        getView().setValues(clave);
        getView().findObject();
    }
}