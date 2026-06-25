package com.proxio.backend.repositories;

import com.proxio.backend.models.WeeklyOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyOfferRepository extends JpaRepository<WeeklyOffer, Long> {
}
