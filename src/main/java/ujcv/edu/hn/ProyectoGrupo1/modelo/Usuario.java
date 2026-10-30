package ujcv.edu.hn.ProyectoGrupo1.modelo;

import jakarta.persistence.*;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.mindrot.jbcrypt.BCrypt;
import org.openxava.annotations.*;
import ujcv.edu.hn.ProyectoGrupo1.validadores.ValidadorUsuarioUnico;

@Entity
@Getter @Setter
@Table(name = "usuarios")
@EntityValidator(value = ValidadorUsuarioUnico.class, properties = {
        @PropertyValue(name = "id"),
        @PropertyValue(name = "correo"),
        @PropertyValue(name = "nombreUsuario")
})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ReadOnly
    private Integer id;

    @Column(length = 100)
    @Required
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Ingrese solo letras")
    private String nombre;

    @Column(name = "nombre_usuario", length = 50, unique = true)
    @Required
    @Pattern(regexp = "^[a-z0-9._]{3,30}$",
            message = "Use de 3 a 30 caracteres: minusculas, numeros, punto o guion bajo")
    private String nombreUsuario;

    @Column(length = 150, unique = true)
    @Required
    @Email(message = "Ingrese un correo valido")
    private String correo;

    @Column(length = 255)
    @Hidden
    private String contrasena;

    @Transient
    @Stereotype("PASSWORD")
    @Pattern(regexp = "^(?:|(?=.*[A-Z])(?=.*\\d).{8,})$",
            message = "La contrasena debe tener al menos 8 caracteres, una mayuscula y un numero")
    private String contrasenaNueva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @DescriptionsList(descriptionProperties = "nombre")
    @Required
    private Rol rol;

    private boolean activo = true;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void antesDeCrear() {
        creadoEn = LocalDateTime.now();
        aplicarContrasena();
    }

    @PreUpdate
    void antesDeActualizar() {
        aplicarContrasena();
    }

    private void aplicarContrasena() {
        if (contrasenaNueva != null && !contrasenaNueva.isBlank()) {
            contrasena = BCrypt.hashpw(contrasenaNueva, BCrypt.gensalt());
        }
        if (contrasena == null) {
            throw new ValidationException("La contrasena es obligatoria");
        }
    }
}