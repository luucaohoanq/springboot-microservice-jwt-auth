package com.lcaohoanq.authserver.domain.location;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeoLocationServiceImpl implements GeoLocationService {

    private final WebClient geoWebClient;

    @Override
    public GeoLocation getLocationFromIp(String ip) {
        // Bỏ qua localhost
        if ("127.0.0.1".equals(ip) || "::1".equals(ip)) {
            return GeoLocation.builder()
                .ip(ip)
                .country("Localhost")
                .city("Localhost")
                .lat(0.0)
                .lon(0.0)
                .build();
        }

        try {
            IpApiResponse response = geoWebClient.get()
                .uri("/json/{ip}", ip)
                .retrieve()
                .bodyToMono(IpApiResponse.class)
                .block();

            if (response == null || !"success".equalsIgnoreCase(response.getStatus())) {
                throw new RuntimeException("Failed to retrieve location for IP: " + ip);
            }

            return GeoLocation.builder()
                .ip(ip)
                .country(response.getCountry() != null ? response.getCountry() : "Unknown")
                .city(response.getCity() != null ? response.getCity() : "Unknown")
                .lat(response.getLat() != null ? response.getLat() : 0.0)
                .lon(response.getLon() != null ? response.getLon() : 0.0)
                .build();
        } catch (Exception e) {
            log.warn("Geo IP lookup failed for IP: {}", ip, e);
            return GeoLocation.builder()
                .ip(ip)
                .country("Unknown")
                .city("Unknown")
                .lat(0.0)
                .lon(0.0)
                .build();
        }
    }
}
