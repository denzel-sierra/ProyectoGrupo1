package ujcv.edu.hn.ProyectoGrupo1.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;
import org.openxava.annotations.*;

@Embeddable
@Getter @Setter
public class Pago {

    @Required
    @Enumerated(EnumType.STRING)
    MetodoPago metodo = MetodoPago.Efectivo;

    @Column(length = 50)
    String referencia;

    // Validación: Solo pide referencia si es Tarjeta
    @AssertTrue(message = "Debe ingresar el número de voucher/referencia para pagos con Tarjeta")
    public boolean isReferenciaValida() {
        if (metodo == null || metodo == MetodoPago.Efectivo) return true;
        return referencia != null && !referencia.trim().isEmpty();
    }
}