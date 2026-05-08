import { renderHook, act } from '@testing-library/react';
import { useSseListener } from '../hooks/useSseListener';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Mock EventSource globally
const mockClose = vi.fn();
const mockAddEventListener = vi.fn();

class MockEventSource {
  onmessage: null | ((event: any) => void) = null;
  onerror: null | ((event: any) => void) = null;
  close = mockClose;
  addEventListener = mockAddEventListener;
  constructor(public url: string) {}
}

describe('useSseListener', () => {
  let originalEventSource: any;

  beforeEach(() => {
    originalEventSource = globalThis.EventSource;
    (globalThis as any).EventSource = MockEventSource;
    vi.clearAllMocks();
  });

  afterEach(() => {
    (globalThis as any).EventSource = originalEventSource;
  });

  it('should initialize with IDLE status', () => {
    const { result } = renderHook(() => useSseListener());
    
    expect(result.current.status).toBe('IDLE');
    expect(result.current.isComputing).toBe(false);
    expect(result.current.result).toBeNull();
    expect(result.current.error).toBeNull();
  });

  it('should set status to PROCESSING and register listeners on connect', () => {
    const { result } = renderHook(() => useSseListener());
    
    act(() => {
      result.current.connect('test-uuid-123');
    });
    
    expect(result.current.status).toBe('PROCESSING');
    expect(result.current.isComputing).toBe(true);
    expect(mockAddEventListener).toHaveBeenCalledWith('PROCESSING', expect.any(Function));
    expect(mockAddEventListener).toHaveBeenCalledWith('COMPLETED', expect.any(Function));
    expect(mockAddEventListener).toHaveBeenCalledWith('ERROR', expect.any(Function));
  });

  it('should disconnect and cleanup correctly', () => {
    const { result } = renderHook(() => useSseListener());
    
    act(() => {
      result.current.connect('test-uuid-123');
    });
    
    act(() => {
      result.current.disconnect();
    });
    
    expect(mockClose).toHaveBeenCalledTimes(1);
  });
  
  it('should reset back to IDLE', () => {
    const { result } = renderHook(() => useSseListener());
    
    act(() => {
      result.current.connect('test-uuid-123');
    });
    
    act(() => {
      result.current.reset();
    });
    
    expect(mockClose).toHaveBeenCalledTimes(1);
    expect(result.current.status).toBe('IDLE');
    expect(result.current.isComputing).toBe(false);
  });
});
