package com.seguridad.Messenger.shared.controller;

import com.seguridad.Messenger.shared.security.UserPrincipal;
import com.seguridad.Messenger.shared.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/archivos")
@RequiredArgsConstructor
@Tag(name = "Archivos", description = "Acceso autenticado a archivos almacenados")
@SecurityRequirement(name = "BearerAuth")
public class ArchivoController {

    private final StorageService storageService;

    /**
     * Genera una presigned URL válida 24h y redirige al cliente hacia ella.
     * El cliente descarga el archivo directamente desde MinIO con la URL firmada.
     *
     * Rutas:
     *   GET /archivos/avatars/abc.webp
     *   GET /archivos/imagen/abc.jpg
     *   GET /archivos/documento/abc.pdf
     */
    @GetMapping("/**")
    @Operation(summary = "Acceder a un archivo almacenado",
            description = "Redirige (302) a una presigned URL válida por 24h. Requiere autenticación.")
    @ApiResponse(responseCode = "302", description = "Redirect a presigned URL de MinIO")
    @ApiResponse(responseCode = "400", description = "Object key vacío")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    public ResponseEntity<Void> servirArchivo(HttpServletRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        String objectKey = request.getRequestURI()
                .substring(request.getContextPath().length() + "/archivos/".length());

        if (objectKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String presignedUrl = storageService.generarPresignedUrl(objectKey);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(presignedUrl))
                .build();
    }
}
