package com.hmbrandt.delay_tracker.dto;

import java.time.LocalDate;
import java.util.List;

public record DelayLogRequestDto(
        Long id,
        Long jobId,
        Long employeeId,
        LocalDate delayDate,
        String location,
        String delayDescription,
        String impactEquipment,
        String summary,
        String resolution,
        String workers,
        String cost,
        String delayStatus,
        List<DelayTimeResponseDto> times,
        List<DelayOptionResponseDto> options,
        List<DelaySignatureRequestDto> signatures
) {}
