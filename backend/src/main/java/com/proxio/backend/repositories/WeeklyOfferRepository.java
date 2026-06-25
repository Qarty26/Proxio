package com.proxio.backend.repositories;

import com.proxio.backend.models.WeeklyOffer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WeeklyOfferRepository extends JpaRepository<WeeklyOffer, Long> {
    Page<WeeklyOffer> findByValidFromLessThanEqualAndValidUntilGreaterThanEqualAndAvailableQuantityGreaterThan(
            LocalDate validFrom,
            LocalDate validUntil,
            Double availableQuantity,
            Pageable pageable
    );

    Page<WeeklyOffer> findByValidFromLessThanEqualAndValidUntilGreaterThanEqualAndAvailableQuantityGreaterThanAndLocationId(
            LocalDate validFrom,
            LocalDate validUntil,
            Double availableQuantity,
            Long locationId,
            Pageable pageable
    );

    @Query("""
            select offer from WeeklyOffer offer
            where offer.validFrom <= :today
              and offer.validUntil >= :today
              and offer.availableQuantity > :availableQuantity
              and (:locationId is null or offer.location.id = :locationId)
              and (:vendorId is null or offer.product.vendor.id = :vendorId)
            """)
    Page<WeeklyOffer> searchActiveOffers(
            LocalDate today,
            Double availableQuantity,
            Long locationId,
            Long vendorId,
            Pageable pageable
    );

    List<WeeklyOffer> findByProductVendorUserEmailOrderByValidUntilDesc(String email);
}
