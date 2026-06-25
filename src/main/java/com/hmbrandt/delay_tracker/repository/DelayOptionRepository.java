package com.hmbrandt.delay_tracker.repository;

import com.hmbrandt.delay_tracker.entity.DelayLog;
import com.hmbrandt.delay_tracker.entity.DelayOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DelayOptionRepository extends JpaRepository<DelayOption, Long> {
}
