import React, { useEffect } from 'react'
import { CheckCircle, AlertCircle, Info, X } from 'lucide-react'
import useAppStore from '../store/useAppStore'

const ICONS = {
  success: <CheckCircle size={16} color="var(--accent-success)" />,
  error: <AlertCircle size={16} color="var(--accent-danger)" />,
  info: <Info size={16} color="var(--accent-primary)" />,
}

function Toast({ toast }) {
  const { removeToast } = useAppStore()

  useEffect(() => {
    const t = setTimeout(() => removeToast(toast.id), 4000)
    return () => clearTimeout(t)
  }, [toast.id, removeToast])

  return (
    <div className={`toast ${toast.type}`} role="alert" aria-live="polite">
      <span className="toast-icon">{ICONS[toast.type] || ICONS.info}</span>
      <span className="toast-message">{toast.message}</span>
      <button
        className="btn btn-ghost btn-icon"
        onClick={() => removeToast(toast.id)}
        style={{ padding: '0.25rem', color: 'var(--text-muted)' }}
        aria-label="Dismiss notification"
      >
        <X size={13} />
      </button>
    </div>
  )
}

export default function ToastContainer() {
  const toasts = useAppStore((s) => s.toasts)

  if (toasts.length === 0) return null

  return (
    <div className="toast-container" aria-label="Notifications">
      {toasts.map((t) => (
        <Toast key={t.id} toast={t} />
      ))}
    </div>
  )
}
