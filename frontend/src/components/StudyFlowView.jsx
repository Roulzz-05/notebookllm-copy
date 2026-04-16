import React, { useCallback, useMemo } from 'react'
import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  Handle,
  Position,
  BackgroundVariant,
} from 'reactflow'
import 'reactflow/dist/style.css'
import useAppStore from '../store/useAppStore'
import { completeTopic } from '../api/client'

// ─── Custom Node ─────────────────────────────────────────────
function StudyNode({ data }) {
  const { markTopicCompleted, addToast } = useAppStore()

  const handleComplete = useCallback(
    async (e) => {
      e.stopPropagation()
      if (data.completed) return
      try {
        await completeTopic(data.id)
        markTopicCompleted(data.id)
      } catch {
        addToast('Failed to update topic', 'error')
      }
    },
    [data.id, data.completed, markTopicCompleted, addToast]
  )

  return (
    <div
      className={`flow-node ${data.importance === 'HIGH' ? 'high-importance' : ''} ${data.completed ? 'completed' : ''}`}
      style={{ position: 'relative' }}
    >
      <Handle type="target" position={Position.Top} />
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.5rem' }}>
        <button
          onClick={handleComplete}
          style={{
            width: 16,
            height: 16,
            borderRadius: 4,
            border: data.completed ? 'none' : '2px solid var(--border-normal)',
            background: data.completed ? 'var(--accent-success)' : 'transparent',
            cursor: data.completed ? 'default' : 'pointer',
            flexShrink: 0,
            marginTop: 2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          aria-label={`Mark "${data.label}" as completed`}
        >
          {data.completed && (
            <svg width="9" height="9" viewBox="0 0 10 10" fill="none">
              <path d="M2 5l2.5 2.5L8 3" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          )}
        </button>
        <span className={`flow-node-title ${data.completed ? 'completed' : ''}`}>{data.label}</span>
        {data.importance === 'HIGH' && (
          <span className="importance-badge HIGH" style={{ flexShrink: 0 }}>KEY</span>
        )}
      </div>
      <Handle type="source" position={Position.Bottom} />
    </div>
  )
}

const NODE_TYPES = { studyNode: StudyNode }

// ─── Layout helpers ────────────────────────────────────────────
function buildNodesAndEdges(topics, parentId = null, x = 0, y = 0, xStep = 260, yStep = 120) {
  const nodes = []
  const edges = []
  const count = topics.length
  const startX = x - ((count - 1) * xStep) / 2

  topics.forEach((topic, i) => {
    const nodeX = startX + i * xStep
    const nodeY = y
    const nodeId = `topic-${topic.id}`

    nodes.push({
      id: nodeId,
      type: 'studyNode',
      position: { x: nodeX, y: nodeY },
      data: {
        id: topic.id,
        label: topic.title,
        importance: topic.importance,
        completed: topic.completed,
      },
    })

    if (parentId) {
      edges.push({
        id: `e-${parentId}-${nodeId}`,
        source: parentId,
        target: nodeId,
        animated: !topic.completed,
        style: {
          stroke: topic.completed ? 'var(--accent-success)' : 'var(--border-accent)',
          strokeWidth: 2,
        },
      })
    }

    if (topic.children && topic.children.length > 0) {
      const { nodes: childNodes, edges: childEdges } = buildNodesAndEdges(
        topic.children,
        nodeId,
        nodeX,
        nodeY + yStep,
        xStep * 0.7,
        yStep
      )
      nodes.push(...childNodes)
      edges.push(...childEdges)
    }
  })

  return { nodes, edges }
}

// ─── Main Component ─────────────────────────────────────────────
export default function StudyFlowView({ topics }) {
  const { nodes: initNodes, edges: initEdges } = useMemo(
    () => buildNodesAndEdges(topics, null, 0, 0),
    [topics]
  )

  const [nodes, setNodes, onNodesChange] = useNodesState(initNodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initEdges)

  // Sync node data when topics change (e.g. marking complete)
  React.useEffect(() => {
    const { nodes: newNodes, edges: newEdges } = buildNodesAndEdges(topics, null, 0, 0)
    setNodes(newNodes)
    setEdges(newEdges)
  }, [topics, setNodes, setEdges])

  return (
    <div style={{ width: '100%', height: '100%' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        nodeTypes={NODE_TYPES}
        fitView
        fitViewOptions={{ padding: 0.2 }}
        minZoom={0.3}
        maxZoom={2}
        attributionPosition="bottom-left"
      >
        <Background
          variant={BackgroundVariant.Dots}
          gap={24}
          size={1}
          color="rgba(255,255,255,0.05)"
        />
        <Controls />
        <MiniMap
          nodeColor={(n) =>
            n.data?.completed
              ? '#10b981'
              : n.data?.importance === 'HIGH'
              ? '#6c63ff'
              : '#22232e'
          }
          maskColor="rgba(10, 11, 15, 0.8)"
          style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-normal)', borderRadius: 8 }}
        />
      </ReactFlow>
    </div>
  )
}
