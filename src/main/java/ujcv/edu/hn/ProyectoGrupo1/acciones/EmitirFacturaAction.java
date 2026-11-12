package ujcv.edu.hn.ProyectoGrupo1.acciones;

import java.util.HashMap;
import java.util.Map;

import org.openxava.actions.ViewBaseAction;
import org.openxava.jpa.XPersistence;

import jakarta.persistence.EntityManager;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Factura;

// Boton "Emitir": convierte el borrador en una factura fiscal emitida.
public class EmitirFacturaAction extends ViewBaseAction {

    @Override
    public void execute() throws Exception {
        Object id = getView().getValue("id");
        if (id == null) {
            addError("error_factura", "Primero grabe la factura y luego emitala.");
            return;
        }

        EntityManager em = XPersistence.getManager();
        Factura factura = em.find(Factura.class, id);
        if (factura == null) {
            addError("error_factura", "No se encontro la factura.");
            return;
        }

        try {
            factura.emitir(em);
        } catch (IllegalStateException e) {
            addError("error_factura", e.getMessage());
            return;
        }

        addMessage("info_factura", "Factura " + factura.getNumeroFactura() + " emitida correctamente.");

        // Recarga la pantalla con los datos nuevos (numero, CAI, estado)
        Map<String, Object> clave = new HashMap<>();
        clave.put("id", factura.getId());
        getView().setValues(clave);
        getView().findObject();
    }
}