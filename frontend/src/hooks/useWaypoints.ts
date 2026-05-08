import { useState, useCallback } from 'react';
import type {  TWaypoint  } from '../types/route.types';

const MAX_WAYPOINTS = 10;
const MIN_WAYPOINTS = 2;

interface IUseWaypointsReturn {
  waypoints: TWaypoint[];
  addWaypoint: (wp: TWaypoint) => boolean;
  removeWaypoint: (index: number) => void;
  clearWaypoints: () => void;
  canEstimate: boolean;
  canAddMore: boolean;
  waypointCount: number;
}

/**
 * Manages the waypoint list state with add/remove/clear operations.
 * Enforces 2–10 waypoint bounds.
 */
export function useWaypoints(): IUseWaypointsReturn {
  const [waypoints, setWaypoints] = useState<TWaypoint[]>([]);

  const addWaypoint = useCallback((wp: TWaypoint): boolean => {
    let added = false;
    setWaypoints(prev => {
      if (prev.length >= MAX_WAYPOINTS) {
        return prev;
      }
      added = true;
      return [...prev, wp];
    });
    return added;
  }, []);

  const removeWaypoint = useCallback((index: number) => {
    setWaypoints(prev => prev.filter((_, i) => i !== index));
  }, []);

  const clearWaypoints = useCallback(() => {
    setWaypoints([]);
  }, []);

  return {
    waypoints,
    addWaypoint,
    removeWaypoint,
    clearWaypoints,
    canEstimate: waypoints.length >= MIN_WAYPOINTS,
    canAddMore: waypoints.length < MAX_WAYPOINTS,
    waypointCount: waypoints.length,
  };
}
