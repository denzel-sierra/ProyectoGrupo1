package ujcv.edu.hn.ProyectoGrupo1.modelo;

import java.time.LocalDate;
import java.util.List;

import org.openxava.annotations.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * Autorizacion de facturacion que entrega el SAR: CAI, rango de numeros y
 * fecha limite de emision. Cada factura emitida toma de aqui su numero.
 * Si hay varias activas, se usa la mas reciente.
 */
@Entity
@Getter @Setter
@Table(name = "configuracion_fiscal")
@View(members =
        "Autorizacion del SAR [" +
                "   cai;" +
                "   fechaLimiteEmision" +
                "];" +
                "Numeracion [" +
                "   establecimiento, puntoEmision, tipoDocumento;" +
                "   rangoInicial, rangoFinal;" +
                "   ultimoNumeroEmitido;" +
                "   rangoAutorizado" +
                "];" +
                "activa"
)
@Tab(
        properties = "cai, establecimiento, puntoEmision, rangoInicial, rangoFinal, ultimoNumeroEmitido, fechaLimiteEmision, activa",
        defaultOrder = "${id} desc"
)
public class ConfiguracionFiscal {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Hidden
    Integer id;

    @Column(length = 50, nullable = false)
    @Required
    String cai;

    @Column(length = 3, nullable = false)
    @Required
    String establecimiento = "001";

    @Column(name = "punto_emision", length = 3, nullable = false)
    @Required
    String puntoEmision = "001";

    @Column(name = "tipo_documento", length = 2, nullable = false)
    @Required
    String tipoDocumento = "01";

    @Column(name = "rango_inicial", nullable = false)
    @Required
    @Min(value = 1, message = "El rango inicial debe ser al menos 1")
    Integer rangoInicial = 1;

    @Column(name = "rango_final", nullable = false)
    @Required
    @Min(value = 1, message = "El rango final debe ser al menos 1")
    Integer rangoFinal;

    @Column(name = "ultimo_numero_emitido")
    @ReadOnly
    Integer ultimoNumeroEmitido = 0;

    @Column(name = "fecha_limite_emision", nullable = false)
    @Required
    LocalDate fechaLimiteEmision;

    boolean activa = true;

    @Depends("establecimiento, puntoEmision, tipoDocumento, rangoInicial, rangoFinal")
    public String getRangoAutorizado() {
        if (rangoInicial == null || rangoFinal == null) {
            return "";
        }
        return formatearNumero(rangoInicial) + " al " + formatearNumero(rangoFinal);
    }

    @AssertTrue(message = "El rango final debe ser mayor o igual al rango inicial")
    public boolean isRangoValido() {
        return rangoInicial == null || rangoFinal == null || rangoFinal >= rangoInicial;
    }

    public String formatearNumero(int numero) {
        return establecimiento + "-" + puntoEmision + "-" + tipoDocumento + "-"
                + String.format("%08d", numero);
    }

    // Busca la configuracion activa y la bloquea hasta terminar la transaccion,
    // para que dos facturas nunca reciban el mismo numero.
    public static ConfiguracionFiscal activaConBloqueo(EntityManager em) {
        List<ConfiguracionFiscal> lista = em.createQuery(
                        "from ConfiguracionFiscal c where c.activa = true order by c.id desc",
                        ConfiguracionFiscal.class)
                .setMaxResults(1)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        return lista.isEmpty() ? null : lista.get(0);
    }

    // Entrega el siguiente numero fiscal y avanza el correlativo.
    public String reservarNumero() {
        if (LocalDate.now().isAfter(fechaLimiteEmision)) {
            throw new IllegalStateException("La fecha limite de emision del SAR ("
                    + fechaLimiteEmision + ") ya vencio. Registre una configuracion fiscal nueva.");
        }
        int actual = ultimoNumeroEmitido == null ? 0 : ultimoNumeroEmitido;
        int siguiente = Math.max(actual + 1, rangoInicial);
        if (siguiente > rangoFinal) {
            throw new IllegalStateException("Se agoto el rango de facturas autorizado por el SAR ("
                    + getRangoAutorizado() + "). Registre una configuracion fiscal nueva.");
        }
        ultimoNumeroEmitido = siguiente;
        return formatearNumero(siguiente);
    }
}