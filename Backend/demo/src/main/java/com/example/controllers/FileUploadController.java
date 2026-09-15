package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.exceptions.FileUploadException;
import com.example.services.FileUploadService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
@CrossOrigin(origins = "http://localhost:4200")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * Sube una imagen para pizzas (se redimensiona automáticamente)
     */
    @PostMapping("/pizzas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadPizzaImage(
            @RequestParam("file") MultipartFile file) {
        return uploadImage(file, "pizzas");
    }

    /**
     * Sube una imagen para extras (se redimensiona automáticamente)
     */
    @PostMapping("/extras")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadExtraImage(
            @RequestParam("file") MultipartFile file) {
        return uploadImage(file, "extras");
    }

    /**
     * Método genérico para subir imágenes.
     * Las excepciones son manejadas por el GlobalExceptionHandler.
     */
    private ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            MultipartFile file, String subdirectory) {
        
        // FileUploadException es lanzada por el servicio y manejada por GlobalExceptionHandler
        String imageUrl = fileUploadService.saveFile(file, subdirectory);
        String thumbnailUrl = fileUploadService.getThumbnailUrl(imageUrl);
        
        Map<String, String> response = Map.of(
            "imageUrl", imageUrl,
            "thumbnailUrl", thumbnailUrl
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Imagen subida y redimensionada exitosamente"));
    }

    /**
     * Elimina una imagen y su thumbnail
     */
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@RequestParam("url") String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new FileUploadException("La URL de la imagen es requerida.");
        }
        
        boolean deleted = fileUploadService.deleteFile(imageUrl);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success(null, "Imagen eliminada correctamente"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "No se encontró la imagen o ya fue eliminada"));
    }
}
