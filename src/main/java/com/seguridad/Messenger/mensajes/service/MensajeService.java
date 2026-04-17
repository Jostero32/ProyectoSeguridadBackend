package com.seguridad.Messenger.mensajes.service;

import com.seguridad.Messenger.conversacion.repository.ConversacionRepository;
import com.seguridad.Messenger.conversacion.repository.ParticipanteRepository;
import com.seguridad.Messenger.mensajes.dto.*;
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
import com.seguridad.Messenger.shared.enums.TipoMensaje;
import com.seguridad.Messenger.shared.exception.AccesoDenegadoException;
import com.seguridad.Messenger.shared.exception.RecursoNoEncontradoException;
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
import java.util.Set;
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
    private final StorageService storageService;
    private final ReaccionRepository reaccionRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ─── Enviar ───────────────────────────────────────────────────────────────

    public MensajeResponse enviarMensaje(UUID conversacionId, UUID usuarioId,
                                         TipoMensaje tipo,
                                         String contenido,
                                         UUID respuestaMensajeId,
                                         MultipartFile archivo,
                                         Integer duracionSegundos,
                                         Integer anchoPx,
                                         Integer altoPx,
                                         BigDecimal latitud,
                                         BigDecimal longitud,
                                         String nombreLugar) {
        verificarParticipante(conversacionId, usuarioId);

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
            mensaje.setContenidoCifrado(contenido);
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
            am.setDuracionSegundos(duracionSegundos);
            am.setAnchoPx(anchoPx);
            am.setAltoPx(altoPx);
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

        MensajeResponse response = toResponse(mensajeFinal, usuarioId, conversacionId);
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

        mensaje.setContenidoCifrado(req.contenido());
        mensaje.setEditadoEn(LocalDateTime.now());
        mensaje = mensajeRepository.save(mensaje);

        return toResponse(mensaje, usuarioId, conversacionId);
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

    // ─── Marcar como leído (batch) ────────────────────────────────────────────

    public void marcarLeido(UUID conversacionId, UUID usuarioId, MarcarLeidoRequest req) {
        verificarParticipante(conversacionId, usuarioId);

        List<UUID> mensajeIds = req.mensajeIds();

        List<Mensaje> mensajesValidos = mensajeRepository.findValidosParaLeer(
                conversacionId, usuarioId, mensajeIds);

        if (mensajesValidos.isEmpty()) {
            return;
        }

        List<UUID> validIds = mensajesValidos.stream()
                .map(Mensaje::getId)
                .collect(Collectors.toList());

        LocalDateTime ahora = LocalDateTime.now();

        estadoMensajeRepository.marcarLeidoBulk(validIds, usuarioId, ahora);

        Set<UUID> existentes = estadoMensajeRepository
                .findByMensajeIdsAndUsuarioId(validIds, usuarioId)
                .stream()
                .map(e -> e.getId().getMensajeId())
                .collect(Collectors.toSet());

        List<EstadoMensaje> nuevos = mensajesValidos.stream()
                .filter(m -> !existentes.contains(m.getId()))
                .map(m -> {
                    EstadoMensaje e = new EstadoMensaje();
                    e.setId(new EstadoMensajeId(m.getId(), usuarioId));
                    e.setEntregadoEn(ahora);
                    e.setLeidoEn(ahora);
                    return e;
                })
                .collect(Collectors.toList());

        if (!nuevos.isEmpty()) {
            estadoMensajeRepository.saveAll(nuevos);
        }

        // Notificar al remitente de cada mensaje que fue leído
        validIds.forEach(id -> eventPublisher.publishEvent(new EstadoEntregaEvent(
                new EstadoEntregaEventPayload(id, conversacionId, usuarioId, null, ahora)
        )));
    }

    // ─── Historial paginado ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<MensajeResponse> historial(UUID conversacionId, UUID usuarioId, Pageable pageable) {
        verificarParticipante(conversacionId, usuarioId);
        return mensajeRepository.findByConversacion(conversacionId, pageable)
                .map(m -> toResponse(m, usuarioId, conversacionId));
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

    // ─── Listar reacciones detalladas ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReaccionResponse> listarReacciones(UUID conversacionId, UUID mensajeId, UUID usuarioId) {
        verificarParticipante(conversacionId, usuarioId);

        Mensaje mensaje = cargarMensaje(mensajeId);
        verificarMensajeEnConversacion(mensaje, conversacionId);

        return reaccionRepository.findByMensajeIdConUsuario(mensajeId).stream()
                .map(r -> new ReaccionResponse(
                        r.getId().getUsuarioId(),
                        r.getUsuario().getUsername(),
                        r.getEmoji(),
                        r.getCreadaEn()))
                .toList();
    }

    // ─── Forward ──────────────────────────────────────────────────────────────

    public MensajeResponse forward(UUID conversacionId, UUID mensajeIdOrigen, UUID usuarioId) {
        verificarParticipante(conversacionId, usuarioId);

        Mensaje original = cargarMensaje(mensajeIdOrigen);
        verificarMensajeEnConversacion(original, conversacionId);

        if (original.isEliminadoParaTodos()) {
            throw new IllegalStateException("No se puede reenviar un mensaje eliminado para todos");
        }

        // Romper cadena: si el original ya es un forward, referenciar su origen
        UUID idAReferenciar = original.getReenviaDe() != null
                ? original.getReenviaDe().getId()
                : original.getId();

        Mensaje forwardMsg = new Mensaje();
        forwardMsg.setConversacion(conversacionRepository.getReferenceById(conversacionId));
        forwardMsg.setRemitenteId(usuarioId);
        forwardMsg.setReenviaDe(mensajeRepository.getReferenceById(idAReferenciar));
        forwardMsg.setTipo(original.getTipo());
        forwardMsg.setContenidoCifrado(null);
        forwardMsg.setCreadoEn(LocalDateTime.now());
        forwardMsg.setEliminadoParaTodos(false);

        final Mensaje forwardGuardado = mensajeRepository.save(forwardMsg);

        LocalDateTime ahora = LocalDateTime.now();
        List<UUID> receptores = participanteRepository
                .findUsuarioIdsByConversacionExcluyendo(conversacionId, usuarioId);

        List<EstadoMensaje> estados = receptores.stream()
                .map(receptorId -> {
                    EstadoMensaje e = new EstadoMensaje();
                    e.setId(new EstadoMensajeId(forwardGuardado.getId(), receptorId));
                    e.setEntregadoEn(ahora);
                    return e;
                })
                .collect(Collectors.toList());

        if (!estados.isEmpty()) {
            estadoMensajeRepository.saveAll(estados);
        }

        MensajeResponse response = toResponse(forwardGuardado, usuarioId, conversacionId);
        eventPublisher.publishEvent(new MensajeEnviadoEvent(response));
        return response;
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private void verificarParticipante(UUID conversacionId, UUID usuarioId) {
        if (!participanteRepository.existsByIdConversacionIdAndIdUsuarioId(conversacionId, usuarioId)) {
            throw new AccesoDenegadoException("No eres participante de esta conversación");
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

    private MensajeResponse toResponse(Mensaje mensaje, UUID usuarioId, UUID conversacionId) {
        // Visibilidad del contenido
        String contenido;
        if (mensaje.isEliminadoParaTodos()) {
            contenido = null;
        } else if (mensaje.getEliminadoEn() != null) {
            contenido = mensaje.getRemitenteId().equals(usuarioId) ? null : mensaje.getContenidoCifrado();
        } else {
            contenido = mensaje.getContenidoCifrado();
        }

        boolean eliminado = mensaje.getEliminadoEn() != null;

        // Mensaje al que responde
        RepliedMessageResponse respuesta = null;
        if (mensaje.getRespuestaMensaje() != null) {
            Mensaje original = mensaje.getRespuestaMensaje();
            respuesta = new RepliedMessageResponse(
                    original.getId(),
                    original.getRemitenteId(),
                    original.isEliminadoParaTodos() ? null : original.getContenidoCifrado(),
                    original.getEliminadoEn() != null
            );
        }

        // Estados de entrega/lectura: solo para el remitente
        List<EstadoMensajeResponse> estados = null;
        if (mensaje.getRemitenteId().equals(usuarioId)) {
            estados = estadoMensajeRepository.findByIdMensajeId(mensaje.getId()).stream()
                    .map(e -> new EstadoMensajeResponse(
                            e.getId().getUsuarioId(),
                            e.getEntregadoEn(),
                            e.getLeidoEn()))
                    .collect(Collectors.toList());
        }

        // Archivo multimedia
        ArchivoMultimediaResponse archivoResp = null;
        if (mensaje.getArchivo() != null && !mensaje.isEliminadoParaTodos()) {
            ArchivoMultimedia am = mensaje.getArchivo();
            archivoResp = new ArchivoMultimediaResponse(
                    storageService.urlPublica(am.getObjectKey()),
                    am.getNombreOriginal(),
                    am.getContentType(),
                    am.getTamanioBytes(),
                    am.getDuracionSegundos(),
                    am.getAnchoPx(),
                    am.getAltoPx()
            );
        }

        // Ubicación
        UbicacionResponse ubicacionResp = null;
        if (mensaje.getUbicacion() != null) {
            UbicacionMensaje ub = mensaje.getUbicacion();
            ubicacionResp = new UbicacionResponse(ub.getLatitud(), ub.getLongitud(), ub.getNombreLugar());
        }

        // Reacciones — agrupadas por emoji (batch-loaded por @BatchSize)
        List<ResumenReaccionesResponse> resumenReacciones =
                construirResumenReacciones(mensaje.getReacciones(), usuarioId);

        // Forward — datos del mensaje original
        ForwardResponse forwardResp = null;
        if (mensaje.getReenviaDe() != null) {
            Mensaje orig = mensaje.getReenviaDe();
            forwardResp = new ForwardResponse(
                    orig.getId(),
                    orig.getRemitenteId(),
                    orig.isEliminadoParaTodos() ? null : orig.getContenidoCifrado(),
                    orig.getTipo(),
                    orig.getEliminadoEn() != null
            );
        }

        return new MensajeResponse(
                mensaje.getId(),
                conversacionId,
                mensaje.getRemitenteId(),
                contenido,
                mensaje.getTipo(),
                mensaje.getCreadoEn(),
                mensaje.getEditadoEn(),
                eliminado,
                mensaje.isEliminadoParaTodos(),
                respuesta,
                estados,
                archivoResp,
                ubicacionResp,
                resumenReacciones,
                forwardResp
        );
    }
}
