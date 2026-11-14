import { useEffect, useState } from 'react'
import { getCurrentUser, login, logout, registerAccount } from './auth/api'
import type { AccountRegistrationRequest } from './auth/api'
import AuthScreen from './auth/AuthScreen'
import AdminDashboard from './admin/AdminDashboard'
import UserDashboard from './user/UserDashboard'
import type { UserResponse } from './user/types'

function App() {
  const [currentUser, setCurrentUser] = useState<UserResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [startupError, setStartupError] = useState('')
  useEffect(() => {
    const controller = new AbortController()
    getCurrentUser(controller.signal)
      .then(setCurrentUser)
      .catch(error => {
        if (!controller.signal.aborted) setStartupError(error instanceof Error ? error.message : 'No se pudo conectar con el servidor.')
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [])
  async function handleLogin(email: string, password: string) {
    const user = await login(email, password)
    setStartupError('')
    setCurrentUser(user)
  }
  async function handleRegister(data: AccountRegistrationRequest) {
    await registerAccount(data)
  }
  async function handleLogout() {
    await logout()
    setCurrentUser(null)
  }
  if (loading) return <div className="flex min-h-screen flex-col items-center justify-center bg-[#F8F6F3] text-[#241C18]"><div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-[#241C18] text-3xl font-bold text-[#E9D5C1]">F.</div><p className="text-sm font-medium text-[#806D5D]">Comprobando sesión...</p></div>
  if (!currentUser) return <AuthScreen onLogin={handleLogin} onRegister={handleRegister} initialError={startupError}/>
  if (currentUser.role === 'ADMIN' || currentUser.role === 'SUPERADMIN') {
    return <AdminDashboard currentUser={currentUser} onLogout={handleLogout}/>
  }
  return <UserDashboard user={currentUser} onLogout={handleLogout}/>
}

export default App
