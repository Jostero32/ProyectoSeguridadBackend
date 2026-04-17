package com.seguridad.Messenger.shared.exception;

public class TipoArchivoNoPermitidoException extends RuntimeException {
    public TipoArchivoNoPermitidoException(String mensaje) {
        super(mensaje);
    }
}
