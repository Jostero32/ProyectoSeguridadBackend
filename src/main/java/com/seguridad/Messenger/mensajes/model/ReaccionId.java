package com.seguridad.Messenger.mensajes.model;

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
public class ReaccionId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "mensaje_id")
    private UUID mensajeId;

    @Column(name = "usuario_id")
    private UUID usuarioId;
}
