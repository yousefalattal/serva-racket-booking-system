import axios from 'axios'

// All calls go to /api/..., which Vite forwards to the Spring Boot backend (see vite.config.js)
const api = axios.create({ baseURL: '/api' })

// Attach the login token to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// If the token expired, clear it and send the user to the login page
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const isAuthCall = err.config?.url?.startsWith('/auth/')
    if (err.response?.status === 401 && !isAuthCall) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

// Turns a backend error into a readable message
export function errorMessage(err) {
  const data = err.response?.data
  if (data?.fields) {
    return Object.entries(data.fields).map(([field, msg]) => `${field}: ${msg}`).join(', ')
  }
  return data?.error ?? 'Could not reach the server'
}

export default api
