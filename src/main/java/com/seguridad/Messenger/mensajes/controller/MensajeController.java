package com.seguridad.Messenger.mensajes.controller;

import com.seguridad.Messenger.mensajes.dto.*;
import com.seguridad.Messenger.mensajes.service.MensajeService;
import com.seguridad.Messenger.shared.enums.TipoMensaje;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/conversaciones/{conversacionId}/mensajes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Mensajes", description = "Envío, edición, eliminación y lectura de mensajes. " +
        "Soporta tipos: TEXTO, IMAGEN, AUDIO, VIDEO, DOCUMENTO, STICKER, GIF y UBICACION.")
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar mensaje",
            description = "Envía un mensaje en la conversación. El campo `tipo` determina qué otros campos son obligatorios: " +
                    "**TEXTO** requiere `contenido`; " +
                    "**IMAGEN / AUDIO / VIDEO / DOCUMENTO / STICKER / GIF** requieren `archivo`; " +
                    "**UBICACION** requiere `latitud` y `longitud`. " +
                    "Todos los tipos admiten `respuestaMensajeId` para responder a un mensaje previo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mensaje enviado"),
            @ApiResponse(responseCode = "400", description = "Payload inválido o respuesta a mensaje eliminado para todos"),
            @ApiResponse(responseCode = "403", description = "No eres participante de la conversación"),
            @ApiResponse(responseCode = "404", description = "Conversación o mensaje de respuesta no encontrado"),
            @ApiResponse(responseCode = "413", description = "Archivo demasiado grande"),
            @ApiResponse(responseCode = "415", description = "Tipo de archivo no permitido")
    })
    public MensajeResponse enviarMensaje(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId,
            @Parameter(description = "Tipo de mensaje") @RequestPart String tipo,
            @Parameter(description = "Contenido de texto (obligatorio para TEXTO)") @RequestPart(required = false) String contenido,
            @Parameter(description = "Archivo adjunto (obligatorio para IMAGEN/AUDIO/VIDEO/DOCUMENTO/STICKER/GIF)") @RequestPart(required = false) MultipartFile archivo,
            @Parameter(description = "ID del mensaje al que se responde") @RequestPart(required = false) String respuestaMensajeId,
            @Parameter(description = "Latitud (obligatorio para UBICACION)") @RequestPart(required = false) String latitud,
            @Parameter(description = "Longitud (obligatorio para UBICACION)") @RequestPart(required = false) String longitud,
            @Parameter(description = "Nombre del lugar (opcional para UBICACION)") @RequestPart(required = false) String nombreLugar,
            @Parameter(description = "Duración en segundos (opcional para AUDIO/VIDEO)") @RequestPart(required = false) String duracionSegundos,
            @Parameter(description = "Ancho en píxeles (opcional para IMAGEN/VIDEO)") @RequestPart(required = false) String anchoPx,
            @Parameter(description = "Alto en píxeles (opcional para IMAGEN/VIDEO)") @RequestPart(required = false) String altoPx) {

        return mensajeService.enviarMensaje(
                conversacionId,
                principal.usuarioId(),
                TipoMensaje.valueOf(tipo.toUpperCase()),
                contenido,
                respuestaMensajeId != null ? UUID.fromString(respuestaMensajeId) : null,
                archivo,
                parseIntOrNull(duracionSegundos),
                parseIntOrNull(anchoPx),
                parseIntOrNull(altoPx),
                parseBigDecimalOrNull(latitud),
                parseBigDecimalOrNull(longitud),
                nombreLugar
        );
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Valor numérico inválido: " + value); }
    }

    private BigDecimal parseBigDecimalOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Coordenada inválida: " + value); }
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

    // ─── Reacciones ───────────────────────────────────────────────────────────

    @PutMapping("/{mensajeId}/reaccion")
    @Operation(summary = "Añadir o reemplazar reacción",
            description = "Establece la reacción del usuario autenticado en el mensaje. " +
                    "Si ya existe una reacción previa, la reemplaza. Un usuario = una reacción por mensaje.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen de reacciones actualizado"),
            @ApiResponse(responseCode = "400", description = "Mensaje eliminado para todos"),
            @ApiResponse(responseCode = "403", description = "No eres participante de la conversación"),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado")
    })
    public List<ResumenReaccionesResponse> reaccionar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId,
            @Parameter(description = "ID del mensaje") @PathVariable UUID mensajeId,
            @Valid @RequestBody ReaccionRequest req) {
        return mensajeService.reaccionar(conversacionId, mensajeId, principal.usuarioId(), req);
    }

    @DeleteMapping("/{mensajeId}/reaccion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quitar reacción",
            description = "Elimina la reacción del usuario autenticado en el mensaje.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reacción eliminada"),
            @ApiResponse(responseCode = "403", description = "No eres participante de la conversación"),
            @ApiResponse(responseCode = "404", description = "No tienes una reacción en este mensaje")
    })
    public void quitarReaccion(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId,
            @Parameter(description = "ID del mensaje") @PathVariable UUID mensajeId) {
        mensajeService.quitarReaccion(conversacionId, mensajeId, principal.usuarioId());
    }

    @GetMapping("/{mensajeId}/reacciones")
    @Operation(summary = "Listar reacciones detalladas",
            description = "Devuelve la lista completa de reacciones del mensaje con el username de cada usuario, " +
                    "ordenada por fecha de creación ASC.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reacciones"),
            @ApiResponse(responseCode = "403", description = "No eres participante de la conversación"),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado")
    })
    public List<ReaccionResponse> listarReacciones(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId,
            @Parameter(description = "ID del mensaje") @PathVariable UUID mensajeId) {
        return mensajeService.listarReacciones(conversacionId, mensajeId, principal.usuarioId());
    }

    // ─── Forward ──────────────────────────────────────────────────────────────

    @PostMapping("/{mensajeId}/forward")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reenviar mensaje dentro de la misma conversación",
            description = "Crea un nuevo mensaje que referencia al mensaje original. " +
                    "El contenido no se copia — se lee del mensaje original. " +
                    "Si el mensaje original ya es un forward, el nuevo forward apunta al origen de la cadena. " +
                    "El campo `reenviaDe.contenido` será null si el original fue eliminado para todos.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mensaje reenviado"),
            @ApiResponse(responseCode = "400", description = "El mensaje original fue eliminado para todos"),
            @ApiResponse(responseCode = "403", description = "No eres participante de la conversación"),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado")
    })
    public MensajeResponse forward(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID de la conversación") @PathVariable UUID conversacionId,
            @Parameter(description = "ID del mensaje a reenviar") @PathVariable UUID mensajeId) {
        return mensajeService.forward(conversacionId, mensajeId, principal.usuarioId());
    }

    // ─── Marcar como leído ────────────────────────────────────────────────────

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
