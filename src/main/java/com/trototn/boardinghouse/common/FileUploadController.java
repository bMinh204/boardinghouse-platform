package com.trototn.boardinghouse.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {
    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.upload.provider:local}")
    private String uploadProvider;

    @Value("${app.upload-dir:uploads/}")
    private String uploadDir;

    @Value("${app.upload.public-base-url:}")
    private String publicBaseUrl;

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudinaryCloudName;

    @Value("${app.cloudinary.api-key:}")
    private String cloudinaryApiKey;

    @Value("${app.cloudinary.api-secret:}")
    private String cloudinaryApiSecret;

    @Value("${app.cloudinary.folder:trototn/rooms}")
    private String cloudinaryFolder;

    @PostMapping
    public ResponseEntity<?> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        try {
            List<String> urls = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                validateImage(file);
                urls.add(uploadImage(file));
            }
            return ResponseEntity.ok(Map.of("urls", urls));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("File upload failed with provider '{}'", uploadProvider, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Không thể lưu ảnh. Vui lòng thử lại sau."));
        }
    }

    private void validateImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Chỉ cho phép tải lên ảnh JPG, PNG, WEBP hoặc GIF: " + file.getOriginalFilename());
        }
    }

    private String uploadImage(MultipartFile file) throws IOException, InterruptedException, NoSuchAlgorithmException {
        if ("cloudinary".equalsIgnoreCase(uploadProvider)) {
            return uploadToCloudinary(file);
        }
        return uploadToLocalStorage(file);
    }

    private String uploadToLocalStorage(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String extension = safeExtension(file.getOriginalFilename());
        String newFilename = UUID.randomUUID() + extension;
        Path filePath = uploadPath.resolve(newFilename).normalize();
        if (!filePath.startsWith(uploadPath)) {
            throw new IOException("Invalid upload path");
        }

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String relativeUrl = "/uploads/" + newFilename;
        String baseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        if (baseUrl.isBlank()) return relativeUrl;
        return baseUrl.replaceAll("/+$", "") + relativeUrl;
    }

    private String uploadToCloudinary(MultipartFile file) throws IOException, InterruptedException, NoSuchAlgorithmException {
        requireCloudinaryConfig();

        String publicId = UUID.randomUUID().toString();
        long timestamp = Instant.now().getEpochSecond();
        String signaturePayload = "folder=" + cloudinaryFolder
                + "&public_id=" + publicId
                + "&timestamp=" + timestamp
                + cloudinaryApiSecret;
        String signature = sha1(signaturePayload);
        String boundary = "----TroTotTNU" + UUID.randomUUID();

        byte[] body = multipartBody(boundary, List.of(
                Part.text("api_key", cloudinaryApiKey),
                Part.text("timestamp", String.valueOf(timestamp)),
                Part.text("folder", cloudinaryFolder),
                Part.text("public_id", publicId),
                Part.text("signature", signature),
                Part.file("file", fallbackFilename(file), file.getContentType(), file.getBytes())
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudinaryCloudName + "/image/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.warn("Cloudinary upload failed. Status: {}, Body: {}", response.statusCode(), response.body());
            throw new IOException("Cloudinary upload failed");
        }

        JsonNode json = objectMapper.readTree(response.body());
        JsonNode secureUrl = json.get("secure_url");
        if (secureUrl == null || secureUrl.asText().isBlank()) {
            throw new IOException("Cloudinary response does not contain secure_url");
        }
        return secureUrl.asText();
    }

    private void requireCloudinaryConfig() {
        if (isBlank(cloudinaryCloudName) || isBlank(cloudinaryApiKey) || isBlank(cloudinaryApiSecret)) {
            throw new IllegalArgumentException("Thiếu cấu hình Cloudinary. Cần CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET.");
        }
    }

    private String safeExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,8}") ? extension : "";
    }

    private String fallbackFilename(MultipartFile file) {
        String original = file.getOriginalFilename();
        return original == null || original.isBlank() ? "upload" + safeExtension(original) : original;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String sha1(String value) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private byte[] multipartBody(String boundary, List<Part> parts) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (Part part : parts) {
            output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            if (part.fileContent == null) {
                output.write(("Content-Disposition: form-data; name=\"" + part.name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(part.value.getBytes(StandardCharsets.UTF_8));
                output.write("\r\n".getBytes(StandardCharsets.UTF_8));
            } else {
                output.write(("Content-Disposition: form-data; name=\"" + part.name + "\"; filename=\"" + part.filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(("Content-Type: " + part.contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(part.fileContent);
                output.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
        }
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private record Part(String name, String value, String filename, String contentType, byte[] fileContent) {
        static Part text(String name, String value) {
            return new Part(name, value, null, null, null);
        }

        static Part file(String name, String filename, String contentType, byte[] fileContent) {
            return new Part(name, null, filename, contentType, fileContent);
        }
    }
}
