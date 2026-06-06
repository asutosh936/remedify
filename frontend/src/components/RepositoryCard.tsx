import { useState } from 'react'
import { RepositoryScan } from '../types'
import { scanAPI } from '../services/api'
import StatusBadge from './StatusBadge'

interface RepositoryCardProps {
  scan: RepositoryScan
  onStatusChange: () => void
}

export default function RepositoryCard({ scan, onStatusChange }: RepositoryCardProps) {
  const [loading, setLoading] = useState(false)

  const handleRetry = async () => {
    try {
      setLoading(true)
      await scanAPI.retryScan(scan.id)
      onStatusChange()
    } catch (error) {
      console.error('Failed to retry scan', error)
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to delete this scan?')) return

    try {
      setLoading(true)
      await scanAPI.deleteScan(scan.id)
      onStatusChange()
    } catch (error) {
      console.error('Failed to delete scan', error)
    } finally {
      setLoading(false)
    }
  }

  const handleDownloadReport = async () => {
    try {
      setLoading(true)
      await scanAPI.downloadReport(scan.id, 'html')
    } catch (error) {
      console.error('Failed to download report', error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="bg-surface-2 border border-hairline rounded-lg p-md hover:border-primary-focus transition-colors">
      <div className="flex items-start justify-between mb-md">
        <div className="flex-1 min-w-0">
          <h3 className="text-body font-semibold text-ink truncate">{scan.repositoryName}</h3>
          <p className="text-body-sm text-ink-subtle truncate text-xs">{scan.gitHubUrl}</p>
        </div>
      </div>

      <div className="flex items-center gap-sm mb-md">
        <StatusBadge stage={scan.currentStage} />
        {scan.currentStage !== 'COMPLETED' && scan.retryCount > 0 && (
          <span className="text-caption text-warning">Retry {scan.retryCount}</span>
        )}
      </div>

      {scan.statusMessage && (
        <p className="text-body-sm text-ink-muted mb-md truncate">{scan.statusMessage}</p>
      )}

      <div className="flex gap-sm">
        {scan.currentStage === 'COMPLETED' && (
          <button
            onClick={handleDownloadReport}
            disabled={loading}
            className="btn-secondary text-sm flex-1"
          >
            {loading ? 'Loading...' : 'Report'}
          </button>
        )}
        {scan.statusMessage?.includes('Error') && (
          <button
            onClick={handleRetry}
            disabled={loading}
            className="btn-secondary text-sm flex-1"
          >
            {loading ? 'Retrying...' : 'Retry'}
          </button>
        )}
        <button
          onClick={handleDelete}
          disabled={loading}
          className="btn-tertiary text-sm"
        >
          ✕
        </button>
      </div>
    </div>
  )
}
