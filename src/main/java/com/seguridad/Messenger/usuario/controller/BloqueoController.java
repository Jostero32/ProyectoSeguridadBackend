package com.seguridad.Messenger.usuario.controller;

import com.seguridad.Messenger.shared.security.UserPrincipal;
import com.seguridad.Messenger.usuario.dto.BloqueoResponse;
import com.seguridad.Messenger.usuario.service.BloqueoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios/me/bloqueos")
@RequiredArgsConstructor
@Tag(name = "Bloqueos", description = "Gestión de usuarios bloqueados")
@SecurityRequirement(name = "BearerAuth")
public class BloqueoController {

    private final BloqueoService bloqueoService;

    @GetMapping
    @Operation(summary = "Listar usuarios bloqueados")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios bloqueados")
    public ResponseEntity<List<BloqueoResponse>> listar(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bloqueoService.listar(principal.usuarioId()));
    }

    @PostMapping("/{bloqueadoId}")
    @Operation(summary = "Bloquear usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario bloqueado (o ya estaba bloqueado)"),
            @ApiResponse(responseCode = "400", description = "No puedes bloquearte a ti mismo"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<Void> bloquear(
            @Parameter(description = "ID del usuario a bloquear") @PathVariable UUID bloqueadoId,
            @AuthenticationPrincipal UserPrincipal principal) {
        bloqueoService.bloquear(principal.usuarioId(), bloqueadoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{bloqueadoId}")
    @Operation(summary = "Desbloquear usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario desbloqueado"),
            @ApiResponse(responseCode = "404", description = "Bloqueo no encontrado")
    })
    public ResponseEntity<Void> desbloquear(
            @Parameter(description = "ID del usuario a desbloquear") @PathVariable UUID bloqueadoId,
            @AuthenticationPrincipal UserPrincipal principal) {
        bloqueoService.desbloquear(principal.usuarioId(), bloqueadoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{bloqueadoId}")
    @Operation(summary = "Verificar si un usuario está bloqueado")
    @ApiResponse(responseCode = "200", description = "true si está bloqueado, false si no")
    public ResponseEntity<Boolean> esBloqueado(
            @Parameter(description = "ID del usuario a verificar") @PathVariable UUID bloqueadoId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bloqueoService.esBloqueado(principal.usuarioId(), bloqueadoId));
    }
}
