import {useState, type FormEvent} from 'react'
import type {AccountRegistrationRequest} from './api'

type Mode = 'login' | 'register'

interface Props {
    onLogin: (email: string, password: string) => Promise<void>
    onRegister: (data: AccountRegistrationRequest) => Promise<void>
    initialError?: string
}

const initialRegister: AccountRegistrationRequest = {
    identification: '',
    firstName: '',
    lastName: '',
    email: '',
    password: ''
}
const inputs: { key: keyof AccountRegistrationRequest; label: string; type: string; autocomplete: string }[] = [
    {key: 'identification', label: 'Identificación', type: 'text', autocomplete: 'off'},
    {key: 'firstName', label: 'Nombres', type: 'text', autocomplete: 'given-name'},
    {key: 'lastName', label: 'Apellidos', type: 'text', autocomplete: 'family-name'},
    {key: 'email', label: 'Correo electrónico', type: 'email', autocomplete: 'email'},
    {key: 'password', label: 'Contraseña', type: 'password', autocomplete: 'new-password'}
]
const inputClass = 'w-full rounded-xl border border-[#E1D8D0] bg-[#FDFBF9] px-4 py-3.5 text-sm text-[#2A211C] outline-none transition placeholder:text-[#B4A59A] focus:border-[#9C704E] focus:ring-2 focus:ring-[#F0E4D8] disabled:opacity-60'

function messageOf(error: unknown) {
    return error instanceof Error ? error.message : 'Ocurrió un error inesperado.'
}

export default function AuthScreen({onLogin, onRegister, initialError = ''}: Props) {
    const [mode, setMode] = useState<Mode>('login')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [registerForm, setRegisterForm] = useState<AccountRegistrationRequest>(initialRegister)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(initialError)
    const [notice, setNotice] = useState('')

    function changeMode(next: Mode) {
        setMode(next);
        setError('');
        setNotice('')
    }

    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setBusy(true)
        setError('')
        setNotice('')
        try {
            if (mode === 'login') {
                await onLogin(email, password)
                setPassword('')
            } else {
                if (new TextEncoder().encode(registerForm.password).length > 72) {
                    throw new Error('La contraseña no puede superar 72 bytes UTF-8.')
                }
                await onRegister(registerForm)
                setEmail(registerForm.email)
                setRegisterForm(initialRegister)
                setMode('login')
                setNotice('Cuenta creada correctamente. Ahora puedes iniciar sesión.')
            }
        } catch (caught) {
            setError(messageOf(caught))
        } finally {
            setBusy(false)
        }
    }

    return <div
        className="min-h-screen bg-[#F8F6F3] text-[#241C18] lg:grid lg:grid-cols-[minmax(0,1.05fr)_minmax(440px,0.95fr)]">
        <aside
            className="relative hidden min-h-screen flex-col justify-between overflow-hidden bg-[#241C18] p-12 text-white lg:flex xl:p-16">
            <div
                className="pointer-events-none absolute -right-36 -top-44 h-[560px] w-[560px] rounded-full border-[92px] border-[#6F4E37]/40"/>
            <div
                className="pointer-events-none absolute -bottom-72 -left-52 h-[680px] w-[680px] rounded-full border-[100px] border-[#A88569]/20"/>
            <div className="relative flex items-center gap-3">
                <div
                    className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#F0DFCC] text-2xl font-black text-[#241C18]">F.
                </div>
                <div>
                    <div className="text-2xl font-extrabold tracking-tight">Fintech</div>
                    <div className="text-[10px] font-bold uppercase tracking-[0.22em] text-[#C6A98F]">Digital Wallet
                    </div>
                </div>
            </div>
            <div className="relative max-w-xl">
                <div
                    className="mb-7 inline-flex items-center gap-2 rounded-full border border-[#83674F] bg-white/5 px-4 py-2 text-[11px] font-semibold uppercase tracking-widest text-[#E7C8AA]">
                    <span className="h-2 w-2 rounded-full bg-[#C7A27F]"/> Tu espacio financiero
                </div>
                <h1 className="text-[clamp(42px,5vw,76px)] font-semibold leading-[1.06] tracking-[-0.07em]">Tu
                    dinero.<br/><span className="text-[#CDAF93]">Tus decisiones.</span><br/>Tu futuro.</h1><p
                className="mt-8 max-w-md text-[15px] leading-7 text-[#C7B9AC]">Una plataforma digital para organizar tus
                billeteras, realizar movimientos y alcanzar tus metas. Estamos construyendo cada funcionalidad paso a
                paso.</p>
                <div className="mt-10 grid max-w-md grid-cols-2 gap-3">
                    <div className="rounded-2xl border border-[#604C3C] bg-white/5 px-5 py-5">
                        <div className="mb-2 text-2xl text-[#D7B99D]">◈</div>
                        <p className="text-xs font-semibold">Tu cuenta protegida</p><p
                        className="mt-1 text-[11px] text-[#AD9E90]">Sesiones autenticadas</p></div>
                    <div className="rounded-2xl border border-[#604C3C] bg-white/5 px-5 py-5">
                        <div className="mb-2 text-2xl text-[#D7B99D]">◇</div>
                        <p className="text-xs font-semibold">Control en un lugar</p><p
                        className="mt-1 text-[11px] text-[#AD9E90]">Billeteras próximamente</p></div>
                </div>
            </div>
            <p className="relative text-xs text-[#8F7A67]">Fintech · Plataforma académica de billeteras digitales</p>
        </aside>
        <main className="flex min-h-screen items-center justify-center px-5 py-10 sm:px-10 lg:px-14">
            <div className="w-full max-w-[480px]">
                <div className="mb-9 flex items-center gap-3 lg:hidden"><span
                    className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#241C18] text-2xl font-bold text-[#EDD7C0]">F.</span><span
                    className="text-2xl font-extrabold">Fintech</span></div>
                <p className="mb-3 text-[11px] font-bold uppercase tracking-[0.22em] text-[#9D795E]">ACCESO A TU
                    CUENTA</p>
                <h2 className="text-4xl font-bold tracking-[-0.055em] sm:text-[42px]">{mode === 'login' ? 'Bienvenido de nuevo' : 'Crea tu cuenta'}</h2>
                <p className="mt-3 text-sm leading-6 text-[#8E8177]">{mode === 'login' ? 'Ingresa con tu correo y contraseña para continuar.' : 'Registra tus datos para empezar a usar Fintech.'}</p>
                <div className="mt-8 grid grid-cols-2 rounded-xl border border-[#E6DED6] bg-[#F0EBE5] p-1">
                    <button type="button" disabled={busy} onClick={() => changeMode('login')}
                            className={`rounded-lg py-3 text-sm font-semibold transition ${mode === 'login' ? 'bg-white text-[#3A281F] shadow-sm' : 'text-[#887569] hover:text-[#4F382A]'}`}>Iniciar
                        sesión
                    </button>
                    <button type="button" disabled={busy} onClick={() => changeMode('register')}
                            className={`rounded-lg py-3 text-sm font-semibold transition ${mode === 'register' ? 'bg-white text-[#3A281F] shadow-sm' : 'text-[#887569] hover:text-[#4F382A]'}`}>Crear
                        cuenta
                    </button>
                </div>
                {notice && <p role="status"
                              className="mt-5 rounded-xl border border-[#D9C5B0] bg-[#F1E8DF] px-4 py-3 text-sm text-[#654832]">{notice}</p>}
                {error && <p role="alert"
                             className="mt-5 rounded-xl border border-[#D7B8A2] bg-[#F8EAE0] px-4 py-3 text-sm text-[#743D28]">{error}</p>}
                <form onSubmit={submit} className="mt-7 space-y-5">
                    {mode === 'login' ? <>
                        <label className="block"><span className="mb-2 block text-xs font-bold text-[#514035]">Correo electrónico</span><input
                            type="email" required autoComplete="username" disabled={busy} value={email}
                            onChange={event => setEmail(event.target.value)} placeholder="correo@ejemplo.com"
                            className={inputClass}/></label>
                        <label className="block"><span
                            className="mb-2 block text-xs font-bold text-[#514035]">Contraseña</span><input
                            type="password" required autoComplete="current-password" disabled={busy} value={password}
                            onChange={event => setPassword(event.target.value)} placeholder="Tu contraseña"
                            className={inputClass}/></label>
                    </> : <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                        {inputs.map(field => <label key={field.key}
                                                    className={`block ${field.key === 'email' || field.key === 'password' || field.key === 'identification' ? 'sm:col-span-2' : ''}`}><span
                            className="mb-2 block text-xs font-bold text-[#514035]">{field.label}</span><input
                            type={field.type} autoComplete={field.autocomplete}
                            minLength={field.key === 'password' ? 12 : undefined} required disabled={busy}
                            value={registerForm[field.key]} onChange={event => setRegisterForm(current => ({
                            ...current,
                            [field.key]: event.target.value
                        }))} placeholder={field.key === 'password' ? 'Mínimo 12 caracteres' : field.label}
                            className={inputClass}/></label>)}
                    </div>}
                    <button type="submit" disabled={busy}
                            className="flex w-full items-center justify-center rounded-xl bg-[#6F4E37] px-5 py-4 text-sm font-bold text-white shadow-[0_8px_22px_rgba(111,78,55,0.17)] transition hover:bg-[#563B2A] disabled:opacity-60">{busy ? 'Procesando...' : mode === 'login' ? 'Entrar a mi cuenta' : 'Crear mi cuenta'}</button>
                </form>
                <p className="mt-10 text-center text-[11px] text-[#B0A297]">Fintech · © 2026</p>
            </div>
        </main>
    </div>
}
