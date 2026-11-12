package ujcv.edu.hn.ProyectoGrupo1.modelo;

import java.math.BigDecimal;

import org.openxava.annotations.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

/**
 * Un pago de una factura. Una factura puede tener varios (pago mixto):
 * por ejemplo L 5,000 en efectivo y L 3,000 con tarjeta.
 */
@Entity
@Getter @Setter
@Table(name = "pagos_factura")
@View(members = "metodo, monto; referencia")
public class PagoFactura {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Hidden
    Integer id;

    // OpenXava la asigna sola al agregar el pago desde la factura
    @ManyToOne(fetch = FetchType.LAZY)
    Factura factura;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @Required
    MetodoPago metodo = MetodoPago.Efectivo;

    @Column(precision = 12, scale = 2, nullable = false)
    @Required
    @DecimalMin(value = "0.01", message = "El monto del pago debe ser mayor que cero")
    @Money
    BigDecimal monto;

    // Numero de autorizacion de la tarjeta, de transferencia, de cheque, etc.
    @Column(length = 60)
    String referencia;
}