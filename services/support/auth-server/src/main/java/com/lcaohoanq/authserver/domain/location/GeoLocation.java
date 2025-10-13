package com.lcaohoanq.authserver.domain.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class GeoLocation {
    private String ip;
    private String country;
    private String city;
    private Double lat;
    private Double lon;
}
