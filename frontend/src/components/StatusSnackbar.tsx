import { useEffect, useState } from 'react';
import type {  TComputationStatus  } from '../types/route.types';

interface IStatusSnackbarProps {
  status: TComputationStatus;
  error: string | null;
}

const STATUS_MESSAGES: Record<TComputationStatus, string> = {
  IDLE: '',
  SUBMITTING: 'Submitting route request...',
  PROCESSING: 'Computing optimal route...',
  COMPLETED: 'Route computed successfully!',
  ERROR: 'Route computation failed.',
};

export default function StatusSnackbar({ status, error }: IStatusSnackbarProps) {
  const [visible, setVisible] = useState(false);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    if (status === 'IDLE') {
      setVisible(false);
      setDismissed(false);
      return;
    }

    setVisible(true);
    setDismissed(false);

    // Auto-dismiss COMPLETED after 4 seconds
    if (status === 'COMPLETED') {
      const timer = setTimeout(() => {
        setDismissed(true);
        setTimeout(() => setVisible(false), 300); // fade-out duration
      }, 4000);
      return () => clearTimeout(timer);
    }
  }, [status]);

  if (!visible || status === 'IDLE') return null;

  const statusClass = `rw-snackbar rw-snackbar-${status.toLowerCase()} ${
    dismissed ? 'rw-snackbar-exit' : 'rw-snackbar-enter'
  }`;

  return (
    <div className={statusClass} role="status" aria-live="polite" id="status-snackbar">
      <div className="rw-snackbar-content">
        {(status === 'SUBMITTING' || status === 'PROCESSING') && (
          <span className="rw-snackbar-spinner" aria-hidden="true" />
        )}
        {status === 'COMPLETED' && (
          <span className="rw-snackbar-icon" aria-hidden="true">✓</span>
        )}
        {status === 'ERROR' && (
          <span className="rw-snackbar-icon rw-snackbar-error-icon" aria-hidden="true">✕</span>
        )}
        <span className="rw-snackbar-text">
          {status === 'ERROR' && error ? error : STATUS_MESSAGES[status]}
        </span>
      </div>
    </div>
  );
}
