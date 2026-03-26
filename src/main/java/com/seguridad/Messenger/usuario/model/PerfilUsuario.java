package com.seguridad.Messenger.usuario.model;

import com.seguridad.Messenger.shared.enums.PrivacidadUltimoVisto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "perfil_usuario")
public class PerfilUsuario {

    @Id
    @Column(name = "usuario_id")
    private UUID id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "url_avatar")
    private String urlAvatar;

    @Column(length = 160)
    private String bio;

    @Column(unique = true)
    private String telefono;

    @Column(name = "ultimo_visto")
    private LocalDateTime ultimoVisto;

    @Column(name = "privacidad_ultimo_visto", nullable = false)
    private PrivacidadUltimoVisto privacidadUltimoVisto = PrivacidadUltimoVisto.TODOS;

    public PerfilUsuario(Usuario usuario) {
        this.usuario = usuario;
        this.privacidadUltimoVisto = PrivacidadUltimoVisto.TODOS;
    }
}
