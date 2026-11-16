package com.foodloop.foodloop_backend.donation;

import com.foodloop.foodloop_backend.common.ApiException;
import com.foodloop.foodloop_backend.donation.DonationDtos.*;
import com.foodloop.foodloop_backend.user.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.*;

@Service
@Transactional
public class DonationService {
    private final DonationRepository repo;
    public DonationService(DonationRepository repo) { this.repo = repo; }

    public Donation create(User donor, DonationRequest r) {
        if (!r.expiresAt().isAfter(LocalDateTime.now())) throw ApiException.bad("Expiry must be in the future");
        Donation d = new Donation();
        d.setDonor(donor);
        apply(d, r, donor);
        d.setPickupToken(UUID.randomUUID().toString());
        d.setDeliveryToken(UUID.randomUUID().toString());
        return repo.save(d);
    }

    public Donation update(User donor, Long id, DonationRequest r) {
        Donation d = get(id);
        if (!d.getDonor().getId().equals(donor.getId())) throw ApiException.forbidden("Not your donation");
        if (d.getStatus() != DonationStatus.CREATED) throw ApiException.bad("Only donations that are not yet accepted can be edited");
        apply(d, r, donor);
        return d;
    }

    private void apply(Donation d, DonationRequest r, User donor) {
        d.setFoodCategory(r.foodCategory());
        d.setQuantityKg(r.quantityKg());
        d.setVegetarian(r.vegetarian() == null || r.vegetarian());
        d.setDescription(r.description());
        d.setPreparedAt(r.preparedAt());
        d.setExpiresAt(r.expiresAt());
        d.setPickupAddress(r.pickupAddress() != null ? r.pickupAddress() : donor.getAddress());
        d.setLatitude(r.latitude() != null ? r.latitude() : donor.getLatitude());
        d.setLongitude(r.longitude() != null ? r.longitude() : donor.getLongitude());
    }

    @Transactional(readOnly = true)
    public Donation get(Long id) { return repo.findById(id).orElseThrow(() -> ApiException.notFound("Donation not found")); }

    @Transactional(readOnly = true)
    public List<Donation> list(User u) {
        Stream<Donation> s = switch (u.getRole()) {
            case DONOR -> repo.findByDonorId(u.getId()).stream();
            case NGO -> Stream.concat(repo.findByStatus(DonationStatus.CREATED).stream(), repo.findByNgoId(u.getId()).stream());
            case VOLUNTEER -> Stream.concat(repo.findByStatus(DonationStatus.NGO_ACCEPTED).stream(), repo.findByVolunteerId(u.getId()).stream());
            case ADMIN -> repo.findAll().stream();
        };
        // most urgent first, finished ones sink to the bottom
        return s.distinct().sorted(Comparator.comparing((Donation d) -> isClosed(d.getStatus()))
                .thenComparing(Donation::getExpiresAt)).collect(Collectors.toList());
    }

    private boolean isClosed(DonationStatus s) { return s == DonationStatus.DELIVERED || s == DonationStatus.EXPIRED || s == DonationStatus.CANCELLED; }

    // ----- lifecycle: every transition goes through here -----
    private void move(Donation d, DonationStatus from, DonationStatus to) {
        if (d.getStatus() != from) throw ApiException.bad("Donation is " + d.getStatus() + ", expected " + from);
        if (d.getExpiresAt().isBefore(LocalDateTime.now()) && to != DonationStatus.DELIVERED && to != DonationStatus.PICKED_UP) {
            d.setStatus(DonationStatus.EXPIRED);
            throw ApiException.bad("Donation has expired");
        }
        d.setStatus(to);
    }

    public Donation ngoAccept(User ngo, Long id) {
        Donation d = get(id);
        move(d, DonationStatus.CREATED, DonationStatus.NGO_ACCEPTED);
        d.setNgo(ngo); d.setAcceptedAt(LocalDateTime.now());
        return d;
    }

    public Donation volunteerAssign(User vol, Long id) {
        Donation d = get(id);
        move(d, DonationStatus.NGO_ACCEPTED, DonationStatus.VOLUNTEER_ASSIGNED);
        d.setVolunteer(vol); d.setAssignedAt(LocalDateTime.now());
        return d;
    }

    public Donation pickup(User vol, Long id, String token) {
        Donation d = get(id);
        ownVolunteer(vol, d);
        if (!clean(token).equals(d.getPickupToken())) throw ApiException.bad("Pickup code does not match");
        move(d, DonationStatus.VOLUNTEER_ASSIGNED, DonationStatus.PICKED_UP);
        d.setPickedUpAt(LocalDateTime.now());
        return d;
    }

    public Donation deliver(User vol, Long id, String token) {
        Donation d = get(id);
        ownVolunteer(vol, d);
        if (!clean(token).equals(d.getDeliveryToken())) throw ApiException.bad("Delivery code does not match");
        move(d, DonationStatus.PICKED_UP, DonationStatus.DELIVERED);
        d.setDeliveredAt(LocalDateTime.now());
        return d;
    }

    public Donation cancel(User donor, Long id) {
        Donation d = get(id);
        if (!d.getDonor().getId().equals(donor.getId())) throw ApiException.forbidden("Not your donation");
        if (d.getStatus() != DonationStatus.CREATED && d.getStatus() != DonationStatus.NGO_ACCEPTED)
            throw ApiException.bad("Too late to cancel: food is already being collected");
        d.setStatus(DonationStatus.CANCELLED);
        return d;
    }

    private void ownVolunteer(User vol, Donation d) {
        if (d.getVolunteer() == null || !d.getVolunteer().getId().equals(vol.getId()))
            throw ApiException.forbidden("This donation is assigned to another volunteer");
    }

    /** QR payload looks like FOODLOOP:PICKUP:12:uuid - accept either the whole string or just the uuid. */
    private String clean(String t) { return t == null ? "" : t.substring(t.lastIndexOf(':') + 1).trim(); }

    @Scheduled(fixedRate = 60_000)
    public void expireOld() {
        var live = List.of(DonationStatus.CREATED, DonationStatus.NGO_ACCEPTED, DonationStatus.VOLUNTEER_ASSIGNED);
        repo.findByStatusInAndExpiresAtBefore(live, LocalDateTime.now()).forEach(d -> d.setStatus(DonationStatus.EXPIRED));
    }
}
