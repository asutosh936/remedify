import { ScanStage } from '../types'

interface StatusBadgeProps {
  stage: ScanStage
}

export default function StatusBadge({ stage }: StatusBadgeProps) {
  const statusConfig: Record<ScanStage, { bg: string; text: string; label: string }> = {
    CLONING: { bg: 'bg-surface-3', text: 'text-ink-subtle', label: 'Cloning' },
    SCANNING: { bg: 'bg-surface-3', text: 'text-ink-subtle', label: 'Scanning' },
    RECOMMENDING: { bg: 'bg-surface-3', text: 'text-ink-subtle', label: 'Recommending' },
    VALIDATING: { bg: 'bg-surface-3', text: 'text-ink-subtle', label: 'Validating' },
    REPORTING: { bg: 'bg-surface-3', text: 'text-ink-subtle', label: 'Reporting' },
    COMPLETED: { bg: 'bg-success', text: 'text-white', label: 'Completed' },
  }

  const config = statusConfig[stage]

  return (
    <span className={`inline-block px-sm py-xs rounded-xs text-caption font-button ${config.bg} ${config.text}`}>
      {config.label}
    </span>
  )
}
