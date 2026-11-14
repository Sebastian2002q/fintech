import { useEffect, useState, type FormEvent } from 'react'
import { createWallet, getWallets, setWalletActive, updateWallet } from './api'
import { WALLET_TYPES, type WalletRequest, type WalletResponse, type WalletType } from './types'

const emptyForm: WalletRequest = { name: '', type: 'SAVINGS' }
const formats = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 2, maximumFractionDigits: 2 })
function formatMoney(balance: number) { return formats.format(balance) }
function errorText(error: unknown) { return error instanceof Error ? error.message : 'No se pudo completar la operación.' }
function walletIcon(type: WalletType) {
  return ({ SAVINGS: '◇', DAILY_EXPENSES: '◈', SHOPPING: '▣', TRANSPORT: '↗', INVESTMENT: '▥' } as const)[type]
}
export default function WalletPanel() {
  const [wallets, setWallets] = useState<WalletResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<WalletResponse | null>(null)
  const [form, setForm] = useState<WalletRequest>(emptyForm)
  const [pendingDeactivate, setPendingDeactivate] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  useEffect(() => {
    const controller = new AbortController()
    getWallets(controller.signal).then(setWallets).catch(caught => {
      if (!controller.signal.aborted) setError(errorText(caught))
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false)
    })
    return () => controller.abort()
  }, [])
  function openCreate() { setEditing(null); setForm(emptyForm); setError(''); setFormOpen(true) }
  function openEdit(wallet: WalletResponse) { setEditing(wallet); setForm({ name: wallet.name, type: wallet.type }); setError(''); setFormOpen(true) }
  function closeForm() { setFormOpen(false); setEditing(null); setForm(emptyForm) }
  async function refresh() {
    setLoading(true); setError('')
    try { setWallets(await getWallets()) } catch (caught) { setError(errorText(caught)) } finally { setLoading(false) }
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setError(''); setMessage('')
    try {
      const saved = editing ? await updateWallet(editing.id, form) : await createWallet(form)
      setWallets(current => editing ? current.map(item => item.id === saved.id ? saved : item) : [...current, saved])
      setMessage(editing ? 'Billetera actualizada.' : 'Billetera creada correctamente.'); closeForm()
    } catch (caught) { setError(errorText(caught)) } finally { setSaving(false) }
  }
  async function changeStatus(wallet: WalletResponse, activate: boolean) {
    setSaving(true); setError(''); setMessage(''); setPendingDeactivate(null)
    try {
      const saved = await setWalletActive(wallet.id, activate)
      setWallets(current => current.map(item => item.id === saved.id ? saved : item))
      setMessage(activate ? 'Billetera activada.' : 'Billetera desactivada.')
    } catch (caught) { setError(errorText(caught)) } finally { setSaving(false) }
  }
  const activeCount = wallets.filter(wallet => wallet.status === 'ACTIVE').length
  return <section id="mis-billeteras" className="scroll-mt-28 space-y-5">
    <div className="flex flex-wrap items-center justify-between gap-4">
      <div><p className="mb-2 text-[11px] font-bold uppercase tracking-[0.19em] text-[#A08067]">MI DINERO</p><h2 className="text-2xl font-bold tracking-tight">Mis billeteras</h2><p className="mt-1 text-[13px] text-[#988D83]">Organiza tus fondos por propósito. Los movimientos se habilitarán en el módulo Transacciones.</p></div>
      <button type="button" onClick={openCreate} className="rounded-xl bg-[#6F4E37] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#563C2B]">+ Nueva billetera</button>
    </div>
    <div className="grid gap-4 sm:grid-cols-2">
      <div className="rounded-[20px] border border-[#E9E0D8] bg-white px-6 py-5"><p className="text-xs text-[#988D83]">Billeteras creadas</p><p className="mt-2 text-3xl font-extrabold">{wallets.length}</p></div>
      <div className="rounded-[20px] border border-[#E9E0D8] bg-white px-6 py-5"><p className="text-xs text-[#988D83]">Billeteras activas</p><p className="mt-2 text-3xl font-extrabold text-[#6F4E37]">{activeCount}</p></div>
    </div>
    {error && <p role="alert" className="rounded-xl border border-[#D7B8A2] bg-[#F8EAE0] px-4 py-3 text-sm text-[#743D28]">{error}</p>}
    {message && <p role="status" className="rounded-xl border border-[#D9C5B0] bg-white px-4 py-3 text-sm text-[#654832]">{message}</p>}
    {formOpen && <form onSubmit={submit} className="rounded-[24px] border border-[#DED2C5] bg-white p-6 shadow-sm">
      <div className="mb-5 flex items-center justify-between"><h3 className="text-lg font-bold">{editing ? 'Editar billetera' : 'Nueva billetera'}</h3><button type="button" onClick={closeForm} className="text-sm font-semibold text-[#8D7967]">Cancelar</button></div>
      <div className="grid gap-4 sm:grid-cols-2"><label className="block text-sm font-semibold">Nombre<input required maxLength={80} value={form.name} onChange={event => setForm(current => ({ ...current, name: event.target.value }))} placeholder="Ej. Vacaciones" disabled={saving} className="mt-2 w-full rounded-xl border border-[#DAD0C6] bg-white px-4 py-3 outline-none focus:border-[#6F4E37]"/></label>
      <label className="block text-sm font-semibold">Tipo<select value={form.type} onChange={event => setForm(current => ({ ...current, type: event.target.value as WalletType }))} disabled={saving} className="mt-2 w-full rounded-xl border border-[#DAD0C6] bg-white px-4 py-3 outline-none focus:border-[#6F4E37]">{Object.entries(WALLET_TYPES).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label></div>
      <button disabled={saving} type="submit" className="mt-5 rounded-xl bg-[#241C18] px-6 py-3 text-sm font-bold text-white hover:bg-[#4B382B] disabled:opacity-60">{saving ? 'Guardando...' : editing ? 'Guardar cambios' : 'Crear billetera'}</button>
    </form>}
    <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
      {wallets.map(wallet => <article key={wallet.id} className="rounded-[24px] border border-[#E9E0D8] bg-white p-6 shadow-sm">
        <div className="flex items-start justify-between gap-3"><span className="flex h-11 w-11 items-center justify-center rounded-xl bg-[#F0E5DA] text-2xl font-bold text-[#6F4E37]">{walletIcon(wallet.type)}</span><span className={`rounded-full px-3 py-1.5 text-[10px] font-bold ${wallet.status === 'ACTIVE' ? 'bg-[#F2E7D9] text-[#6F4E37]' : 'bg-[#EAE7E4] text-[#55504A]'}`}>{wallet.status === 'ACTIVE' ? 'ACTIVA' : wallet.status === 'BLOCKED' ? 'BLOQUEADA' : 'INACTIVA'}</span></div>
        <h3 className="mt-5 truncate text-lg font-bold">{wallet.name}</h3><p className="text-xs text-[#988D83]">{WALLET_TYPES[wallet.type]}</p>
        <p className="mt-6 text-xs text-[#A39589]">Saldo disponible</p><p className="mt-1 text-2xl font-extrabold tracking-tight">{formatMoney(wallet.balance)}</p>
        <div className="mt-6 flex flex-wrap gap-2 border-t border-[#F1EAE4] pt-4"><button disabled={saving || wallet.status !== 'ACTIVE'} onClick={() => openEdit(wallet)} className="rounded-lg border border-[#D8C9BB] px-3 py-2 text-xs font-semibold text-[#6F4E37] disabled:opacity-40">Editar</button>
        {wallet.status === 'ACTIVE' ? <button disabled={saving} onClick={() => pendingDeactivate === wallet.id ? void changeStatus(wallet, false) : setPendingDeactivate(wallet.id)} className="rounded-lg border border-[#D8C9BB] px-3 py-2 text-xs font-semibold text-[#6F4E37] disabled:opacity-40">{pendingDeactivate === wallet.id ? 'Confirmar desactivación' : 'Desactivar'}</button> : wallet.status === 'INACTIVE' ? <button disabled={saving} onClick={() => void changeStatus(wallet, true)} className="rounded-lg border border-[#D8C9BB] px-3 py-2 text-xs font-semibold text-[#6F4E37] disabled:opacity-40">Activar</button> : null}</div>
      </article>)}
    </div>
    {loading && <p className="rounded-[24px] border border-[#E9E0D8] bg-white px-6 py-10 text-center text-sm text-[#988D83]">Cargando tus billeteras...</p>}
    {!loading && wallets.length === 0 && <div className="rounded-[24px] border border-dashed border-[#CDBBAA] bg-[#FCFAF8] px-6 py-12 text-center"><p className="text-lg font-bold">Aún no tienes billeteras</p><p className="mt-2 text-sm text-[#988D83]">Crea tu primera billetera para organizar tus finanzas.</p><button type="button" onClick={openCreate} className="mt-5 rounded-xl bg-[#6F4E37] px-5 py-3 text-sm font-bold text-white">Crear mi primera billetera</button></div>}
    <button type="button" onClick={() => void refresh()} disabled={loading} className="text-xs font-semibold text-[#6F4E37] hover:underline disabled:opacity-50">Actualizar billeteras</button>
  </section>
}
