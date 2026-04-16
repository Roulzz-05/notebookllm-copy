import React, { useCallback, useRef, useState, useEffect } from 'react'
import { UploadCloud } from 'lucide-react'
import useAppStore from '../store/useAppStore'
import { uploadDocument, getAllDocuments } from '../api/client'

export default function UploadZone() {
  const [dragging, setDragging] = useState(false)
  const [uploading, setUploading] = useState(false)
  const inputRef = useRef(null)
  const pollRef = useRef(null)
  const { addDocument, addToast, updateDocument } = useAppStore()

  useEffect(() => {
    return () => {
      if (pollRef.current) clearInterval(pollRef.current)
    }
  }, [])

  const handleUpload = useCallback(
    async (file) => {
      if (!file || file.type !== 'application/pdf') {
        addToast('Only PDF files are supported', 'error')
        return
      }
      setUploading(true)
      try {
        const { data } = await uploadDocument(file)
        addDocument(data)
        addToast(`"${data.filename}" uploaded! Processing…`, 'info')

        // Poll for status every 3s until READY or FAILED
        pollRef.current = setInterval(async () => {
          try {
            const res = await getAllDocuments()
            const updated = res.data.find((d) => d.id === data.id)
            if (updated) {
              updateDocument(updated)
              if (updated.status === 'READY') {
                clearInterval(pollRef.current)
                addToast(`"${updated.filename}" is ready!`, 'success')
              } else if (updated.status === 'FAILED') {
                clearInterval(pollRef.current)
                addToast(`Processing failed for "${updated.filename}"`, 'error')
              }
            }
          } catch (e) {
            clearInterval(pollRef.current)
          }
        }, 3000)
      } catch (err) {
        addToast('Upload failed. Is the backend running?', 'error')
      } finally {
        setUploading(false)
      }
    },
    [addDocument, addToast, updateDocument]
  )

  const onDrop = useCallback(
    (e) => {
      e.preventDefault()
      setDragging(false)
      const file = e.dataTransfer.files[0]
      if (file) handleUpload(file)
    },
    [handleUpload]
  )

  return (
    <div
      className={`doc-upload-zone ${dragging ? 'drag-over' : ''}`}
      onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
      onDragLeave={() => setDragging(false)}
      onDrop={onDrop}
      onClick={() => !uploading && inputRef.current?.click()}
      role="button"
      tabIndex={0}
      aria-label="Upload PDF document"
      onKeyDown={(e) => e.key === 'Enter' && inputRef.current?.click()}
    >
      <input
        ref={inputRef}
        type="file"
        accept="application/pdf"
        style={{ display: 'none' }}
        onChange={(e) => handleUpload(e.target.files[0])}
      />
      <div className="upload-icon">
        {uploading ? (
          <div className="spinner" style={{ borderTopColor: 'var(--accent-primary)', borderColor: 'rgba(108,99,255,0.3)' }} />
        ) : (
          <UploadCloud size={20} />
        )}
      </div>
      <p className="upload-text">{uploading ? 'Uploading…' : 'Drop PDF here'}</p>
      <p className="upload-hint">or click to browse</p>
    </div>
  )
}
