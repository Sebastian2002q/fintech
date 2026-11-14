import { csrfFetch, readJson } from '../auth/api'
import type { UserRequest, UserResponse } from './types'

export async function getUsers(signal?: AbortSignal): Promise<UserResponse[]> {
  const response = await fetch('/api/users', { credentials: 'same-origin', cache: 'no-store', signal })
  return readJson<UserResponse[]>(response)
}

export async function registerUser(user: UserRequest): Promise<UserResponse> {
  const response = await csrfFetch('/api/users', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(user)
  })
  return readJson<UserResponse>(response)
}
