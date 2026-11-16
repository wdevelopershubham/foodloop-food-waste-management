package com.foodloop.foodloop_backend.donation;

import com.foodloop.foodloop_backend.user.*;
import jakarta.validation.constraints.*;
import java.time.*;

public class DonationDtos {
    public record DonationRequest(
        @NotBlank String foodCategory,
        @Positive double quantityKg,
        Boolean vegetarian, String description,
        LocalDateTime preparedAt,
        @NotNull LocalDateTime expiresAt,
        String pickupAddress, Double latitude, Double longitude) {}

    public record TokenRequest(@NotBlank String token) {}

    public record DonationDto(Long id, String foodCategory, double quantityKg, boolean vegetarian, String description,
                              LocalDateTime preparedAt, LocalDateTime expiresAt, String pickupAddress,
                              Double latitude, Double longitude, DonationStatus status, boolean urgent, long minutesLeft,
                              String donorName, Long ngoId, String ngoName, String volunteerName,
                              LocalDateTime createdAt, LocalDateTime deliveredAt) {
        public static DonationDto of(Donation d) {
            long mins = Duration.between(LocalDateTime.now(), d.getExpiresAt()).toMinutes();
            boolean live = d.getStatus() != DonationStatus.DELIVERED && d.getStatus() != DonationStatus.EXPIRED
                    && d.getStatus() != DonationStatus.CANCELLED && d.getStatus() != DonationStatus.PICKED_UP;
            User donor = d.getDonor(), ngo = d.getNgo(), vol = d.getVolunteer();
            return new DonationDto(d.getId(), d.getFoodCategory(), d.getQuantityKg(), d.isVegetarian(), d.getDescription(),
                d.getPreparedAt(), d.getExpiresAt(), d.getPickupAddress(), d.getLatitude(), d.getLongitude(),
                d.getStatus(), live && mins < 120, mins,
                donor.getOrgName() != null && !donor.getOrgName().isBlank() ? donor.getOrgName() : donor.getName(),
                ngo == null ? null : ngo.getId(),
                ngo == null ? null : (ngo.getOrgName() != null ? ngo.getOrgName() : ngo.getName()),
                vol == null ? null : vol.getName(), d.getCreatedAt(), d.getDeliveredAt());
        }
    }
}
