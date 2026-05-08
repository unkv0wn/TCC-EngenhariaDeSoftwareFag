import type {  TWaypoint  } from '../types/route.types';

interface IWaypointPanelProps {
  waypoints: TWaypoint[];
  onRemove: (index: number) => void;
  disabled: boolean;
}

export default function WaypointPanel({
  waypoints,
  onRemove,
  disabled,
}: IWaypointPanelProps) {
  if (waypoints.length === 0) {
    return (
      <div className="rw-waypoint-panel" id="waypoint-panel">
        <h3 className="rw-panel-title">
          <span className="rw-icon">📍</span> Waypoints
        </h3>
        <p className="rw-empty-msg">
          Click on the map to place waypoints.
          <br />
          <span className="rw-subtle">Minimum 2, maximum 10.</span>
        </p>
      </div>
    );
  }

  return (
    <div className="rw-waypoint-panel" id="waypoint-panel">
      <h3 className="rw-panel-title">
        <span className="rw-icon">📍</span> Waypoints
        <span className="rw-badge">{waypoints.length}/10</span>
      </h3>
      <ul className="rw-waypoint-list" role="list" aria-label="Waypoint list">
        {waypoints.map((wp, idx) => (
          <li key={idx} className="rw-waypoint-item">
            <div className="rw-waypoint-number">{idx + 1}</div>
            <div className="rw-waypoint-coords">
              <span>{wp.lat.toFixed(5)}</span>
              <span className="rw-coord-sep">,</span>
              <span>{wp.lng.toFixed(5)}</span>
            </div>
            <button
              className="rw-remove-btn"
              onClick={() => onRemove(idx)}
              disabled={disabled}
              aria-label={`Remove waypoint ${idx + 1}`}
              title="Remove waypoint"
              id={`remove-waypoint-${idx}`}
            >
              ✕
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
