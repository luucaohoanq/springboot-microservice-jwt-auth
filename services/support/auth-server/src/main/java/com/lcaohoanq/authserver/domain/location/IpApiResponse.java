package com.lcaohoanq.authserver.domain.location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IpApiResponse {
    private String status;
    private String country;
    private String city;
    private Double lat;
    private Double lon;
}
