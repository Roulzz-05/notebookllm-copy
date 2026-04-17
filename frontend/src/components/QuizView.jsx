import React, { useState } from 'react'
import { CheckCircle2, XCircle, ArrowRight, RotateCcw, Award } from 'lucide-react'
import useAppStore from '../store/useAppStore'

export default function QuizView() {
  const { activeQuiz, setActiveQuiz } = useAppStore()
  const [currentIndex, setCurrentIndex] = useState(0)
  const [selectedOption, setSelectedOption] = useState(null)
  const [isAnswered, setIsAnswered] = useState(false)
  const [score, setScore] = useState(0)
  const [showResults, setShowResults] = useState(false)

  if (!activeQuiz) return null

  const questions = activeQuiz.questions || []
  
  if (questions.length === 0) {
    return (
      <div className="quiz-container">
        <div className="quiz-header">
          <span className="quiz-progress">Error</span>
          <button className="quiz-close" onClick={() => setActiveQuiz(null)}>✕</button>
        </div>
        <div className="quiz-question-area" style={{ textAlign: 'center', padding: '2rem' }}>
          <h3 className="quiz-question-text">Failed to load quiz questions. The AI may have returned an invalid format. Please try generating again.</h3>
        </div>
      </div>
    )
  }

  const currentQuestion = questions[currentIndex]

  const handleOptionSelect = (idx) => {
    if (isAnswered) return
    setSelectedOption(idx)
  }

  const handleConfirm = () => {
    if (selectedOption === null) return
    setIsAnswered(true)
    if (selectedOption === currentQuestion.correctAnswerIndex) {
      setScore(score + 1)
    }
  }

  const handleNext = () => {
    if (currentIndex < questions.length - 1) {
      setCurrentIndex(currentIndex + 1)
      setSelectedOption(null)
      setIsAnswered(false)
    } else {
      setShowResults(true)
    }
  }

  const handleReset = () => {
    setCurrentIndex(0)
    setSelectedOption(null)
    setIsAnswered(false)
    setScore(0)
    setShowResults(false)
  }

  if (showResults) {
    return (
      <div className="quiz-results">
        <div className="results-header">
          <Award size={48} className="results-icon" />
          <h2>Quiz Completed!</h2>
          <div className="score-display">
            <span className="score-big">{score}</span>
            <span className="score-total">/ {questions.length}</span>
          </div>
          <p className="results-text">
            {score === questions.length ? "Perfect score! You've mastered this material." : 
             score >= questions.length / 2 ? "Well done! You have a good grasp of the content." : 
             "Keep studying! Review the document and try again."}
          </p>
        </div>
        
        <div className="results-actions">
          <button className="btn btn-primary" onClick={handleReset}>
            <RotateCcw size={16} /> Try Again
          </button>
          <button className="btn btn-secondary" onClick={() => setActiveQuiz(null)}>
            Close Quiz
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="quiz-container">
      <div className="quiz-header">
        <span className="quiz-progress">Question {currentIndex + 1} of {questions.length}</span>
        <button className="quiz-close" onClick={() => setActiveQuiz(null)}>✕</button>
      </div>

      <div className="quiz-question-area">
        <h3 className="quiz-question-text">{currentQuestion.text}</h3>

        <div className="quiz-options">
          {currentQuestion.options.map((option, idx) => {
            let className = "quiz-option"
            if (isAnswered) {
              if (idx === currentQuestion.correctAnswerIndex) className += " correct"
              else if (selectedOption === idx) className += " incorrect"
              else className += " disabled"
            } else if (selectedOption === idx) {
              className += " selected"
            }

            return (
              <div 
                key={idx} 
                className={className}
                onClick={() => handleOptionSelect(idx)}
              >
                <div className="option-indicator">{String.fromCharCode(65 + idx)}</div>
                <div className="option-text">{option}</div>
                {isAnswered && idx === currentQuestion.correctAnswerIndex && <CheckCircle2 size={18} className="status-icon" />}
                {isAnswered && selectedOption === idx && idx !== currentQuestion.correctAnswerIndex && <XCircle size={18} className="status-icon" />}
              </div>
            )
          })}
        </div>

        {isAnswered && (
          <div className={`quiz-explanation ${selectedOption === currentQuestion.correctAnswerIndex ? 'correct' : 'incorrect'}`}>
            <strong>{selectedOption === currentQuestion.correctAnswerIndex ? 'Correct!' : 'Incorrect'}</strong>
            <p>{currentQuestion.explanation}</p>
          </div>
        )}
      </div>

      <div className="quiz-footer">
        {!isAnswered ? (
          <button 
            className="btn btn-primary quiz-action-btn" 
            onClick={handleConfirm}
            disabled={selectedOption === null}
          >
            Check Answer
          </button>
        ) : (
          <button className="btn btn-primary quiz-action-btn" onClick={handleNext}>
            {currentIndex === questions.length - 1 ? 'See Results' : 'Next Question'} <ArrowRight size={16} />
          </button>
        )}
      </div>
    </div>
  )
}
