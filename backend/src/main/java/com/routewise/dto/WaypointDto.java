package com.routewise.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * A single geographic coordinate supplied by the user.
 *
 * @param lat latitude  in degrees [-90,  90]
 * @param lng longitude in degrees [-180, 180]
 */
public record WaypointDto(

  @NotNull(message = "Latitude must not be null")
  @DecimalMin(value = "-90.0",  message = "Latitude must be >= -90")
  @DecimalMax(value = "90.0",   message = "Latitude must be <= 90")
  Double lat,

  @NotNull(message = "Longitude must not be null")
  @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
  @DecimalMax(value = "180.0",  message = "Longitude must be <= 180")
  Double lng

) {}
