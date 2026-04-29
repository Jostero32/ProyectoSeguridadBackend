package com.seguridad.Messenger.mensajes.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload especifico del mensaje (polimorfico por tipo)")
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "tipo"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextoPayload.class, name = "TEXTO"),
        @JsonSubTypes.Type(value = MultimediaPayload.class, name = "IMAGEN"),
        @JsonSubTypes.Type(value = MultimediaPayload.class, name = "AUDIO"),
        @JsonSubTypes.Type(value = MultimediaPayload.class, name = "VIDEO"),
        @JsonSubTypes.Type(value = MultimediaPayload.class, name = "DOCUMENTO"),
        @JsonSubTypes.Type(value = UbicacionPayload.class, name = "UBICACION"),
        @JsonSubTypes.Type(value = MultimediaPayload.class, name = "STICKER"),
        @JsonSubTypes.Type(value = MultimediaPayload.class, name = "GIF")
})
public interface MensajePayload {}
