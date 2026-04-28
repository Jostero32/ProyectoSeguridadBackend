package com.seguridad.Messenger.conversacion.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seguridad.Messenger.conversacion.dto.*;
import com.seguridad.Messenger.conversacion.service.ConversacionService;
import com.seguridad.Messenger.mensajes.service.MensajeService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Tag(name = "Chats", description = "Gestión de chats individuales y grupales")
@SecurityRequirement(name = "BearerAuth")
public class ConversacionController {

    private final ConversacionService conversacionService;
    private final MensajeService mensajeService;
    private final ObjectMapper objectMapper;

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

    @PostMapping(value = "/grupo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear grupo",
            description = "El creador queda como administrador. miembrosIds debe ser un JSON array de UUIDs.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Grupo creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Uno o más usuarios en miembrosIds no encontrados")
    })
    public ConversacionResponse crearGrupo(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("titulo") String titulo,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            @RequestPart("miembrosIds") String miembrosIdsJson) {
        try {
            List<UUID> miembrosIds = objectMapper.readValue(miembrosIdsJson, new TypeReference<>() {});
            return conversacionService.crearGrupo(principal.usuarioId(), titulo, avatar, miembrosIds);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("miembrosIds debe ser un JSON array de UUIDs: " + e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Listar mis chats",
            description = "Devuelve todos los chats donde el usuario es participante con último mensaje y conteo de no leídos, ordenados por fecha de creación DESC. Los archivados se excluyen por defecto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de chats")
    })
    public List<ChatResumenResponse> listarChats(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Incluir chats archivados en el resultado", example = "false")
            @RequestParam(defaultValue = "false") boolean incluirArchivadas) {
        return conversacionService.listarChats(principal.usuarioId(), incluirArchivadas);
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

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Actualizar nombre o avatar del grupo",
            description = "Solo los campos presentes se actualizan. Solo el administrador puede ejecutar esta acción.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grupo actualizado"),
            @ApiResponse(responseCode = "400", description = "No se puede modificar un chat individual"),
            @ApiResponse(responseCode = "403", description = "No tienes permisos de administrador"),
            @ApiResponse(responseCode = "404", description = "Conversación no encontrada")
    })
    public ConversacionResponse actualizarGrupo(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID id,
            @RequestPart(value = "titulo", required = false) String titulo,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        return conversacionService.actualizarGrupo(id, principal.usuarioId(), titulo, avatar);
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

    @PostMapping("/{id}/leido")
    @Operation(summary = "Marcar todos los mensajes no leídos de este chat como leídos",
            description = "El cliente llama a este endpoint al abrir un chat. Marca en bulk todos los mensajes pendientes de lectura para el usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mensajes marcados como leídos"),
            @ApiResponse(responseCode = "403", description = "No eres participante de este chat"),
            @ApiResponse(responseCode = "404", description = "Chat no encontrado")
    })
    public ResponseEntity<Void> marcarTodosLeidos(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID del chat") @PathVariable UUID id) {
        mensajeService.marcarTodosLeidos(id, principal.usuarioId());
        return ResponseEntity.noContent().build();
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
