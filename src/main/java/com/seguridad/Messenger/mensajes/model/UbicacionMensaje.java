package com.seguridad.Messenger.mensajes.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ubicacion_mensaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionMensaje {

    @Id
    @Column(name = "mensaje_id")
    private UUID mensajeId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "mensaje_id")
    private Mensaje mensaje;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "nombre_lugar")
    private String nombreLugar;
}
