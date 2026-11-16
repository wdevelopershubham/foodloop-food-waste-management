package com.foodloop.foodloop_backend.common;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping
    public Map<String, String> health() { return Map.of("status", "UP", "app", "FoodLoop"); }
}
