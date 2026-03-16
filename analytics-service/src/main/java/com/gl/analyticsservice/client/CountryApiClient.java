package com.gl.analyticsservice.client;

import com.gl.analyticsservice.dto.CountryInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CountryApiClient {

    private final WebClient webClient;

    public CountryApiClient(WebClient.Builder webClientBuilder,
                            @Value("${restcountries.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public CountryInfo getCountry(String country) {
        if (country == null || country.isBlank()) {
            return CountryInfo.empty();
        }
        try {
            List<Map> response = webClient.get()
                    .uri("/name/{country}?fullText=false", country)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .collectList()
                    .block(Duration.ofSeconds(3));

            if (response == null || response.isEmpty()) {
                return CountryInfo.empty();
            }

            Map<String, Object> first = response.get(0);
            Map<String, Object> name = (Map<String, Object>) first.get("name");
            List<String> timezones = (List<String>) first.getOrDefault("timezones", List.of("unknown"));
            Map<String, Object> currencies = (Map<String, Object>) first.getOrDefault("currencies", Map.of());

            return new CountryInfo(
                    name != null ? (String) name.get("common") : "unknown",
                    (String) first.getOrDefault("region", "unknown"),
                    (String) first.getOrDefault("subregion", "unknown"),
                    ((Number) first.getOrDefault("population", 0)).longValue(),
                    timezones.isEmpty() ? "unknown" : timezones.get(0),
                    currencies.isEmpty() ? "unknown" : currencies.keySet().iterator().next()
            );

        } catch (Exception e) {
            log.warn("Country API call failed for {}: {}", country, e.getMessage());
            return CountryInfo.empty();
        }
    }



}
