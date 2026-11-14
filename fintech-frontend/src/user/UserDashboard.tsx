import { useState } from 'react'
import type { UserResponse } from './types'
import WalletPanel from '../wallet/WalletPanel'

interface Props {
  user: UserResponse
  onLogout: () => Promise<void>
}
function formatDate(date: string) {
  const value = new Date(date)
  return Number.isNaN(value.getTime()) ? 'Sin fecha' : value.toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' })
}
const upcoming = [
  { symbol: '▣', title: 'Mis billeteras', description: 'Organiza el dinero en distintas billeteras.' },
  { symbol: '⇄', title: 'Transferencias', description: 'Envía dinero entre billeteras y usuarios.' },
  { symbol: '◷', title: 'Movimientos', description: 'Consulta el historial de tus operaciones.' },
  { symbol: '◇', title: 'Recompensas', description: 'Acumula puntos y desbloquea beneficios.' }
]
export default function UserDashboard({ user, onLogout }: Props) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)
  async function handleLogout() {
    setBusy(true)
    setError('')
    try { await onLogout() } catch (caught) { setError(caught instanceof Error ? caught.message : 'No se pudo cerrar sesión.'); setBusy(false) }
  }
  return <div className="min-h-screen bg-[#F8F6F3] font-sans text-[#241C18]">
    {menuOpen && <button aria-label="Cerrar menú" onClick={() => setMenuOpen(false)} className="fixed inset-0 z-30 bg-black/50 lg:hidden"/>}
    <aside className={`fixed inset-y-0 left-0 z-40 flex w-[252px] flex-col border-r border-[#E7E0D9] bg-white transition-transform lg:translate-x-0 ${menuOpen ? 'translate-x-0' : '-translate-x-full'}`}>
      <div className="flex h-[84px] items-center gap-3 border-b border-[#F0EBE7] px-7"><div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#241C18] text-xl font-extrabold text-[#E9D5C1]">F.</div><div><p className="text-xl font-extrabold tracking-tight">Fintech</p><p className="text-[10px] font-semibold uppercase tracking-widest text-[#947A66]">Digital Wallet</p></div></div>
      <nav className="flex-1 space-y-1.5 px-4 py-7"><p className="px-4 pb-3 text-[10px] font-bold uppercase tracking-widest text-[#A89B90]">MI ESPACIO</p><a href="#mis-billeteras" onClick={() => setMenuOpen(false)} className="block rounded-xl bg-[#F1E9E1] px-4 py-3.5 text-[13px] font-semibold text-[#6F4E37]">◫ Mis billeteras</a>{upcoming.filter(item => item.title !== 'Mis billeteras').map(item => <div key={item.title} className="flex items-center justify-between rounded-xl px-4 py-3.5 text-[13px] font-medium text-[#A49A91]"><span>{item.symbol} {item.title}</span><span className="text-[9px]">PRONTO</span></div>)}</nav>
      <div className="p-4"><div className="rounded-2xl border border-[#E8DED5] bg-[#FAF7F3] p-4"><p className="text-[12px] font-bold text-[#6F4E37]">Cuenta personal</p><p className="mt-2 text-[11px] leading-5 text-[#93867B]">Tus billeteras y movimientos estarán organizados aquí, en un solo lugar.</p></div></div>
    </aside>
    <div className="lg:pl-[252px]"><header className="sticky top-0 z-20 flex h-[84px] items-center justify-between gap-4 border-b border-[#EDE8E2] bg-white/95 px-5 backdrop-blur sm:px-8 lg:px-10"><div className="flex items-center gap-3"><button aria-label="Abrir menú" onClick={() => setMenuOpen(true)} className="rounded-lg border border-[#E8DED5] px-3 py-2 lg:hidden">☰</button><span className="text-[13px] font-bold">Mi espacio / Resumen</span></div><div className="flex items-center gap-3"><div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#E9D9CA] text-xs font-extrabold text-[#64452F]">{user.firstName.slice(0, 1)}{user.lastName.slice(0, 1)}</div><button type="button" disabled={busy} onClick={() => void handleLogout()} className="rounded-xl border border-[#E8DED5] px-4 py-2.5 text-xs font-bold text-[#6F4E37] hover:bg-[#F7F1EB] disabled:opacity-60">{busy ? 'Saliendo...' : 'Cerrar sesión'}</button></div></header>
      <main className="mx-auto max-w-[1400px] px-5 py-8 sm:px-8 lg:px-10 lg:py-10"><p className="mb-2 text-[11px] font-bold uppercase tracking-[0.2em] text-[#A08067]">MI CUENTA FINTECH</p><h1 className="text-4xl font-bold tracking-[-0.055em]">Hola, {user.firstName}</h1><p className="mt-3 text-[13px] text-[#988D83]">Bienvenido a tu espacio financiero personal.</p>{error && <p role="alert" className="mt-6 rounded-xl border border-[#D7BBA5] bg-[#F6ECE3] px-4 py-3 text-sm text-[#72482D]">{error}</p>}
        <div className="mt-8 grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(280px,1fr)]"><section className="relative flex min-h-[290px] flex-col justify-between overflow-hidden rounded-[26px] bg-[#241C18] p-8 text-white sm:p-10"><div className="pointer-events-none absolute -right-24 -top-32 h-80 w-80 rounded-full border-[52px] border-[#7A573F]/30"/><div className="pointer-events-none absolute -bottom-44 right-6 h-80 w-80 rounded-full border-[65px] border-[#C09C78]/15"/><div className="relative"><span className="rounded-full border border-[#896E59]/45 bg-white/10 px-3 py-2 text-[10px] font-bold tracking-widest text-[#E4CBB1]">TU CUENTA DIGITAL</span><h2 className="mt-8 max-w-md text-3xl font-semibold leading-tight tracking-tight sm:text-4xl">Tus finanzas, a tu manera.</h2><p className="mt-4 max-w-sm text-[13px] leading-6 text-[#BFB0A4]">Crea y organiza tus billeteras digitales en un mismo lugar.</p></div><p className="relative mt-8 text-xs font-semibold text-[#D2B79B]">Tus billeteras están listas para empezar</p></section><section className="rounded-[26px] border border-[#EBE2DA] bg-[#F0E5DA] p-8"><div className="mb-6 flex items-center justify-between"><h3 className="text-[11px] font-bold uppercase tracking-widest text-[#8B6B52]">MI PERFIL</h3><span className="rounded-full bg-white/70 px-3 py-1 text-[10px] font-bold text-[#745138]">{user.status}</span></div><p className="text-2xl font-bold text-[#3B2B21]">{user.firstName} {user.lastName}</p><p className="mt-2 break-words text-sm text-[#806D5D]">{user.email}</p><div className="mt-7 space-y-4 border-t border-[#D8C7B7] pt-5 text-xs"><div><p className="mb-1 text-[#958072]">Identificación</p><p className="font-semibold text-[#3B2B21]">{user.identification}</p></div><div><p className="mb-1 text-[#958072]">Miembro desde</p><p className="font-semibold text-[#3B2B21]">{formatDate(user.createdAt)}</p></div></div></section></div>
        <WalletPanel/><section className="mt-8"><div className="mb-5"><h3 className="text-xl font-bold">Próximas funciones</h3><p className="mt-1 text-[13px] text-[#988D83]">Estas funciones se habilitarán a medida que avancemos con la plataforma.</p></div><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{upcoming.filter(item => item.title !== 'Mis billeteras').map(item => <div key={item.title} className="rounded-[22px] border border-[#ECE6DF] bg-white p-6"><div className="mb-5 flex h-11 w-11 items-center justify-center rounded-xl bg-[#F1E8E0] text-2xl text-[#745138]">{item.symbol}</div><p className="text-sm font-bold">{item.title}</p><p className="mt-2 min-h-12 text-xs leading-5 text-[#9D9187]">{item.description}</p><span className="mt-5 inline-block rounded-full bg-[#F4EDE6] px-3 py-1.5 text-[10px] font-bold text-[#8C7059]">PRÓXIMAMENTE</span></div>)}</div></section>
        <footer className="mt-12 border-t border-[#E7E0D9] py-6 text-[11px] text-[#AA9B8F]">Fintech · Plataforma académica de billeteras digitales</footer>
      </main>
    </div>
  </div>
}
