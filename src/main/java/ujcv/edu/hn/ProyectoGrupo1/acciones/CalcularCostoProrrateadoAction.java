package ujcv.edu.hn.ProyectoGrupo1.acciones;

import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.openxava.actions.ViewBaseAction;
import org.openxava.jpa.XPersistence;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Autoparte;
import ujcv.edu.hn.ProyectoGrupo1.modelo.Lote;


public class CalcularCostoProrrateadoAction extends ViewBaseAction {

    @Override
    public void execute() throws Exception {
        Integer loteId = (Integer) getView().getValue("id");
        if (loteId == null) {
            addError("Guarde primero el lote antes de calcular el costo prorrateado.");
            return;
        }

        Lote lote = XPersistence.getManager().find(Lote.class, loteId);
        if (lote == null) {
            addError("Lote no encontrado.");
            return;
        }

        TypedQuery<Autoparte> query = XPersistence.getManager().createQuery(
                "select a from Autoparte a where a.lote.id = :loteId", Autoparte.class);
        query.setParameter("loteId", loteId);
        List<Autoparte> piezas = query.getResultList();

        if (piezas.isEmpty()) {
            addWarning("El lote no tiene piezas registradas.");
            return;
        }


        int pesoTotal = 0;
        for (Autoparte pieza : piezas) {
            int peso = (pieza.getCategoria() != null) ? pieza.getCategoria().getPesoRelativo() : 1;
            pesoTotal += peso;
        }
        if (pesoTotal <= 0) {
            pesoTotal = 1;
        }


        BigDecimal costoTotalLote = lote.getCostoTotal() != null ? lote.getCostoTotal() : BigDecimal.ZERO;
        BigDecimal bdPesoTotal = BigDecimal.valueOf(pesoTotal);

        for (Autoparte pieza : piezas) {
            int peso = (pieza.getCategoria() != null) ? pieza.getCategoria().getPesoRelativo() : 1;
            BigDecimal costoProrrateado = BigDecimal.valueOf(peso)
                    .multiply(costoTotalLote)
                    .divide(bdPesoTotal, 2, RoundingMode.HALF_UP);
            pieza.setCostoProrrateado(costoProrrateado);
            XPersistence.getManager().merge(pieza);
        }

        XPersistence.commit();
        getView().refresh();
        addMessage("Costo prorrateado calculado para " + piezas.size() + " piezas.");
    }
}