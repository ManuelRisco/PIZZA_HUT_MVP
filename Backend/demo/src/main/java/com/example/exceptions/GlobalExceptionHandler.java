package com.example.exceptions;

import com.example.dtos.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(BadRequestException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStockException(InsufficientStockException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    // Reglas de negocio y estado ilegal
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(RuntimeException ex, WebRequest request) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Solicitud inválida.";
        return new ResponseEntity<>(ApiResponse.error(msg), HttpStatus.BAD_REQUEST);
    }

    // Uso inválido de argumentos en consultas JPA (ej. ID nulo enviado por el cliente)
    @ExceptionHandler(org.springframework.dao.InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidDataAccessApiUsageException(org.springframework.dao.InvalidDataAccessApiUsageException ex) {
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        return new ResponseEntity<>(ApiResponse.error("Parámetros de consulta inválidos o incompletos: " + detail), HttpStatus.BAD_REQUEST);
    }

    // Errores de validación de DTOs (@Valid)
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse("Error de validación");
        return new ResponseEntity<>(ApiResponse.error(errorMessage), HttpStatus.BAD_REQUEST);
    }

    // Violaciones de base de datos (Unique constraints, FK, etc.)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        String specificMessage = ex.getMostSpecificCause().getMessage();
        String mensaje = "Error de integridad de datos. Posible registro duplicado o en uso.";

        if (specificMessage != null) {
            if (specificMessage.contains("foreign key constraint") || specificMessage.contains("a foreign key constraint fails")) {
                mensaje = "No se puede eliminar el registro porque tiene otros elementos asociados en el sistema (por ejemplo, pedidos, detalles, etc.). Se recomienda inactivarlo en su lugar.";
            } else if (specificMessage.contains("Duplicate entry")) {
                mensaje = "No se puede crear o actualizar el registro porque ya existe otro igual (Dato duplicado).";
            }
        }

        return new ResponseEntity<>(ApiResponse.error(mensaje), HttpStatus.CONFLICT);
    }

    // Seguridad: Acceso denegado (Rol incorrecto)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        return new ResponseEntity<>(ApiResponse.error("Acceso denegado: " + ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    // Seguridad: Fallo de autenticación (Token inválido/expirado)
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        return new ResponseEntity<>(ApiResponse.error("Autenticación fallida: " + ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    // Error en el formato de la petición (JSON mal formado, etc.)
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return new ResponseEntity<>(ApiResponse.error("El formato de los datos enviados es incorrecto o está incompleto."), HttpStatus.BAD_REQUEST);
    }

    // Error cuando se envía un tipo de dato incorrecto en la URL (ej. texto en lugar de número)
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return new ResponseEntity<>(ApiResponse.error("El parámetro '" + ex.getName() + "' tiene un valor inválido."), HttpStatus.BAD_REQUEST);
    }

    // Error cuando se usa un método HTTP no soportado (ej. GET en vez de POST)
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        return new ResponseEntity<>(ApiResponse.error("Método HTTP no soportado para esta ruta."), HttpStatus.METHOD_NOT_ALLOWED);
    }

    // Falta un parámetro obligatorio en la URL o Request
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        return new ResponseEntity<>(ApiResponse.error("Falta el parámetro requerido: '" + ex.getParameterName() + "'."), HttpStatus.BAD_REQUEST);
    }

    // Tipo de contenido no soportado (ej. enviar XML en vez de JSON)
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(org.springframework.web.HttpMediaTypeNotSupportedException ex) {
        return new ResponseEntity<>(ApiResponse.error("Tipo de contenido no soportado. Se esperaba formato JSON."), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // Ruta no encontrada (404)
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(org.springframework.web.servlet.NoHandlerFoundException ex) {
        return new ResponseEntity<>(ApiResponse.error("La ruta solicitada no existe."), HttpStatus.NOT_FOUND);
    }

    // Tamaño máximo de archivo excedido (Para subida de imágenes/archivos)
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return new ResponseEntity<>(ApiResponse.error("El archivo enviado es demasiado grande. El tamaño máximo permitido es 5MB."), HttpStatus.CONTENT_TOO_LARGE);
    }

    // Error en la subida de archivos (validación de tipo, tamaño, etc.)
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileUploadException(FileUploadException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    // Error de I/O al procesar archivos (lectura/escritura)
    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<ApiResponse<Void>> handleIOException(java.io.IOException ex) {
        logger.error("Error de I/O procesando archivo", ex);
        return new ResponseEntity<>(ApiResponse.error("Error al procesar el archivo: " + ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Validaciones a nivel de método (ej. @Min, @Max, @NotNull en parámetros sueltos)
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(jakarta.validation.ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse("Error de validación");
        return new ResponseEntity<>(ApiResponse.error("Datos inválidos: " + errorMessage), HttpStatus.BAD_REQUEST);
    }

    // Errores genéricos de Base de Datos que no sean de Integridad
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(org.springframework.dao.DataAccessException ex) {
        logger.error("Error de acceso a base de datos", ex);
        return new ResponseEntity<>(ApiResponse.error("Ocurrió un problema de comunicación con la base de datos."), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Error de NullPointerException (para evitar que se filtre información sensible)
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointerException(NullPointerException ex) {
        logger.error("NullPointerException inesperada", ex);
        return new ResponseEntity<>(ApiResponse.error("Error interno del servidor. Un dato requerido llegó vacío inesperadamente."), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex, WebRequest request) {
        logger.error("Excepción no controlada en {}", request.getDescription(false), ex);
        return new ResponseEntity<>(ApiResponse.error("Ocurrió un error inesperado en el servidor. Por favor, intente más tarde."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
