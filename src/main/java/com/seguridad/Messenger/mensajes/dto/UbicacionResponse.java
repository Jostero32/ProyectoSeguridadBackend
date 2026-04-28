package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Coordenadas geográficas del mensaje de ubicación")
public record UbicacionResponse(

        @Schema(description = "Latitud", example = "40.4168")
        BigDecimal latitud,

        @Schema(description = "Longitud", example = "-3.7038")
        BigDecimal longitud,

        @Schema(description = "Nombre del lugar (opcional)", example = "Madrid, España")
        String nombreLugar

) {}
