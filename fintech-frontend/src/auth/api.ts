import type {UserResponse} from '../user/types'

export interface AccountRegistrationRequest {
    identification: string
    firstName: string
    lastName: string
    email: string
    password: string
}

interface CsrfToken {
    headerName: string
    token: string
}

export class ApiError extends Error {
    readonly status: number

    constructor(status: number, message: string) {
        super(message)
        this.name = 'ApiError'
        this.status = status
    }
}

export async function readJson<T>(response: Response): Promise<T> {
    if (!response.ok) {
        let message = `No se pudo completar la solicitud (${response.status}).`
        const body: unknown = await response.json().catch(() => null)
        if (body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') {
            message = body.message
        }
        throw new ApiError(response.status, message)
    }
    return (await response.json()) as T
}

async function getCsrfToken(): Promise<CsrfToken> {
    const response = await fetch('/api/auth/csrf', {credentials: 'same-origin', cache: 'no-store'})
    return readJson<CsrfToken>(response)
}

export async function csrfFetch(url: string, options: RequestInit): Promise<Response> {
    const csrf = await getCsrfToken()
    const headers = new Headers(options.headers)
    headers.set(csrf.headerName, csrf.token)
    return fetch(url, {...options, headers, credentials: 'same-origin'})
}

export async function getCurrentUser(signal?: AbortSignal): Promise<UserResponse | null> {
    const response = await fetch('/api/auth/me', {credentials: 'same-origin', cache: 'no-store', signal})
    if (response.status === 401) return null
    return readJson<UserResponse>(response)
}

export async function login(email: string, password: string): Promise<UserResponse> {
    const body = new URLSearchParams({email, password})
    const response = await csrfFetch('/api/auth/login', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body
    })
    await readJson<{ message: string }>(response)
    const user = await getCurrentUser()
    if (!user) throw new Error('La sesión no se pudo recuperar. Intenta iniciar sesión nuevamente.')
    return user
}

export async function registerAccount(request: AccountRegistrationRequest): Promise<UserResponse> {
    const response = await csrfFetch('/api/auth/register', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(request)
    })
    return readJson<UserResponse>(response)
}

export async function logout(): Promise<void> {
    const response = await csrfFetch('/api/auth/logout', {method: 'POST'})
    if (!response.ok) throw new ApiError(response.status, 'No se pudo cerrar la sesión.')
}
