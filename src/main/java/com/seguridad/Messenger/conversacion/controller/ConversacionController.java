package com.seguridad.Messenger.conversacion.controller;

import com.seguridad.Messenger.conversacion.dto.*;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/conversaciones")
@RequiredArgsConstructor
@Tag(name = "Conversaciones", description = "Gestión de conversaciones individuales y grupales")
@SecurityRequirement(name = "BearerAuth")
public class ConversacionController {

    private final ConversacionService conversacionService;

    @PostMapping("/individual")
    @Operation(summary = "Crear o recuperar chat individual",
            description = "Retorna 201 si el chat se creó, 200 si ya existía.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chat individual ya existía"),
            @ApiResponse(responseCode = "201", description = "Chat individual creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o intento de chat con uno mismo"),
            @ApiResponse(responseCode = "403", description = "Chat con usuario bloqueado"),
            @ApiResponse(responseCode = "404", description = "Usuario destinatario no encontrado")
    })
    public ResponseEntity<ConversacionResponse> crearChatIndividual(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CrearChatIndividualRequest req) {
        ChatIndividualResult result = conversacionService.crearChatIndividual(principal.usuarioId(), req);
        return result.creada()
                ? ResponseEntity.status(HttpStatus.CREATED).body(result.conversacion())
                : ResponseEntity.ok(result.conversacion());
    }

    @PostMapping("/grupo")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear grupo",
            description = "El creador queda como administrador. Los miembrosIds se agregan con rol MIEMBRO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Grupo creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Uno o más usuarios en miembrosIds no encontrados")
    })
    public ConversacionResponse crearGrupo(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CrearGrupoRequest req) {
        return conversacionService.crearGrupo(principal.usuarioId(), req);
    }

    @GetMapping
    @Operation(summary = "Listar mis conversaciones",
            description = "Devuelve todas las conversaciones donde el usuario es participante, ordenadas por fecha de creación DESC. Las archivadas se excluyen por defecto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de conversaciones")
    })
    public List<ConversacionResponse> listarConversaciones(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Incluir conversaciones archivadas en el resultado", example = "false")
            @RequestParam(defaultValue = "false") boolean incluirArchivadas) {
        return conversacionService.listarConversaciones(principal.usuarioId(), incluirArchivadas);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de una conversación")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la conversación"),
            @ApiResponse(responseCode = "403", description = "No eres participante de esta conversación"),
            @ApiResponse(responseCode = "404", description = "Conversación no encontrada")
    })
    public ConversacionResponse obtenerConversacion(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID id) {
        return conversacionService.obtenerConversacion(id, principal.usuarioId());
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar nombre o avatar del grupo",
            description = "Solo los campos no nulos se actualizan. Solo el administrador puede ejecutar esta acción.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grupo actualizado"),
            @ApiResponse(responseCode = "400", description = "No se puede modificar un chat individual"),
            @ApiResponse(responseCode = "403", description = "No tienes permisos de administrador"),
            @ApiResponse(responseCode = "404", description = "Conversación no encontrada")
    })
    public ConversacionResponse actualizarGrupo(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID id,
            @Valid @RequestBody ActualizarGrupoRequest req) {
        return conversacionService.actualizarGrupo(id, principal.usuarioId(), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar grupo",
            description = "Hard delete. Elimina en cascada todos los participantes y configuraciones. Solo el administrador puede ejecutar esta acción.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Grupo eliminado"),
            @ApiResponse(responseCode = "400", description = "No se puede eliminar un chat individual con este endpoint"),
            @ApiResponse(responseCode = "403", description = "No tienes permisos de administrador"),
            @ApiResponse(responseCode = "404", description = "Conversación no encontrada")
    })
    public void eliminarGrupo(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID id) {
        conversacionService.eliminarGrupo(id, principal.usuarioId());
    }

    @GetMapping("/{id}/configuracion")
    @Operation(summary = "Obtener mi configuración personal para este chat",
            description = "Devuelve la configuración de silencio y archivo para el usuario autenticado. Si no existe configuración previa, devuelve los valores por defecto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuración actual"),
            @ApiResponse(responseCode = "403", description = "No eres participante de esta conversación"),
            @ApiResponse(responseCode = "404", description = "Conversación no encontrada")
    })
    public ConfiguracionChatResponse obtenerConfiguracion(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID id) {
        return conversacionService.obtenerConfiguracion(id, principal.usuarioId());
    }

    @PutMapping("/{id}/configuracion")
    @Operation(summary = "Actualizar silenciar / archivar el chat",
            description = "Upsert: crea la configuración si no existe, actualiza si ya existe. El silencio y el archivo son por usuario y no afectan a otros participantes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuración actualizada"),
            @ApiResponse(responseCode = "403", description = "No eres participante de esta conversación"),
            @ApiResponse(responseCode = "404", description = "Conversación no encontrada")
    })
    public ConfiguracionChatResponse actualizarConfiguracion(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID id,
            @Valid @RequestBody ActualizarConfiguracionRequest req) {
        return conversacionService.actualizarConfiguracion(id, principal.usuarioId(), req);
    }
}
