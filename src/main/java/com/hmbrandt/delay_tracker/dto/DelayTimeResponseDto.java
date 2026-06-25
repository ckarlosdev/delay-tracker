package com.hmbrandt.delay_tracker.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record DelayTimeResponseDto(
        Long id,
        LocalDate logDate,
        LocalTime startTime,
        LocalTime endTime
) {
}
