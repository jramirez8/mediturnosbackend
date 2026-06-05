package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private Long id;
    private String nombreArchivo;
    private String mimeType;
    private Long originalSizeBytes;
    private Long compressedSizeBytes;
    private String url;
    private String message;
}
