package com.hmbrandt.delay_tracker.service;

import com.hmbrandt.delay_tracker.dto.DelayLogRequestDto;
import com.hmbrandt.delay_tracker.dto.DelayLogResponseDto;
import com.hmbrandt.delay_tracker.dto.OptionsItemRequestDto;

import java.util.List;

public interface DelayLogService {

    DelayLogResponseDto save(DelayLogRequestDto delayLog);

    DelayLogResponseDto update(Long id, DelayLogRequestDto delayLog);

    DelayLogResponseDto findById(Long id);

    List<DelayLogResponseDto> findByJobId(Long jobId);

    void delete(Long id);

    List<OptionsItemRequestDto> findOptions();

    DelayLogResponseDto finalizeOrder(Long id);

}
