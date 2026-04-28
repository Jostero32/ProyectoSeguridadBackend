package com.seguridad.Messenger.usuario.controller;

import com.seguridad.Messenger.shared.exception.ErrorResponse;
import com.seguridad.Messenger.shared.security.UserPrincipal;
import com.seguridad.Messenger.usuario.dto.PerfilResponse;
import com.seguridad.Messenger.usuario.dto.RegistroRequest;
import com.seguridad.Messenger.usuario.model.Usuario;
import com.seguridad.Messenger.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "Registro, perfil y búsqueda de usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/registro")
    @Operation(summary = "Registrar un nuevo usuario")
    @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de registro inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email o username ya registrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<PerfilResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        Usuario usuario = usuarioService.registrar(request);
        PerfilResponse perfil = usuarioService.obtenerPerfilPropio(usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(perfil);
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener el perfil del usuario autenticado")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponse(responseCode = "200", description = "Perfil del usuario")
    @ApiResponse(responseCode = "401", description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<PerfilResponse> obtenerMiPerfil(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(usuarioService.obtenerPerfilPropio(principal.usuarioId()));
    }

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Actualizar perfil del usuario autenticado")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponse(responseCode = "200", description = "Perfil actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<PerfilResponse> actualizarPerfil(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart(required = false) MultipartFile avatar,
            @RequestPart(required = false) String bio,
            @RequestPart(required = false) String privacidadUltimoVisto) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(
                principal.usuarioId(), avatar, bio, privacidadUltimoVisto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener perfil público de un usuario")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponse(responseCode = "200", description = "Perfil público del usuario")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<PerfilResponse> obtenerPerfil(
            @Parameter(description = "ID del usuario") @PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.obtenerPerfilPublico(id));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar usuarios por username, nombre o apellido (contains, case insensitive)")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponse(responseCode = "200", description = "Resultados de búsqueda paginados")
    public ResponseEntity<Page<PerfilResponse>> buscar(
            @Parameter(description = "Texto a buscar en username, nombre o apellido") @RequestParam String q,
            @Parameter(description = "Número de página (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(usuarioService.buscar(q, page, size));
    }
}
