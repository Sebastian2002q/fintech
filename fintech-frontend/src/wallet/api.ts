import { csrfFetch, readJson } from '../auth/api'
import type { WalletRequest, WalletResponse } from './types'

export async function getWallets(signal?: AbortSignal): Promise<WalletResponse[]> {
  const response = await fetch('/api/wallets', { credentials: 'same-origin', cache: 'no-store', signal })
  return readJson<WalletResponse[]>(response)
}
export async function createWallet(request: WalletRequest): Promise<WalletResponse> {
  const response = await csrfFetch('/api/wallets', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(request) })
  return readJson<WalletResponse>(response)
}
export async function updateWallet(id: string, request: WalletRequest): Promise<WalletResponse> {
  const response = await csrfFetch(`/api/wallets/${encodeURIComponent(id)}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(request) })
  return readJson<WalletResponse>(response)
}
export async function setWalletActive(id: string, active: boolean): Promise<WalletResponse> {
  const action = active ? 'activate' : 'deactivate'
  const response = await csrfFetch(`/api/wallets/${encodeURIComponent(id)}/${action}`, { method: 'PATCH' })
  return readJson<WalletResponse>(response)
}
