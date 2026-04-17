import { create } from 'zustand'

const useAppStore = create((set, get) => ({
  // ─── Documents ────────────────────────────────────────────
  documents: [],
  selectedDocId: null,

  setDocuments: (docs) => set({ documents: docs }),

  addDocument: (doc) =>
    set((state) => ({ documents: [...state.documents, doc] })),

  updateDocument: (updatedDoc) =>
    set((state) => ({
      documents: state.documents.map((d) =>
        d.id === updatedDoc.id ? updatedDoc : d
      ),
    })),

  removeDocument: (id) =>
    set((state) => ({
      documents: state.documents.filter((d) => d.id !== id),
      selectedDocId: state.selectedDocId === id ? null : state.selectedDocId,
      topics: state.selectedDocId === id ? [] : state.topics,
      chatMessages: state.selectedDocId === id ? [] : state.chatMessages,
    })),

  selectDocument: (id) =>
    set({
      selectedDocId: id,
      chatMessages: [], // clear chat on doc switch
      topics: [],
    }),



  // ─── Chat ──────────────────────────────────────────────────
  chatMessages: [],
  isChatLoading: false,
  learningMode: 'teacher',
  setLearningMode: (mode) => set({ learningMode: mode }),

  addUserMessage: (text) =>
    set((state) => ({
      chatMessages: [
        ...state.chatMessages,
        { id: Date.now(), role: 'user', content: text },
      ],
    })),

  addAssistantMessage: (text) =>
    set((state) => ({
      chatMessages: [
        ...state.chatMessages,
        { id: Date.now(), role: 'assistant', content: text },
      ],
    })),

  setChatLoading: (v) => set({ isChatLoading: v }),

  // ─── Study Session / Topics ────────────────────────────────
  topics: [],
  isTopicsLoading: false,
  activeView: 'chat', // 'chat' | 'flow'
  pdfSummary: '',
  setPdfSummary: (v) => set({ pdfSummary: v }),

  setTopics: (topics) => set({ topics }),
  setTopicsLoading: (v) => set({ isTopicsLoading: v }),
  setActiveView: (v) => set({ activeView: v }),

  toggleTopicCompleted: (id, completed) => {
    const toggleInTree = (nodes) =>
      nodes.map((n) => ({
        ...n,
        completed: n.id === id ? completed : n.completed,
        children: n.children ? toggleInTree(n.children) : [],
      }))
    set((state) => ({ topics: toggleInTree(state.topics) }))
  },



  // ─── UI ───────────────────────────────────────────────────
  toasts: [],
  addToast: (message, type = 'info') =>
    set((state) => ({
      toasts: [
        ...state.toasts,
        { id: Date.now(), message, type },
      ],
    })),
  removeToast: (id) =>
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) })),

  // ─── Quizzes ──────────────────────────────────────────────
  quizzes: [],
  isQuizLoading: false,
  activeQuiz: null,

  setQuizzes: (quizzes) => set({ quizzes }),
  setQuizLoading: (v) => set({ isQuizLoading: v }),
  setActiveQuiz: (quiz) => set({ activeQuiz: quiz }),

  // ─── Stories ──────────────────────────────────────────────
  stories: [],
  isStoryLoading: false,
  activeStory: null,

  setStories: (stories) => set({ stories }),
  setStoryLoading: (v) => set({ isStoryLoading: v }),
  setActiveStory: (story) => set({ activeStory: story }),

  // ─── YouTube Recommendations ───────────────────────────────
  youtubeRecs: [],
  isYouTubeLoading: false,
  setYouTubeRecs: (recs) => set({ youtubeRecs: recs }),
  setYouTubeLoading: (v) => set({ isYouTubeLoading: v }),
}))

export default useAppStore
