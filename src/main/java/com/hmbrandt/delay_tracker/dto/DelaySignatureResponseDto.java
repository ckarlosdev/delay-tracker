package com.hmbrandt.delay_tracker.dto;

public record DelaySignatureResponseDto(
        Long id,
        String signatureRole,
        String company,
        String filePath
) {
}
