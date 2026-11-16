package com.foodloop.foodloop_backend.auth;

import com.foodloop.foodloop_backend.auth.AuthDtos.*;
import com.foodloop.foodloop_backend.common.ApiException;
import com.foodloop.foodloop_backend.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest r) {
        if (r.role() == Role.ADMIN) throw ApiException.bad("Admin accounts cannot be self-registered");
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw ApiException.bad("Email already registered");
        User u = User.builder().name(r.name()).email(email).passwordHash(encoder.encode(r.password()))
                .role(r.role()).orgName(r.orgName()).address(r.address())
                .latitude(r.latitude()).longitude(r.longitude()).capacityKg(r.capacityKg())
                .acceptedFoodTypes(r.acceptedFoodTypes()).vehicle(r.vehicle()).build();
        users.save(u);
        return new AuthResponse(jwt.generate(u), UserInfo.of(u));
    }

    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));
        if (!u.isActive()) throw ApiException.forbidden("Account disabled");
        return new AuthResponse(jwt.generate(u), UserInfo.of(u));
    }
}
