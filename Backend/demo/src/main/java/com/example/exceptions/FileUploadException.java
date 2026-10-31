package com.example.exceptions;

/**
 * Excepción para errores en la subida de archivos.
 */
public class FileUploadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
