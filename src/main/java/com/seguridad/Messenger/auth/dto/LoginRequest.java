package com.seguridad.Messenger.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud de inicio de sesión")
public record LoginRequest(
        @Schema(description = "Correo electrónico del usuario", example = "usuario@email.com")
        @NotBlank @Email String email,

        @Schema(description = "Contraseña del usuario", minLength = 8)
        @NotBlank @Size(min = 8) String password
) {}
