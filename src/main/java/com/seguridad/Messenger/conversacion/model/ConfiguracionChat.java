package com.seguridad.Messenger.conversacion.model;

import com.seguridad.Messenger.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_chat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionChat {

    @EmbeddedId
    private ConfiguracionChatId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversacionId")
    @JoinColumn(name = "conversacion_id")
    private Conversacion conversacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "silenciado_hasta")
    private LocalDateTime silenciadoHasta;

    @Column(nullable = false)
    private boolean archivado = false;

    @Column(nullable = false)
    private boolean fijado = false;
}
