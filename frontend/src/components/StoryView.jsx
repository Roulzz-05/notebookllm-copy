import React from 'react'
import { BookOpen, X } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import useAppStore from '../store/useAppStore'

export default function StoryView() {
  const { activeStory, setActiveStory } = useAppStore()

  if (!activeStory) return null

  return (
    <div className="story-container">
      <div className="story-header" style={{ position: 'sticky', top: 0, zIndex: 10, background: 'var(--bg-elevated)', borderBottom: '1px solid var(--border-normal)', padding: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <BookOpen className="text-accent" size={20} />
          <span style={{ fontWeight: 600, fontSize: '1.1rem' }}>AI Storytime</span>
        </div>
        <button 
          className="btn btn-ghost btn-sm btn-icon" 
          onClick={() => setActiveStory(null)}
        >
          <X size={20} />
        </button>
      </div>
      
      <div className="story-content-area" style={{ padding: '2rem', maxWidth: '75ch', margin: '0 auto', fontSize: '1.05rem', lineHeight: '1.8', color: 'var(--text-normal)' }}>
        <ReactMarkdown 
          components={{
            h1: ({node, ...props}) => <h1 style={{ fontSize: '2.2rem', marginBottom: '1.5rem', color: 'var(--text-strong)', borderBottom: '2px solid var(--border-subtle)', paddingBottom: '0.5rem' }} {...props} />,
            h2: ({node, ...props}) => <h2 style={{ fontSize: '1.8rem', marginTop: '2rem', marginBottom: '1rem', color: 'var(--text-strong)' }} {...props} />,
            h3: ({node, ...props}) => <h3 style={{ fontSize: '1.4rem', marginTop: '1.5rem', marginBottom: '0.8rem', color: 'var(--text-strong)' }} {...props} />,
            p: ({node, ...props}) => <p style={{ marginBottom: '1.2rem', color: 'var(--text-normal)' }} {...props} />,
            strong: ({node, ...props}) => <strong style={{ color: 'var(--accent-primary)', fontWeight: 600 }} {...props} />,
            ul: ({node, ...props}) => <ul style={{ marginBottom: '1.5rem', paddingLeft: '2rem' }} {...props} />,
            li: ({node, ...props}) => <li style={{ marginBottom: '0.5rem' }} {...props} />,
            blockquote: ({node, ...props}) => <blockquote style={{ borderLeft: '4px solid var(--accent-primary)', paddingLeft: '1rem', fontStyle: 'italic', color: 'var(--text-muted)', margin: '1.5rem 0' }} {...props} />
          }}
        >
          {activeStory.content}
        </ReactMarkdown>
      </div>

      <div className="story-footer" style={{ borderTop: '1px solid var(--border-normal)', padding: '1.5rem', textAlign: 'center', background: 'var(--bg-elevated)' }}>
        <button className="btn btn-primary" onClick={() => setActiveStory(null)}>
          Close Story
        </button>
      </div>
    </div>
  )
}
