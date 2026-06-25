package com.proxio.backend.repositories;

import com.proxio.backend.models.PickupSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PickupSlotRepository extends JpaRepository<PickupSlot, Long> {
    List<PickupSlot> findByLocationIdOrderByStartTimeAsc(Long locationId);
}
