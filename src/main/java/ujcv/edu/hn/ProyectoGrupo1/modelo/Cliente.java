package ujcv.edu.hn.ProyectoGrupo1.modelo;

import jakarta.persistence.*;
import org.openxava.annotations.*;
import lombok.*;
import java.util.ArrayList;
import java.util.Collection;
import org.openxava.jpa.XPersistence;

@Entity @Getter @Setter
@View(members =
        "Identificacion [ id ];" +
                "Datos del Cliente [" +
                "   nombre;" +
                "   rtnDni, tipo;" +
                "   telefono, correo" +
                "];" +
                "ComprasEmitidas [ comprasEmitidas ];" +
                "GarantiasVigentes [ piezasConGarantiaVigente ];" +
                "PiezasCompradas [ piezasCompradas ]"
)
@Tab(properties = "id, nombre, rtnDni, tipo, telefono, correo")
public class Cliente {

    // Id del cliente: lo asigna la base de datos al guardar
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @ReadOnly
    Integer id;

    @Column(length=150) @Required
    String nombre;

    @Column(length=20) @Required
    String rtnDni;

    @Column(length=20)
    String telefono;

    @Column(length=150)
    String correo;

    @Column(length=20)
    String tipo = "Particular"; // Particular o Taller

    // ---------------- HU-12: historial de compras del cliente ----------------

    // CA-01: compras (facturas emitidas), de la mas reciente a la mas antigua
    @ReadOnly
    @ListProperties("numeroFactura, fechaEmision, subtotal, impuesto, total")
    public Collection<Factura> getComprasEmitidas() {
        if (id == null) return new ArrayList<>();
        return XPersistence.getManager().createQuery(
                        "select f from Factura f where f.cliente.id = :id and f.estado = :estado "
                                + "order by f.fechaEmision desc", Factura.class)
                .setParameter("id", id)
                .setParameter("estado", EstadoFactura.Emitida)
                .getResultList();
    }

    // Todas las piezas que compro, con su fecha de venta y el estado de su garantia
    @ReadOnly
    @ListProperties("sku, descripcion, marca, modelo, fechaVenta, precioBase, estadoGarantia")
    public Collection<Autoparte> getPiezasCompradas() {
        if (id == null) return new ArrayList<>();
        return XPersistence.getManager().createQuery(
                        "select a from Factura f join f.autopartes a where f.cliente.id = :id "
                                + "and f.estado = :estado order by f.fechaEmision desc", Autoparte.class)
                .setParameter("id", id)
                .setParameter("estado", EstadoFactura.Emitida)
                .getResultList();
    }

    // CA-02: solo las piezas cuya garantia sigue vigente
    @ReadOnly
    @ListProperties("sku, descripcion, fechaVenta, estadoGarantia")
    public Collection<Autoparte> getPiezasConGarantiaVigente() {
        Collection<Autoparte> vigentes = new ArrayList<>();
        for (Autoparte a : getPiezasCompradas()) {
            if (a.isGarantiaVigente()) vigentes.add(a);
        }
        return vigentes;
    }
}
