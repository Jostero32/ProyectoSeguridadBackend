package com.seguridad.Messenger.auth.controller;

import com.seguridad.Messenger.auth.dto.LoginRequest;
import com.seguridad.Messenger.auth.dto.LoginResponse;
import com.seguridad.Messenger.auth.dto.SesionResponse;
import com.seguridad.Messenger.auth.model.DispositivoSesion;
import com.seguridad.Messenger.auth.repository.DispositivoSesionRepository;
import com.seguridad.Messenger.shared.exception.ErrorResponse;
import com.seguridad.Messenger.shared.security.UserPrincipal;
import com.seguridad.Messenger.shared.util.TokenGenerator;
import com.seguridad.Messenger.usuario.model.Usuario;
import com.seguridad.Messenger.usuario.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Endpoints de login, logout y gestión de sesiones")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final DispositivoSesionRepository sesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión con email y contraseña")
    @ApiResponse(responseCode = "200", description = "Login exitoso")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!usuario.isActivo()) {
            throw new BadCredentialsException("Cuenta desactivada");
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String token = tokenGenerator.generarToken();
        String userAgent = httpRequest.getHeader("User-Agent");

        DispositivoSesion sesion = new DispositivoSesion(usuario, token, userAgent);
        sesionRepository.save(sesion);

        return ResponseEntity.ok(new LoginResponse(token, usuario.getId()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar la sesión actual")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada")
    @ApiResponse(responseCode = "401", description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        sesionRepository.eliminarPorToken(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Cerrar todas las sesiones del usuario")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponse(responseCode = "204", description = "Todas las sesiones cerradas")
    @ApiResponse(responseCode = "401", description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
        sesionRepository.eliminarPorUsuarioId(principal.usuarioId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sesiones")
    @Operation(summary = "Listar dispositivos/sesiones activas del usuario")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponse(responseCode = "200", description = "Lista de sesiones activas")
    @ApiResponse(responseCode = "401", description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<SesionResponse>> listarSesiones(@AuthenticationPrincipal UserPrincipal principal) {
        List<SesionResponse> sesiones = sesionRepository.findByUsuarioId(principal.usuarioId()).stream()
                .map(s -> new SesionResponse(s.getId(), s.getInfoDispositivo(), s.getUltimoAcceso(), s.getPlataformaPush()))
                .toList();
        return ResponseEntity.ok(sesiones);
    }
}
