package com.seguridad.Messenger.mensajes.service;

import com.seguridad.Messenger.conversacion.repository.ConversacionRepository;
import com.seguridad.Messenger.conversacion.repository.ParticipanteRepository;
import com.seguridad.Messenger.mensajes.dto.*;
import com.seguridad.Messenger.mensajes.mapper.MensajeMapper;
import com.seguridad.Messenger.mensajes.model.ArchivoMultimedia;
import com.seguridad.Messenger.mensajes.model.EstadoMensaje;
import com.seguridad.Messenger.mensajes.model.EstadoMensajeId;
import com.seguridad.Messenger.mensajes.model.Mensaje;
import com.seguridad.Messenger.mensajes.model.Reaccion;
import com.seguridad.Messenger.mensajes.model.ReaccionId;
import com.seguridad.Messenger.mensajes.model.UbicacionMensaje;
import com.seguridad.Messenger.mensajes.repository.EstadoMensajeRepository;
import com.seguridad.Messenger.mensajes.repository.MensajeRepository;
import com.seguridad.Messenger.mensajes.repository.ReaccionRepository;
import com.seguridad.Messenger.shared.enums.TipoConversacion;
import com.seguridad.Messenger.shared.enums.TipoMensaje;
import com.seguridad.Messenger.shared.service.StorageService;
import com.seguridad.Messenger.shared.service.ThumbnailService;
import com.seguridad.Messenger.shared.exception.AccesoDenegadoException;
import com.seguridad.Messenger.shared.exception.RecursoNoEncontradoException;
import com.seguridad.Messenger.usuario.repository.BloqueoRepository;
import com.seguridad.Messenger.websocket.dto.EstadoEntregaEventPayload;
import com.seguridad.Messenger.websocket.dto.ReaccionEventPayload;
import com.seguridad.Messenger.websocket.event.EstadoEntregaEvent;
import com.seguridad.Messenger.websocket.event.MensajeEnviadoEvent;
import com.seguridad.Messenger.websocket.event.ReaccionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final EstadoMensajeRepository estadoMensajeRepository;
    private final ConversacionRepository conversacionRepository;
    private final ParticipanteRepository participanteRepository;
    private final BloqueoRepository bloqueoRepository;
    private final StorageService storageService;
    private final ThumbnailService thumbnailService;
    private final ReaccionRepository reaccionRepository;
    private final MensajeMapper mensajeMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ─── Enviar ───────────────────────────────────────────────────────────────

    public MensajeResponse enviarMensaje(UUID conversacionId, UUID usuarioId,
                                         TipoMensaje tipo,
                                         String contenido,
                                         UUID respuestaMensajeId,
                                         MultipartFile archivo,
                                         BigDecimal latitud,
                                         BigDecimal longitud,
                                         String nombreLugar) {
        verificarParticipante(conversacionId, usuarioId);
        verificarBloqueoEnChatIndividual(conversacionId, usuarioId);

        if (tipo == TipoMensaje.TEXTO && (contenido == null || contenido.isBlank())) {
            throw new IllegalArgumentException("El contenido es obligatorio para mensajes de tipo TEXTO");
        }
        if (tipo == TipoMensaje.UBICACION && (latitud == null || longitud == null)) {
            throw new IllegalArgumentException("latitud y longitud son obligatorios para mensajes de tipo UBICACION");
        }
        if (tipo != TipoMensaje.TEXTO && tipo != TipoMensaje.UBICACION && (archivo == null || archivo.isEmpty())) {
            throw new IllegalArgumentException("El archivo es obligatorio para mensajes de tipo " + tipo);
        }

        Mensaje respuestaMensaje = null;
        if (respuestaMensajeId != null) {
            respuestaMensaje = mensajeRepository.findById(respuestaMensajeId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje de respuesta no encontrado"));
            if (!respuestaMensaje.getConversacion().getId().equals(conversacionId)) {
                throw new RecursoNoEncontradoException("El mensaje de respuesta no pertenece a esta conversación");
            }
            if (respuestaMensaje.isEliminadoParaTodos()) {
                throw new IllegalStateException("No se puede responder a un mensaje eliminado para todos");
            }
        }

        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacionRepository.getReferenceById(conversacionId));
        mensaje.setRemitenteId(usuarioId);
        mensaje.setRespuestaMensaje(respuestaMensaje);
        mensaje.setTipo(tipo);
        mensaje.setCreadoEn(LocalDateTime.now());
        mensaje.setEliminadoParaTodos(false);

        if (tipo == TipoMensaje.TEXTO) {
            mensaje.setContenido(contenido);
        }

        final Mensaje mensajeGuardado = mensajeRepository.save(mensaje);

        if (tipo != TipoMensaje.TEXTO && tipo != TipoMensaje.UBICACION) {
            StorageService.StorageResult result = storageService.subir(archivo, tipo);
            ArchivoMultimedia am = new ArchivoMultimedia();
            am.setMensaje(mensajeGuardado);
            am.setNombreOriginal(archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo");
            am.setObjectKey(result.objectKey());
            am.setContentType(result.contentType());
            am.setTamanioBytes(result.tamanioBytes());

            if (tipo == TipoMensaje.IMAGEN) {
                try {
                    am.setThumbnailBase64(thumbnailService.generarThumbnailBase64(archivo.getBytes()));
                } catch (java.io.IOException e) {
                    am.setThumbnailBase64(null);
                }
            }

            mensajeGuardado.setArchivo(am);
        }

        if (tipo == TipoMensaje.UBICACION) {
            UbicacionMensaje ub = new UbicacionMensaje();
            ub.setMensaje(mensajeGuardado);
            ub.setLatitud(latitud);
            ub.setLongitud(longitud);
            ub.setNombreLugar(nombreLugar);
            mensajeGuardado.setUbicacion(ub);
        }

        final Mensaje mensajeFinal = mensajeRepository.save(mensajeGuardado);

        LocalDateTime ahora = LocalDateTime.now();
        List<UUID> receptores = participanteRepository
                .findUsuarioIdsByConversacionExcluyendo(conversacionId, usuarioId);

        List<EstadoMensaje> estados = receptores.stream()
                .map(receptorId -> {
                    EstadoMensaje e = new EstadoMensaje();
                    e.setId(new EstadoMensajeId(mensajeFinal.getId(), receptorId));
                    e.setEntregadoEn(ahora);
                    return e;
                })
                .collect(Collectors.toList());

        if (!estados.isEmpty()) {
            estadoMensajeRepository.saveAll(estados);
        }

        // Notificar al remitente cuando cada receptor recibe el mensaje (entregado)
        receptores.forEach(receptorId -> eventPublisher.publishEvent(new EstadoEntregaEvent(
                new EstadoEntregaEventPayload(mensajeFinal.getId(), conversacionId, receptorId, ahora, null)
        )));

        MensajeResponse response = mensajeMapper.toResponse(mensajeFinal);
        eventPublisher.publishEvent(new MensajeEnviadoEvent(response));
        return response;
    }

    // ─── Editar ───────────────────────────────────────────────────────────────

    public MensajeResponse editarMensaje(UUID conversacionId, UUID mensajeId, UUID usuarioId,
                                          EditarMensajeRequest req) {
        verificarParticipante(conversacionId, usuarioId);

        Mensaje mensaje = cargarMensaje(mensajeId);
        verificarMensajeEnConversacion(mensaje, conversacionId);

        if (!mensaje.getRemitenteId().equals(usuarioId)) {
            throw new AccesoDenegadoException("Solo el autor puede editar el mensaje");
        }
        if (mensaje.getEliminadoEn() != null) {
            throw new IllegalStateException("No se puede editar un mensaje eliminado");
        }
        if (mensaje.getTipo() != TipoMensaje.TEXTO) {
            throw new IllegalStateException("Solo se pueden editar mensajes de tipo texto");
        }

        mensaje.setContenido(req.contenido());
        mensaje.setEditadoEn(LocalDateTime.now());
        mensaje = mensajeRepository.save(mensaje);

        return mensajeMapper.toResponse(mensaje);
    }

    // ─── Eliminar ─────────────────────────────────────────────────────────────

    public void eliminarMensaje(UUID conversacionId, UUID mensajeId, UUID usuarioId, String tipo) {
        verificarParticipante(conversacionId, usuarioId);

        Mensaje mensaje = cargarMensaje(mensajeId);
        verificarMensajeEnConversacion(mensaje, conversacionId);

        LocalDateTime ahora = LocalDateTime.now();

        if ("para_todos".equals(tipo)) {
            if (!mensaje.getRemitenteId().equals(usuarioId)) {
                throw new AccesoDenegadoException("Solo el remitente puede eliminar el mensaje para todos");
            }
            if (mensaje.getArchivo() != null) {
                storageService.eliminar(mensaje.getArchivo().getObjectKey());
            }
            mensaje.setEliminadoEn(ahora);
            mensaje.setEliminadoParaTodos(true);
        } else {
            mensaje.setEliminadoEn(ahora);
            mensaje.setEliminadoParaTodos(false);
        }

        mensajeRepository.save(mensaje);
    }

    // ─── Marcar como leídos ──────────────────────────────────────────────────

    public void marcarTodosLeidos(UUID conversacionId, UUID usuarioId) {
        verificarParticipante(conversacionId, usuarioId);

        List<UUID> pendientes = estadoMensajeRepository
                .findMensajeIdsPendientesDeLectura(conversacionId, usuarioId);

        if (pendientes.isEmpty()) return;

        LocalDateTime ahora = LocalDateTime.now();
        estadoMensajeRepository.marcarTodosLeidosPorConversacion(conversacionId, usuarioId, ahora);

        pendientes.forEach(mensajeId -> eventPublisher.publishEvent(new EstadoEntregaEvent(
                new EstadoEntregaEventPayload(mensajeId, conversacionId, usuarioId, null, ahora)
        )));
    }

    // ─── Historial paginado ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<MensajeResponse> historial(UUID conversacionId, UUID usuarioId, Pageable pageable) {
        verificarParticipante(conversacionId, usuarioId);
        return mensajeRepository.findByConversacion(conversacionId, pageable)
            .map(mensajeMapper::toResponse);
    }

    // ─── Reaccionar (upsert) ──────────────────────────────────────────────────

    public List<ResumenReaccionesResponse> reaccionar(UUID conversacionId, UUID mensajeId,
                                                       UUID usuarioId, ReaccionRequest req) {
        verificarParticipante(conversacionId, usuarioId);

        Mensaje mensaje = cargarMensaje(mensajeId);
        verificarMensajeEnConversacion(mensaje, conversacionId);

        if (mensaje.isEliminadoParaTodos()) {
            throw new IllegalStateException("No se puede reaccionar a un mensaje eliminado para todos");
        }

        reaccionRepository.upsert(mensajeId, usuarioId, req.emoji(), LocalDateTime.now());

        List<ResumenReaccionesResponse> resumen =
                construirResumenReacciones(reaccionRepository.findByMensajeId(mensajeId), usuarioId);

        eventPublisher.publishEvent(new ReaccionEvent(
                "NUEVA_REACCION",
                new ReaccionEventPayload(mensajeId, conversacionId, usuarioId, req.emoji(), resumen)
        ));

        return resumen;
    }

    // ─── Quitar reacción ──────────────────────────────────────────────────────

    public void quitarReaccion(UUID conversacionId, UUID mensajeId, UUID usuarioId) {
        verificarParticipante(conversacionId, usuarioId);

        Mensaje mensaje = cargarMensaje(mensajeId);
        verificarMensajeEnConversacion(mensaje, conversacionId);

        ReaccionId reaccionId = new ReaccionId(mensajeId, usuarioId);
        if (!reaccionRepository.existsById(reaccionId)) {
            throw new RecursoNoEncontradoException("No tienes una reacción en este mensaje");
        }
        reaccionRepository.deleteById(reaccionId);

        List<ResumenReaccionesResponse> resumen =
                construirResumenReacciones(reaccionRepository.findByMensajeId(mensajeId), usuarioId);

        eventPublisher.publishEvent(new ReaccionEvent(
                "REACCION_ELIMINADA",
                new ReaccionEventPayload(mensajeId, conversacionId, usuarioId, null, resumen)
        ));
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private void verificarParticipante(UUID conversacionId, UUID usuarioId) {
        if (!participanteRepository.existsByIdConversacionIdAndIdUsuarioId(conversacionId, usuarioId)) {
            throw new AccesoDenegadoException("No eres participante de esta conversación");
        }
    }

    /**
     * En chats INDIVIDUAL, rechaza el envío si existe un bloqueo en cualquier
     * dirección entre los dos participantes. El mensaje de error es genérico —
     * no se revela si el bloqueo es del remitente, del receptor o mutuo.
     * En grupos no se evalúa: el bloqueo se entiende solo como relación entre pares.
     */
    private void verificarBloqueoEnChatIndividual(UUID conversacionId, UUID usuarioId) {
        TipoConversacion tipo = conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Conversación no encontrada"))
                .getTipo();

        if (tipo != TipoConversacion.INDIVIDUAL) return;

        UUID otroUsuarioId = participanteRepository
                .findOtroParticipante(conversacionId, usuarioId)
                .orElse(null);
        if (otroUsuarioId == null) return;

        if (bloqueoRepository.existsByIdUsuarioIdAndIdBloqueadoId(usuarioId, otroUsuarioId)
                || bloqueoRepository.existsByIdUsuarioIdAndIdBloqueadoId(otroUsuarioId, usuarioId)) {
            throw new AccesoDenegadoException("No puedes enviar mensajes a este usuario");
        }
    }

    private Mensaje cargarMensaje(UUID mensajeId) {
        return mensajeRepository.findById(mensajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado"));
    }

    private void verificarMensajeEnConversacion(Mensaje mensaje, UUID conversacionId) {
        if (!mensaje.getConversacion().getId().equals(conversacionId)) {
            throw new RecursoNoEncontradoException("El mensaje no pertenece a esta conversación");
        }
    }

    private List<ResumenReaccionesResponse> construirResumenReacciones(List<Reaccion> reacciones, UUID usuarioId) {
        return reacciones.stream()
                .collect(Collectors.groupingBy(Reaccion::getEmoji, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ResumenReaccionesResponse(
                        e.getKey(),
                        e.getValue(),
                        reacciones.stream().anyMatch(r ->
                                r.getId().getUsuarioId().equals(usuarioId) && r.getEmoji().equals(e.getKey()))
                ))
                .toList();
    }

    
}
