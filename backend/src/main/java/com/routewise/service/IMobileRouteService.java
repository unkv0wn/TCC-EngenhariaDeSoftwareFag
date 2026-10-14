package com.routewise.service;

import com.routewise.dto.MobileEventBatchResultDto;
import com.routewise.dto.MobileEventRequestDto;
import com.routewise.dto.MobileRouteDto;
import com.routewise.dto.RouteEventDto;

import java.util.List;

public interface IMobileRouteService {

  MobileRouteDto findByCode(String accessCode);

  RouteEventDto createEvent(String accessCode, MobileEventRequestDto request);

  List<MobileEventBatchResultDto> createEvents(String accessCode, List<MobileEventRequestDto> requests);
}
