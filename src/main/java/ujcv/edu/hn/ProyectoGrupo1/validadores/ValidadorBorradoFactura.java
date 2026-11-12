package ujcv.edu.hn.ProyectoGrupo1.validadores;

import org.openxava.util.Messages;
import org.openxava.validators.IRemoveValidator;

import ujcv.edu.hn.ProyectoGrupo1.modelo.Factura;

// Una factura emitida es definitiva: no se borra, se anula.
public class ValidadorBorradoFactura implements IRemoveValidator {

    private Factura factura;

    @Override
    public void setEntity(Object entity) throws Exception {
        this.factura = (Factura) entity;
    }

    @Override
    public void validate(Messages errors) throws Exception {
        if (!factura.esBorrador()) {
            errors.add("error_factura",
                    "Solo se pueden borrar facturas en borrador. Una factura emitida no se borra: se anula.");
        }
    }
}