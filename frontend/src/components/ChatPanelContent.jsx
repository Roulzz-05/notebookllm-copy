import React, { useRef, useEffect, useState } from 'react'
import { Send, Bot, AlertCircle, Mic, MicOff } from 'lucide-react'
import useAppStore from '../store/useAppStore'
import { sendChatMessage } from '../api/client'

const LEARNING_MODES = [
  { id: 'teacher',   emoji: '👨‍🏫', label: 'Teacher',   desc: 'Structured lessons' },
  { id: 'beginner',  emoji: '🧑‍🎓', label: 'Beginner',  desc: 'Simple & friendly' },
  { id: 'meme',      emoji: '😂',   label: 'Meme',      desc: 'Fun explanations' },
  { id: 'interview', emoji: '⚡',   label: 'Interview', desc: 'Crisp & tricky' },
]

function TypingIndicator() {
  return (
    <div className="message-group assistant">
      <div className="message-header">
        <div className="message-avatar assistant"><Bot size={12} /></div>
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
          <div className="message-avatar assistant"><Bot size={12} /></div>
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
    learningMode,
    setLearningMode,
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

  const [isListening, setIsListening] = useState(false)
  const recognitionRef = useRef(null)

  useEffect(() => {
    if (typeof window !== 'undefined' && (window.SpeechRecognition || window.webkitSpeechRecognition)) {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
      recognitionRef.current = new SpeechRecognition()
      recognitionRef.current.continuous = false
      recognitionRef.current.interimResults = false
      recognitionRef.current.lang = 'en-US'

      recognitionRef.current.onresult = (event) => {
        const transcript = event.results[0][0].transcript
        setQuery(prev => prev ? `${prev} ${transcript}` : transcript)
        setIsListening(false)
      }

      recognitionRef.current.onerror = (event) => {
        console.error('Speech recognition error:', event.error)
        setIsListening(false)
        addToast(`Voice error: ${event.error}`, 'error')
      }

      recognitionRef.current.onend = () => {
        setIsListening(false)
      }
    }
  }, [addToast])

  const toggleListening = () => {
    if (!recognitionRef.current) {
      addToast('Speech recognition not supported in this browser.', 'error')
      return
    }

    if (isListening) {
      recognitionRef.current.stop()
    } else {
      try {
        recognitionRef.current.start()
        setIsListening(true)
      } catch (err) {
        console.error('Failed to start speech recognition:', err)
      }
    }
  }

  const handleSend = async () => {
    const trimmed = query.trim()
    if (!trimmed || !selectedDocId || isChatLoading) return
    setQuery('')
    if (textareaRef.current) textareaRef.current.style.height = 'auto'
    addUserMessage(trimmed)
    setChatLoading(true)
    try {
      const { data } = await sendChatMessage(selectedDocId, trimmed, learningMode)
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
  const activeMode = LEARNING_MODES.find(m => m.id === learningMode) || LEARNING_MODES[0]

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

      {/* Learning Mode Selector */}
      <div style={{
        display: 'flex',
        gap: '0.4rem',
        padding: '0.625rem 1.25rem',
        borderBottom: '1px solid var(--border-subtle)',
        flexShrink: 0,
        background: 'rgba(26, 27, 35, 0.5)',
        overflowX: 'auto',
      }}>
        {LEARNING_MODES.map((mode) => {
          const isActive = learningMode === mode.id
          return (
            <button
              key={mode.id}
              id={`mode-${mode.id}`}
              onClick={() => setLearningMode(mode.id)}
              title={mode.desc}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.375rem',
                padding: '0.375rem 0.75rem',
                borderRadius: '999px',
                border: isActive ? '1px solid var(--border-accent)' : '1px solid var(--border-subtle)',
                background: isActive
                  ? 'linear-gradient(135deg, rgba(108,99,255,0.2) 0%, rgba(168,85,247,0.2) 100%)'
                  : 'var(--bg-elevated)',
                color: isActive ? 'var(--accent-primary)' : 'var(--text-secondary)',
                cursor: 'pointer',
                fontSize: '0.8rem',
                fontWeight: isActive ? 600 : 400,
                fontFamily: 'var(--font-sans)',
                whiteSpace: 'nowrap',
                transition: 'all 150ms cubic-bezier(0.4, 0, 0.2, 1)',
                boxShadow: isActive ? 'var(--shadow-glow)' : 'none',
                transform: isActive ? 'translateY(-1px)' : 'none',
              }}
            >
              <span style={{ fontSize: '1rem' }}>{mode.emoji}</span>
              {mode.label}
            </button>
          )
        })}
      </div>

      {/* Messages or welcome */}
      {chatMessages.length === 0 ? (
        <div className="chat-welcome">
          <div className="chat-welcome-icon">
            <span style={{ fontSize: '1.75rem' }}>{activeMode.emoji}</span>
          </div>
          <h2>{activeMode.label} Mode</h2>
          <p style={{ marginBottom: '0.25rem' }}>
            {selectedDocId
              ? `Ask me anything about your document! I'll explain it in ${activeMode.label.toLowerCase()} style — ${activeMode.desc.toLowerCase()}.`
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
        {/* Active mode pill */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.375rem',
          marginBottom: '0.5rem',
          fontSize: '0.75rem',
          color: 'var(--text-muted)',
        }}>
          <span>{activeMode.emoji}</span>
          <span>Replying in <strong style={{ color: 'var(--accent-primary)' }}>{activeMode.label} mode</strong> · {activeMode.desc}</span>
        </div>

        <div className="chat-input-wrapper">
          <textarea
            ref={textareaRef}
            id="chat-input"
            className="chat-input"
            placeholder={
              isDocReady
                ? `Ask in ${activeMode.label} mode… (Enter to send, Shift+Enter for new line)`
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
          
          <div style={{ display: 'flex', gap: '0.4rem', marginLeft: '0.5rem' }}>
            <button
              onClick={toggleListening}
              className={`chat-mic-btn ${isListening ? 'listening' : ''}`}
              disabled={!isDocReady || isChatLoading}
              title="Voice to Text"
              style={{
                background: isListening ? 'rgba(239, 68, 68, 0.15)' : 'var(--bg-elevated)',
                border: isListening ? '1px solid var(--accent-danger)' : '1px solid var(--border-subtle)',
                color: isListening ? 'var(--accent-danger)' : 'var(--text-secondary)',
                borderRadius: '8px',
                width: '36px',
                height: '36px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
              }}
            >
              {isListening ? <Mic size={16} className="pulse-animation" /> : <Mic size={16} />}
            </button>

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
        </div>
        {selectedDoc?.totalChunks > 0 && (
          <p className="chunks-info" style={{ marginTop: '0.375rem' }}>
            {selectedDoc.totalChunks} text chunks indexed · RAG-powered · {activeMode.label} mode active
          </p>
        )}
      </div>
    </>
  )
}
