package com.example.services;

import com.example.exceptions.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests Unitarios: FileUploadService")
class FileUploadServiceTest {

    private FileUploadService fileUploadService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService();
        ReflectionTestUtils.setField(fileUploadService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileUploadService, "maxWidth", 800);
        ReflectionTestUtils.setField(fileUploadService, "maxHeight", 800);
        ReflectionTestUtils.setField(fileUploadService, "thumbnailWidth", 200);
        ReflectionTestUtils.setField(fileUploadService, "thumbnailHeight", 200);
        fileUploadService.init();
    }

    @Test
    @DisplayName("Debe rechazar archivos vacíos")
    void testRechazarArchivoVacio() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
        assertThrows(FileUploadException.class, () -> fileUploadService.saveFile(emptyFile, "pizzas"));
    }

    @Test
    @DisplayName("Debe rechazar archivos con tipos MIME no permitidos")
    void testRechazarTipoInvalido() {
        MockMultipartFile pdfFile = new MockMultipartFile("file", "document.pdf", "application/pdf", "dummy pdf content".getBytes());
        assertThrows(FileUploadException.class, () -> fileUploadService.saveFile(pdfFile, "pizzas"));
    }

    @Test
    @DisplayName("Debe guardar y generar URL relativa para una imagen válida")
    void testGuardarImagenValida() throws IOException {
        BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile validFile = new MockMultipartFile("file", "test-pizza.png", "image/png", imageBytes);

        String resultUrl = fileUploadService.saveFile(validFile, "pizzas");

        assertNotNull(resultUrl);
        assertTrue(resultUrl.contains("/uploads/pizzas/"));
        assertTrue(resultUrl.endsWith(".webp") || resultUrl.endsWith(".png") || resultUrl.endsWith(".jpg"));

        // Probar getThumbnailUrl
        String thumbUrl = fileUploadService.getThumbnailUrl(resultUrl);
        assertNotNull(thumbUrl);
        assertTrue(thumbUrl.contains("/uploads/pizzas/thumbs/"));

        // Probar deleteFile
        boolean deleted = fileUploadService.deleteFile(resultUrl);
        assertTrue(deleted);
    }

    @Test
    @DisplayName("Debe redimensionar imagen respetando límites")
    void testResizeImage() {
        BufferedImage largeImage = new BufferedImage(1600, 1200, BufferedImage.TYPE_INT_RGB);
        BufferedImage resized = fileUploadService.resizeImage(largeImage, 800, 800);
        assertNotNull(resized);
        assertTrue(resized.getWidth() <= 800);
        assertTrue(resized.getHeight() <= 800);
    }
}
