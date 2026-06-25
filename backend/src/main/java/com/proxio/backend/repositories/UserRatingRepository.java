package com.proxio.backend.repositories;

import com.proxio.backend.models.UserRating;
import com.proxio.backend.models.enums.RatingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRatingRepository extends JpaRepository<UserRating, Long> {
    Optional<UserRating> findByOrderIdAndRaterEmail(Long orderId, String email);

    boolean existsByOrderIdAndRaterEmail(Long orderId, String email);

    long countByRatedIdAndStatus(Long ratedId, RatingStatus status);

    @Query("select avg(r.score) from UserRating r where r.rated.id = :ratedId and r.status = com.proxio.backend.models.enums.RatingStatus.APPROVED")
    Double averageScoreForRatedId(Long ratedId);
}
