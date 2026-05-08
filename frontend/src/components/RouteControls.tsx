import type {  TRouteMode  } from '../types/route.types';

interface IRouteControlsProps {
  routeMode: TRouteMode;
  onRouteModeChange: (mode: TRouteMode) => void;
  onEstimate: () => void;
  onReset: () => void;
  canEstimate: boolean;
  isComputing: boolean;
  hasResult: boolean;
}

export default function RouteControls({
  routeMode,
  onRouteModeChange,
  onEstimate,
  onReset,
  canEstimate,
  isComputing,
  hasResult,
}: IRouteControlsProps) {
  return (
    <div className="rw-controls" id="route-controls">
      <h3 className="rw-panel-title">
        <span className="rw-icon">⚙️</span> Route Options
      </h3>

      {/* Route options checkboxes */}
      <div className="rw-options-group" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1rem' }}>
        <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', color: 'var(--rw-text-secondary)' }}>
          <input 
            type="checkbox" 
            checked={routeMode === 'FIXED_START_END'}
            disabled={isComputing}
            onChange={(e) => onRouteModeChange(e.target.checked ? 'FIXED_START_END' : 'OPEN_ROUTE')}
          />
          Adicionar ponto inicial e final da rota
        </label>
      </div>

      {/* Action buttons */}
      <div className="rw-action-buttons">
        <button
          className="rw-btn rw-btn-primary"
          onClick={onEstimate}
          disabled={!canEstimate || isComputing}
          id="estimate-route-btn"
          aria-label="Estimate Route"
        >
          {isComputing ? (
            <>
              <span className="rw-spinner" aria-hidden="true" />
              Computing...
            </>
          ) : (
            <>
              <span className="rw-btn-icon">🧭</span>
              Estimate Route
            </>
          )}
        </button>

        {(hasResult || isComputing) && (
          <button
            className="rw-btn rw-btn-secondary"
            onClick={onReset}
            id="reset-btn"
            aria-label="Reset"
          >
            <span className="rw-btn-icon">↺</span>
            Reset
          </button>
        )}
      </div>
    </div>
  );
}
