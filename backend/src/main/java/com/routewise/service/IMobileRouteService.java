package com.routewise.service;

import com.routewise.dto.MobileEventBatchResultDto;
import com.routewise.dto.MobileEventRequestDto;
import com.routewise.dto.MobileRouteDto;
import com.routewise.dto.RouteEventDto;

import java.util.List;

public interface IMobileRouteService {

  MobileRouteDto findByToken(String accessToken);

  RouteEventDto createEvent(String accessToken, MobileEventRequestDto request);

  List<MobileEventBatchResultDto> createEvents(String accessToken, List<MobileEventRequestDto> requests);
}
