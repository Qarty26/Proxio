package com.proxio.backend.repositories;

import com.proxio.backend.models.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCustomerUserEmailOrderBySubscribedAtDesc(String email);

    boolean existsByCustomerUserEmailAndVendorId(String email, Long vendorId);

    Optional<Subscription> findByCustomerUserEmailAndVendorId(String email, Long vendorId);
}
