package com.hmbrandt.delay_tracker.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record DelayTimeRequestDto(
        LocalDate logDate,
        LocalTime startTime,
        LocalTime endTime
) {}
