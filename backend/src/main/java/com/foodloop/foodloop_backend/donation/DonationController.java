package com.foodloop.foodloop_backend.donation;

import com.foodloop.foodloop_backend.common.ApiException;
import com.foodloop.foodloop_backend.donation.DonationDtos.*;
import com.foodloop.foodloop_backend.matching.MatchingService;
import com.foodloop.foodloop_backend.ml.MlClient;
import com.foodloop.foodloop_backend.qr.QrService;
import com.foodloop.foodloop_backend.user.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class DonationController {
    private final DonationService service;
    private final QrService qr;
    private final MatchingService matching;
    private final MlClient ml;

    public DonationController(DonationService s, QrService qr, MatchingService m, MlClient ml) {
        this.service = s; this.qr = qr; this.matching = m; this.ml = ml;
    }

    @PostMapping("/donations") @PreAuthorize("hasRole('DONOR')")
    public DonationDto create(@AuthenticationPrincipal User u, @Valid @RequestBody DonationRequest r) { return DonationDto.of(service.create(u, r)); }

    @GetMapping("/donations")
    public List<DonationDto> list(@AuthenticationPrincipal User u) { return service.list(u).stream().map(DonationDto::of).toList(); }

    @GetMapping("/donations/{id}")
    public DonationDto one(@PathVariable Long id) { return DonationDto.of(service.get(id)); }

    @PutMapping("/donations/{id}") @PreAuthorize("hasRole('DONOR')")
    public DonationDto update(@AuthenticationPrincipal User u, @PathVariable Long id, @Valid @RequestBody DonationRequest r) { return DonationDto.of(service.update(u, id, r)); }

    @PostMapping("/donations/{id}/cancel") @PreAuthorize("hasRole('DONOR')")
    public DonationDto cancel(@AuthenticationPrincipal User u, @PathVariable Long id) { return DonationDto.of(service.cancel(u, id)); }

    @PostMapping("/donations/{id}/accept") @PreAuthorize("hasRole('NGO')")
    public DonationDto accept(@AuthenticationPrincipal User u, @PathVariable Long id) { return DonationDto.of(service.ngoAccept(u, id)); }

    @PostMapping("/donations/{id}/assign") @PreAuthorize("hasRole('VOLUNTEER')")
    public DonationDto assign(@AuthenticationPrincipal User u, @PathVariable Long id) { return DonationDto.of(service.volunteerAssign(u, id)); }

    @PostMapping("/donations/{id}/pickup") @PreAuthorize("hasRole('VOLUNTEER')")
    public DonationDto pickup(@AuthenticationPrincipal User u, @PathVariable Long id, @Valid @RequestBody TokenRequest t) { return DonationDto.of(service.pickup(u, id, t.token())); }

    @PostMapping("/donations/{id}/deliver") @PreAuthorize("hasRole('VOLUNTEER')")
    public DonationDto deliver(@AuthenticationPrincipal User u, @PathVariable Long id, @Valid @RequestBody TokenRequest t) { return DonationDto.of(service.deliver(u, id, t.token())); }

    /** Donor gets the pickup QR, the accepting NGO gets the delivery QR. */
    @GetMapping("/donations/{id}/qr")
    public Map<String, String> qr(@AuthenticationPrincipal User u, @PathVariable Long id) {
        Donation d = service.get(id);
        String type, token;
        if (d.getDonor().getId().equals(u.getId())) { type = "PICKUP"; token = d.getPickupToken(); }
        else if (d.getNgo() != null && d.getNgo().getId().equals(u.getId())) { type = "DELIVERY"; token = d.getDeliveryToken(); }
        else throw ApiException.forbidden("No QR available for you on this donation");
        String payload = "FOODLOOP:" + type + ":" + id + ":" + token;
        return Map.of("type", type, "token", token, "qr", qr.dataUrl(payload));
    }

    @GetMapping("/donations/{id}/matches") @PreAuthorize("hasAnyRole('DONOR','ADMIN')")
    public List<MatchingService.Match> matches(@PathVariable Long id) { return matching.top(service.get(id), 5); }

    @GetMapping("/predict/surplus") @PreAuthorize("hasAnyRole('DONOR','ADMIN')")
    public Map<String, Object> predict(@RequestParam double preparedQty, @RequestParam(defaultValue = "false") boolean eventFlag,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ml.predict(preparedQty, eventFlag, date != null ? date : LocalDate.now());
    }
}
