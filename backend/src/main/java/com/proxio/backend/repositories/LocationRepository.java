package com.proxio.backend.repositories;

import com.proxio.backend.models.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findAllByOrderByCityAscNameAsc();

    List<Location> findByVendorIdOrderByIdAsc(Long vendorId);
}
