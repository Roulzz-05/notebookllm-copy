import React, { useEffect } from 'react'
import { FileText, Layers, Trash2 } from 'lucide-react'
import useAppStore from '../store/useAppStore'
import UploadZone from './UploadZone'
import { getAllDocuments, deleteDocument } from '../api/client'

const STATUS_DOTS = {
  READY: '',
  PROCESSING: 'pulse',
  UPLOADING: 'pulse',
  FAILED: '',
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

export default function DocumentPanel() {
  const { documents, selectedDocId, selectDocument, setDocuments, removeDocument, addToast } = useAppStore()
 
  const handleDelete = async (e, id) => {
    e.stopPropagation()
    if (!window.confirm('Are you sure you want to delete this document and all its study sessions?')) return
    try {
      await deleteDocument(id)
      removeDocument(id)
      addToast('Document deleted', 'success')
    } catch {
      addToast('Failed to delete document', 'error')
    }
  }

  useEffect(() => {
    getAllDocuments()
      .then(({ data }) => setDocuments(data))
      .catch(() => {}) // backend might not be running yet
  }, [setDocuments])

  return (
    <div className="panel panel-left">
      <div className="panel-header">
        <span className="panel-title">
          <Layers size={13} />
          Documents
        </span>
        {documents.length > 0 && (
          <span className="badge doc-count-badge">{documents.length}</span>
        )}
      </div>

      <div className="panel-content" style={{ padding: 0 }}>
        <UploadZone />

        {documents.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">
              <FileText size={22} />
            </div>
            <p>Upload a PDF to get started with AI-powered study sessions</p>
          </div>
        ) : (
          <div className="doc-list" style={{ padding: '0 0.75rem 0.75rem' }}>
            {documents.map((doc) => (
              <div
                key={doc.id}
                className={`doc-item ${selectedDocId === doc.id ? 'active' : ''}`}
                onClick={() => selectDocument(doc.id)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => e.key === 'Enter' && selectDocument(doc.id)}
                aria-label={`Select document ${doc.filename}`}
              >
                <div className="doc-icon">
                  <FileText size={16} />
                </div>
                <div className="doc-info">
                  <div className="doc-name" title={doc.filename}>{doc.filename}</div>
                  <div className="doc-meta">
                    <span className={`status-badge ${doc.status}`}>
                      <span className={`status-dot ${STATUS_DOTS[doc.status] || ''}`} />
                      {doc.status}
                    </span>
                    {doc.totalChunks > 0 && (
                      <span>{doc.totalChunks} chunks</span>
                    )}
                  </div>
                  {doc.uploadedAt && (
                    <div className="doc-meta" style={{ marginTop: '0.125rem' }}>
                      {formatDate(doc.uploadedAt)}
                    </div>
                  )}
                </div>
 
                <button 
                  className="doc-delete-btn"
                  onClick={(e) => handleDelete(e, doc.id)}
                  title="Delete source"
                >
                  <Trash2 size={13} />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
