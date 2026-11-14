import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { getUsers, registerUser } from '../user/api'
import type { UserRequest, UserResponse } from '../user/types'

type View = 'overview' | 'users'
type IconName = 'grid' | 'users' | 'wallet' | 'transfer' | 'activity' | 'gift' | 'plus' | 'search' | 'refresh' | 'close' | 'menu' | 'arrow' | 'shield' | 'calendar' | 'clock' | 'check' | 'spark' | 'chevron'
const emptyForm: UserRequest = { identification: '', firstName: '', lastName: '', email: '' }
const formFields: { key: keyof UserRequest; label: string; type: string; placeholder: string }[] = [
  { key: 'identification', label: 'Identificación', type: 'text', placeholder: 'Número de identificación' },
  { key: 'firstName', label: 'Nombres', type: 'text', placeholder: 'Nombres del usuario' },
  { key: 'lastName', label: 'Apellidos', type: 'text', placeholder: 'Apellidos del usuario' },
  { key: 'email', label: 'Correo electrónico', type: 'email', placeholder: 'correo@ejemplo.com' }
]
const menuItems: { label: string; icon: IconName; view?: View }[] = [
  { label: 'Resumen', icon: 'grid', view: 'overview' },
  { label: 'Mis billeteras', icon: 'wallet' },
  { label: 'Transferencias', icon: 'transfer' },
  { label: 'Movimientos', icon: 'activity' },
  { label: 'Recompensas', icon: 'gift' },
  { label: 'Usuarios', icon: 'users', view: 'users' }
]
function Icon({ name, size = 20, className = '' }: { name: IconName; size?: number; className?: string }) {
  const paths: Record<IconName, ReactNode> = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="1.4"/><rect x="14" y="3" width="7" height="7" rx="1.4"/><rect x="3" y="14" width="7" height="7" rx="1.4"/><rect x="14" y="14" width="7" height="7" rx="1.4"/></>,
    users: <><circle cx="9" cy="8" r="3"/><path d="M3 20v-2a6 6 0 0 1 12 0v2M17 5.5a3 3 0 0 1 0 5.5M17 14a5 5 0 0 1 4 5v1"/></>,
    wallet: <><rect x="3" y="6" width="18" height="14" rx="3"/><path d="M4.5 6V4.5A2.5 2.5 0 0 1 7 2h11M15 13h6M16 13h1"/></>,
    transfer: <><path d="M4 7h16m-4-4 4 4-4 4M20 17H4m4-4-4 4 4 4"/></>,
    activity: <><path d="M3 12h4l3-7 4 14 3-7h4"/></>,
    gift: <><rect x="3" y="9" width="18" height="12" rx="2"/><path d="M12 9v12M3 13h18M12 9H7a3 3 0 1 1 3-3l2 3Zm0 0h5a3 3 0 1 0-3-3l-2 3Z"/></>,
    plus: <path d="M12 5v14M5 12h14"/>,
    search: <><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 5 5"/></>,
    refresh: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.5 9a7.5 7.5 0 0 1 13-2L20 12M4 12l1.5 5a7.5 7.5 0 0 0 13-2"/></>,
    close: <path d="M5 5l14 14M19 5 5 19"/>,
    menu: <path d="M4 7h16M4 12h16M4 17h16"/>,
    arrow: <path d="M5 12h14m-6-6 6 6-6 6"/>,
    shield: <><path d="m12 2 8 4v6c0 5-3.3 8-8 10-4.7-2-8-5-8-10V6l8-4Z"/><path d="m9 12 2 2 4-4"/></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 2v6M17 2v6M3 10h18"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    check: <path d="m5 12 4 4L19 6"/>,
    spark: <><path d="m12 2 2.5 7.5L22 12l-7.5 2.5L12 22l-2.5-7.5L2 12l7.5-2.5L12 2Z"/></>,
    chevron: <path d="m9 6 6 6-6 6"/>
  }
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" className={className} aria-hidden="true">{paths[name]}</svg>
}
function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Ocurrió un error inesperado'
}
function dateLabel(date: string): string {
  const parsed = new Date(date)
  return Number.isNaN(parsed.getTime()) ? 'Sin fecha' : parsed.toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' })
}
function initials(user: UserResponse): string {
  return `${user.firstName.slice(0, 1)}${user.lastName.slice(0, 1)}`.toUpperCase()
}
function Sidebar({ view, onNavigate, mobileOpen, onClose }: { view: View; onNavigate: (view: View) => void; mobileOpen: boolean; onClose: () => void }) {
  return <>
    {mobileOpen && <button aria-label="Cerrar menú" onClick={onClose} className="fixed inset-0 z-30 bg-black/50 lg:hidden"/>}
    <aside className={`fixed inset-y-0 left-0 z-40 flex w-[252px] flex-col border-r border-[#E7E0D9] bg-white transition-transform lg:translate-x-0 ${mobileOpen ? 'translate-x-0' : '-translate-x-full'}`}>
      <div className="flex h-[84px] items-center gap-3 border-b border-[#F0EBE7] px-7">
        <div className="flex h-11 w-11 items-center justify-center rounded-[14px] bg-[#241C18] text-xl font-extrabold text-[#F6E8D9] shadow-sm">F<span className="text-[#BC9879]">.</span></div>
        <div><div className="text-xl font-extrabold tracking-[-0.04em] text-[#201915]">Fintech</div><div className="text-[10px] font-semibold uppercase tracking-[0.18em] text-[#947A66]">Digital Wallet</div></div>
        <button aria-label="Cerrar menú" className="ml-auto text-[#6B5748] lg:hidden" onClick={onClose}><Icon name="close"/></button>
      </div>
      <div className="flex-1 overflow-y-auto px-4 py-7">
        <div className="px-4 pb-3 text-[10px] font-bold uppercase tracking-[0.2em] text-[#A89B90]">PLATAFORMA</div>
        <nav aria-label="Navegación principal" className="space-y-1.5">
          {menuItems.map(item => {
            const active = item.view === view
            return <button key={item.label} type="button" disabled={!item.view} onClick={() => item.view && onNavigate(item.view)} className={`group flex w-full items-center gap-3 rounded-xl px-4 py-3.5 text-left text-[13px] font-semibold transition ${active ? 'bg-[#F1E9E1] text-[#6F4E37]' : item.view ? 'text-[#71665E] hover:bg-[#F7F4F0] hover:text-[#241C18]' : 'cursor-not-allowed text-[#B9B0A9]'}`}>
              <Icon name={item.icon} size={19}/><span className="flex-1">{item.label}</span>{!item.view && <span className="text-[9px] font-semibold text-[#B19D8B]">PRONTO</span>}{active && <span className="h-1.5 w-1.5 rounded-full bg-[#6F4E37]"/>}
            </button>
          })}
        </nav>
      </div>
      <div className="p-4"><div className="rounded-2xl border border-[#EAE2D9] bg-[#FAF7F3] p-4"><div className="mb-2 flex items-center gap-2 text-[#77583F]"><Icon name="shield" size={17}/><span className="text-xs font-bold">Entorno de desarrollo</span></div><p className="text-[11px] leading-relaxed text-[#94877A]">Panel administrativo protegido. Los módulos financieros se habilitarán progresivamente.</p></div></div>
    </aside>
  </>
}
function StatCard({ label, value, icon, foot }: { label: string; value: number; icon: IconName; foot: string }) {
  return <div className="rounded-[22px] border border-[#EDE7E1] bg-white p-5 shadow-[0_6px_30px_rgba(42,25,12,0.025)] sm:p-6">
    <div className="mb-5 flex items-center justify-between"><span className="text-[12px] font-semibold text-[#8A7F75]">{label}</span><span className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#F4EEE7] text-[#78543B]"><Icon name={icon} size={18}/></span></div>
    <div className="text-[33px] font-bold tracking-[-0.05em] text-[#221A16]">{value}</div><p className="mt-1 text-[11px] text-[#A3978E]">{foot}</p>
  </div>
}
function AdminDashboard({ currentUser, onLogout }: { currentUser: UserResponse; onLogout: () => Promise<void> }) {
  const [view, setView] = useState<View>('overview')
  const [mobileOpen, setMobileOpen] = useState(false)
  const [users, setUsers] = useState<UserResponse[]>([])
  const [search, setSearch] = useState('')
  const [form, setForm] = useState<UserRequest>(emptyForm)
  const [registerOpen, setRegisterOpen] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  useEffect(() => {
    const controller = new AbortController()
    getUsers(controller.signal).then(setUsers).catch(error => {
      if (!controller.signal.aborted) setError(getErrorMessage(error))
    }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [])
  const activeUsers = useMemo(() => users.filter(user => user.status === 'ACTIVE').length, [users])
  const blockedUsers = useMemo(() => users.filter(user => user.status !== 'ACTIVE').length, [users])
  const filteredUsers = useMemo(() => {
    const query = search.trim().toLocaleLowerCase()
    return users.filter(user => `${user.identification} ${user.firstName} ${user.lastName} ${user.email}`.toLocaleLowerCase().includes(query))
  }, [users, search])
  const recentUsers = useMemo(() => [...users].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()).slice(0, 4), [users])
  function navigate(next: View) {
    setView(next)
    setMobileOpen(false)
    setError('')
    setNotice('')
  }
  function openRegister() {
    setRegisterOpen(true)
    setError('')
    setNotice('')
  }
  async function refreshUsers() {
    setLoading(true)
    setError('')
    try { setUsers(await getUsers()) } catch (error) { setError(getErrorMessage(error)) } finally { setLoading(false) }
  }
  async function handleRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const created = await registerUser(form)
      setUsers(current => [created, ...current])
      setForm(emptyForm)
      setRegisterOpen(false)
      setNotice('Usuario registrado correctamente.')
    } catch (error) { setError(getErrorMessage(error)) } finally { setSaving(false) }
  }
  return <div className="min-h-screen bg-[#F8F6F3] font-sans text-[#241C18]">
    <Sidebar view={view} onNavigate={navigate} mobileOpen={mobileOpen} onClose={() => setMobileOpen(false)}/>
    <div className="min-h-screen lg:pl-[252px]">
      <header className="sticky top-0 z-20 flex h-[84px] items-center justify-between gap-4 border-b border-[#EDE8E2] bg-white/95 px-5 backdrop-blur sm:px-8 lg:px-10">
        <div className="flex items-center gap-3"><button type="button" aria-label="Abrir menú" className="rounded-lg p-2 text-[#6F4E37] lg:hidden" onClick={() => setMobileOpen(true)}><Icon name="menu"/></button><div className="hidden text-xs font-medium text-[#A39588] sm:block">Plataforma</div><Icon name="chevron" size={14} className="hidden text-[#C5B9B0] sm:block"/><span className="text-[13px] font-semibold text-[#392C24]">{view === 'overview' ? 'Resumen general' : 'Gestión de usuarios'}</span></div>
        <div className="flex items-center gap-3"><span className="hidden rounded-full border border-[#E5DAD0] bg-[#FBF8F5] px-3 py-1.5 text-[11px] font-semibold text-[#75563F] sm:block">ADMINISTRACIÓN</span><div className="flex h-9 w-9 items-center justify-center rounded-full bg-[#E9D9CA] text-xs font-extrabold text-[#64452F]">{initials(currentUser)}</div><button type="button" onClick={() => void onLogout()} className="rounded-xl border border-[#E8DED5] px-3 py-2 text-[11px] font-bold text-[#73543B] hover:bg-[#F6EEE7]">Cerrar sesión</button></div>
      </header>
      <main className="mx-auto max-w-[1400px] px-5 py-8 sm:px-8 lg:px-10 lg:py-10">
        <div className="mb-8 flex flex-wrap items-end justify-between gap-4"><div><div className="mb-2 flex items-center gap-2 text-[11px] font-bold uppercase tracking-[0.19em] text-[#A08067]"><Icon name="spark" size={14}/> FINTECH / {view === 'overview' ? 'INICIO' : 'ADMINISTRACIÓN'}</div><h1 className="text-[29px] font-bold tracking-[-0.055em] text-[#241C18] sm:text-[36px]">{view === 'overview' ? 'Centro de control' : 'Gestión de usuarios'}</h1><p className="mt-2 text-[13px] text-[#988D83]">{view === 'overview' ? 'Una vista general de tu plataforma financiera.' : 'Consulta y administra las cuentas registradas en Fintech.'}</p></div><button type="button" onClick={openRegister} className="inline-flex items-center gap-2 rounded-xl bg-[#6F4E37] px-5 py-3 text-xs font-bold text-white shadow-[0_5px_15px_rgba(111,78,55,0.18)] transition hover:bg-[#573C2B]"><Icon name="plus" size={17}/> Nuevo usuario</button></div>
        {(error && !registerOpen) && <div role="alert" className="mb-5 rounded-xl border border-[#D7BBA5] bg-[#F6ECE3] px-4 py-3 text-[13px] text-[#72482D]">{error}</div>}
        {notice && <div role="status" className="mb-5 flex items-center gap-2 rounded-xl border border-[#DECDBD] bg-[#F5EEE6] px-4 py-3 text-[13px] font-medium text-[#6F4E37]"><Icon name="check" size={17}/>{notice}</div>}
        {view === 'overview' ? <>
          <section className="grid gap-5 xl:grid-cols-[minmax(0,1.65fr)_minmax(300px,1fr)]">
            <div className="relative min-h-[300px] overflow-hidden rounded-[26px] bg-[#241C18] p-7 text-white sm:p-9">
              <div className="absolute -right-24 -top-32 h-80 w-80 rounded-full border-[52px] border-[#7A573F]/30"/><div className="absolute -bottom-44 right-6 h-80 w-80 rounded-full border-[65px] border-[#C09C78]/15"/>
              <div className="relative flex h-full flex-col justify-between"><div><span className="inline-flex items-center gap-2 rounded-full border border-[#896E59]/45 bg-white/10 px-3 py-1.5 text-[10px] font-bold tracking-wider text-[#E4CBB1]"><Icon name="spark" size={13}/> TU PLATAFORMA DIGITAL</span><h2 className="mt-6 max-w-[480px] text-[30px] font-semibold leading-[1.18] tracking-[-0.045em] sm:text-[36px]">Tus finanzas, en un solo lugar.</h2><p className="mt-3 max-w-[420px] text-[13px] leading-6 text-[#BFB0A4]">Estamos construyendo tus billeteras, transferencias y recompensas. La gestión de cuentas ya está disponible.</p></div><div className="mt-8 flex flex-wrap items-center gap-3"><button type="button" onClick={() => navigate('users')} className="inline-flex items-center gap-2 rounded-xl bg-[#E9D5C1] px-4 py-3 text-[12px] font-bold text-[#3F2A1D] transition hover:bg-white">Administrar usuarios <Icon name="arrow" size={16}/></button><span className="text-[11px] text-[#B9A99C]">Próximamente: billeteras y movimientos</span></div></div>
            </div>
            <div className="flex flex-col justify-between rounded-[26px] border border-[#EBE2DA] bg-[#F0E5DA] p-7 sm:p-8"><div><div className="mb-6 flex items-center justify-between"><span className="text-[11px] font-bold uppercase tracking-[0.15em] text-[#8B6B52]">ESTADO DEL SISTEMA</span><span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/70 text-[#745138]"><Icon name="shield" size={21}/></span></div><h3 className="max-w-[250px] text-[24px] font-semibold leading-tight tracking-[-0.04em] text-[#3B2B21]">Gestión de cuentas disponible</h3><p className="mt-4 text-[13px] leading-6 text-[#8D7767]">El registro y la consulta utilizan los servicios y repositorios reales de Spring Boot.</p></div><div className="mt-6 border-t border-[#D9C6B5] pt-4 text-xs font-semibold text-[#775840]">{loading ? 'Consultando usuarios…' : `${users.length} usuario${users.length === 1 ? '' : 's'} registrado${users.length === 1 ? '' : 's'}`}</div></div>
          </section>
          <section className="mt-6 grid gap-4 sm:grid-cols-3"><StatCard label="Usuarios registrados" value={users.length} icon="users" foot="Cuentas en la plataforma"/><StatCard label="Usuarios activos" value={activeUsers} icon="check" foot="Estado ACTIVE"/><StatCard label="Otros estados" value={blockedUsers} icon="shield" foot="Bloqueados o inactivos"/></section>
          <section className="mt-7 grid gap-6 xl:grid-cols-[minmax(0,1.25fr)_minmax(0,1fr)]">
            <div className="rounded-[24px] border border-[#ECE6DF] bg-white p-6 sm:p-7"><div className="mb-6 flex items-center justify-between"><div><h3 className="text-lg font-bold tracking-tight">Próximas funciones</h3><p className="mt-1 text-[12px] text-[#9D9187]">Módulos que incorporaremos a Fintech</p></div><span className="rounded-full bg-[#F4EBE3] px-3 py-1 text-[10px] font-semibold text-[#865F43]">EN DESARROLLO</span></div><div className="space-y-3">{[{ icon: 'wallet' as IconName, title: 'Billeteras digitales', description: 'Saldos, recargas y retiros' }, { icon: 'transfer' as IconName, title: 'Transferencias', description: 'Entre billeteras y usuarios' }, { icon: 'gift' as IconName, title: 'Recompensas', description: 'Puntos, niveles y beneficios' }].map(item => <div key={item.title} className="flex items-center gap-4 rounded-xl border border-[#F0EBE6] bg-[#FCFBF9] px-4 py-4"><div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#F0E4D9] text-[#76543D]"><Icon name={item.icon}/></div><div className="flex-1"><p className="text-[13px] font-bold text-[#34261E]">{item.title}</p><p className="mt-1 text-[11px] text-[#A09388]">{item.description}</p></div><span className="text-[10px] font-semibold text-[#AB9E91]">PRONTO</span></div>)}</div></div>
            <div className="rounded-[24px] border border-[#ECE6DF] bg-white p-6 sm:p-7"><div className="mb-6 flex items-center justify-between"><div><h3 className="text-lg font-bold tracking-tight">Registros recientes</h3><p className="mt-1 text-[12px] text-[#9D9187]">Usuarios añadidos a la plataforma</p></div><Icon name="clock" size={19} className="text-[#947763]"/></div>{recentUsers.length ? <div className="space-y-5">{recentUsers.map(user => <div key={user.id} className="flex items-center gap-3"><div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#EADFD5] text-[11px] font-bold text-[#664832]">{initials(user)}</div><div className="min-w-0 flex-1"><p className="truncate text-[12px] font-bold text-[#322820]">{user.firstName} {user.lastName}</p><p className="truncate text-[11px] text-[#A79B91]">Usuario registrado</p></div><span className="whitespace-nowrap text-[10px] text-[#A99D93]">{dateLabel(user.createdAt)}</span></div>)}</div> : <div className="flex min-h-[190px] flex-col items-center justify-center text-center"><span className="mb-3 rounded-full bg-[#F5EEE7] p-4 text-[#927258]"><Icon name="users" size={23}/></span><p className="text-sm font-semibold">Sin registros todavía</p><p className="mt-1 text-xs text-[#AB9E94]">Los nuevos usuarios aparecerán aquí</p></div>}<button onClick={() => navigate('users')} className="mt-6 flex items-center gap-2 text-[12px] font-bold text-[#77543A] hover:underline">Ver todos los usuarios <Icon name="arrow" size={15}/></button></div>
          </section>
        </> : <>
          <section className="mb-6 grid gap-4 sm:grid-cols-3"><StatCard label="Total de usuarios" value={users.length} icon="users" foot="Registros existentes"/><StatCard label="Cuentas activas" value={activeUsers} icon="check" foot="Disponibles en la plataforma"/><StatCard label="Otros estados" value={blockedUsers} icon="shield" foot="Bloqueadas o inactivas"/></section>
          <section className="overflow-hidden rounded-[24px] border border-[#ECE6DF] bg-white shadow-[0_10px_40px_rgba(55,33,12,0.025)]"><div className="flex flex-wrap items-center justify-between gap-4 border-b border-[#F0EBE6] px-6 py-6 sm:px-7"><div><h2 className="text-lg font-bold tracking-tight">Directorio de usuarios</h2><p className="mt-1 text-xs text-[#A2968C]">{filteredUsers.length} resultado{filteredUsers.length === 1 ? '' : 's'}</p></div><div className="flex w-full flex-wrap items-center gap-2 sm:w-auto"><div className="flex min-w-0 flex-1 items-center gap-2 rounded-xl border border-[#E8E0D9] bg-[#FCFBFA] px-3 text-[#9B8573] sm:w-64 sm:flex-none"><Icon name="search" size={17}/><input aria-label="Buscar usuarios" type="search" placeholder="Buscar usuario..." value={search} onChange={event => setSearch(event.target.value)} className="min-w-0 w-full bg-transparent py-2.5 text-xs text-[#33271F] outline-none placeholder:text-[#B1A69C]"/></div><button aria-label="Actualizar usuarios" title="Actualizar usuarios" type="button" disabled={loading} onClick={() => void refreshUsers()} className="flex h-10 w-10 items-center justify-center rounded-xl border border-[#E8E0D9] text-[#78543D] transition hover:bg-[#F3ECE5] disabled:opacity-50"><Icon name="refresh" size={17}/></button></div></div>
            <div className="overflow-x-auto"><table className="w-full min-w-[730px] text-left"><thead className="bg-[#FAF8F5] text-[10px] font-bold uppercase tracking-[0.09em] text-[#9B8B7E]"><tr><th className="px-7 py-4">Usuario</th><th className="px-5 py-4">Identificación</th><th className="px-5 py-4">Rol</th><th className="px-5 py-4">Estado</th><th className="px-5 py-4">Fecha de registro</th></tr></thead><tbody className="divide-y divide-[#F2EDE8]">{filteredUsers.map(user => <tr key={user.id} className="transition hover:bg-[#FCFAF8]"><td className="px-7 py-4"><div className="flex items-center gap-3"><span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#ECE0D5] text-[11px] font-bold text-[#755238]">{initials(user)}</span><div className="min-w-0"><p className="text-[12px] font-bold text-[#2E241E]">{user.firstName} {user.lastName}</p><p className="mt-1 text-[11px] text-[#9A8B80]">{user.email}</p></div></div></td><td className="px-5 py-4 text-[12px] text-[#796C62]">{user.identification}</td><td className="px-5 py-4 text-[11px] font-semibold text-[#765C49]">{user.role}</td><td className="px-5 py-4"><span className={`inline-flex rounded-full px-3 py-1.5 text-[10px] font-bold ${user.status === 'ACTIVE' ? 'bg-[#F0E6DA] text-[#73543C]' : 'bg-[#EAE7E3] text-[#6D635D]'}`}>{user.status}</span></td><td className="px-5 py-4 text-[11px] text-[#94877D]">{dateLabel(user.createdAt)}</td></tr>)}</tbody></table>{!loading && filteredUsers.length === 0 && <div className="py-16 text-center"><p className="text-sm font-semibold text-[#544237]">{search ? 'No encontramos coincidencias' : 'Aún no hay usuarios'}</p><p className="mt-2 text-xs text-[#A99B8F]">{search ? 'Prueba con otro nombre, correo o identificación.' : 'Registra el primer usuario para comenzar.'}</p></div>}{loading && <div className="py-10 text-center text-xs text-[#9E8E80]">Consultando usuarios...</div>}</div>
          </section>
        </>}
        <footer className="mt-12 flex flex-wrap items-center justify-between gap-2 border-t border-[#E7E0D9] py-6 text-[11px] text-[#AA9B8F]"><span>© Fintech · Plataforma de billeteras digitales</span><span>Sesión administrativa · {currentUser.firstName} {currentUser.lastName}</span></footer>
      </main>
    </div>
    {registerOpen && <div className="fixed inset-0 z-50 flex justify-end bg-[#17120F]/50 backdrop-blur-[2px]" onMouseDown={event => { if (event.target === event.currentTarget && !saving) setRegisterOpen(false) }}><section role="dialog" aria-modal="true" aria-labelledby="register-title" className="flex h-full w-full max-w-[480px] flex-col overflow-y-auto bg-white shadow-2xl" onKeyDown={event => { if (event.key === 'Escape' && !saving) setRegisterOpen(false) }}><div className="flex items-start justify-between border-b border-[#F0EAE5] px-7 py-7"><div><p className="mb-2 text-[10px] font-bold uppercase tracking-[0.17em] text-[#A28266]">GESTIÓN DE CUENTAS</p><h2 id="register-title" className="text-2xl font-bold tracking-tight text-[#241C18]">Registrar usuario</h2><p className="mt-2 text-xs text-[#9F9288]">Ingresa la información para crear un registro.</p></div><button aria-label="Cerrar formulario" type="button" disabled={saving} onClick={() => setRegisterOpen(false)} className="rounded-lg p-2 text-[#7F6C5D] hover:bg-[#F7F2EC] disabled:opacity-50"><Icon name="close"/></button></div><form onSubmit={handleRegister} className="flex flex-1 flex-col px-7 py-7"><div className="space-y-5">{formFields.map(field => <label key={field.key} className="block"><span className="mb-2 block text-xs font-bold text-[#4A3A2E]">{field.label}</span><input required disabled={saving} type={field.type} value={form[field.key]} onChange={event => setForm(current => ({ ...current, [field.key]: event.target.value }))} placeholder={field.placeholder} className="w-full rounded-xl border border-[#E3DBD3] bg-[#FCFBFA] px-4 py-3 text-sm text-[#241C18] outline-none transition placeholder:text-[#B5A99F] focus:border-[#A67A56] focus:ring-2 focus:ring-[#F2E5D8] disabled:opacity-50"/></label>)}</div>{error && <p role="alert" className="mt-5 rounded-xl border border-[#DBC2AE] bg-[#F7ECE3] p-3 text-xs text-[#754D30]">{error}</p>}<div className="mt-auto space-y-3 pt-8"><p className="flex items-start gap-2 text-[11px] leading-5 text-[#9E8F82]"><Icon name="shield" size={17} className="mt-0.5 shrink-0"/> Este registro administrativo crea un perfil sin contraseña. Para iniciar sesión se necesita una cuenta con credenciales.</p><button disabled={saving} type="submit" className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#6F4E37] py-3.5 text-sm font-bold text-white transition hover:bg-[#563C2B] disabled:opacity-50"><Icon name="plus" size={18}/>{saving ? 'Registrando...' : 'Crear usuario'}</button><button type="button" disabled={saving} onClick={() => setRegisterOpen(false)} className="w-full rounded-xl border border-[#E6DED7] py-3 text-xs font-semibold text-[#806954] hover:bg-[#FAF7F4] disabled:opacity-50">Cancelar</button></div></form></section></div>}
  </div>
}
export default AdminDashboard
