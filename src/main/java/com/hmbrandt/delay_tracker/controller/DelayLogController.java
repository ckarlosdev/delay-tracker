package com.hmbrandt.delay_tracker.controller;


import com.hmbrandt.delay_tracker.dto.DelayLogRequestDto;
import com.hmbrandt.delay_tracker.dto.DelayLogResponseDto;
import com.hmbrandt.delay_tracker.dto.OptionsItemRequestDto;
import com.hmbrandt.delay_tracker.service.DelayLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/delay-log")
@RequiredArgsConstructor
@Tag(name = "Delay Tracker", description = "Service to track the delay situations in a job ")
public class DelayLogController {

    private final DelayLogService delayLogService;

    @PostMapping
    public ResponseEntity<DelayLogResponseDto> create(@RequestBody DelayLogRequestDto delayLog){
        return new ResponseEntity<>(delayLogService.save(delayLog), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a delay form", description = "Finds delay log and updates its fields with the data from DTO.")
    @PutMapping("/{id}")
    public ResponseEntity<DelayLogResponseDto> update(
            @PathVariable Long id,
            @RequestBody DelayLogRequestDto delayLog
    ){
        DelayLogResponseDto updatedDelayLog = delayLogService.update(id, delayLog);
        return ResponseEntity.ok(updatedDelayLog);
    }

    @PutMapping("/{id}/finalize")
    public ResponseEntity<DelayLogResponseDto> finalizeOrder(@PathVariable Long id) {
        DelayLogResponseDto response = delayLogService.finalizeOrder(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DelayLogResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(delayLogService.findById(id));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<DelayLogResponseDto>> getByJobId(@PathVariable Long jobId) {
        return ResponseEntity.ok(delayLogService.findByJobId(jobId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id){
        delayLogService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/options")
    public ResponseEntity<List<OptionsItemRequestDto>> getFormOptions(){
        return ResponseEntity.ok(delayLogService.findOptions());
    }

}
