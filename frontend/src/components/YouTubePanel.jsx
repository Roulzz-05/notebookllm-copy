import React from 'react'
import { Youtube, ExternalLink, X, Tv2, Tag } from 'lucide-react'
import useAppStore from '../store/useAppStore'

const CATEGORY_COLORS = {
  'Fundamentals': { bg: 'rgba(108,99,255,0.12)', color: 'var(--accent-primary)', border: 'rgba(108,99,255,0.3)' },
  'Deep Dive':    { bg: 'rgba(168,85,247,0.12)', color: '#a855f7',                border: 'rgba(168,85,247,0.3)' },
  'Visual':       { bg: 'rgba(6,182,212,0.12)',  color: 'var(--accent-tertiary)', border: 'rgba(6,182,212,0.3)'  },
  'Practice':     { bg: 'rgba(16,185,129,0.12)', color: 'var(--accent-success)',  border: 'rgba(16,185,129,0.3)' },
  'Overview':     { bg: 'rgba(245,158,11,0.12)', color: 'var(--accent-warning)',  border: 'rgba(245,158,11,0.3)' },
  'Advanced':     { bg: 'rgba(239,68,68,0.12)',  color: 'var(--accent-danger)',   border: 'rgba(239,68,68,0.3)'  },
}

function getCategoryStyle(category) {
  return CATEGORY_COLORS[category] || CATEGORY_COLORS['Fundamentals']
}

function RecCard({ rec, index }) {
  const youtubeSearchUrl = `https://www.youtube.com/results?search_query=${encodeURIComponent(rec.query)}`
  const catStyle = getCategoryStyle(rec.category)

  return (
    <a
      href={youtubeSearchUrl}
      target="_blank"
      rel="noopener noreferrer"
      id={`yt-rec-${index}`}
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '0.5rem',
        padding: '0.875rem 1rem',
        background: 'var(--bg-elevated)',
        border: '1px solid var(--border-subtle)',
        borderRadius: 'var(--radius-md)',
        textDecoration: 'none',
        transition: 'all 200ms cubic-bezier(0.4, 0, 0.2, 1)',
        cursor: 'pointer',
        position: 'relative',
        overflow: 'hidden',
      }}
      onMouseEnter={e => {
        e.currentTarget.style.borderColor = '#ff0000aa'
        e.currentTarget.style.background = 'rgba(255,0,0,0.04)'
        e.currentTarget.style.transform = 'translateY(-1px)'
        e.currentTarget.style.boxShadow = '0 4px 20px rgba(255,0,0,0.15)'
      }}
      onMouseLeave={e => {
        e.currentTarget.style.borderColor = 'var(--border-subtle)'
        e.currentTarget.style.background = 'var(--bg-elevated)'
        e.currentTarget.style.transform = 'translateY(0)'
        e.currentTarget.style.boxShadow = 'none'
      }}
    >
      {/* Top row: youtube icon + title + external link */}
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.625rem' }}>
        <div style={{
          width: 32, height: 32, borderRadius: 8, flexShrink: 0,
          background: 'rgba(255,0,0,0.1)', border: '1px solid rgba(255,0,0,0.2)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <Youtube size={16} color="#ff0000" />
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{
            fontSize: '0.875rem', fontWeight: 600,
            color: 'var(--text-primary)', lineHeight: 1.35,
            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          }}>
            {rec.title}
          </div>
        </div>
        <ExternalLink size={13} color="var(--text-muted)" style={{ flexShrink: 0, marginTop: 2 }} />
      </div>

      {/* Description */}
      <p style={{
        fontSize: '0.78rem', color: 'var(--text-secondary)',
        lineHeight: 1.5, margin: 0,
        display: '-webkit-box', WebkitLineClamp: 2,
        WebkitBoxOrient: 'vertical', overflow: 'hidden',
      }}>
        {rec.description}
      </p>

      {/* Bottom row: category badge + query preview */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '0.5rem' }}>
        <span style={{
          fontSize: '0.6875rem', fontWeight: 700, letterSpacing: '0.04em',
          padding: '0.125rem 0.5rem', borderRadius: '999px',
          background: catStyle.bg, color: catStyle.color, border: `1px solid ${catStyle.border}`,
        }}>
          {rec.category}
        </span>
        <span style={{
          fontSize: '0.7rem', color: 'var(--text-muted)',
          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '60%',
          fontFamily: 'var(--font-mono)',
        }}>
          "{rec.query}"
        </span>
      </div>
    </a>
  )
}

export default function YouTubePanel({ onClose }) {
  const { youtubeRecs } = useAppStore()

  return (
    <div style={{
      position: 'absolute', inset: 0,
      background: 'var(--bg-surface)',
      zIndex: 100, display: 'flex', flexDirection: 'column',
    }}>
      {/* Header */}
      <div style={{
        padding: '1rem 1.25rem',
        borderBottom: '1px solid var(--border-subtle)',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        flexShrink: 0,
        background: 'rgba(26,27,35,0.7)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
          <div style={{
            width: 32, height: 32, borderRadius: 8,
            background: 'rgba(255,0,0,0.12)', border: '1px solid rgba(255,0,0,0.25)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Tv2 size={16} color="#ff0000" />
          </div>
          <div>
            <div style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              YouTube Recommendations
            </div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
              {youtubeRecs.length} curated videos based on your document
            </div>
          </div>
        </div>
        <button
          onClick={onClose}
          style={{
            background: 'transparent', border: 'none',
            color: 'var(--text-muted)', cursor: 'pointer',
            padding: '0.25rem', display: 'flex', alignItems: 'center',
            borderRadius: 6, transition: 'color 150ms',
          }}
          onMouseEnter={e => e.currentTarget.style.color = 'var(--text-primary)'}
          onMouseLeave={e => e.currentTarget.style.color = 'var(--text-muted)'}
        >
          <X size={18} />
        </button>
      </div>

      {/* Legend */}
      <div style={{
        padding: '0.625rem 1.25rem',
        borderBottom: '1px solid var(--border-subtle)',
        display: 'flex', flexWrap: 'wrap', gap: '0.375rem', flexShrink: 0,
      }}>
        {Object.entries(CATEGORY_COLORS).map(([cat, style]) => (
          <span key={cat} style={{
            fontSize: '0.65rem', fontWeight: 600, letterSpacing: '0.03em',
            padding: '0.1rem 0.45rem', borderRadius: '999px',
            background: style.bg, color: style.color, border: `1px solid ${style.border}`,
          }}>
            {cat}
          </span>
        ))}
      </div>

      {/* Cards */}
      <div style={{
        flex: 1, overflowY: 'auto',
        padding: '0.875rem', display: 'flex', flexDirection: 'column', gap: '0.625rem',
      }}>
        {youtubeRecs.length === 0 ? (
          <div style={{
            display: 'flex', flexDirection: 'column', alignItems: 'center',
            justifyContent: 'center', height: '100%', gap: '1rem', color: 'var(--text-muted)',
          }}>
            <Youtube size={40} color="rgba(255,0,0,0.3)" />
            <p style={{ fontSize: '0.875rem', textAlign: 'center' }}>No recommendations available.</p>
          </div>
        ) : (
          youtubeRecs.map((rec, i) => <RecCard key={i} rec={rec} index={i} />)
        )}
      </div>

      {/* Footer */}
      <div style={{
        padding: '0.75rem 1.25rem',
        borderTop: '1px solid var(--border-subtle)',
        background: 'rgba(26,27,35,0.5)',
        fontSize: '0.72rem', color: 'var(--text-muted)', textAlign: 'center', flexShrink: 0,
      }}>
        Clicking a card opens YouTube search with an AI-crafted query specific to your document.
      </div>
    </div>
  )
}
