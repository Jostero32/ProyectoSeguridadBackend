package com.seguridad.Messenger.mensajes.model;

import com.seguridad.Messenger.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reaccion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reaccion {

    @EmbeddedId
    private ReaccionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("mensajeId")
    @JoinColumn(name = "mensaje_id")
    private Mensaje mensaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private String emoji;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn;
}
