import { useState } from 'react'
import { scanAPI } from '../services/api'

interface AddRepositoryModalProps {
  onClose: () => void
}

export default function AddRepositoryModal({ onClose }: AddRepositoryModalProps) {
  const [gitHubUrl, setGitHubUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!gitHubUrl.trim()) {
      setError('Please enter a GitHub URL')
      return
    }

    try {
      setLoading(true)
      await scanAPI.createScan(gitHubUrl)
      setGitHubUrl('')
      onClose()
      // TODO: Refresh the board
    } catch (err) {
      setError('Failed to create scan. Please check the URL and try again.')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="card w-full max-w-md">
        <h2 className="text-headline font-semibold mb-lg">Add Repository</h2>

        <form onSubmit={handleSubmit} className="space-y-lg">
          <div>
            <label htmlFor="url" className="block text-body-sm font-medium text-ink mb-xs">
              GitHub Repository URL
            </label>
            <input
              id="url"
              type="text"
              placeholder="https://github.com/owner/repo"
              value={gitHubUrl}
              onChange={(e) => setGitHubUrl(e.target.value)}
              className="input-base w-full"
              disabled={loading}
            />
            <p className="text-caption text-ink-subtle mt-xs">
              Public repositories only (no authentication required)
            </p>
          </div>

          {error && (
            <div className="bg-error/10 border border-error text-error text-body-sm p-md rounded-md">
              {error}
            </div>
          )}

          <div className="flex gap-md justify-end">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="btn-secondary"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="btn-primary"
            >
              {loading ? 'Creating...' : 'Create Scan'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
