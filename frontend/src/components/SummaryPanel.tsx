import type {  TRouteResult, TRouteMode  } from '../types/route.types';

interface ISummaryPanelProps {
  result: TRouteResult;
  selectedSegment: number | null;
  onSegmentClick: (index: number) => void;
}

function formatDistance(km: number): string {
  return km < 1 ? `${Math.round(km * 1000)} m` : `${km.toFixed(2)} km`;
}

function formatDuration(min: number): string {
  if (min < 1) return `${Math.round(min * 60)} sec`;
  if (min < 60) return `${Math.round(min)} min`;
  const hours = Math.floor(min / 60);
  const mins = Math.round(min % 60);
  return `${hours}h ${mins}min`;
}

function modeLabel(mode: TRouteMode): string {
  return mode === 'ROUND_TRIP' ? '🔄 Round Trip' : '➡️ Open Route';
}

export default function SummaryPanel({ result, selectedSegment, onSegmentClick }: ISummaryPanelProps) {
  return (
    <div className="rw-summary-panel" id="summary-panel">
      <h3 className="rw-panel-title">
        <span className="rw-icon">📊</span> Route Summary
      </h3>

      {/* Main metrics */}
      <div className="rw-metrics-grid">
        <div className="rw-metric-card">
          <span className="rw-metric-label">Distance</span>
          <span className="rw-metric-value">
            {formatDistance(result.totalDistanceKm)}
          </span>
        </div>
        <div className="rw-metric-card">
          <span className="rw-metric-label">Duration</span>
          <span className="rw-metric-value">
            {formatDuration(result.totalDurationMin)}
          </span>
        </div>
        <div className="rw-metric-card rw-metric-wide">
          <span className="rw-metric-label">Mode</span>
          <span className="rw-metric-value rw-metric-mode">
            {modeLabel(result.routeMode)}
          </span>
        </div>
      </div>

      {/* Per-segment breakdown */}
      {result.segments && result.segments.length > 0 && (() => {
        const total = result.orderedWaypoints.length;
        const isFixedEnds = result.routeMode === 'FIXED_START_END' || result.routeMode === 'OPEN_ROUTE';

        /** Returns the visual label matching the map marker for a given optimized position */
        const posLabel = (pos: number): string => {
          if (isFixedEnds && pos === 0) return 'A';
          if (isFixedEnds && pos === total - 1) return 'B';
          return String(pos + 1);
        };

        return (
          <div className="rw-segments">
            <h4 className="rw-segments-title">Segments</h4>
            <ul className="rw-segment-list" role="list" aria-label="Route segments">
              {result.segments.map((seg) => (
                <li 
                  key={seg.segmentIndex} 
                  className={`rw-segment-item ${selectedSegment === seg.segmentIndex ? 'rw-segment-item-selected' : ''}`}
                  onClick={() => onSegmentClick(seg.segmentIndex)}
                >
                  <span
                    className="rw-segment-color"
                    style={{ backgroundColor: seg.color }}
                    aria-hidden="true"
                  />
                  <span className="rw-segment-label">
                    {posLabel(seg.segmentIndex)} → {posLabel(seg.segmentIndex + 1)}
                  </span>
                  <span className="rw-segment-detail">
                    {formatDistance(seg.distanceKm)}
                  </span>
                  <span className="rw-segment-detail rw-segment-duration">
                    {formatDuration(seg.durationMin)}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        );
      })()}

      {/* OSRM validation */}
      {result.osrmValidation && result.osrmValidation.status === 'SUCCESS' && (
        <div className="rw-osrm-validation">
          <h4 className="rw-segments-title">OSRM Validation</h4>
          <div className="rw-osrm-grid">
            <span className="rw-osrm-label">Distance:</span>
            <span className="rw-osrm-value">
              {formatDistance(result.osrmValidation.distanceKm)}
            </span>
            <span className="rw-osrm-label">Duration:</span>
            <span className="rw-osrm-value">
              {formatDuration(result.osrmValidation.durationMin)}
            </span>
          </div>
          <span className="rw-osrm-badge rw-osrm-success">✓ Validated</span>
        </div>
      )}
    </div>
  );
}
