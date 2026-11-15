import { useState, type FormEvent } from 'react'
import { RefreshCw, UtensilsCrossed, LogOut, ClipboardList } from 'lucide-react'
import './App.css'

type OrderItem = {
  itemName: string
  unitPrice: number
  quantity: number
  lineTotal: number
}

type Order = {
  id: string
  orderNumber: string
  tableNumber: string
  customerName: string
  customerPhone: string
  status: string
  totalAmount: number
  createdAt: string
  items: OrderItem[]
}

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

function App() {
  const [username, setUsername] = useState('manager')
  const [password, setPassword] = useState('')
  const [authHeader, setAuthHeader] = useState('')
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const loadOrders = async (authorization: string) => {
    setLoading(true)
    setError('')
    try {
      const response = await fetch(`${API_BASE}/api/manager/orders`, {
        headers: { Authorization: authorization },
      })
      if (response.status === 401 || response.status === 403) {
        throw new Error('Sign-in failed. Check your manager username and password.')
      }
      if (!response.ok) throw new Error('Could not load orders. Please try again.')
      setOrders(await response.json() as Order[])
      setAuthHeader(authorization)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to connect to the backend.')
      setAuthHeader('')
    } finally {
      setLoading(false)
    }
  }

  const signIn = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await loadOrders(`Basic ${btoa(`${username}:${password}`)}`)
  }

  const signOut = () => {
    setAuthHeader('')
    setPassword('')
    setOrders([])
    setError('')
  }

  if (!authHeader) {
    return (
      <main className="manager-login-shell">
        <form className="manager-login" onSubmit={signIn}>
          <div className="manager-brand"><span><UtensilsCrossed size={20} /></span> olive & ember <small>MANAGER</small></div>
          <p className="manager-eyebrow">RESTAURANT OPERATIONS</p>
          <h1>Welcome back.</h1>
          <p className="login-copy">Sign in to view incoming customer orders.</p>
          <label>Username<input autoComplete="username" value={username} onChange={event => setUsername(event.target.value)} required /></label>
          <label>Password<input type="password" autoComplete="current-password" value={password} onChange={event => setPassword(event.target.value)} required /></label>
          {error && <p className="manager-error" role="alert">{error}</p>}
          <button className="manager-primary" disabled={loading}>{loading ? 'Signing in…' : 'Sign in'}</button>
          <p className="login-hint">Local development default: manager / change-me-now. Set MANAGER_USERNAME and MANAGER_PASSWORD before deployment.</p>
        </form>
      </main>
    )
  }

  return (
    <main className="manager-shell">
      <header className="manager-topbar">
        <div className="manager-brand"><span><UtensilsCrossed size={20} /></span> olive & ember <small>MANAGER</small></div>
        <div className="manager-actions">
          <button className="manager-secondary" onClick={() => loadOrders(authHeader)} disabled={loading}><RefreshCw size={15} /> Refresh</button>
          <button className="manager-logout" onClick={signOut}><LogOut size={16} /> Sign out</button>
        </div>
      </header>
      <section className="orders-heading">
        <div><p className="manager-eyebrow">RESTAURANT OPERATIONS</p><h1>Orders<span>.</span></h1><p>Latest customer orders and table details.</p></div>
        <div className="orders-count"><ClipboardList size={18} /><strong>{orders.length}</strong><span>recent orders</span></div>
      </section>
      {error && <p className="manager-error" role="alert">{error}</p>}
      {loading && <p className="manager-loading">Loading orders…</p>}
      {!loading && orders.length === 0 && <section className="manager-empty"><ClipboardList size={30} /><h2>No orders yet</h2><p>New customer orders will appear here after checkout.</p></section>}
      <section className="orders-list">
        {orders.map(order => (
          <article className="order-card" key={order.id}>
            <div className="order-card-head">
              <div><span className="order-number">{order.orderNumber}</span><span className="order-time">{new Date(order.createdAt).toLocaleString()}</span></div>
              <span className={`order-status ${order.status.toLowerCase().replaceAll('_', '-')}`}>{order.status.replaceAll('_', ' ')}</span>
            </div>
            <div className="order-customer-grid">
              <div><small>TABLE</small><strong>{order.tableNumber || '—'}</strong></div>
              <div><small>CUSTOMER</small><strong>{order.customerName || '—'}</strong></div>
              <div><small>PHONE</small><strong>{order.customerPhone || '—'}</strong></div>
            </div>
            <div className="order-items">
              {order.items.map((item, index) => <div className="order-item" key={`${order.id}-${index}`}><span>{item.quantity} × {item.itemName}</span><strong>₹{Number(item.lineTotal).toFixed(2)}</strong></div>)}
            </div>
            <div className="order-total"><span>Order total</span><strong>₹{Number(order.totalAmount).toFixed(2)}</strong></div>
          </article>
        ))}
      </section>
      <p className="manager-footnote">Orders are awaiting payment verification. Accept/reject and payment controls will be added in the next workflow step.</p>
    </main>
  )
}

export default App
