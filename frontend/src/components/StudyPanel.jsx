import React, { useState, useMemo } from 'react'
import { ChevronRight, CheckCircle, Circle, Sparkles, RotateCcw, BrainCircuit, BookOpen, Youtube } from 'lucide-react'
import useAppStore from '../store/useAppStore'
import { completeTopic, generateQuiz, generateStory, getYouTubeRecommendations } from '../api/client'
import QuizView from './QuizView'
import StoryView from './StoryView'
import YouTubePanel from './YouTubePanel'

function TopicNode({ topic, depth = 0 }) {
  const [expanded, setExpanded] = useState(depth === 0)
  const { toggleTopicCompleted, addToast } = useAppStore()

  const hasChildren = topic.children && topic.children.length > 0

  const handleToggle = async (e) => {
    e.stopPropagation()
    try {
      const { completeTopic } = await import('../api/client')
      const { data } = await completeTopic(topic.id)
      toggleTopicCompleted(topic.id, data.completed)
      addToast(data.completed ? 'Topic completed!' : 'Topic reset.', 'info')
    } catch {
      addToast('Failed to update topic', 'error')
    }
  }

  return (
    <div className="topic-node">
      <div
        className={`topic-node-header ${topic.completed ? 'completed' : ''} ${topic.importance === 'HIGH' ? 'high-importance' : ''}`}
        onClick={() => hasChildren && setExpanded(!expanded)}
        role={hasChildren ? 'button' : 'listitem'}
        tabIndex={0}
        onKeyDown={(e) => e.key === 'Enter' && hasChildren && setExpanded(!expanded)}
        aria-expanded={hasChildren ? expanded : undefined}
      >
        <ChevronRight
          size={13}
          className={`topic-expand-icon ${expanded ? 'expanded' : ''}`}
          style={{ opacity: hasChildren ? 1 : 0 }}
        />

        <button
          className={`topic-check ${topic.completed ? 'checked' : ''}`}
          onClick={handleToggle}
          aria-label={`Toggle "${topic.title}" completion`}
          style={{ cursor: 'pointer' }}
        >
          {topic.completed && (
            <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
              <path d="M2 5l2.5 2.5L8 3" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          )}
        </button>

        <span className={`topic-title ${topic.completed ? 'completed' : ''}`}>
          {topic.title}
        </span>

        {topic.importance === 'HIGH' && (
          <span className="importance-badge HIGH">KEY</span>
        )}
      </div>

      {hasChildren && expanded && (
        <div className="topic-children">
          {topic.children.map((child) => (
            <TopicNode key={child.id} topic={child} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  )
}

export default function StudyPanel() {
  const {
    selectedDocId,
    documents,
    topics,
    isTopicsLoading,
    setTopics,
    setTopicsLoading,
    pdfSummary,
    setPdfSummary,
    toggleTopicCompleted,
    addToast,
    activeView,
    setActiveView,
    activeQuiz,
    setActiveQuiz,
    isQuizLoading,
    setQuizLoading,
    isStoryLoading,
    setStoryLoading,
    activeStory,
    setActiveStory,
    youtubeRecs,
    isYouTubeLoading,
    setYouTubeRecs,
    setYouTubeLoading,
  } = useAppStore()

  const [showYouTube, setShowYouTube] = useState(false)

  const selectedDoc = documents.find((d) => d.id === selectedDocId)
  
  const completionStats = useMemo(() => {
    const flatten = (nodes) => nodes.flatMap((n) => [n, ...(n.children ? flatten(n.children) : [])])
    const all = flatten(topics)
    const completed = all.filter((t) => t.completed)
    return { total: all.length, completed: completed.length }
  }, [topics])

  const handleGenerate = async () => {
    if (!selectedDocId || isTopicsLoading) return
    setTopicsLoading(true)
    try {
      const { getStudySession } = await import('../api/client')
      const { data } = await getStudySession(selectedDocId)
      setTopics(data.topics || [])
      addToast('Study roadmap generated!', 'success')
    } catch (err) {
      const msg = err.response?.data?.error || err.response?.data?.message || err.message || '';
      if (err.response?.status === 429 || msg.toLowerCase().includes('quota')) {
        addToast('Gemini API rate limit hit. Please wait a moment.', 'error')
      } else if (msg.toLowerCase().includes('roadmap format')) {
        addToast('The AI response was not in the expected format. Please try again.', 'error')
      } else if (msg.toLowerCase().includes('api key')) {
        addToast('Invalid Gemini API Key. Please check your backend configuration.', 'error')
      } else {
        addToast(`Failed to generate topics: ${msg || 'Is the document ready?'}`, 'error')
      }
    } finally {
      setTopicsLoading(false)
    }
  }

  const handleComplete = async (topicId, currentStatus) => {
    try {
      const { completeTopic } = await import('../api/client')
      const { data } = await completeTopic(topicId)
      toggleTopicCompleted(topicId, data.completed)
      addToast(data.completed ? 'Topic marked as completed!' : 'Topic reset.', 'info')
    } catch (err) {
      addToast('Failed to update status', 'error')
    }
  }

  const handleGenerateQuiz = async () => {
    if (!selectedDocId || isQuizLoading) return
    setQuizLoading(true)
    try {
      const { data } = await generateQuiz(selectedDocId)
      setActiveQuiz(data)
      addToast('Quiz generated successfully!', 'success')
    } catch (err) {
      const msg = err.response?.data?.error || err.response?.data?.message || err.message || '';
      if (msg.includes('429') || msg.toLowerCase().includes('quota')) {
        addToast('Gemini API quota exceeded. Please wait a minute.', 'error')
      } else if (msg.toLowerCase().includes('api key')) {
        addToast('Invalid Gemini API Key. Please check your backend configuration.', 'error')
      } else {
        addToast(`Failed to generate quiz: ${msg || 'Try again.'}`, 'error')
      }
    } finally {
      setQuizLoading(false)
    }
  }

  const handleGenerateStory = async () => {
    if (!selectedDocId || isStoryLoading) return
    setStoryLoading(true)
    try {
      const { data } = await generateStory(selectedDocId)
      setActiveStory(data)
      addToast('Story generated successfully!', 'success')
    } catch (err) {
      const msg = err.response?.data?.message || '';
      if (msg.includes('429') || msg.toLowerCase().includes('quota')) {
        addToast('Gemini API quota exceeded. Please wait a minute and try again.', 'error')
      } else {
        addToast('Failed to generate story.', 'error')
      }
    } finally {
      setStoryLoading(false)
    }
  }

  const handleGetYouTubeRecs = async () => {
    if (!selectedDocId || isYouTubeLoading) return
    setYouTubeLoading(true)
    try {
      const { data } = await getYouTubeRecommendations(selectedDocId)
      setYouTubeRecs(data || [])
      setShowYouTube(true)
      addToast('YouTube recommendations ready!', 'success')
    } catch (err) {
      addToast('Failed to get recommendations. Try again.', 'error')
    } finally {
      setYouTubeLoading(false)
    }
  }

  const progressPct = completionStats.total > 0
    ? Math.round((completionStats.completed / completionStats.total) * 100)
    : 0

  return (
    <div className="panel panel-right study-session-panel">
      {/* Header */}
      <div className="panel-header">
        <span className="panel-title">
          <Sparkles size={13} />
          Study Session
        </span>
        {topics.length > 0 && (
          <div style={{ display: 'flex', gap: '0.25rem' }}>
            <button
              className={`btn btn-ghost btn-sm btn-icon`}
              onClick={() => setActiveView('chat')}
              title="Chat view"
              style={{ color: activeView === 'chat' ? 'var(--accent-primary)' : undefined }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            </button>
            <button
              className={`btn btn-ghost btn-sm btn-icon`}
              onClick={() => setActiveView('flow')}
              title="Flow view"
              style={{ color: activeView === 'flow' ? 'var(--accent-primary)' : undefined }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/>
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
              </svg>
            </button>
          </div>
        )}
      </div>

      {/* Generate button */}
      <div className="study-generate-area">
        <button
          id="generate-topics-btn"
          className="study-generate-btn"
          onClick={handleGenerate}
          disabled={!selectedDocId || selectedDoc?.status !== 'READY' || isTopicsLoading}
        >
          {isTopicsLoading ? (
            <>
              <div className="spinner" />
              Generating topics…
            </>
          ) : topics.length > 0 ? (
            <>
              <RotateCcw size={15} />
              Regenerate Topics
            </>
          ) : (
            <>
              <Sparkles size={15} />
              Generate Study Topics
            </>
          )}
        </button>

        <button
          className="study-generate-btn secondary"
          style={{ marginTop: '0.5rem', background: 'var(--bg-elevated)', border: '1px solid var(--border-normal)' }}
          onClick={handleGenerateQuiz}
          disabled={!selectedDocId || selectedDoc?.status !== 'READY' || isQuizLoading}
        >
          {isQuizLoading ? (
            <>
              <div className="spinner" />
              Creating test…
            </>
          ) : (
            <>
              <BrainCircuit size={15} />
              Generate AI Test
            </>
          )}
        </button>

        <button
          className="study-generate-btn secondary"
          style={{ marginTop: '0.5rem', background: 'var(--bg-elevated)', border: '1px solid var(--border-normal)' }}
          onClick={handleGenerateStory}
          disabled={!selectedDocId || selectedDoc?.status !== 'READY' || isStoryLoading}
        >
          {isStoryLoading ? (
            <>
              <div className="spinner" />
              Writing Story…
            </>
          ) : (
            <>
              <BookOpen size={15} />
              Generate AI Story
            </>
          )}
        </button>

        <button
          className="study-generate-btn secondary"
          style={{ marginTop: '0.5rem', background: 'var(--bg-elevated)', border: '1px solid rgba(255,68,68,0.3)' }}
          onClick={handleGetYouTubeRecs}
          disabled={!selectedDocId || selectedDoc?.status !== 'READY' || isYouTubeLoading}
        >
          {isYouTubeLoading ? (
            <>
              <div className="spinner" />
              Finding Videos…
            </>
          ) : (
            <>
              <Youtube size={15} color="#ff4444" />
              YouTube Recommendations
            </>
          )}
        </button>

        {!selectedDocId && (
          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textAlign: 'center', marginTop: '0.5rem' }}>
            Select a document first
          </p>
        )}
        {selectedDocId && selectedDoc?.status !== 'READY' && (
          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textAlign: 'center', marginTop: '0.5rem' }}>
            Document must be READY to generate topics
          </p>
        )}
      </div>


      {/* Topic Tree */}
      <div className="topic-tree">
        {isTopicsLoading ? (
          <div className="loading-overlay">
            <div className="spinner spinner-lg" />
            <p>AI is analyzing your document…</p>
          </div>
        ) : topics.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">
              <Sparkles size={22} />
            </div>
            <p>Click "Generate Study Topics" to create a personalized learning roadmap from your document.</p>
          </div>
        ) : (
          <div role="list" aria-label="Study topics">
            {topics.map((topic) => (
              <TopicNode key={topic.id} topic={topic} />
            ))}
          </div>
        )}
      </div>

      {/* Progress */}
      {topics.length > 0 && (
        <div className="progress-container">
          <div className="progress-label">
            <span>Progress</span>
            <span>{completionStats.completed} / {completionStats.total} topics · {progressPct}%</span>
          </div>
          <div className="progress-bar">
            <div className="progress-fill" style={{ width: `${progressPct}%` }} />
          </div>
        </div>
      )}

      {activeQuiz && (
        <div className="quiz-overlay">
          <QuizView />
        </div>
      )}

      {activeStory && (
        <div className="quiz-overlay story-overlay">
          <StoryView />
        </div>
      )}

      {showYouTube && (
        <YouTubePanel onClose={() => setShowYouTube(false)} />
      )}
    </div>
  )
}
