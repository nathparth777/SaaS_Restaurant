import { useMemo, useState } from 'react'
import { ArrowRight, Clock3, Leaf, Minus, Plus, Search, ShoppingBag, Sparkles, UtensilsCrossed, X } from 'lucide-react'
import './App.css'

type MenuItem = {
  id: number
  backendId: string
  name: string
  description: string
  price: number
  category: string
  tag?: string
  vegetarian: boolean
  emoji: string
}

const categories = ['All', 'Starters', 'Mains', 'Pizza', 'Drinks', 'Desserts']

const menu: MenuItem[] = [
  { id: 1, backendId: '20000000-0000-4000-8000-000000000001', name: 'Truffle Parmesan Fries', description: 'Crispy golden fries, parmesan, herbs and truffle aioli.', price: 189, category: 'Starters', tag: 'Bestseller', vegetarian: true, emoji: '🍟' },
  { id: 2, backendId: '20000000-0000-4000-8000-000000000002', name: 'Crispy Lotus Stem', description: 'Sweet chilli glaze, toasted sesame and spring onion.', price: 219, category: 'Starters', vegetarian: true, emoji: '🥢' },
  { id: 3, backendId: '20000000-0000-4000-8000-000000000003', name: 'Garden Pesto Bowl', description: 'Basil pesto, roasted vegetables, cherry tomatoes and feta.', price: 329, category: 'Mains', tag: 'Fresh pick', vegetarian: true, emoji: '🥗' },
  { id: 4, backendId: '20000000-0000-4000-8000-000000000004', name: 'Smoky Paneer Tikka', description: 'Chargrilled paneer, peppers, mint yoghurt and pickled onion.', price: 349, category: 'Mains', vegetarian: true, emoji: '🍢' },
  { id: 5, backendId: '20000000-0000-4000-8000-000000000005', name: 'Classic Margherita', description: 'San Marzano tomato, fresh mozzarella and basil.', price: 399, category: 'Pizza', tag: 'Bestseller', vegetarian: true, emoji: '🍕' },
  { id: 6, backendId: '20000000-0000-4000-8000-000000000006', name: 'Wild Mushroom Pizza', description: 'Roasted mushrooms, mozzarella, thyme and garlic cream.', price: 469, category: 'Pizza', vegetarian: true, emoji: '🍄' },
  { id: 7, backendId: '20000000-0000-4000-8000-000000000007', name: 'Peach Iced Tea', description: 'Freshly brewed tea, peach and a hint of lemon.', price: 129, category: 'Drinks', vegetarian: true, emoji: '🍹' },
  { id: 8, backendId: '20000000-0000-4000-8000-000000000008', name: 'Cold Coffee', description: 'Slow-brewed coffee, chilled milk and vanilla.', price: 149, category: 'Drinks', vegetarian: true, emoji: '🧋' },
  { id: 9, backendId: '20000000-0000-4000-8000-000000000009', name: 'Lotus Biscoff Cheesecake', description: 'Creamy baked cheesecake with a caramel biscuit crumb.', price: 229, category: 'Desserts', tag: 'Must try', vegetarian: true, emoji: '🍰' },
]

function App() {
  const [activeCategory, setActiveCategory] = useState('All')
  const [search, setSearch] = useState('')
  const [cart, setCart] = useState<Record<number, number>>({})
  const [cartOpen, setCartOpen] = useState(false)
  const [checkoutOpen, setCheckoutOpen] = useState(false)
  const [tableNumber, setTableNumber] = useState('')
  const [customerName, setCustomerName] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [formError, setFormError] = useState('')
  const [checkoutSubmitting, setCheckoutSubmitting] = useState(false)

  const visibleItems = useMemo(() => menu.filter((item) => {
    const matchesCategory = activeCategory === 'All' || item.category === activeCategory
    const matchesSearch = `${item.name} ${item.description}`.toLowerCase().includes(search.toLowerCase())
    return matchesCategory && matchesSearch
  }), [activeCategory, search])

  const cartItems = menu.filter((item) => cart[item.id])
  const itemCount = Object.values(cart).reduce((sum, quantity) => sum + quantity, 0)
  const subtotal = cartItems.reduce((sum, item) => sum + item.price * cart[item.id], 0)

  const changeQuantity = (id: number, delta: number) => {
    setCart((current) => {
      const next = { ...current, [id]: Math.max(0, (current[id] ?? 0) + delta) }
      if (next[id] === 0) delete next[id]
      return next
    })
  }

  const continueToPayment = async () => {
    if (!tableNumber.trim() || !customerName.trim() || !/^\\d{10}$/.test(phoneNumber.trim())) {
      setFormError('Enter your table number, name and a valid 10-digit phone number.')
      return
    }

    setFormError('')
    setCheckoutSubmitting(true)
    try {
      const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
      const response = await fetch(`${apiBase}/api/public/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tableNumber: tableNumber.trim(),
          customerName: customerName.trim(),
          phoneNumber: phoneNumber.trim(),
          items: cartItems.map((item) => ({ menuItemId: item.backendId, quantity: cart[item.id] })),
        }),
      })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload.detail || payload.message || 'We could not submit your order. Please try again.')
      window.alert(`Order ${payload.orderNumber} has been created. Total: ₹${payload.totalAmount}. Payment will be available in the next step.`)
      setCheckoutOpen(false)
      setCart({})
      setCartOpen(false)
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Unable to connect to the restaurant. Please try again.')
    } finally {
      setCheckoutSubmitting(false)
    }
  }
  return (
    <div className="storefront">
      <header className="topbar">
        <a className="brand" href="#" aria-label="Olive and Ember home"><span className="brand-mark"><UtensilsCrossed size={19} /></span><span>olive <i>&</i> ember</span></a>
        <div className="table-pill"><span className="live-dot" /> SCAN & ORDER</div>
      </header>

      <main>
        <section className="hero">
          <div className="hero-copy">
            <span className="eyebrow"><Sparkles size={14} /> GOOD FOOD, GOOD MOOD</span>
            <h1>A little something<br /><em>delicious.</em></h1>
            <p>Made fresh, served with love. Take a look around and find your new favourite.</p>
            <div className="hero-meta"><span><Clock3 size={15} /> 20–30 min</span><span><Leaf size={15} /> Freshly prepared</span></div>
          </div>
          <div className="hero-art" role="img" aria-label="Illustration of a freshly prepared meal"><div className="plate"><span>🥑</span><span>🥬</span><span>🍅</span><span>🥕</span><span>🍋</span></div><div className="art-note">made fresh<br />for you ✳</div></div>
        </section>

        <section className="menu-section">
          <div className="section-heading"><div><span className="eyebrow">FROM OUR KITCHEN</span><h2>The menu<span>.</span></h2></div><div className="search-box"><Search size={17} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Find something tasty" aria-label="Search menu" />{search && <button onClick={() => setSearch('')} aria-label="Clear search"><X size={15} /></button>}</div></div>
          <nav className="category-tabs" aria-label="Menu categories">{categories.map((category) => <button key={category} className={activeCategory === category ? 'active' : ''} onClick={() => setActiveCategory(category)}>{category}</button>)}</nav>
          <div className="menu-grid">{visibleItems.map((item) => <article className="menu-card" key={item.id}><div className={`food-art food-${item.id}`}><span>{item.emoji}</span>{item.tag && <span className="food-tag">{item.tag}</span>}</div><div className="food-info"><div className="food-title-row"><h3>{item.name}</h3>{item.vegetarian && <span className="veg-mark" title="Vegetarian"><span /></span>}</div><p>{item.description}</p><div className="food-bottom"><strong>₹{item.price}</strong>{cart[item.id] ? <div className="quantity-control"><button onClick={() => changeQuantity(item.id, -1)} aria-label={`Remove one ${item.name}`}><Minus size={14} /></button><span>{cart[item.id]}</span><button onClick={() => changeQuantity(item.id, 1)} aria-label={`Add one ${item.name}`}><Plus size={14} /></button></div> : <button className="add-button" onClick={() => changeQuantity(item.id, 1)}><Plus size={15} /> Add</button>}</div></div></article>)}</div>
          {visibleItems.length === 0 && <div className="empty-state"><span>🔎</span><h3>No dishes found</h3><p>Try another search or choose a different category.</p></div>}
        </section>
      </main>

      <footer className="footer"><span>olive <i>&</i> ember</span><span>Thoughtfully made. Happily eaten.</span></footer>

      {itemCount > 0 && <button className="cart-fab" onClick={() => setCartOpen(true)}><span className="cart-count">{itemCount}</span><ShoppingBag size={19} /><span>View your order</span><strong>₹{subtotal}</strong><ArrowRight size={17} /></button>}

      {cartOpen && <div className="drawer-backdrop" onClick={() => setCartOpen(false)}><aside className="cart-drawer" onClick={(event) => event.stopPropagation()}><div className="drawer-head"><div><span className="eyebrow">YOUR SELECTION</span><h2>Your order<span>.</span></h2></div><button className="close-button" onClick={() => setCartOpen(false)} aria-label="Close cart"><X /></button></div>{cartItems.length ? <><div className="cart-lines">{cartItems.map((item) => <div className="cart-line" key={item.id}><span className="cart-emoji">{item.emoji}</span><div className="cart-line-info"><strong>{item.name}</strong><small>₹{item.price} each</small><div className="quantity-control"><button onClick={() => changeQuantity(item.id, -1)} aria-label={`Remove one ${item.name}`}><Minus size={13} /></button><span>{cart[item.id]}</span><button onClick={() => changeQuantity(item.id, 1)} aria-label={`Add one ${item.name}`}><Plus size={13} /></button></div></div><strong>₹{item.price * cart[item.id]}</strong></div>)}</div><div className="drawer-total"><div><span>Subtotal</span><strong>₹{subtotal}</strong></div><small>Taxes, if applicable, are calculated at checkout.</small><button className="checkout-button" onClick={() => { setCartOpen(false); setCheckoutOpen(true) }}>Continue to checkout <ArrowRight size={17} /></button></div></> : <div className="empty-state"><span>🛍️</span><h3>Your bag is empty</h3><p>Add a dish to get started.</p></div>}</aside></div>}

      {checkoutOpen && <div className="drawer-backdrop" onClick={() => setCheckoutOpen(false)}><section className="checkout-panel" onClick={(event) => event.stopPropagation()} aria-labelledby="checkout-title"><div className="drawer-head"><div><span className="eyebrow">ALMOST THERE</span><h2 id="checkout-title">Your details<span>.</span></h2></div><button className="close-button" onClick={() => setCheckoutOpen(false)} aria-label="Close checkout"><X /></button></div><p className="checkout-intro">Please tell us where to bring your order. These details are collected only when you check out.</p><form className="checkout-form" onSubmit={(event) => { event.preventDefault(); continueToPayment() }}><label>Table number<input required value={tableNumber} onChange={(event) => setTableNumber(event.target.value)} placeholder="e.g. 8" /></label><label>Customer name<input required value={customerName} onChange={(event) => setCustomerName(event.target.value)} placeholder="Your name" /></label><label>Phone number<input required inputMode="numeric" autoComplete="tel" maxLength={10} pattern="[0-9]{10}" value={phoneNumber} onChange={(event) => setPhoneNumber(event.target.value.replace(/\D/g, '').slice(0, 10))} placeholder="10-digit mobile number" /></label>{formError && <p className="form-error" role="alert">{formError}</p>}<div className="checkout-summary"><span>{itemCount} item(s)</span><strong>₹{subtotal}</strong></div><button className="checkout-button" type="submit" disabled={checkoutSubmitting}>{checkoutSubmitting ? "Submitting order..." : "Place order"} <ArrowRight size={17} /></button></form></section></div>}
    </div>
  )
}

export default App
