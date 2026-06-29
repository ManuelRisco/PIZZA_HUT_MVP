package com.example.exceptions;

import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(BadRequestException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    // Reglas de negocio y estado ilegal
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(RuntimeException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    // Errores de validaci\u00f3n de DTOs (@Valid)
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse("Validation error");
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

    // Seguridad: Fallo de autenticaci\u00f3n (Token inv\u00e1lido/expirado)
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        return new ResponseEntity<>(ApiResponse.error("Autenticaci\u00f3n fallida: " + ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    // Error en el formato de la petici\u00f3n (JSON mal formado, etc.)
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return new ResponseEntity<>(ApiResponse.error("El formato de los datos enviados es incorrecto o est\u00e1 incompleto."), HttpStatus.BAD_REQUEST);
    }

    // Error cuando se env\u00eda un tipo de dato incorrecto en la URL (ej. texto en lugar de n\u00famero)
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return new ResponseEntity<>(ApiResponse.error("El par\u00e1metro '" + ex.getName() + "' tiene un valor inv\u00e1lido."), HttpStatus.BAD_REQUEST);
    }

    // Error cuando se usa un m\u00e9todo HTTP no soportado (ej. GET en vez de POST)
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        return new ResponseEntity<>(ApiResponse.error("M\u00e9todo HTTP no soportado para esta ruta."), HttpStatus.METHOD_NOT_ALLOWED);
    }
    // Falta un par\u00e1metro obligatorio en la URL o Request
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        return new ResponseEntity<>(ApiResponse.error("Falta el par\u00e1metro requerido: '" + ex.getParameterName() + "'."), HttpStatus.BAD_REQUEST);
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

    // Tama\u00f1o m\u00e1ximo de archivo excedido (Para subida de im\u00e1genes/archivos)
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return new ResponseEntity<>(ApiResponse.error("El archivo enviado es demasiado grande. Por favor suba un archivo m\u00e1s peque\u00f1o."), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    // Validaciones a nivel de m\u00e9todo (ej. @Min, @Max, @NotNull en par\u00e1metros sueltos)
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(jakarta.validation.ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse("Error de validaci\u00f3n");
        return new ResponseEntity<>(ApiResponse.error("Datos inv\u00e1lidos: " + errorMessage), HttpStatus.BAD_REQUEST);
    }

    // Errores gen\u00e9ricos de Base de Datos que no sean de Integridad
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(org.springframework.dao.DataAccessException ex) {
        ex.printStackTrace(); // Log del error real
        return new ResponseEntity<>(ApiResponse.error("Ocurri\u00f3 un problema de comunicaci\u00f3n con la base de datos."), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Error de NullPointerException (para evitar que se filtre informaci\u00f3n sensible)
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointerException(NullPointerException ex) {
        ex.printStackTrace(); // Log del error real
        return new ResponseEntity<>(ApiResponse.error("Error interno del servidor. Un dato requerido lleg\u00f3 vac\u00edo inesperadamente."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex, WebRequest request) {
        ex.printStackTrace(); // \u00fatil para depurar el error real en la consola del backend
        return new ResponseEntity<>(ApiResponse.error("Ocurri\u00f3 un error inesperado en el servidor. Por favor, intente m\u00e1s tarde."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
