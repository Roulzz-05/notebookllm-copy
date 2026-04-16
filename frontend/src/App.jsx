import React, { Suspense, lazy } from 'react'
import Header from './components/Header'
import DocumentPanel from './components/DocumentPanel'
import StudyPanel from './components/StudyPanel'
import ToastContainer from './components/ToastContainer'
import ChatPanelContent from './components/ChatPanelContent'
import useAppStore from './store/useAppStore'

// Lazy-load ReactFlow to keep initial bundle small
const StudyFlowView = lazy(() => import('./components/StudyFlowView'))

function CenterPanel() {
  const activeView = useAppStore((s) => s.activeView)
  const topics = useAppStore((s) => s.topics)
  const selectedDocId = useAppStore((s) => s.selectedDocId)
  const setActiveView = useAppStore((s) => s.setActiveView)
  const documents = useAppStore((s) => s.documents)
  const selectedDoc = documents.find((d) => d.id === selectedDocId)

  return (
    <div className="panel panel-center">
      {/* Panel header */}
      <div className="panel-header">
        <span className="panel-title">
          {activeView === 'chat' ? (
            <>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
              Chat
            </>
          ) : (
            <>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/>
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
              </svg>
              Mind Map
            </>
          )}
        </span>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          {selectedDoc && (
            <span className="badge" style={{ maxWidth: 160, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
              title={selectedDoc.filename}>
              {selectedDoc.filename}
            </span>
          )}
          {/* View toggle tabs */}
          {topics.length > 0 && selectedDocId && (
            <div style={{ display: 'flex', gap: '0.125rem', background: 'var(--bg-base)', borderRadius: 'var(--radius-sm)', padding: '0.125rem' }}>
              <button
                id="view-tab-chat"
                onClick={() => setActiveView('chat')}
                style={{
                  padding: '0.25rem 0.625rem',
                  borderRadius: 6,
                  border: 'none',
                  background: activeView === 'chat' ? 'var(--bg-elevated)' : 'transparent',
                  color: activeView === 'chat' ? 'var(--accent-primary)' : 'var(--text-muted)',
                  fontSize: '0.75rem',
                  fontWeight: 500,
                  cursor: 'pointer',
                  transition: 'all 150ms',
                }}
              >
                💬 Chat
              </button>
              <button
                id="view-tab-flow"
                onClick={() => setActiveView('flow')}
                style={{
                  padding: '0.25rem 0.625rem',
                  borderRadius: 6,
                  border: 'none',
                  background: activeView === 'flow' ? 'var(--bg-elevated)' : 'transparent',
                  color: activeView === 'flow' ? 'var(--accent-primary)' : 'var(--text-muted)',
                  fontSize: '0.75rem',
                  fontWeight: 500,
                  cursor: 'pointer',
                  transition: 'all 150ms',
                }}
              >
                🗺️ Map
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Content area */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        {activeView === 'chat' ? (
          <ChatPanelContent />
        ) : (
          <Suspense fallback={
            <div className="loading-overlay">
              <div className="spinner spinner-lg" />
              <p>Loading mind map…</p>
            </div>
          }>
            <StudyFlowView topics={topics} />
          </Suspense>
        )}
      </div>
    </div>
  )
}

export default function App() {
  return (
    <div className="app-container">
      <Header />
      <div className="app-body">
        <DocumentPanel />
        <CenterPanel />
        <StudyPanel />
      </div>
      <ToastContainer />
    </div>
  )
}
