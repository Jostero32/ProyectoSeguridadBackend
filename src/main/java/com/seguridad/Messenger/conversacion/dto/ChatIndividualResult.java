package com.seguridad.Messenger.conversacion.dto;

/**
 * DTO interno usado entre el servicio y el controlador para indicar si el chat
 * individual fue creado (201) o ya existía (200).
 */
public record ChatIndividualResult(
        ConversacionResponse conversacion,
        boolean creada
) {}
