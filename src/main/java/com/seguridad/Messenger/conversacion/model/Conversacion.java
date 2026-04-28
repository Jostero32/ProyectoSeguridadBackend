package com.seguridad.Messenger.conversacion.model;

import com.seguridad.Messenger.shared.enums.TipoConversacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoConversacion tipo;

    @Column(name = "canal_hash", unique = true)
    private String canalHash;

    @Column(name = "titulo_grupo")
    private String tituloGrupo;

    @Column(name = "url_avatar_grupo")
    private String urlAvatarGrupo;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @OneToMany(mappedBy = "conversacion", fetch = FetchType.LAZY)
    private List<Participante> participantes = new ArrayList<>();
}
