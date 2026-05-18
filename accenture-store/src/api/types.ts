export interface Product {
  id?: string;
  sku?: string;
  name: string;
  description?: string;
  category?: string;
  price?: number;
  unitPrice?: number;
  basePrice?: number;
  imageUrl?: string;
  stock?: number;
  stockQuantity?: number;
}

export interface ProductPayload {
  sku: string;
  name: string;
  description?: string;
  category: string;
  basePrice: number;
  stockQuantity: number;
  imageUrl?: string;
}

export interface Paginated<T> {
  content?: T[];
  items?: T[];
  data?: T[];
  totalPages?: number;
  totalElements?: number;
  number?: number;
  size?: number;
}

export interface CartItem {
  sku: string;
  name: string;
  unitPrice: number;
  imageUrl?: string;
  quantity: number;
}

export interface Customer {
  id?: string;
  customerId?: string;
  name?: string;
  email?: string;
  cpf?: string;
  phone?: string;
}

export interface Address {
  id?: string;
  zipCode: string;
  street: string;
  number: string;
  complement?: string;
  neighborhood?: string;
  district?: string;
  city: string;
  state: string;
}

export interface CepLookup {
  zipCode?: string;
  street?: string;
  logradouro?: string;
  complement?: string;
  neighborhood?: string;
  district?: string;
  bairro?: string;
  city?: string;
  localidade?: string;
  state?: string;
  uf?: string;
}

export interface OrderItem {
  sku: string;
  name?: string;
  quantity: number;
  unitPrice: number;
}

export type OrderStatus =
  | 'PENDING'
  | 'RESERVED'
  | 'PAID'
  | 'CANCELED'
  | 'REFUNDED'
  | string;

export interface OrderDeliveryAddress {
  zipCode?: string;
  street?: string;
  number?: string;
  complement?: string;
  neighborhood?: string;
  city?: string;
  state?: string;
}

export interface Order {
  id?: string;
  orderId?: string;
  customerId: string;
  status?: OrderStatus;
  items: OrderItem[];
  total?: number;
  amount?: number;
  totalAmount?: number;
  deliveryAddress?: OrderDeliveryAddress;
  createdAt?: string;
}

export interface CreateOrderRequest {
  addressId: string;
  items: { sku: string; quantity: number; unitPrice: number }[];
}

export type PaymentMethod = 'WALLET' | 'EXTERNAL_GATEWAY' | string;
export type PaymentStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'APPROVED'
  | 'REFUSED'
  | 'CANCELED'
  | 'REFUNDED'
  | string;

export interface Payment {
  id: string;
  orderId: string;
  customerId: string;
  method: PaymentMethod;
  addressId?: string;
  amount: number;
  status?: PaymentStatus;
}

export interface PaymentRequest {
  orderId: string;
  customerId: string;
  amount: number;
  method: PaymentMethod;
}

export type WalletOwnerType = 'CUSTOMER' | 'COMPANY' | string;
export type WalletTransactionType = 'CREDIT' | 'DEBIT' | string;
export type WalletTransactionReason =
  | 'TOP_UP'
  | 'PAYMENT'
  | 'SALE_RECEIVED'
  | 'REFUND'
  | 'CANCELLATION'
  | string;

export interface WalletTransaction {
  id: string;
  type: WalletTransactionType;
  reason?: WalletTransactionReason;
  description?: string;
  amount: number;
  createdAt?: string;
}

export interface Wallet {
  id?: string;
  ownerId: string;
  ownerType?: WalletOwnerType;
  balance: number;
  transactions?: WalletTransaction[];
}

export interface TopUpRequest {
  customerId: string;
  amount: number;
  customerEmail: string;
}

export interface TopUpResponse {
  id: string;
  walletId?: string;
  customerId?: string;
  amount?: number;
  status?: string;
}

export interface TopUpSubmitResponse {
  topUpId: string;
  externalOrderId?: string;
  status?: string;
  qrCode: string;
  qrCodeBase64: string;
  ticketUrl?: string;
}

export interface AuthLoginResponse {
  token?: string;
  accessToken?: string;
  customerId?: string;
  email?: string;
  name?: string;
  roles?: string[];
}

export interface RegisterPayload {
  email: string;
  password: string;
  name: string;
  cpf: string;
  phone: string;
}

export interface AssistantMessage {
  role: 'user' | 'assistant';
  content: string;
}
