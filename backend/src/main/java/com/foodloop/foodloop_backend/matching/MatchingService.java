package com.foodloop.foodloop_backend.matching;

import com.foodloop.foodloop_backend.common.GeoUtil;
import com.foodloop.foodloop_backend.donation.Donation;
import com.foodloop.foodloop_backend.user.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class MatchingService {
    public record Match(Long ngoId, String ngoName, String address, Double distanceKm, double score) {}

    private final UserRepository users;
    public MatchingService(UserRepository users) { this.users = users; }

    /** score = 0.40 distance + 0.25 capacity + 0.20 food type + 0.15 availability */
    public List<Match> top(Donation d, int n) {
        List<Match> out = new ArrayList<>();
        for (User ngo : users.findByRoleAndActiveTrue(Role.NGO)) {
            Double km = (d.getLatitude() != null && ngo.getLatitude() != null)
                    ? GeoUtil.km(d.getLatitude(), d.getLongitude(), ngo.getLatitude(), ngo.getLongitude()) : null;
            double dist = km == null ? 0.3 : Math.max(0, 1 - km / 30.0);
            double cap = ngo.getCapacityKg() == null ? 0.5 : Math.min(1, ngo.getCapacityKg() / Math.max(d.getQuantityKg(), 0.1));
            String types = ngo.getAcceptedFoodTypes();
            boolean typeOk = types == null || types.isBlank() || types.toLowerCase().contains(d.getFoodCategory().toLowerCase());
            double score = 0.40 * dist + 0.25 * cap + 0.20 * (typeOk ? 1 : 0.2) + 0.15 * (ngo.isAvailable() ? 1 : 0);
            out.add(new Match(ngo.getId(), ngo.getOrgName() != null ? ngo.getOrgName() : ngo.getName(), ngo.getAddress(),
                    km == null ? null : Math.round(km * 10) / 10.0, Math.round(score * 1000) / 1000.0));
        }
        out.sort(Comparator.comparingDouble(Match::score).reversed());
        return out.subList(0, Math.min(n, out.size()));
    }
}
