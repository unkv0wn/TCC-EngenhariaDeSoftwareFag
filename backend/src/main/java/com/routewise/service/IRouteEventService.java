package com.routewise.service;

import com.routewise.dto.RouteEventDto;
import com.routewise.dto.RouteEventRequestDto;

import java.util.List;
import java.util.UUID;

public interface IRouteEventService {

  List<RouteEventDto> findByRoute(UUID routeId);

  RouteEventDto create(UUID routeId, RouteEventRequestDto request);

  void delete(UUID routeId, UUID id);
}
