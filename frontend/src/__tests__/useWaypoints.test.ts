import { renderHook, act } from '@testing-library/react';
import { useWaypoints } from '../hooks/useWaypoints';
import { describe, it, expect } from 'vitest';

describe('useWaypoints', () => {
  it('should initialize with empty waypoints', () => {
    const { result } = renderHook(() => useWaypoints());
    expect(result.current.waypoints).toEqual([]);
    expect(result.current.canEstimate).toBe(false);
    expect(result.current.canAddMore).toBe(true);
  });

  it('should add a waypoint', () => {
    const { result } = renderHook(() => useWaypoints());
    
    act(() => {
      result.current.addWaypoint({ lat: -23.5, lng: -46.6 });
    });
    
    expect(result.current.waypoints).toHaveLength(1);
    expect(result.current.waypoints[0]).toEqual({ lat: -23.5, lng: -46.6 });
    expect(result.current.canEstimate).toBe(false); // need 2
  });

  it('should allow estimation with 2 waypoints', () => {
    const { result } = renderHook(() => useWaypoints());
    
    act(() => {
      result.current.addWaypoint({ lat: -23.5, lng: -46.6 });
      result.current.addWaypoint({ lat: -23.6, lng: -46.7 });
    });
    
    expect(result.current.canEstimate).toBe(true);
  });

  it('should enforce maximum 10 waypoints', () => {
    const { result } = renderHook(() => useWaypoints());
    
    act(() => {
      for (let i = 0; i < 12; i++) {
        result.current.addWaypoint({ lat: i, lng: i });
      }
    });
    
    expect(result.current.waypoints).toHaveLength(10);
    expect(result.current.canAddMore).toBe(false);
  });

  it('should remove a waypoint by index', () => {
    const { result } = renderHook(() => useWaypoints());
    
    act(() => {
      result.current.addWaypoint({ lat: 1, lng: 1 });
      result.current.addWaypoint({ lat: 2, lng: 2 });
      result.current.addWaypoint({ lat: 3, lng: 3 });
    });
    
    act(() => {
      result.current.removeWaypoint(1); // Remove the middle one
    });
    
    expect(result.current.waypoints).toHaveLength(2);
    expect(result.current.waypoints[0].lat).toBe(1);
    expect(result.current.waypoints[1].lat).toBe(3);
  });

  it('should clear all waypoints', () => {
    const { result } = renderHook(() => useWaypoints());
    
    act(() => {
      result.current.addWaypoint({ lat: 1, lng: 1 });
      result.current.addWaypoint({ lat: 2, lng: 2 });
    });
    
    act(() => {
      result.current.clearWaypoints();
    });
    
    expect(result.current.waypoints).toHaveLength(0);
    expect(result.current.canEstimate).toBe(false);
  });
});
