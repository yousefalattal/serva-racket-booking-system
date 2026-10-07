import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { errorMessage } from '../api'
import { useAuth } from '../AuthContext'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await register({ ...form, phone: form.phone || null })
      navigate('/')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="auth" onSubmit={handleSubmit}>
      <h1>Create account</h1>
      <p className="muted">Members only. Payment is at the club.</p>
      {error && <div className="error">{error}</div>}
      <input placeholder="Full name" value={form.fullName} required onChange={set('fullName')} />
      <input type="email" placeholder="Email" value={form.email} required onChange={set('email')} />
      <input type="tel" placeholder="Phone (optional)" value={form.phone} onChange={set('phone')} />
      <input type="password" placeholder="Password (8+ characters)" value={form.password}
             required minLength={8} onChange={set('password')} />
      <button className="primary" disabled={busy}>{busy ? 'Creating...' : 'Sign up'}</button>
      <p className="muted">Already a member? <Link to="/login">Log in</Link></p>
    </form>
  )
}
