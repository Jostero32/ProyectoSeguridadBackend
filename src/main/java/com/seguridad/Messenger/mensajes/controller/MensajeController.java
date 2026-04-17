package com.seguridad.Messenger.mensajes.controller;

import com.seguridad.Messenger.mensajes.dto.*;
import com.seguridad.Messenger.mensajes.service.MensajeService;
import com.seguridad.Messenger.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/conversaciones/{conversacionId}/mensajes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Mensajes", description = "Envío, edición, eliminación y lectura de mensajes de texto. " +
        "Los tipos adicionales (imagen, audio, video, documento, etc.) se añadirán en versiones futuras.")
@SecurityRequirement(name = "BearerAuth")
public class MensajeController {

    private final MensajeService mensajeService;

    @GetMapping
    @Operation(summary = "Historial paginado de mensajes",
            description = "Devuelve los mensajes de la conversación ordenados por fecha de envío DESC (más recientes primero). " +
                    "Excluye mensajes eliminados para todos. El tamaño máximo de página es 50.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de mensajes"),
            @ApiResponse(responseCode = "400", description = "Parámetro size mayor a 50 o page negativo"),
            @ApiResponse(responseCode = "403", description = "No eres participante de la conversación"),
            @ApiResponse(responseCode = "404", description = "Conversación no encontrada")
    })
    public Page<MensajeResponse> historial(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId,
            @Parameter(description = "Número de página, inicia en 0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Cantidad de mensajes por página (máximo 50)") @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return mensajeService.historial(conversacionId, principal.usuarioId(), PageRequest.of(page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar mensaje",
            description = "Envía un mensaje de texto en la conversación. " +
                    "Opcionalmente puede ser una respuesta a otro mensaje (respuestaMensajeId). " +
                    "Crea automáticamente el estado de entrega para cada participante.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mensaje enviado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o respuesta a mensaje eliminado para todos"),
            @ApiResponse(responseCode = "403", description = "No eres participante de la conversación"),
            @ApiResponse(responseCode = "404", description = "Conversación o mensaje de respuesta no encontrado")
    })
    public MensajeResponse enviarMensaje(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId,
            @Valid @RequestBody EnviarMensajeRequest req) {
        return mensajeService.enviarMensaje(conversacionId, principal.usuarioId(), req);
    }

    @PatchMapping("/{mensajeId}")
    @Operation(summary = "Editar mensaje",
            description = "Edita el contenido de un mensaje de texto. Solo el autor puede editar su propio mensaje. " +
                    "No se pueden editar mensajes eliminados ni mensajes de otros tipos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensaje editado"),
            @ApiResponse(responseCode = "400", description = "Mensaje eliminado o tipo no editable"),
            @ApiResponse(responseCode = "403", description = "No eres el autor del mensaje"),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado")
    })
    public MensajeResponse editarMensaje(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId,
            @Parameter(description = "ID del mensaje a editar") @PathVariable UUID mensajeId,
            @Valid @RequestBody EditarMensajeRequest req) {
        return mensajeService.editarMensaje(conversacionId, mensajeId, principal.usuarioId(), req);
    }

    @DeleteMapping("/{mensajeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar mensaje",
            description = "Elimina un mensaje. " +
                    "**para_mi** (default): cualquier participante puede eliminar el mensaje de su propia vista. " +
                    "**para_todos**: solo el remitente puede eliminar para todos los participantes.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mensaje eliminado"),
            @ApiResponse(responseCode = "403", description = "No eres participante o intentas eliminar para todos sin ser el remitente"),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado")
    })
    public void eliminarMensaje(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId,
            @Parameter(description = "ID del mensaje a eliminar") @PathVariable UUID mensajeId,
            @Parameter(description = "Tipo de eliminación: `para_mi` (solo tú) o `para_todos` (solo el remitente puede usarlo)")
            @RequestParam(defaultValue = "para_mi") String tipo) {
        mensajeService.eliminarMensaje(conversacionId, mensajeId, principal.usuarioId(), tipo);
    }

    @PostMapping("/{mensajeId}/leido")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marcar mensajes como leídos (batch)",
            description = "Marca uno o varios mensajes como leídos en un solo request. " +
                    "Se ignoran los IDs que no pertenezcan a la conversación o que correspondan a mensajes del propio usuario. " +
                    "Si el estado de entrega no existía, se crea con entregado_en = leido_en = ahora.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mensajes marcados como leídos"),
            @ApiResponse(responseCode = "400", description = "Lista de mensajes vacía"),
            @ApiResponse(responseCode = "403", description = "No eres participante de la conversación"),
            @ApiResponse(responseCode = "404", description = "Conversación no encontrada")
    })
    public void marcarLeido(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId,
            @Parameter(description = "ID del mensaje (requerido por la ruta, los IDs reales van en el body)")
            @PathVariable UUID mensajeId,
            @Valid @RequestBody MarcarLeidoRequest req) {
        mensajeService.marcarLeido(conversacionId, principal.usuarioId(), req);
    }
}
