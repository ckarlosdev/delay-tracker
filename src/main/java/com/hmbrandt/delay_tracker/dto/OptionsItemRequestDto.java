package com.hmbrandt.delay_tracker.dto;

public record OptionsItemRequestDto(
        Long id,
        String optionType,
        String optionName
) {
}
