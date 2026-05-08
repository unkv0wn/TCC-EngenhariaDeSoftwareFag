package com.routewise.dto.osrm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Deserialised response from the OSRM {@code /table/v1/driving} endpoint.
 *
 * <p>The OSRM table API returns a matrix of travel durations (seconds) and,
 * optionally, distances (metres) between all source/destination pairs.
 *
 * @param code      "Ok" on success, otherwise an OSRM error code
 * @param durations N×N matrix of durations in seconds  (null if not requested)
 * @param distances N×N matrix of distances in metres   (null if not requested)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmTableResponse(
  String code,
  double[][] durations,
  double[][] distances
) {}
