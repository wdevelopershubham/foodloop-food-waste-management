package com.foodloop.foodloop_backend.ml;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.LocalDate;
import java.util.Map;

@Service
public class MlClient {
    private final RestClient client;

    public MlClient(@Value("${ml.url}") String url) {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(2000); f.setReadTimeout(4000);
        this.client = RestClient.builder().baseUrl(url).requestFactory(f).build();
    }

    public Map<String, Object> predict(double preparedQty, boolean event, LocalDate date) {
        try {
            Map<?, ?> r = client.post().uri("/predict").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("day_of_week", date.getDayOfWeek().getValue() - 1, "month", date.getMonthValue(),
                             "event_flag", event ? 1 : 0, "prepared_qty", preparedQty))
                .retrieve().body(Map.class);
            return Map.of("predictedSurplusKg", r.get("predicted_surplus"), "source", "ml-model");
        } catch (Exception e) {
            return Map.of("predictedSurplusKg", Math.round(preparedQty * 0.12 * 100) / 100.0, "source", "fallback-estimate (ML service offline)");
        }
    }
}
