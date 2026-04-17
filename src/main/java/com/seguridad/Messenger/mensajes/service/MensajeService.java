package com.seguridad.Messenger.mensajes.service;

import com.seguridad.Messenger.conversacion.repository.ConversacionRepository;
import com.seguridad.Messenger.conversacion.repository.ParticipanteRepository;
import com.seguridad.Messenger.mensajes.dto.*;
import com.seguridad.Messenger.mensajes.model.EstadoMensaje;
import com.seguridad.Messenger.mensajes.model.EstadoMensajeId;
import com.seguridad.Messenger.mensajes.model.Mensaje;
import com.seguridad.Messenger.mensajes.repository.EstadoMensajeRepository;
import com.seguridad.Messenger.mensajes.repository.MensajeRepository;
import com.seguridad.Messenger.shared.enums.TipoMensaje;
import com.seguridad.Messenger.shared.exception.AccesoDenegadoException;
import com.seguridad.Messenger.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ─── Enviar ───────────────────────────────────────────────────────────────

    public MensajeResponse enviarMensaje(UUID conversacionId, UUID usuarioId, EnviarMensajeRequest req) {
        verificarParticipante(conversacionId, usuarioId);

        Mensaje respuestaMensaje = null;
        if (req.respuestaMensajeId() != null) {
            respuestaMensaje = mensajeRepository.findById(req.respuestaMensajeId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje de respuesta no encontrado"));
            if (!respuestaMensaje.getConversacion().getId().equals(conversacionId)) {
                throw new RecursoNoEncontradoException(
                        "El mensaje de respuesta no pertenece a esta conversación");
            }
            if (respuestaMensaje.isEliminadoParaTodos()) {
                throw new IllegalStateException(
                        "No se puede responder a un mensaje eliminado para todos");
            }
        }

        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacionRepository.getReferenceById(conversacionId));
        mensaje.setRemitenteId(usuarioId);
        mensaje.setRespuestaMensaje(respuestaMensaje);
        mensaje.setTipo(TipoMensaje.TEXTO);
        mensaje.setContenidoCifrado(req.contenido());
        mensaje.setCreadoEn(LocalDateTime.now());
        mensaje.setEliminadoParaTodos(false);
        final Mensaje mensajeGuardado = mensajeRepository.save(mensaje);

        // Crear Estado_Mensaje para cada participante excepto el remitente
        LocalDateTime ahora = LocalDateTime.now();
        List<UUID> receptores = participanteRepository
                .findUsuarioIdsByConversacionExcluyendo(conversacionId, usuarioId);

        List<EstadoMensaje> estados = receptores.stream()
                .map(receptorId -> {
                    EstadoMensaje e = new EstadoMensaje();
                    e.setId(new EstadoMensajeId(mensajeGuardado.getId(), receptorId));
                    e.setEntregadoEn(ahora);
                    return e;
                })
                .collect(Collectors.toList());

        if (!estados.isEmpty()) {
            estadoMensajeRepository.saveAll(estados);
        }

        return toResponse(mensajeGuardado, usuarioId, conversacionId);
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
                throw new AccesoDenegadoException(
                        "Solo el remitente puede eliminar el mensaje para todos");
            }
            mensaje.setEliminadoEn(ahora);
            mensaje.setEliminadoParaTodos(true);
        } else {
            // para_mi: cualquier participante puede eliminar para sí mismo
            mensaje.setEliminadoEn(ahora);
            mensaje.setEliminadoParaTodos(false);
        }

        mensajeRepository.save(mensaje);
    }

    // ─── Marcar como leído (batch) ────────────────────────────────────────────

    public void marcarLeido(UUID conversacionId, UUID usuarioId, MarcarLeidoRequest req) {
        verificarParticipante(conversacionId, usuarioId);

        List<UUID> mensajeIds = req.mensajeIds();

        // Filtrar mensajes válidos: pertenecen a la conversación y no fueron enviados por el usuario
        List<Mensaje> mensajesValidos = mensajeRepository.findValidosParaLeer(
                conversacionId, usuarioId, mensajeIds);

        if (mensajesValidos.isEmpty()) {
            return;
        }

        List<UUID> validIds = mensajesValidos.stream()
                .map(Mensaje::getId)
                .collect(Collectors.toList());

        LocalDateTime ahora = LocalDateTime.now();

        // Bulk update de filas existentes
        estadoMensajeRepository.marcarLeidoBulk(validIds, usuarioId, ahora);

        // Crear filas inexistentes
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
    }

    // ─── Historial paginado ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<MensajeResponse> historial(UUID conversacionId, UUID usuarioId, Pageable pageable) {
        verificarParticipante(conversacionId, usuarioId);
        return mensajeRepository.findByConversacion(conversacionId, pageable)
                .map(m -> toResponse(m, usuarioId, conversacionId));
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
            throw new RecursoNoEncontradoException(
                    "El mensaje no pertenece a esta conversación");
        }
    }

    private MensajeResponse toResponse(Mensaje mensaje, UUID usuarioId, UUID conversacionId) {
        // Visibilidad del contenido según estado de eliminación
        String contenido;
        if (mensaje.isEliminadoParaTodos()) {
            contenido = null;
        } else if (mensaje.getEliminadoEn() != null) {
            // Eliminado "para mí": solo el remitente (quien realizó la acción) lo ve como null
            contenido = mensaje.getRemitenteId().equals(usuarioId) ? null : mensaje.getContenidoCifrado();
        } else {
            contenido = mensaje.getContenidoCifrado();
        }

        boolean eliminado = mensaje.getEliminadoEn() != null;

        // Mensaje al que responde (si aplica)
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
                estados
        );
    }
}
