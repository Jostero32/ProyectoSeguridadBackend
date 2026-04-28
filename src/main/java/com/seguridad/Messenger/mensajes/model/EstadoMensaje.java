package com.seguridad.Messenger.mensajes.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "estado_mensaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstadoMensaje {

    @EmbeddedId
    private EstadoMensajeId id;

    @Column(name = "entregado_en")
    private LocalDateTime entregadoEn;

    @Column(name = "leido_en")
    private LocalDateTime leidoEn;
}
