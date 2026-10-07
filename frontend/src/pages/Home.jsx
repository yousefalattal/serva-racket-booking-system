import { useEffect, useState } from 'react'
import api from '../api'
import { useAuth } from '../AuthContext'

// Placeholder home page: proves login + backend calls work. The booking calendar replaces this next.
export default function Home() {
  const { user, logout } = useAuth()
  const [courts, setCourts] = useState([])

  useEffect(() => {
    api.get('/courts').then((res) => setCourts(res.data))
  }, [])

  return (
    <div className="page">
      <header className="topbar">
        <strong>Serva Racket Club</strong>
        <span>
          {user.fullName} <button className="link" onClick={logout}>Log out</button>
        </span>
      </header>
      <h2>Courts</h2>
      <div className="grid">
        {courts.map((c) => (
          <div className="card" key={c.id}>
            <h3>{c.name}</h3>
            <p>{c.sport}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
