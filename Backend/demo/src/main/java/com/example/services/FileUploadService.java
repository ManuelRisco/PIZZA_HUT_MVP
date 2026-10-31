package com.example.services;

import com.example.exceptions.FileUploadException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;

@Service
public class FileUploadService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-width:800}")
    private int maxWidth;

    @Value("${app.upload.max-height:800}")
    private int maxHeight;

    @Value("${app.upload.thumbnail-width:200}")
    private int thumbnailWidth;

    @Value("${app.upload.thumbnail-height:200}")
    private int thumbnailHeight;

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * Resuelve el directorio raíz de uploads independientemente de si la aplicación
     * se inicia desde Backend/demo (gradlew bootRun) o desde la raíz del monorepo (IDE Run).
     */
    public static Path resolveUploadRoot(String uploadDir) {
        String dir = (uploadDir == null || uploadDir.isBlank()) ? "uploads" : uploadDir.trim();
        Path configured = Paths.get(dir);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        if ("uploads".equals(dir)) {
            if (Files.exists(Paths.get("Backend", "demo", "build.gradle"))) {
                return Paths.get("Backend", "demo", dir).toAbsolutePath().normalize();
            }
            if (Files.exists(Paths.get("demo", "build.gradle"))) {
                return Paths.get("demo", dir).toAbsolutePath().normalize();
            }
        }
        return configured.toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Path uploadPath = resolveUploadRoot(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            // Crear subdirectorios para cada tipo de entidad
            Files.createDirectories(uploadPath.resolve("pizzas"));
            Files.createDirectories(uploadPath.resolve("extras"));
            // Subdirectorios para thumbnails
            Files.createDirectories(uploadPath.resolve("pizzas").resolve("thumbs"));
            Files.createDirectories(uploadPath.resolve("extras").resolve("thumbs"));
        } catch (IOException e) {
            throw new FileUploadException("No se pudo crear el directorio de uploads: " + e.getMessage(), e);
        }
    }

    /**
     * Guarda un archivo subido, lo redimensiona y genera un thumbnail.
     * @param file El archivo a guardar
     * @param subdirectory Subdirectorio (pizzas, extras)
     * @return URL relativa del archivo guardado
     */
    public String saveFile(MultipartFile file, String subdirectory) {
        // Validar que el archivo no esté vacío
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("El archivo está vacío o no fue proporcionado.");
        }

        // Validar tipo de archivo
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new FileUploadException(
                "Tipo de archivo no permitido (" + contentType + "). Solo se aceptan: JPG, PNG, WEBP, GIF."
            );
        }

        // Validar tamaño
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException(
                "El archivo excede el tamaño máximo permitido (5MB). Tamaño actual: "
                + String.format("%.2f MB", file.getSize() / (1024.0 * 1024.0))
            );
        }

        // Validar subdirectorio (prevenir path traversal)
        if (!subdirectory.equals("pizzas") && !subdirectory.equals("extras")) {
            throw new FileUploadException("Subdirectorio no válido: " + subdirectory);
        }

        try {
            // Generar nombre único
            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Rutas de destino
            Path targetDir = resolveUploadRoot(uploadDir).resolve(subdirectory);
            Path thumbDir = targetDir.resolve("thumbs");
            Files.createDirectories(thumbDir);
            Path targetPath = targetDir.resolve(uniqueFilename);
            Path thumbPath = thumbDir.resolve(uniqueFilename);

            // Leer la imagen original
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            if (originalImage != null) {
                // Redimensionar imagen principal (máx 800x800 manteniendo proporción)
                BufferedImage resized = resizeImage(originalImage, maxWidth, maxHeight);
                String formatName = getFormatName(extension);
                writeImage(resized, formatName, targetPath);

                // Generar thumbnail (máx 200x200)
                BufferedImage thumbnail = resizeImage(originalImage, thumbnailWidth, thumbnailHeight);
                writeImage(thumbnail, formatName, thumbPath);
            } else {
                // Si no se pudo leer como imagen (ej. WebP sin decodificador ImageIO), guardar original y thumbnail
                try (var in = file.getInputStream()) {
                    Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                Files.copy(targetPath, thumbPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Retornar URL relativa
            return "/uploads/" + subdirectory + "/" + uniqueFilename;

        } catch (IOException e) {
            throw new FileUploadException("Error al guardar la imagen: " + e.getMessage(), e);
        }
    }

    /**
     * Redimensiona una imagen manteniendo la proporción.
     * Solo reduce tamaño, nunca agranda.
     */
    public BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        int origWidth = original.getWidth();
        int origHeight = original.getHeight();

        // Si la imagen ya es más pequeña, no redimensionar
        if (origWidth <= targetWidth && origHeight <= targetHeight) {
            return original;
        }

        // Calcular nuevo tamaño manteniendo proporción
        double widthRatio = (double) targetWidth / origWidth;
        double heightRatio = (double) targetHeight / origHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = Math.max(1, (int) (origWidth * ratio));
        int newHeight = Math.max(1, (int) (origHeight * ratio));

        // Crear imagen redimensionada con tipo compatible con ImageIO
        int imageType = original.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resized = new BufferedImage(newWidth, newHeight, imageType);

        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resized;
    }

    /**
     * Redimensiona una imagen a dimensiones específicas (sin mantener proporción).
     * Útil para generar imágenes de tamaño fijo.
     */
    public BufferedImage resizeImageExact(BufferedImage original, int width, int height) {
        int imageType = original.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resized = new BufferedImage(Math.max(1, width), Math.max(1, height), imageType);

        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();

        return resized;
    }

    /**
     * Elimina un archivo y su thumbnail dado su URL relativa
     */
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return false;
        }

        try {
            // Convertir URL a path del sistema validando path traversal
            String relativePath = fileUrl.substring("/uploads/".length());
            Path rootPath = resolveUploadRoot(uploadDir);
            Path filePath = rootPath.resolve(relativePath).normalize();
            if (!filePath.startsWith(rootPath)) {
                return false;
            }
            boolean mainDeleted = Files.deleteIfExists(filePath);

            // Intentar eliminar thumbnail también
            String filename = filePath.getFileName().toString();
            Path thumbPath = filePath.getParent().resolve("thumbs").resolve(filename);
            Files.deleteIfExists(thumbPath);

            return mainDeleted;
        } catch (IOException e) {
            // No lanzar excepción, solo retornar false
            return false;
        }
    }

    /**
     * Obtiene la URL del thumbnail dado la URL principal
     */
    public String getThumbnailUrl(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
            return imageUrl;
        }
        // /uploads/pizzas/file.jpg → /uploads/pizzas/thumbs/file.jpg
        int lastSlash = imageUrl.lastIndexOf('/');
        return imageUrl.substring(0, lastSlash) + "/thumbs" + imageUrl.substring(lastSlash);
    }

    // ===== Métodos Privados =====

    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".")).toLowerCase();
        }
        return ".jpg"; // Default
    }

    private String getFormatName(String extension) {
        return switch (extension.toLowerCase()) {
            case ".png" -> "png";
            case ".gif" -> "gif";
            case ".webp" -> "webp";
            default -> "jpg";
        };
    }

    private void writeImage(BufferedImage image, String formatName, Path path) throws IOException {
        BufferedImage targetImage = image;
        if ("jpg".equalsIgnoreCase(formatName) && image.getColorModel().hasAlpha()) {
            targetImage = toRgbImage(image);
        }
        boolean written;
        try (OutputStream os = Files.newOutputStream(path)) {
            written = ImageIO.write(targetImage, formatName, os);
        }
        if (!written) {
            // Si el formato no está soportado (ej. webp), guardar como jpg RGB
            BufferedImage rgbImage = targetImage.getColorModel().hasAlpha() ? toRgbImage(targetImage) : targetImage;
            try (OutputStream fallbackOs = Files.newOutputStream(path)) {
                ImageIO.write(rgbImage, "jpg", fallbackOs);
            }
        }
    }

    private BufferedImage toRgbImage(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, source.getWidth(), source.getHeight());
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return rgb;
    }
}
