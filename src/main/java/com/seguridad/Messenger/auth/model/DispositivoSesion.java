package com.seguridad.Messenger.auth.model;

import com.seguridad.Messenger.usuario.model.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "dispositivo_sesion", indexes = {
        @Index(name = "idx_dispositivo_token", columnList = "token_sesion", unique = true)
})
public class DispositivoSesion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_sesion", nullable = false, unique = true)
    private String tokenSesion;

    @Column(name = "info_dispositivo")
    private String infoDispositivo;

    @Column(name = "ultimo_acceso", nullable = false)
    private LocalDateTime ultimoAcceso;

    @Column(name = "token_push")
    private String tokenPush;

    @Column(name = "plataforma_push")
    private String plataformaPush;

    public DispositivoSesion(Usuario usuario, String tokenSesion, String infoDispositivo) {
        this.usuario = usuario;
        this.tokenSesion = tokenSesion;
        this.infoDispositivo = infoDispositivo;
        this.ultimoAcceso = LocalDateTime.now();
    }
}
