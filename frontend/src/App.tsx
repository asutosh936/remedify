import { useState } from 'react'
import TopNav from './components/TopNav'
import KanbanBoard from './components/KanbanBoard'
import AddRepositoryModal from './components/AddRepositoryModal'

export default function App() {
  const [showModal, setShowModal] = useState(false)

  return (
    <div className="min-h-screen bg-canvas text-ink">
      <TopNav onAddRepo={() => setShowModal(true)} />
      <main className="p-lg">
        <KanbanBoard />
      </main>
      {showModal && <AddRepositoryModal onClose={() => setShowModal(false)} />}
    </div>
  )
}
