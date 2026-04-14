export interface CustomerSession {
  authenticated: boolean;
  customerId: number;
  email: string;
  firstName: string;
  lastName: string;
}

export interface CartItem {
  movieId: string;
  title: string;
  year?: number;
  quantity: number;
  price: number;
  subtotal: number;
}

export interface CartData {
  items: CartItem[];
  itemCount: number;
  totalPrice: number;
}

export interface OrderListItem {
  orderId: number;
  orderDate: string;
  itemCount: number;
  totalPrice: number;
}

export interface OrderItem {
  saleId: number;
  movieId: string;
  title: string;
  quantity: number;
  price: number;
}

export interface OrderDetail {
  orderId: number;
  customerId: number;
  items: OrderItem[];
  totalPrice: number;
  orderDate: string;
}

export interface LibraryItem {
  movieId: string;
  title: string;
  year?: number;
  titleType?: string;
  lastPurchasedAt?: string;
}
