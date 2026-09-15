package com.example.exceptions;

/**
 * Excepción para errores en la subida de archivos.
 */
public class FileUploadException extends RuntimeException {

    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
