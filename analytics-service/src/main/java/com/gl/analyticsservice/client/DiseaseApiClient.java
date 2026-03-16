package com.gl.analyticsservice.client;

import com.gl.analyticsservice.dto.DiseaseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DiseaseApiClient {
    private final WebClient webClient;

    public DiseaseApiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://en.wikipedia.org/api/rest_v1")
                .build();
    }

    public DiseaseInfo getDisease(String diagnosis){
        if(diagnosis == null || diagnosis.isBlank()){
            return DiseaseInfo.empty();
        }
        try {
            Map response = webClient.get()
                    .uri("/page/summary/{disease}", diagnosis)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(3));

            if(response == null ){
                return DiseaseInfo.empty();
            }

            return new DiseaseInfo(
                    (String) response.getOrDefault("name", "unknown"),
                    (String) response.getOrDefault("description", "")
            );
        }catch (Exception e){
            log.warn("Disease API call failed for {}: {}", diagnosis, e.getMessage());
            return DiseaseInfo.empty();
        }
    }


//    private List<String> getList(Map<String, Object> map, String key) {
//        Object val = map.get(key);
//        if(val instanceof List) return (List<String>) val;
//        return List.of();
//    }


}
