package com.seguridad.Messenger.shared.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Slf4j
@Service
public class ThumbnailService {

    private static final int THUMB_WIDTH = 320;
    private static final float THUMB_QUALITY = 0.75f;

    /**
     * Genera un thumbnail JPEG de 320px de ancho y lo devuelve como base64 sin
     * prefijo {@code data:image/...}. Llamar solo para {@code TipoMensaje.IMAGEN}.
     * No propaga excepciones: si la generación falla, devuelve null y el upload
     * del mensaje continúa.
     */
    public String generarThumbnailBase64(byte[] bytes) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Thumbnails.of(new ByteArrayInputStream(bytes))
                    .width(THUMB_WIDTH)
                    .outputFormat("JPEG")
                    .outputQuality(THUMB_QUALITY)
                    .toOutputStream(out);

            return Base64.getEncoder().encodeToString(out.toByteArray());

        } catch (Exception e) {
            log.warn("Error generando thumbnail: {}", e.getMessage());
            return null;
        }
    }
}
