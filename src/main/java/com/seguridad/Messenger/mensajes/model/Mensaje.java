package com.seguridad.Messenger.mensajes.model;

import com.seguridad.Messenger.conversacion.model.Conversacion;
import com.seguridad.Messenger.shared.enums.TipoMensaje;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mensaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private Conversacion conversacion;

    @Column(name = "remitente_id", nullable = false)
    private UUID remitenteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respuesta_mensaje_id")
    private Mensaje respuestaMensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMensaje tipo;

    @Column(name = "contenido_cifrado")
    private String contenidoCifrado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "editado_en")
    private LocalDateTime editadoEn;

    @Column(name = "eliminado_en")
    private LocalDateTime eliminadoEn;

    @Column(name = "eliminado_para_todos", nullable = false)
    private boolean eliminadoParaTodos = false;

    @OneToOne(mappedBy = "mensaje", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ArchivoMultimedia archivo;

    @OneToOne(mappedBy = "mensaje", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UbicacionMensaje ubicacion;

    @OneToMany(mappedBy = "mensaje", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 30)
    private List<Reaccion> reacciones = new ArrayList<>();
}
