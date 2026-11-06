package ujcv.edu.hn.ProyectoGrupo1.servicios;

import java.math.BigDecimal;
import org.openxava.jpa.XPersistence;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Bitacora;

public class RegistroBitacora {

    /** Guarda un evento. Debe llamarse dentro de una accion de OpenXava (comparte su transaccion). */
    public static void registrar(String evento, String entidad, Object idEntidad,
                                 String detalle, String motivo, BigDecimal monto) {
        Bitacora b = new Bitacora();
        b.setEvento(evento);
        b.setEntidad(entidad);
        b.setIdEntidad(idEntidad != null ? String.valueOf(idEntidad) : null);
        b.setDetalle(detalle);
        b.setMotivo(motivo);
        b.setMonto(monto);
        XPersistence.getManager().persist(b);
    }
}