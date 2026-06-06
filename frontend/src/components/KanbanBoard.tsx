import { useEffect, useState } from 'react'
import { RepositoryScan, ScanStage } from '../types'
import { scanAPI } from '../services/api'
import PipelineStage from './PipelineStage'

const STAGES: ScanStage[] = ['CLONING', 'SCANNING', 'RECOMMENDING', 'VALIDATING', 'REPORTING', 'COMPLETED']

const STAGE_NAMES: Record<ScanStage, string> = {
  CLONING: 'Repository Clone',
  SCANNING: 'Vulnerability Detection',
  RECOMMENDING: 'AI Recommendations',
  VALIDATING: 'Build & Test Validation',
  REPORTING: 'Report Generation',
  COMPLETED: 'Completed',
}

export default function KanbanBoard() {
  const [scans, setScans] = useState<RepositoryScan[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadScans()
    // TODO: Set up real-time updates via WebSocket or polling
  }, [])

  const loadScans = async () => {
    try {
      setLoading(true)
      const response = await scanAPI.listScans()
      setScans(response.content)
    } catch (error) {
      console.error('Failed to load scans', error)
    } finally {
      setLoading(false)
    }
  }

  const getScansByStage = (stage: ScanStage): RepositoryScan[] => {
    return scans.filter(scan => scan.currentStage === stage)
  }

  if (loading) {
    return <div className="text-center py-lg text-ink-muted">Loading...</div>
  }

  return (
    <div className="space-y-lg">
      <div className="flex gap-lg overflow-x-auto pb-lg">
        {STAGES.map(stage => (
          <PipelineStage
            key={stage}
            stage={stage}
            stageName={STAGE_NAMES[stage]}
            scans={getScansByStage(stage)}
            onRefresh={loadScans}
          />
        ))}
      </div>

      {scans.length === 0 && (
        <div className="text-center py-xxl">
          <p className="text-ink-subtle text-body">No scans yet. Add a repository to get started!</p>
        </div>
      )}
    </div>
  )
}
