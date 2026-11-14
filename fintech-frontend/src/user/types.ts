export type Role = 'USER' | 'ADMIN' | 'SUPERADMIN'

export type UserStatus = 'ACTIVE' | 'BLOCKED' | 'INACTIVE'

export interface UserRequest {
    identification: string
    firstName: string
    lastName: string
    email: string
}

export interface UserResponse extends UserRequest {
    id: string
    role: Role
    status: UserStatus
    createdAt: string
}
