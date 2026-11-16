package com.foodloop.foodloop_backend.auth;

import com.foodloop.foodloop_backend.user.*;
import jakarta.validation.constraints.*;

public class AuthDtos {
    public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotNull Role role,
        String orgName, String address, Double latitude, Double longitude,
        Double capacityKg, String acceptedFoodTypes, String vehicle) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record UserInfo(Long id, String name, String email, Role role, String orgName) {
        public static UserInfo of(User u) { return new UserInfo(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getOrgName()); }
    }

    public record AuthResponse(String token, UserInfo user) {}
}
