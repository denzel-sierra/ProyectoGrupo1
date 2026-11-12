package ujcv.edu.hn.ProyectoGrupo1.acciones;

import java.util.HashMap;
import java.util.Map;

import org.openxava.actions.ViewBaseAction;
import org.openxava.jpa.XPersistence;

import jakarta.persistence.EntityManager;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Cotizacion;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Factura;

// Boton "Convertir en factura": crea una factura en borrador con el cliente
// y las autopartes de la cotizacion.
public class ConvertirCotizacionAction extends ViewBaseAction {

    @Override
    public void execute() throws Exception {
        Object id = getView().getValue("id");
        if (id == null) {
            addError("error_factura", "Primero grabe la cotizacion.");
            return;
        }

        EntityManager em = XPersistence.getManager();
        Cotizacion cotizacion = em.find(Cotizacion.class, id);
        if (cotizacion == null) {
            addError("error_factura", "No se encontro la cotizacion.");
            return;
        }

        Factura factura;
        try {
            factura = cotizacion.convertirEnFactura(em);
        } catch (IllegalStateException e) {
            addError("error_factura", e.getMessage());
            return;
        }

        addMessage("info_factura", "Se creo la factura en borrador No. " + factura.getId()
                + ". Abrala en el modulo Factura, registre los pagos y emitala.");

        Map<String, Object> clave = new HashMap<>();
        clave.put("id", cotizacion.getId());
        getView().setValues(clave);
        getView().findObject();
    }
}