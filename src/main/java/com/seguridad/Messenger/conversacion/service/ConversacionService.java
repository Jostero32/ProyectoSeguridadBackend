package com.seguridad.Messenger.conversacion.service;

import com.seguridad.Messenger.conversacion.dto.*;
import com.seguridad.Messenger.conversacion.model.*;
import com.seguridad.Messenger.conversacion.repository.*;
import com.seguridad.Messenger.mensajes.model.Mensaje;
import com.seguridad.Messenger.mensajes.repository.EstadoMensajeRepository;
import com.seguridad.Messenger.mensajes.repository.MensajeRepository;
import com.seguridad.Messenger.shared.enums.RolParticipante;
import com.seguridad.Messenger.shared.enums.TipoMensaje;
import com.seguridad.Messenger.shared.enums.TipoConversacion;
import com.seguridad.Messenger.shared.exception.AccesoDenegadoException;
import com.seguridad.Messenger.shared.exception.RecursoNoEncontradoException;
import com.seguridad.Messenger.usuario.model.Usuario;
import com.seguridad.Messenger.shared.service.StorageService;
import com.seguridad.Messenger.usuario.repository.BloqueoRepository;
import com.seguridad.Messenger.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class ConversacionService {

    private final ConversacionRepository conversacionRepository;
    private final ParticipanteRepository participanteRepository;
    private final ConfiguracionChatRepository configuracionChatRepository;
    private final UsuarioRepository usuarioRepository;
    private final MensajeRepository mensajeRepository;
    private final EstadoMensajeRepository estadoMensajeRepository;
    private final BloqueoRepository bloqueoRepository;
    private final StorageService storageService;

    // ─── Chat individual ───────────────────────────────────────────────────────

    public ChatIndividualResult crearChatIndividual(UUID usuarioId, CrearChatIndividualRequest req) {
        if (usuarioId.equals(req.destinatarioId())) {
            throw new IllegalArgumentException("No puedes iniciar un chat contigo mismo");
        }

        if (bloqueoRepository.existsByIdUsuarioIdAndIdBloqueadoId(usuarioId, req.destinatarioId()) ||
                bloqueoRepository.existsByIdUsuarioIdAndIdBloqueadoId(req.destinatarioId(), usuarioId)) {
            throw new AccesoDenegadoException("No puedes chatear con este usuario");
        }

        String hash = generarCanalHash(usuarioId, req.destinatarioId());

        Optional<Conversacion> existente = conversacionRepository.findByCanalHash(hash);
        if (existente.isPresent()) {
            Conversacion conv = conversacionRepository
                    .findByIdConDetalles(existente.get().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Conversación no encontrada"));
            return new ChatIndividualResult(toConversacionResponse(conv, usuarioId), false);
        }

        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado"));
        Usuario destinatario = usuarioRepository.findByIdAndActivoTrue(req.destinatarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario destinatario no encontrado"));

        Conversacion conv = new Conversacion();
        conv.setTipo(TipoConversacion.INDIVIDUAL);
        conv.setCanalHash(hash);
        conv.setCreadaEn(LocalDateTime.now());
        conv = conversacionRepository.save(conv);

        guardarParticipante(conv, usuario, RolParticipante.MIEMBRO);
        guardarParticipante(conv, destinatario, RolParticipante.MIEMBRO);

        // Para chat individual: el "título" es el nombre del destinatario
        String titulo = destinatario.getPersona().getNombres() + " " + destinatario.getPersona().getApellidos();
        String urlAvatar = destinatario.getPerfil() != null ? destinatario.getPerfil().getUrlAvatar() : null;
        ConversacionResponse response = new ConversacionResponse(
                conv.getId(), TipoConversacion.INDIVIDUAL, titulo, urlAvatar, conv.getCreadaEn(), false, 2);
        return new ChatIndividualResult(response, true);
    }

    // ─── Grupos ───────────────────────────────────────────────────────────────

    public ConversacionResponse crearGrupo(UUID usuarioId, String titulo, MultipartFile avatar,
                                            List<UUID> miembrosIds) {
        Usuario creador = usuarioRepository.findByIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        List<UUID> miembrosFiltrados = miembrosIds.stream()
                .filter(id -> !id.equals(usuarioId))
                .distinct()
                .collect(Collectors.toList());

        List<Usuario> miembros = miembrosFiltrados.stream()
                .map(id -> usuarioRepository.findByIdAndActivoTrue(id)
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "Usuario con id " + id + " no encontrado")))
                .collect(Collectors.toList());

        String urlAvatarGrupo = null;
        if (avatar != null && !avatar.isEmpty()) {
            urlAvatarGrupo = storageService.subirAvatar(avatar);
        }

        Conversacion conv = new Conversacion();
        conv.setTipo(TipoConversacion.GRUPO);
        conv.setTituloGrupo(titulo);
        conv.setUrlAvatarGrupo(urlAvatarGrupo);
        conv.setCreadaEn(LocalDateTime.now());
        conv = conversacionRepository.save(conv);

        guardarParticipante(conv, creador, RolParticipante.ADMIN);
        for (Usuario miembro : miembros) {
            guardarParticipante(conv, miembro, RolParticipante.MIEMBRO);
        }

        int totalMiembros = 1 + miembros.size();
        return new ConversacionResponse(
                conv.getId(), TipoConversacion.GRUPO, titulo, urlAvatarGrupo,
                conv.getCreadaEn(), true, totalMiembros);
    }

    // ─── Listar / Detalle ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChatResumenResponse> listarChats(UUID usuarioId, boolean incluirArchivadas) {
        List<Conversacion> conversaciones = conversacionRepository.findConversacionesConDetalles(usuarioId);

        if (!incluirArchivadas) {
            Set<UUID> idsArchivadas = configuracionChatRepository
                    .findByIdUsuarioIdAndArchivadoTrue(usuarioId)
                    .stream()
                    .map(cc -> cc.getId().getConversacionId())
                    .collect(Collectors.toSet());

            conversaciones = conversaciones.stream()
                    .filter(c -> !idsArchivadas.contains(c.getId()))
                    .collect(Collectors.toList());
        }

        return conversaciones.stream()
                .map(c -> toChatResumenResponse(c, usuarioId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversacionResponse obtenerConversacion(UUID conversacionId, UUID usuarioId) {
        verificarParticipante(conversacionId, usuarioId);
        Conversacion conv = conversacionRepository.findByIdConDetalles(conversacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Conversación no encontrada"));
        return toConversacionResponse(conv, usuarioId);
    }

    // ─── Actualizar / Eliminar grupo ──────────────────────────────────────────

    public ConversacionResponse actualizarGrupo(UUID conversacionId, UUID usuarioId, String titulo,
                                                 MultipartFile avatar) {
        Conversacion conv = cargarConversacion(conversacionId);
        if (conv.getTipo() == TipoConversacion.INDIVIDUAL) {
            throw new IllegalArgumentException("No se puede modificar un chat individual");
        }
        verificarAdmin(conversacionId, usuarioId);

        if (titulo != null && !titulo.isBlank()) {
            conv.setTituloGrupo(titulo);
        }
        if (avatar != null && !avatar.isEmpty()) {
            storageService.eliminarAvatar(conv.getUrlAvatarGrupo());
            conv.setUrlAvatarGrupo(storageService.subirAvatar(avatar));
        }
        conv = conversacionRepository.save(conv);

        int totalMiembros = (int) participanteRepository.countByIdConversacionId(conversacionId);
        return new ConversacionResponse(
                conv.getId(), TipoConversacion.GRUPO, conv.getTituloGrupo(), conv.getUrlAvatarGrupo(),
                conv.getCreadaEn(), true, totalMiembros);
    }

    public void eliminarGrupo(UUID conversacionId, UUID usuarioId) {
        Conversacion conv = cargarConversacion(conversacionId);
        if (conv.getTipo() == TipoConversacion.INDIVIDUAL) {
            throw new IllegalArgumentException("No se puede eliminar un chat individual con este endpoint");
        }
        verificarAdmin(conversacionId, usuarioId);
        conversacionRepository.delete(conv);
    }

    // ─── Miembros ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ParticipanteResponse> listarMiembros(UUID conversacionId, UUID usuarioId) {
        verificarParticipante(conversacionId, usuarioId);
        return participanteRepository.findByIdConversacionIdConUsuario(conversacionId).stream()
                .map(this::toParticipanteResponse)
                .collect(Collectors.toList());
    }

    public List<ParticipanteResponse> agregarMiembros(UUID conversacionId, UUID adminId, AgregarMiembrosRequest req) {
        Conversacion conv = cargarConversacion(conversacionId);
        verificarAdmin(conversacionId, adminId);

        for (UUID uid : req.usuarioIds()) {
            if (participanteRepository.existsByIdConversacionIdAndIdUsuarioId(conversacionId, uid)) {
                continue; // idempotente: ignorar si ya es miembro
            }
            Usuario usuario = usuarioRepository.findByIdAndActivoTrue(uid)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Usuario con id " + uid + " no encontrado"));
            guardarParticipante(conv, usuario, RolParticipante.MIEMBRO);
        }

        return participanteRepository.findByIdConversacionIdConUsuario(conversacionId).stream()
                .map(this::toParticipanteResponse)
                .collect(Collectors.toList());
    }

    public void expulsarMiembro(UUID conversacionId, UUID adminId, UUID objetivoId) {
        cargarConversacion(conversacionId);
        verificarAdmin(conversacionId, adminId);

        Participante objetivo = participanteRepository
                .findByIdConversacionIdAndIdUsuarioId(conversacionId, objetivoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El usuario no es miembro de esta conversación"));

        if (objetivo.getRol() == RolParticipante.ADMIN) {
            throw new AccesoDenegadoException("No puedes expulsar a otro administrador");
        }

        participanteRepository.delete(objetivo);
    }

    public void abandonarGrupo(UUID conversacionId, UUID usuarioId) {
        Conversacion conv = cargarConversacion(conversacionId);

        if (conv.getTipo() == TipoConversacion.INDIVIDUAL) {
            throw new IllegalArgumentException("No puedes abandonar un chat individual");
        }

        Participante participante = verificarParticipante(conversacionId, usuarioId);
        long totalMiembros = participanteRepository.countByIdConversacionId(conversacionId);

        if (totalMiembros == 1) {
            conversacionRepository.delete(conv);
            return;
        }

        if (participante.getRol() == RolParticipante.ADMIN) {
            long totalAdmins = participanteRepository.countByIdConversacionIdAndRol(
                    conversacionId, RolParticipante.ADMIN);
            if (totalAdmins == 1) {
                List<Participante> candidatos = participanteRepository.findMasAntiguoExcluyendo(
                        conversacionId, usuarioId, PageRequest.of(0, 1));
                if (!candidatos.isEmpty()) {
                    Participante nuevo = candidatos.get(0);
                    nuevo.setRol(RolParticipante.ADMIN);
                    participanteRepository.save(nuevo);
                }
            }
        }

        participanteRepository.delete(participante);
    }

    // ─── Configuración ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ConfiguracionChatResponse obtenerConfiguracion(UUID conversacionId, UUID usuarioId) {
        verificarParticipante(conversacionId, usuarioId);
        return configuracionChatRepository
                .findByIdConversacionIdAndIdUsuarioId(conversacionId, usuarioId)
                .map(this::toConfiguracionResponse)
                .orElse(new ConfiguracionChatResponse(false, null, false, false));
    }

    public ConfiguracionChatResponse actualizarConfiguracion(UUID conversacionId, UUID usuarioId,
                                                              ActualizarConfiguracionRequest req) {
        verificarParticipante(conversacionId, usuarioId);
        Conversacion conv = cargarConversacion(conversacionId);
        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        ConfiguracionChat config = configuracionChatRepository
                .findByIdConversacionIdAndIdUsuarioId(conversacionId, usuarioId)
                .orElseGet(() -> {
                    ConfiguracionChat c = new ConfiguracionChat();
                    c.setId(new ConfiguracionChatId(conversacionId, usuarioId));
                    c.setConversacion(conv);
                    c.setUsuario(usuario);
                    return c;
                });

        config.setSilenciadoHasta(req.silenciadoHasta());
        if (req.archivado() != null) {
            config.setArchivado(req.archivado());
        }
        if (req.fijado() != null) {
            config.setFijado(req.fijado());
        }

        config = configuracionChatRepository.save(config);
        return toConfiguracionResponse(config);
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private Participante verificarParticipante(UUID conversacionId, UUID usuarioId) {
        return participanteRepository
                .findByIdConversacionIdAndIdUsuarioId(conversacionId, usuarioId)
                .orElseThrow(() -> new AccesoDenegadoException(
                        "No eres participante de esta conversación"));
    }

    private void verificarAdmin(UUID conversacionId, UUID usuarioId) {
        Participante p = verificarParticipante(conversacionId, usuarioId);
        if (p.getRol() != RolParticipante.ADMIN) {
            throw new AccesoDenegadoException(
                    "No tienes permisos de administrador en esta conversación");
        }
    }

    private Conversacion cargarConversacion(UUID conversacionId) {
        return conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Conversación no encontrada"));
    }

    private void guardarParticipante(Conversacion conv, Usuario usuario, RolParticipante rol) {
        Participante p = new Participante();
        p.setId(new ParticipanteId(conv.getId(), usuario.getId()));
        p.setConversacion(conv);
        p.setUsuario(usuario);
        p.setRol(rol);
        p.setFechaUnion(LocalDateTime.now());
        participanteRepository.save(p);
    }

    private String generarCanalHash(UUID uid1, UUID uid2) {
        String sorted = Stream.of(uid1.toString(), uid2.toString())
                .sorted()
                .collect(Collectors.joining(":"));
        return DigestUtils.sha256Hex(sorted);
    }

    private ChatResumenResponse toChatResumenResponse(Conversacion c, UUID usuarioId) {
        String titulo;
        String urlAvatar;

        if (c.getTipo() == TipoConversacion.INDIVIDUAL) {
            Participante otro = c.getParticipantes().stream()
                    .filter(p -> !p.getId().getUsuarioId().equals(usuarioId))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNoEncontradoException("Participante no encontrado"));
            Usuario otroUsuario = otro.getUsuario();
            titulo = otroUsuario.getPersona().getNombres() + " " + otroUsuario.getPersona().getApellidos();
            urlAvatar = otroUsuario.getPerfil() != null ? otroUsuario.getPerfil().getUrlAvatar() : null;
        } else {
            titulo = c.getTituloGrupo();
            urlAvatar = c.getUrlAvatarGrupo();
        }

        boolean esAdmin = c.getParticipantes().stream()
                .anyMatch(p -> p.getId().getUsuarioId().equals(usuarioId)
                        && p.getRol() == RolParticipante.ADMIN);

        UltimoMensajeResponse ultimoMensajeDto = mensajeRepository
                .findTopByConversacionIdOrderByCreadoEnDesc(c.getId())
                .map(m -> new UltimoMensajeResponse(
                        m.getId(),
                        m.getRemitenteId(),
                        usuarioRepository.findUsernameById(m.getRemitenteId()),
                        m.getTipo(),
                        generarPreview(m),
                        m.getCreadoEn(),
                        m.isEliminadoParaTodos()
                ))
                .orElse(null);

        int noLeidos = (int) estadoMensajeRepository.countNoLeidos(c.getId(), usuarioId);

        return new ChatResumenResponse(
                c.getId(), c.getTipo(), titulo, urlAvatar, c.getCreadaEn(), esAdmin,
                ultimoMensajeDto, noLeidos);
    }

    private String generarPreview(Mensaje m) {
        if (m.isEliminadoParaTodos()) return "Mensaje eliminado";
        return switch (m.getTipo()) {
            case TEXTO     -> truncar(m.getContenido(), 60);
            case IMAGEN    -> "Foto";
            case VIDEO     -> "Video";
            case AUDIO     -> "Audio";
            case DOCUMENTO -> "Documento";
            case STICKER   -> "Sticker";
            case GIF       -> "GIF";
            case UBICACION -> "Ubicación";
        };
    }

    private String truncar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max) + "...";
    }

    private ConversacionResponse toConversacionResponse(Conversacion c, UUID usuarioId) {
        String titulo;
        String urlAvatar;

        if (c.getTipo() == TipoConversacion.INDIVIDUAL) {
            Participante otro = c.getParticipantes().stream()
                    .filter(p -> !p.getId().getUsuarioId().equals(usuarioId))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNoEncontradoException("Participante no encontrado"));
            Usuario otroUsuario = otro.getUsuario();
            titulo = otroUsuario.getPersona().getNombres() + " " + otroUsuario.getPersona().getApellidos();
            urlAvatar = otroUsuario.getPerfil() != null ? otroUsuario.getPerfil().getUrlAvatar() : null;
        } else {
            titulo = c.getTituloGrupo();
            urlAvatar = c.getUrlAvatarGrupo();
        }

        boolean esAdmin = c.getParticipantes().stream()
                .anyMatch(p -> p.getId().getUsuarioId().equals(usuarioId)
                        && p.getRol() == RolParticipante.ADMIN);
        int totalMiembros = c.getParticipantes().size();

        return new ConversacionResponse(
                c.getId(), c.getTipo(), titulo, urlAvatar, c.getCreadaEn(), esAdmin, totalMiembros);
    }

    private ParticipanteResponse toParticipanteResponse(Participante p) {
        Usuario u = p.getUsuario();
        String nombreCompleto = u.getPersona().getNombres() + " " + u.getPersona().getApellidos();
        String urlAvatar = u.getPerfil() != null ? u.getPerfil().getUrlAvatar() : null;
        return new ParticipanteResponse(
                u.getId(), u.getUsername(), nombreCompleto, urlAvatar, p.getRol(), p.getFechaUnion());
    }

    private ConfiguracionChatResponse toConfiguracionResponse(ConfiguracionChat config) {
        boolean silenciado = config.getSilenciadoHasta() != null
                && config.getSilenciadoHasta().isAfter(LocalDateTime.now());
        return new ConfiguracionChatResponse(
                silenciado, config.getSilenciadoHasta(), config.isArchivado(), config.isFijado());
    }
}
