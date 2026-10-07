import { createContext, useContext, useEffect, useState } from 'react'
import api from './api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(!!localStorage.getItem('token'))

  // On page load, check the stored token is still valid
  useEffect(() => {
    if (!localStorage.getItem('token')) return
    api.get('/auth/me')
      .then((res) => setUser(res.data))
      .catch(() => localStorage.removeItem('token'))
      .finally(() => setLoading(false))
  }, [])

  async function authenticate(path, body) {
    const { data } = await api.post(path, body)
    localStorage.setItem('token', data.token)
    setUser(data.user)
  }

  const login = (email, password) => authenticate('/auth/login', { email, password })
  const register = (form) => authenticate('/auth/register', form)
  const logout = () => {
    localStorage.removeItem('token')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
