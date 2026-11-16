package com.foodloop.foodloop_backend.donation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByDonorId(Long id);
    List<Donation> findByNgoId(Long id);
    List<Donation> findByVolunteerId(Long id);
    List<Donation> findByStatus(DonationStatus s);
    List<Donation> findByStatusInAndExpiresAtBefore(Collection<DonationStatus> s, LocalDateTime t);
}
