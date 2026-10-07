import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { errorMessage } from '../api'
import { useAuth } from '../AuthContext'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="auth" onSubmit={handleSubmit}>
      <h1>Serva Racket Club</h1>
      <p className="muted">Log in to book a court</p>
      {error && <div className="error">{error}</div>}
      <input type="email" placeholder="Email" value={email} required
             onChange={(e) => setEmail(e.target.value)} />
      <input type="password" placeholder="Password" value={password} required
             onChange={(e) => setPassword(e.target.value)} />
      <button className="primary" disabled={busy}>{busy ? 'Logging in...' : 'Log in'}</button>
      <p className="muted">No account? <Link to="/register">Sign up</Link></p>
    </form>
  )
}
