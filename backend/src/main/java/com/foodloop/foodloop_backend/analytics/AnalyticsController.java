package com.foodloop.foodloop_backend.analytics;

import com.foodloop.foodloop_backend.donation.*;
import com.foodloop.foodloop_backend.user.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final DonationRepository donations;
    private final UserRepository users;
    public AnalyticsController(DonationRepository d, UserRepository u) { this.donations = d; this.users = u; }

    @GetMapping
    public Map<String, Object> stats() {
        List<Donation> all = donations.findAll();
        List<Donation> done = all.stream().filter(d -> d.getStatus() == DonationStatus.DELIVERED).toList();
        double kg = done.stream().mapToDouble(Donation::getQuantityKg).sum();
        Map<String, Long> byStatus = all.stream().collect(Collectors.groupingBy(d -> d.getStatus().name(), TreeMap::new, Collectors.counting()));
        List<Map<String, Object>> top = done.stream()
            .collect(Collectors.groupingBy(d -> d.getDonor().getOrgName() != null ? d.getDonor().getOrgName() : d.getDonor().getName(),
                     Collectors.summingDouble(Donation::getQuantityKg)))
            .entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(5)
            .map(e -> Map.<String, Object>of("donor", e.getKey(), "kg", Math.round(e.getValue() * 10) / 10.0)).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalDonations", all.size());
        out.put("completedDonations", done.size());
        out.put("kgRescued", Math.round(kg * 10) / 10.0);
        out.put("mealsServed", Math.round(kg * 2.5));   // ~2.5 meals per kg
        out.put("activeNgos", users.countByRole(Role.NGO));
        out.put("volunteers", users.countByRole(Role.VOLUNTEER));
        out.put("donors", users.countByRole(Role.DONOR));
        out.put("byStatus", byStatus);
        out.put("topDonors", top);
        return out;
    }
}
