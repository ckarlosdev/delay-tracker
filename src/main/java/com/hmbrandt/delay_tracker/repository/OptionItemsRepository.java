package com.hmbrandt.delay_tracker.repository;

import com.hmbrandt.delay_tracker.entity.OptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OptionItemsRepository extends JpaRepository<OptionItem, Long> {
}
