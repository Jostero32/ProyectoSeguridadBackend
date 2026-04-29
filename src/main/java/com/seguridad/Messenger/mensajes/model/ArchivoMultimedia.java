package com.seguridad.Messenger.mensajes.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "archivo_multimedia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArchivoMultimedia {

    @Id
    @Column(name = "mensaje_id")
    private UUID mensajeId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "mensaje_id")
    private Mensaje mensaje;

    @Column(name = "nombre_original", nullable = false)
    private String nombreOriginal;

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "tamanio_bytes", nullable = false)
    private long tamanioBytes;

    @Column(name = "thumbnail_base64", columnDefinition = "TEXT")
    private String thumbnailBase64;
}
