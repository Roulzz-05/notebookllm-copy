import React from 'react'
import { BookOpen, Zap, Github } from 'lucide-react'

export default function Header() {
  return (
    <header className="app-header">
      <a className="logo" href="/" aria-label="StudyAI home">
        <div className="logo-icon" aria-hidden="true">
          <BookOpen size={16} color="white" />
        </div>
        <span className="logo-text">StudyAI</span>
      </a>

      <div className="header-spacer" />

      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <span style={{
          fontSize: '0.75rem',
          color: 'var(--text-muted)',
          display: 'flex',
          alignItems: 'center',
          gap: '0.375rem',
          padding: '0.25rem 0.625rem',
          background: 'rgba(108,99,255,0.08)',
          borderRadius: 'var(--radius-full)',
          border: '1px solid var(--border-accent)',
        }}>
          <Zap size={11} color="var(--accent-primary)" />
          Powered by Gemini
        </span>
      </div>
    </header>
  )
}
