import { useState, useCallback } from 'react';
import { useWaypoints } from './hooks/useWaypoints';
import { useSseListener } from './hooks/useSseListener';
import { submitRoute } from './services/routeApi';
import type { TRouteMode, TWaypoint } from './types/route.types';
import MapView from './components/MapView';
import WaypointPanel from './components/WaypointPanel';
import RouteControls from './components/RouteControls';
import SummaryPanel from './components/SummaryPanel';
import StatusSnackbar from './components/StatusSnackbar';
import './App.css';

function App() {
  const {
    waypoints,
    addWaypoint,
    removeWaypoint,
    clearWaypoints,
    canEstimate,
  } = useWaypoints();

  const [routeMode, setRouteMode] = useState<TRouteMode>('ROUND_TRIP');
  
  const {
    status,
    result,
    error,
    isComputing,
    connect,
    reset: resetSse,
  } = useSseListener();

  const [selectedSegment, setSelectedSegment] = useState<number | null>(null);

  const handleMapClick = useCallback((wp: TWaypoint) => {
    // Only allow adding waypoints if not computing and result is not shown
    if (!isComputing && !result) {
      addWaypoint(wp);
    }
  }, [addWaypoint, isComputing, result]);

  const handleEstimate = useCallback(async () => {
    if (!canEstimate || isComputing) return;

    try {
      setSelectedSegment(null);
      // 1. Send POST request to get requestId
      const response = await submitRoute({ waypoints, routeMode });
      
      // 2. Connect to SSE stream using requestId
      connect(response.requestId);
    } catch (err) {
      console.error('Failed to submit route request:', err);
      alert(err instanceof Error ? err.message : 'Failed to submit route request');
    }
  }, [waypoints, routeMode, canEstimate, isComputing, connect]);

  const handleReset = useCallback(() => {
    clearWaypoints();
    resetSse();
    setSelectedSegment(null);
  }, [clearWaypoints, resetSse]);

  return (
    <div className="rw-app-container">
      {/* Map Area */}
      <main className="rw-main">
        <MapView
          waypoints={waypoints}
          result={result}
          routeMode={routeMode}
          onMapClick={handleMapClick}
          isComputing={isComputing}
          selectedSegment={selectedSegment}
          onSegmentClick={setSelectedSegment}
        />
      </main>

      {/* Sidebar Area */}
      <aside className="rw-sidebar">
        <header className="rw-header">
          <h1 className="rw-brand">
            <span className="rw-brand-icon">🗺️</span>
            RouteWise
          </h1>
          <p className="rw-brand-tagline">Urban Route Optimizer</p>
        </header>

        <div className="rw-sidebar-content">
          <WaypointPanel
            waypoints={waypoints}
            onRemove={removeWaypoint}
            disabled={isComputing || result !== null}
          />
          
          <RouteControls
            routeMode={routeMode}
            onRouteModeChange={setRouteMode}
            onEstimate={handleEstimate}
            onReset={handleReset}
            canEstimate={canEstimate}
            isComputing={isComputing}
            hasResult={result !== null}
          />

          {result && (
            <SummaryPanel 
              result={result} 
              selectedSegment={selectedSegment}
              onSegmentClick={setSelectedSegment}
            />
          )}
        </div>
      </aside>

      {/* Floating Status / Error notifications */}
      <StatusSnackbar status={status} error={error} />
    </div>
  );
}

export default App;
