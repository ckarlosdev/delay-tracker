package com.hmbrandt.delay_tracker.repository;

import com.hmbrandt.delay_tracker.entity.DelayLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DelayLogRepository extends JpaRepository<DelayLog, Long> {
    List<DelayLog> findByJobId(Long jobId);
}
