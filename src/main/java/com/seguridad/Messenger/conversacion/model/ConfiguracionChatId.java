package com.seguridad.Messenger.conversacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ConfiguracionChatId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "conversacion_id")
    private UUID conversacionId;

    @Column(name = "usuario_id")
    private UUID usuarioId;
}
