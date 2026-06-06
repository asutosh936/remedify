import { RepositoryScan, ScanStage } from '../types'
import RepositoryCard from './RepositoryCard'

interface PipelineStageProps {
  stage: ScanStage
  stageName: string
  scans: RepositoryScan[]
  onRefresh: () => void
}

export default function PipelineStage({ stage, stageName, scans, onRefresh }: PipelineStageProps) {
  return (
    <div className="flex-shrink-0 w-80">
      <div className="card">
        <h2 className="text-card-title font-semibold mb-md">{stageName}</h2>
        <div className="text-body-sm text-ink-subtle mb-lg">{scans.length} item(s)</div>

        <div className="space-y-md max-h-[calc(100vh-300px)] overflow-y-auto">
          {scans.length === 0 ? (
            <div className="text-body-sm text-ink-subtle py-lg text-center">
              No items in this stage
            </div>
          ) : (
            scans.map(scan => (
              <RepositoryCard
                key={scan.id}
                scan={scan}
                onStatusChange={onRefresh}
              />
            ))
          )}
        </div>
      </div>
    </div>
  )
}
