package com.hmbrandt.delay_tracker.dto;

public record DelaySignatureRequestDto(
        String company,
        String signatureRole,
        String signatureData
) {}
