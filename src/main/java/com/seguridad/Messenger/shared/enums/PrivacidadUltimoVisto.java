package com.seguridad.Messenger.shared.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum PrivacidadUltimoVisto {

    TODOS("todos"),
    CONTACTOS("contactos"),
    NADIE("nadie");

    private final String valor;

    PrivacidadUltimoVisto(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static PrivacidadUltimoVisto fromValor(String valor) {
        for (PrivacidadUltimoVisto p : values()) {
            if (p.valor.equals(valor)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Valor inválido: " + valor);
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<PrivacidadUltimoVisto, String> {

        @Override
        public String convertToDatabaseColumn(PrivacidadUltimoVisto attribute) {
            return attribute == null ? null : attribute.getValor();
        }

        @Override
        public PrivacidadUltimoVisto convertToEntityAttribute(String dbData) {
            return dbData == null ? null : PrivacidadUltimoVisto.fromValor(dbData);
        }
    }
}
