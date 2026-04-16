import React, { useRef, useEffect, useState } from 'react'
import { Send, Bot, MessageSquare, AlertCircle } from 'lucide-react'
import useAppStore from '../store/useAppStore'
import { sendChatMessage } from '../api/client'

function TypingIndicator() {
  return (
    <div className="message-group assistant">
      <div className="message-header">
        <div className="message-avatar assistant">
          <Bot size={12} />
        </div>
      </div>
      <div className="message-bubble assistant">
        <div className="typing-indicator">
          <div className="typing-dot" />
          <div className="typing-dot" />
          <div className="typing-dot" />
        </div>
      </div>
    </div>
  )
}

function ChatMessage({ message }) {
  const isUser = message.role === 'user'
  return (
    <div className={`message-group ${message.role}`}>
      {!isUser && (
        <div className="message-header">
          <div className="message-avatar assistant">
            <Bot size={12} />
          </div>
        </div>
      )}
      <div className={`message-bubble ${message.role}`}>
        {message.content.split('\n').map((line, i, arr) => (
          <React.Fragment key={i}>
            {line}
            {i < arr.length - 1 && <br />}
          </React.Fragment>
        ))}
      </div>
      {isUser && (
        <div className="message-header" style={{ justifyContent: 'flex-end' }}>
          <div className="message-avatar user">U</div>
        </div>
      )}
    </div>
  )
}

export default function ChatPanelContent() {
  const {
    selectedDocId,
    documents,
    chatMessages,
    isChatLoading,
    addUserMessage,
    addAssistantMessage,
    setChatLoading,
    addToast,
  } = useAppStore()

  const [query, setQuery] = useState('')
  const messagesEndRef = useRef(null)
  const textareaRef = useRef(null)

  const selectedDoc = documents.find((d) => d.id === selectedDocId)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [chatMessages, isChatLoading])

  const handleSend = async () => {
    const trimmed = query.trim()
    if (!trimmed || !selectedDocId || isChatLoading) return
    setQuery('')
    if (textareaRef.current) textareaRef.current.style.height = 'auto'
    addUserMessage(trimmed)
    setChatLoading(true)
    try {
      const { data } = await sendChatMessage(selectedDocId, trimmed)
      addAssistantMessage(data.response || 'No response received.')
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to get response. Check backend connection.'
      addAssistantMessage(`⚠️ ${msg}`)
      addToast('Chat request failed', 'error')
    } finally {
      setChatLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const isDocReady = selectedDoc?.status === 'READY'
  const isDocProcessing = selectedDoc?.status === 'PROCESSING' || selectedDoc?.status === 'UPLOADING'

  return (
    <>
      {/* Processing banner */}
      {isDocProcessing && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: '0.5rem',
          padding: '0.625rem 1.25rem',
          background: 'rgba(245, 158, 11, 0.08)',
          borderBottom: '1px solid rgba(245, 158, 11, 0.2)',
          fontSize: '0.8125rem', color: 'var(--accent-warning)',
          flexShrink: 0,
        }}>
          <div className="spinner" style={{ borderTopColor: 'var(--accent-warning)', borderColor: 'rgba(245,158,11,0.2)', width: 12, height: 12 }} />
          Document is being processed… Chat will be available shortly.
        </div>
      )}

      {/* Messages or welcome */}
      {chatMessages.length === 0 ? (
        <div className="chat-welcome">
          <div className="chat-welcome-icon"><Bot size={28} /></div>
          <h2>Ask anything</h2>
          <p>
            {selectedDocId
              ? "I have read your document and I'm ready to help. Ask me questions, request summaries, or explore the content."
              : 'Select a document from the left panel to start a conversation.'}
          </p>
          {selectedDocId && !isDocReady && !isDocProcessing && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--accent-danger)', fontSize: '0.875rem' }}>
              <AlertCircle size={16} />
              Document failed to process
            </div>
          )}
        </div>
      ) : (
        <div className="chat-messages" id="chat-messages-list">
          {chatMessages.map((msg) => <ChatMessage key={msg.id} message={msg} />)}
          {isChatLoading && <TypingIndicator />}
          <div ref={messagesEndRef} />
        </div>
      )}

      {/* Input */}
      <div className="chat-input-area">
        <div className="chat-input-wrapper">
          <textarea
            ref={textareaRef}
            id="chat-input"
            className="chat-input"
            placeholder={
              isDocReady
                ? 'Ask a question… (Enter to send, Shift+Enter for new line)'
                : isDocProcessing
                ? 'Waiting for document to finish processing…'
                : 'Select a ready document to start chatting'
            }
            value={query}
            onChange={(e) => {
              const ta = e.target
              ta.style.height = 'auto'
              ta.style.height = Math.min(ta.scrollHeight, 150) + 'px'
              setQuery(ta.value)
            }}
            onKeyDown={handleKeyDown}
            disabled={!isDocReady || isChatLoading}
            rows={1}
            aria-label="Chat message input"
          />
          <button
            id="chat-send-btn"
            className="chat-send-btn"
            onClick={handleSend}
            disabled={!query.trim() || !isDocReady || isChatLoading}
            aria-label="Send message"
          >
            {isChatLoading ? <div className="spinner" /> : <Send size={15} />}
          </button>
        </div>
        {selectedDoc?.totalChunks > 0 && (
          <p className="chunks-info" style={{ marginTop: '0.375rem' }}>
            {selectedDoc.totalChunks} text chunks indexed · RAG-powered responses
          </p>
        )}
      </div>
    </>
  )
}
