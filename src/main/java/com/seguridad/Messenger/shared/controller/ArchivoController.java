package com.seguridad.Messenger.shared.controller;

import com.seguridad.Messenger.shared.dto.PresignedUrlResponse;
import com.seguridad.Messenger.shared.security.UserPrincipal;
import com.seguridad.Messenger.shared.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/archivos")
@RequiredArgsConstructor
@Tag(name = "Archivos", description = "Acceso autenticado a archivos privados almacenados")
@SecurityRequirement(name = "BearerAuth")
public class ArchivoController {

    private final StorageService storageService;

    /** Validez de la presigned URL — debe coincidir con el {@code expiry} usado en {@link StorageService}. */
    private static final long EXPIRY_SECONDS = 24L * 60 * 60;

    /**
     * Devuelve una presigned URL válida 24h en JSON. El cliente debe hacer una
     * segunda request GET a esa URL <strong>sin ningún header de autenticación</strong> —
     * MinIO rechaza la combinación de firma en query string + Bearer token con
     * {@code InvalidRequest: request has multiple authentication types}.
     *
     * Solo se usa para archivos privados de mensajes (bucket {@code messenger-archivos}).
     * Los avatares viven en un bucket público y se sirven directamente desde MinIO,
     * no por este controller.
     *
     * Rutas:
     *   GET /archivos/imagen/abc.jpg
     *   GET /archivos/documento/abc.pdf
     */
    @GetMapping("/**")
    @Operation(
            summary = "Obtener URL de acceso a un archivo",
            description = """
                    Devuelve una URL prefirmada válida por 24 horas para descargar el archivo
                    directamente desde el almacenamiento. El cliente debe hacer una segunda
                    request GET a esa URL **sin ningún header de autenticación** — la firma
                    completa va en los query params.
                    """
    )
    @ApiResponse(responseCode = "200", description = "URL generada correctamente")
    @ApiResponse(responseCode = "400", description = "Object key vacío")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    public ResponseEntity<PresignedUrlResponse> obtenerUrl(
            HttpServletRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        String objectKey = request.getRequestURI()
                .substring(request.getContextPath().length() + "/archivos/".length());

        if (objectKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String presignedUrl = storageService.generarPresignedUrl(objectKey);
        return ResponseEntity.ok(new PresignedUrlResponse(presignedUrl, EXPIRY_SECONDS));
    }
}
