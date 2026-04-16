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

  setTopics: (topics) => set({ topics }),
  setTopicsLoading: (v) => set({ isTopicsLoading: v }),
  setActiveView: (v) => set({ activeView: v }),

  markTopicCompleted: (id) => {
    const markInTree = (nodes) =>
      nodes.map((n) => ({
        ...n,
        completed: n.id === id ? true : n.completed,
        children: n.children ? markInTree(n.children) : [],
      }))
    set((state) => ({ topics: markInTree(state.topics) }))
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
}))

export default useAppStore
