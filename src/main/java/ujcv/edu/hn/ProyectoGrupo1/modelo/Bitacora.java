package ujcv.edu.hn.ProyectoGrupo1.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.*;
import org.openxava.util.Users;

@Entity
@Getter @Setter
@Table(name = "bitacora")
@View(members = "fechaHora, usuario; evento, entidad, idEntidad; detalle; motivo; monto")
@Tab(properties = "fechaHora, usuario, evento, entidad, idEntidad, detalle, monto",
        defaultOrder = "${fechaHora} desc")
public class Bitacora {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Hidden
    Integer id;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    @ReadOnly
    LocalDateTime fechaHora;

    @Column(length = 100, nullable = false, updatable = false)
    @ReadOnly
    String usuario;

    // Ejemplos: PRECIO_BASE, DESCUENTO, FACTURA_ANULADA
    @Column(length = 40, nullable = false, updatable = false)
    @ReadOnly
    String evento;

    @Column(length = 40, updatable = false)
    @ReadOnly
    String entidad;

    @Column(name = "id_entidad", length = 40, updatable = false)
    @ReadOnly
    String idEntidad;

    @Column(length = 400, updatable = false)
    @ReadOnly
    String detalle;

    @Column(length = 200, updatable = false)
    @ReadOnly
    String motivo;

    @Column(precision = 12, scale = 2, updatable = false)
    @Money
    @ReadOnly
    BigDecimal monto;

    @PrePersist
    void antesDeCrear() {
        fechaHora = LocalDateTime.now();
        if (usuario == null) {
            String actual = Users.getCurrent();
            usuario = actual != null ? actual : "sistema";
        }
    }

    // La bitacora no se edita ni se borra (RN-08)
    @PreUpdate
    void bloquearEdicion() {
        throw new IllegalStateException("La bitacora no se puede modificar");
    }

    @PreRemove
    void bloquearBorrado() {
        throw new IllegalStateException("La bitacora no se puede eliminar");
    }
}