package com.ghanaride.model;

import lombok.Data;

@Data
public class LocationUpdate {
    private String tripId;
    private String driverId;
    private double latitude;
    private double longitude;
    private double heading;
    private double speed;
}
