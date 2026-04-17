package com.seguridad.Messenger.mensajes.service;

import com.seguridad.Messenger.conversacion.repository.ConversacionRepository;
import com.seguridad.Messenger.conversacion.repository.ParticipanteRepository;
import com.seguridad.Messenger.mensajes.dto.*;
import com.seguridad.Messenger.mensajes.model.ArchivoMultimedia;
import com.seguridad.Messenger.mensajes.model.EstadoMensaje;
import com.seguridad.Messenger.mensajes.model.EstadoMensajeId;
import com.seguridad.Messenger.mensajes.model.Mensaje;
import com.seguridad.Messenger.mensajes.model.UbicacionMensaje;
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

        // Validar coherencia tipo / payload
        if (tipo == TipoMensaje.TEXTO && (contenido == null || contenido.isBlank())) {
            throw new IllegalArgumentException("El contenido es obligatorio para mensajes de tipo TEXTO");
        }
        if (tipo == TipoMensaje.UBICACION && (latitud == null || longitud == null)) {
            throw new IllegalArgumentException("latitud y longitud son obligatorios para mensajes de tipo UBICACION");
        }
        if (tipo != TipoMensaje.TEXTO && tipo != TipoMensaje.UBICACION && (archivo == null || archivo.isEmpty())) {
            throw new IllegalArgumentException("El archivo es obligatorio para mensajes de tipo " + tipo);
        }

        // Mensaje al que responde
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

        // Archivo multimedia
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

        // Ubicación
        if (tipo == TipoMensaje.UBICACION) {
            UbicacionMensaje ub = new UbicacionMensaje();
            ub.setMensaje(mensajeGuardado);
            ub.setLatitud(latitud);
            ub.setLongitud(longitud);
            ub.setNombreLugar(nombreLugar);
            mensajeGuardado.setUbicacion(ub);
        }

        // Guardar archivo/ubicación vía cascada re-saving el mensaje
        final Mensaje mensajeFinal = mensajeRepository.save(mensajeGuardado);

        // Estado_Mensaje para cada participante receptor
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

        return toResponse(mensajeFinal, usuarioId, conversacionId);
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
            // Eliminar archivo de MinIO si existe
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
            throw new RecursoNoEncontradoException("El mensaje no pertenece a esta conversación");
        }
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
                ubicacionResp
        );
    }
}
