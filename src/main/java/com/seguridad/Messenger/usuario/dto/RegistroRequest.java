package com.seguridad.Messenger.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Solicitud de registro de nuevo usuario")
public record RegistroRequest(
        @Schema(description = "Nombres del usuario", example = "Juan Carlos")
        @NotBlank String nombres,

        @Schema(description = "Apellidos del usuario", example = "Pérez García")
        @NotBlank String apellidos,

        @Schema(description = "Fecha de nacimiento", example = "1995-06-15")
        @NotNull LocalDate fechaNacimiento,

        @Schema(description = "Nombre de usuario único", example = "juanperez")
        @NotBlank String username,

        @Schema(description = "Correo electrónico", example = "juan@email.com")
        @NotBlank @Email String email,

        @Schema(description = "Contraseña (mínimo 8 caracteres)", minLength = 8)
        @NotBlank @Size(min = 8) String password
) {}
