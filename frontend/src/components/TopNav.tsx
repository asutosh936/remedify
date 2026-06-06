interface TopNavProps {
  onAddRepo: () => void
}

export default function TopNav({ onAddRepo }: TopNavProps) {
  return (
    <nav className="sticky top-0 bg-canvas border-b border-hairline h-14 flex items-center justify-between px-lg">
      <div className="flex items-center gap-lg">
        <h1 className="text-headline font-bold">Remedify</h1>
        <span className="text-body-sm text-ink-subtle">Code Review Pipeline</span>
      </div>
      <button onClick={onAddRepo} className="btn-primary">
        + Add Repository
      </button>
    </nav>
  )
}
