package com.proxio.backend.repositories;

import com.proxio.backend.models.PickupSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PickupSlotRepository extends JpaRepository<PickupSlot, Long> {
}
