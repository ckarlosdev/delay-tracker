package com.hmbrandt.delay_tracker.repository;

import com.hmbrandt.delay_tracker.entity.DelayTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DelayTimeRepository extends JpaRepository<DelayTime, Long> {
}
