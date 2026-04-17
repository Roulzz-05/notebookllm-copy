import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' },
})

// ─── Documents ─────────────────────────────────────────────────
export const uploadDocument = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/api/docs/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const getAllDocuments = () => api.get('/api/docs')

export const deleteDocument = (id) => api.delete(`/api/docs/${id}`)

// ─── Chat ───────────────────────────────────────────────────────
export const sendChatMessage = (documentId, query, mode = 'teacher') =>
  api.post('/api/chat', { documentId, query, mode })


// ─── Quiz ───────────────────────────────────────────────────────
export const generateQuiz = (documentId) => api.post(`/api/quiz/generate/${documentId}`)
export const getQuizzes = (documentId) => api.get(`/api/quiz/document/${documentId}`)

// ─── Story ──────────────────────────────────────────────────────
export const generateStory = (documentId) => api.post(`/api/story/generate/${documentId}`)
export const getStories = (documentId) => api.get(`/api/story/document/${documentId}`)

// ─── YouTube Recommendations ─────────────────────────────────
export const getYouTubeRecommendations = (documentId) => api.get(`/api/youtube/recommendations/${documentId}`)


// ─── Study Session ──────────────────────────────────────────────
export const getStudySession = (documentId) =>
  api.get(`/api/study-session/${documentId}`)

export const completeTopic = (topicId) =>
  api.post(`/api/topic/complete/${topicId}`)

export default api
