package com.lcaohoanq.authserver.domain.location;

public interface GeoLocationService {

    GeoLocation getLocationFromIp(String ipAddress);

}
