package com.seguridad.Messenger.conversacion.controller;

import com.seguridad.Messenger.conversacion.dto.AgregarMiembrosRequest;
import com.seguridad.Messenger.conversacion.dto.ParticipanteResponse;
import com.seguridad.Messenger.conversacion.service.ConversacionService;
import com.seguridad.Messenger.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chats/{conversacionId}/miembros")
@RequiredArgsConstructor
@Tag(name = "Miembros", description = "Gestión de miembros en chats grupales")
@SecurityRequirement(name = "BearerAuth")
public class MiembroController {

    private final ConversacionService conversacionService;

    @GetMapping
    @Operation(summary = "Listar miembros de una conversación")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de miembros"),
            @ApiResponse(responseCode = "403", description = "No eres participante de esta conversación"),
            @ApiResponse(responseCode = "404", description = "Conversación no encontrada")
    })
    public List<ParticipanteResponse> listarMiembros(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId) {
        return conversacionService.listarMiembros(conversacionId, principal.usuarioId());
    }

    @PostMapping
    @Operation(summary = "Agregar miembros al grupo",
            description = "Solo el administrador puede agregar miembros. Los usuarios que ya son participantes se ignoran (idempotente). Devuelve la lista completa de miembros actualizada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Miembros agregados — devuelve la lista completa actualizada"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "403", description = "No tienes permisos de administrador"),
            @ApiResponse(responseCode = "404", description = "Conversación o usuario no encontrado")
    })
    public List<ParticipanteResponse> agregarMiembros(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId,
            @Valid @RequestBody AgregarMiembrosRequest req) {
        return conversacionService.agregarMiembros(conversacionId, principal.usuarioId(), req);
    }

    @DeleteMapping("/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Expulsar miembro del grupo",
            description = "Solo el administrador puede expulsar miembros. No se puede expulsar a otro administrador.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Miembro expulsado"),
            @ApiResponse(responseCode = "403", description = "No eres admin, o el objetivo también es admin"),
            @ApiResponse(responseCode = "404", description = "Conversación o miembro no encontrado")
    })
    public void expulsarMiembro(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId,
            @Parameter(description = "ID del usuario a expulsar") @PathVariable UUID usuarioId) {
        conversacionService.expulsarMiembro(conversacionId, principal.usuarioId(), usuarioId);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Abandonar grupo",
            description = "Cualquier participante puede abandonar. Si el que abandona es el último admin, se promueve automáticamente al miembro más antiguo. Si era el único miembro, el grupo se elimina.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Abandonaste el grupo (o el grupo fue eliminado por ser el último miembro)"),
            @ApiResponse(responseCode = "400", description = "No aplica a chats individuales"),
            @ApiResponse(responseCode = "403", description = "No eres participante de esta conversación"),
            @ApiResponse(responseCode = "404", description = "Conversación no encontrada")
    })
    public void abandonarGrupo(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId) {
        conversacionService.abandonarGrupo(conversacionId, principal.usuarioId());
    }
}
