package com.proxio.backend.repositories;

import com.proxio.backend.models.UserRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRatingRepository extends JpaRepository<UserRating, Long> {
    boolean existsByOrderIdAndRaterId(Long orderId, Long raterId);
}
