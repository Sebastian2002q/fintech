export type WalletType = 'SAVINGS' | 'DAILY_EXPENSES' | 'SHOPPING' | 'TRANSPORT' | 'INVESTMENT'
export type WalletStatus = 'ACTIVE' | 'BLOCKED' | 'INACTIVE'
export interface WalletRequest {
  name: string
  type: WalletType
}
export interface WalletResponse extends WalletRequest {
  id: string
  ownerId: string
  balance: number
  status: WalletStatus
  createdAt: string
}
export const WALLET_TYPES: Record<WalletType, string> = {
  SAVINGS: 'Ahorros',
  DAILY_EXPENSES: 'Gastos diarios',
  SHOPPING: 'Compras',
  TRANSPORT: 'Transporte',
  INVESTMENT: 'Inversión'
}
