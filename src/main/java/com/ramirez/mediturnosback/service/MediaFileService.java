package com.ramirez.mediturnosback.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Service
public class MediaFileService {
    private static final String MIME_IMAGE_JPEG = "image/jpeg";

    @Value("${app.upload.dir:/data/uploads}")
    private String uploadDir;

    @Value("${app.upload.max-input-bytes:6291456}")
    private long maxInputBytes;

    @Value("${app.upload.image.max-width:1280}")
    private int maxWidth;

    @Value("${app.upload.image.max-height:1280}")
    private int maxHeight;

    @Value("${app.upload.image.jpeg-quality:0.58}")
    private float jpegQuality;

    @Value("${app.upload.image.target-max-bytes:350000}")
    private long targetMaxBytes;

    public StoredImage storeCompressedImage(MultipartFile file, String category, Long ownerId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ningún archivo");
        }
        if (file.getSize() > maxInputBytes) {
            throw new IllegalArgumentException("La imagen supera el tamaño máximo permitido");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Solo se aceptan imágenes");
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage original = ImageIO.read(inputStream);
            if (original == null) {
                throw new IllegalArgumentException("Formato de imagen no soportado. Usá JPG o PNG.");
            }

            BufferedImage resized = resizeToJpegCanvas(original);
            byte[] compressed = compressJpeg(resized);

            String safeCategory = sanitizePathPart(category == null ? "general" : category);
            String date = LocalDate.now().toString();
            Path directory = root().resolve(safeCategory).resolve(String.valueOf(ownerId == null ? 0 : ownerId)).resolve(date);
            Files.createDirectories(directory);

            String originalName = file.getOriginalFilename() == null ? "imagen" : file.getOriginalFilename();
            String baseName = sanitizeFileName(originalName.replaceAll("\\.[^.]*$", ""));
            String filename = baseName + "-" + UUID.randomUUID() + ".jpg";
            Path target = directory.resolve(filename);
            Files.write(target, compressed, StandardOpenOption.CREATE_NEW);

            String relativePath = root().relativize(target).toString().replace('\\', '/');
            return new StoredImage(
                    originalName,
                    MIME_IMAGE_JPEG,
                    relativePath,
                    file.getSize(),
                    (long) compressed.length
            );
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen comprimida");
        }
    }


    public StoredFile storeMedicalDocument(MultipartFile file, String category, Long ownerId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ningún archivo");
        }
        long maxMedicalDocumentBytes = 1_048_576L;
        if (file.getSize() > maxMedicalDocumentBytes) {
            throw new IllegalArgumentException("El documento supera 1 MB. Usá PDF/JPG/PNG liviano.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = extensionFromContentType(contentType, file.getOriginalFilename());
        if (extension == null) {
            throw new IllegalArgumentException("Solo se aceptan PDF, JPG o PNG");
        }

        if (contentType.startsWith("image/")) {
            try (InputStream inputStream = file.getInputStream()) {
                if (ImageIO.read(inputStream) == null) {
                    throw new IllegalArgumentException("Imagen inválida. Usá JPG o PNG.");
                }
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo validar la imagen");
            }
        }

        try (InputStream inputStream = file.getInputStream()) {
            String safeCategory = sanitizePathPart(category == null ? "general" : category);
            String date = LocalDate.now().toString();
            Path directory = root().resolve(safeCategory).resolve(String.valueOf(ownerId == null ? 0 : ownerId)).resolve(date);
            Files.createDirectories(directory);

            String originalName = file.getOriginalFilename() == null ? "documento" + extension : file.getOriginalFilename();
            String baseName = sanitizeFileName(originalName.replaceAll("\\.[^.]*$", ""));
            String filename = baseName + "-" + UUID.randomUUID() + extension;
            Path target = directory.resolve(filename);
            Files.copy(inputStream, target);

            String relativePath = root().relativize(target).toString().replace('\\', '/');
            return new StoredFile(originalName, normalizeDocumentMimeType(extension), relativePath, file.getSize(), Files.size(target));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el documento");
        }
    }

    public Resource loadAsResource(String relativePath) {
        try {
            Path filePath = resolveSafe(relativePath);
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new IllegalArgumentException("Archivo no encontrado");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Archivo no disponible");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Ruta de archivo inválida");
        }
    }

    public void deleteQuietly(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Files.deleteIfExists(resolveSafe(relativePath));
        } catch (Exception ignored) {
            // No frenamos una operación funcional por limpieza de archivo viejo.
        }
    }

    private BufferedImage resizeToJpegCanvas(BufferedImage original) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        double scale = Math.min(1.0, Math.min(maxWidth / (double) originalWidth, maxHeight / (double) originalHeight));
        int targetWidth = Math.max(1, (int) Math.round(originalWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(originalHeight * scale));

        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, targetWidth, targetHeight);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return output;
    }

    private byte[] compressJpeg(BufferedImage image) throws IOException {
        float quality = Math.max(0.30f, Math.min(0.90f, jpegQuality));
        byte[] best = writeJpeg(image, quality);
        while (best.length > targetMaxBytes && quality > 0.34f) {
            quality -= 0.08f;
            best = writeJpeg(image, Math.max(0.32f, quality));
        }
        return best;
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No hay encoder JPG disponible");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
            ios.flush();
            return baos.toByteArray();
        } finally {
            writer.dispose();
        }
    }


    private String extensionFromContentType(String contentType, String originalFilename) {
        String filename = originalFilename == null ? "" : originalFilename.toLowerCase(Locale.ROOT);
        if ("application/pdf".equals(contentType) || filename.endsWith(".pdf")) return ".pdf";
        if (MIME_IMAGE_JPEG.equals(contentType) || "image/jpg".equals(contentType) || filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return ".jpg";
        if ("image/png".equals(contentType) || filename.endsWith(".png")) return ".png";
        return null;
    }

    private String normalizeDocumentMimeType(String extension) {
        if (".pdf".equals(extension)) return "application/pdf";
        if (".png".equals(extension)) return "image/png";
        return MIME_IMAGE_JPEG;
    }

    private Path root() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private Path resolveSafe(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Ruta vacía");
        }
        Path resolved = root().resolve(relativePath).normalize();
        if (!resolved.startsWith(root())) {
            throw new IllegalArgumentException("Ruta inválida");
        }
        return resolved;
    }

    private String sanitizePathPart(String value) {
        return sanitizeFileName(value).replace('.', '-');
    }

    private String sanitizeFileName(String value) {
        String normalized = Normalizer.normalize(value == null ? "archivo" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-_]", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-)|(-$)", "");
        return normalized.isBlank() ? "archivo" : normalized.substring(0, Math.min(normalized.length(), 60));
    }

    public record StoredImage(String originalName, String mimeType, String relativePath, Long originalSizeBytes, Long compressedSizeBytes) {}
    public record StoredFile(String originalName, String mimeType, String relativePath, Long originalSizeBytes, Long storedSizeBytes) {}
}
