package com.seguridad.Messenger.conversacion.model;

import com.seguridad.Messenger.shared.enums.RolParticipante;
import com.seguridad.Messenger.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "participante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Participante {

    @EmbeddedId
    private ParticipanteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversacionId")
    @JoinColumn(name = "conversacion_id")
    private Conversacion conversacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolParticipante rol;

    @Column(name = "fecha_union", nullable = false)
    private LocalDateTime fechaUnion;
}
