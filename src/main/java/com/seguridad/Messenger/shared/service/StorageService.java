package com.seguridad.Messenger.shared.service;

import com.seguridad.Messenger.config.StorageProperties;
import com.seguridad.Messenger.shared.enums.TipoMensaje;
import com.seguridad.Messenger.shared.exception.ArchivoDemasiadoGrandeException;
import com.seguridad.Messenger.shared.exception.TipoArchivoNoPermitidoException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;
    private final StorageProperties props;

    private static final Tika TIKA = new Tika();
    private static final int PRESIGNED_URL_EXPIRY_DAYS = 1;

    public static final Set<String> MIMES_AVATAR = Set.of("image/jpeg", "image/png", "image/webp");
    public static final long MAX_BYTES_AVATAR = 5L * 1024 * 1024;

    private static final Map<TipoMensaje, Set<String>> TIPOS_PERMITIDOS = Map.of(
            TipoMensaje.IMAGEN,    Set.of("image/jpeg", "image/png", "image/webp", "image/gif"),
            TipoMensaje.AUDIO,     Set.of("audio/mpeg", "audio/ogg", "audio/opus", "audio/wav", "audio/aac"),
            TipoMensaje.VIDEO,     Set.of("video/mp4", "video/webm", "video/ogg"),
            TipoMensaje.DOCUMENTO, Set.of(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "text/plain"
            ),
            TipoMensaje.STICKER,   Set.of("image/webp"),
            TipoMensaje.GIF,       Set.of("image/gif")
    );

    private static final Map<TipoMensaje, Long> TAMANIOS_MAX = Map.of(
            TipoMensaje.IMAGEN,    10L  * 1024 * 1024,
            TipoMensaje.AUDIO,     20L  * 1024 * 1024,
            TipoMensaje.VIDEO,     100L * 1024 * 1024,
            TipoMensaje.DOCUMENTO, 50L  * 1024 * 1024,
            TipoMensaje.STICKER,   512L * 1024,
            TipoMensaje.GIF,       5L   * 1024 * 1024
    );

    public record StorageResult(String objectKey, String contentType, long tamanioBytes) {}

    // ─── SUBIDA ───────────────────────────────────────────────────────────────

    public StorageResult subir(MultipartFile archivo, TipoMensaje tipo) {
        try {
            byte[] bytes = archivo.getBytes();
            String contentType = TIKA.detect(bytes);

            validarContentType(contentType, tipo);
            validarTamanio(bytes.length, tipo);

            String extension = getExtension(archivo.getOriginalFilename());
            String objectKey = tipo.name().toLowerCase() + "/" + UUID.randomUUID() + extension;

            putObject(objectKey, bytes, contentType);
            return new StorageResult(objectKey, contentType, bytes.length);

        } catch (TipoArchivoNoPermitidoException | ArchivoDemasiadoGrandeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al subir archivo: " + e.getMessage(), e);
        }
    }

    /**
     * Sube un avatar. Devuelve el object_key (no la URL).
     */
    public String subirAvatar(MultipartFile avatar) {
        try {
            byte[] bytes = avatar.getBytes();
            String mime = TIKA.detect(bytes);

            if (!MIMES_AVATAR.contains(mime)) {
                throw new TipoArchivoNoPermitidoException("El avatar debe ser JPEG, PNG o WebP");
            }
            if (bytes.length > MAX_BYTES_AVATAR) {
                throw new ArchivoDemasiadoGrandeException("El avatar no puede superar 5 MB");
            }

            String objectKey = "avatars/" + UUID.randomUUID() + extensionDesdeMime(mime);
            putObject(objectKey, bytes, mime);
            return objectKey;

        } catch (TipoArchivoNoPermitidoException | ArchivoDemasiadoGrandeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al subir avatar: " + e.getMessage(), e);
        }
    }

    // ─── PRESIGNED URL ────────────────────────────────────────────────────────

    /**
     * Genera una presigned URL válida por 1 día para el object_key dado.
     */
    public String generarPresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(props.getBucket())
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(PRESIGNED_URL_EXPIRY_DAYS, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error generando presigned URL para: " + objectKey, e);
        }
    }

    // ─── ELIMINACIÓN ──────────────────────────────────────────────────────────

    public void eliminar(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Error eliminando objeto {}: {}", objectKey, e.getMessage());
        }
    }

    // ─── PRIVADOS ─────────────────────────────────────────────────────────────

    private void putObject(String objectKey, byte[] bytes, String mimeType) {
        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(objectKey)
                            .stream(is, bytes.length, -1)
                            .contentType(mimeType)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al subir objeto a MinIO: " + e.getMessage(), e);
        }
    }

    private void validarContentType(String mimeDetectado, TipoMensaje tipo) {
        Set<String> permitidos = TIPOS_PERMITIDOS.get(tipo);
        if (permitidos == null) {
            throw new TipoArchivoNoPermitidoException(
                    "El tipo de mensaje " + tipo + " no admite archivos adjuntos");
        }
        if (!permitidos.contains(mimeDetectado)) {
            throw new TipoArchivoNoPermitidoException(
                    "Tipo de archivo no permitido para " + tipo + ": " + mimeDetectado);
        }
    }

    private void validarTamanio(long size, TipoMensaje tipo) {
        Long maxBytes = TAMANIOS_MAX.get(tipo);
        if (maxBytes != null && size > maxBytes) {
            String limite = maxBytes >= 1024 * 1024
                    ? (maxBytes / (1024 * 1024)) + " MB"
                    : (maxBytes / 1024) + " KB";
            throw new ArchivoDemasiadoGrandeException(
                    "El archivo supera el tamaño máximo de " + limite + " para tipo " + tipo);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }

    private String extensionDesdeMime(String mime) {
        return switch (mime) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default           -> "";
        };
    }
}
