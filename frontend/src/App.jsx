import { useState, useEffect, useCallback } from 'react';
import { api } from './api';

const FLOW = ['CREATED', 'NGO_ACCEPTED', 'VOLUNTEER_ASSIGNED', 'PICKED_UP', 'DELIVERED'];
const label = (s) => s.replaceAll('_', ' ').toLowerCase().replace(/^./, (c) => c.toUpperCase());
const fmt = (t) => (t ? new Date(t).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '-');
const localIso = (offsetH = 0) => {
  const d = new Date(Date.now() + offsetH * 3600e3 - new Date().getTimezoneOffset() * 60e3);
  return d.toISOString().slice(0, 16);
};
const left = (m) => (m <= 0 ? 'expired' : m < 60 ? `${m} min left` : `${Math.floor(m / 60)} h ${m % 60} min left`);

/* ---------------- sign in / register ---------------- */
function Auth({ onAuth }) {
  const [mode, setMode] = useState('login');
  const [f, setF] = useState({ role: 'DONOR' });
  const [err, setErr] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const locate = () =>
    navigator.geolocation?.getCurrentPosition(
      (p) => setF((x) => ({ ...x, latitude: p.coords.latitude.toFixed(5), longitude: p.coords.longitude.toFixed(5) })),
      () => setErr('Location blocked. Type latitude and longitude instead.'));

  const submit = async (e) => {
    e.preventDefault(); setErr('');
    try {
      const body = mode === 'login' ? { email: f.email, password: f.password }
        : { ...f, latitude: f.latitude ? +f.latitude : null, longitude: f.longitude ? +f.longitude : null,
            capacityKg: f.capacityKg ? +f.capacityKg : null };
      onAuth(await api(`/auth/${mode}`, { method: 'POST', body }));
    } catch (x) { setErr(x.message); }
  };

  return (
    <div className="wrap" style={{ maxWidth: 560 }}>
      <h1 className="logo" style={{ fontSize: '2.4rem' }}>FoodLoop</h1>
      <p className="muted">Surplus food from kitchens to the people who need it, before it expires.</p>
      <form className="card form" onSubmit={submit}>
        <h2 className="full">{mode === 'login' ? 'Sign in' : 'Create an account'}</h2>
        {mode === 'register' && <>
          <label>Your name<input required onChange={set('name')} /></label>
          <label>I am a
            <select value={f.role} onChange={set('role')}>
              <option value="DONOR">Donor (restaurant, hotel, canteen, event)</option>
              <option value="NGO">NGO / food bank</option>
              <option value="VOLUNTEER">Volunteer</option>
            </select></label>
          {f.role !== 'VOLUNTEER' && <label>Organisation name<input onChange={set('orgName')} /></label>}
          {f.role !== 'VOLUNTEER' && <label>Address<input onChange={set('address')} /></label>}
          <label>Latitude<input type="number" step="any" value={f.latitude || ''} onChange={set('latitude')} /></label>
          <label>Longitude<input type="number" step="any" value={f.longitude || ''} onChange={set('longitude')} /></label>
          <button type="button" className="ghost full" onClick={locate}>Use my current location</button>
          {f.role === 'NGO' && <>
            <label>Storage capacity (kg)<input type="number" onChange={set('capacityKg')} /></label>
            <label>Food types accepted (blank = all)<input placeholder="Cooked meals, Bakery" onChange={set('acceptedFoodTypes')} /></label></>}
          {f.role === 'VOLUNTEER' && <label>Vehicle<input placeholder="Bike, car, van" onChange={set('vehicle')} /></label>}
        </>}
        <label>Email<input type="email" required onChange={set('email')} /></label>
        <label>Password<input type="password" required minLength={6} onChange={set('password')} /></label>
        {err && <div className="err full">{err}</div>}
        <button className="full">{mode === 'login' ? 'Sign in' : 'Create account'}</button>
        <button type="button" className="ghost full" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setErr(''); }}>
          {mode === 'login' ? 'New here? Create an account' : 'Have an account? Sign in'}
        </button>
      </form>
    </div>
  );
}

/* ---------------- impact numbers ---------------- */
function Impact({ token }) {
  const [s, setS] = useState(null);
  useEffect(() => { api('/analytics', { token }).then(setS).catch(() => {}); }, [token]);
  if (!s) return null;
  const items = [['kgRescued', 'kg of food rescued'], ['mealsServed', 'meals served'], ['completedDonations', 'deliveries completed'],
                 ['activeNgos', 'NGOs'], ['volunteers', 'volunteers']];
  return (
    <div className="grid" style={{ marginBottom: '1rem' }}>
      {items.map(([k, t]) => <div className="card stat" key={k}><b>{s[k]}</b><span>{t}</span></div>)}
      {s.topDonors.length > 0 && <div className="card"><h3>Top donors</h3>
        {s.topDonors.map((d) => <div key={d.donor} className="row between"><span>{d.donor}</span><b>{d.kg} kg</b></div>)}</div>}
    </div>
  );
}

/* ---------------- one donation card, actions depend on role ---------------- */
function DonationCard({ d, user, token, reload }) {
  const [msg, setMsg] = useState(null);
  const [qr, setQr] = useState(null);
  const [matches, setMatches] = useState(null);
  const [code, setCode] = useState('');
  const role = user.role;
  const isMine = (d.ngoId && d.ngoId === user.id);

  const act = async (path, body) => {
    setMsg(null);
    try { await api(`/donations/${d.id}/${path}`, { method: 'POST', body, token }); setCode(''); reload(); }
    catch (e) { setMsg(e.message); }
  };
  const showQr = async () => { try { setQr(await api(`/donations/${d.id}/qr`, { token })); } catch (e) { setMsg(e.message); } };
  const findNgos = async () => { try { setMatches(await api(`/donations/${d.id}/matches`, { token })); } catch (e) { setMsg(e.message); } };
  const idx = FLOW.indexOf(d.status);

  return (
    <div className="card">
      <div className="row between">
        <h3>{d.foodCategory} · {d.quantityKg} kg {d.vegetarian ? '(veg)' : '(non-veg)'}</h3>
        <div className="row">
          {d.urgent && <span className="badge urgent">Urgent</span>}
          <span className={`badge s-${d.status}`}>{label(d.status)}</span>
        </div>
      </div>
      <div className="steps" aria-hidden="true">{FLOW.map((s, i) => <i key={s} className={i <= idx ? 'on' : ''} />)}</div>
      <p className="muted">
        From {d.donorName}{d.pickupAddress ? `, ${d.pickupAddress}` : ''}. Best before {fmt(d.expiresAt)}
        {d.status === 'CREATED' || d.status === 'NGO_ACCEPTED' || d.status === 'VOLUNTEER_ASSIGNED' ? ` (${left(d.minutesLeft)})` : ''}.
        {d.ngoName && <> NGO: {d.ngoName}.</>}{d.volunteerName && <> Volunteer: {d.volunteerName}.</>}
      </p>
      {d.description && <p>{d.description}</p>}

      <div className="row">
        {role === 'DONOR' && ['CREATED', 'NGO_ACCEPTED', 'VOLUNTEER_ASSIGNED'].includes(d.status) &&
          <button className="ghost" onClick={showQr}>Show pickup QR</button>}
        {role === 'DONOR' && d.status === 'CREATED' && <button className="ghost" onClick={findNgos}>Find best NGOs</button>}
        {role === 'DONOR' && ['CREATED', 'NGO_ACCEPTED'].includes(d.status) && <button className="danger" onClick={() => act('cancel')}>Cancel</button>}

        {role === 'NGO' && d.status === 'CREATED' && <button onClick={() => act('accept')}>Accept donation</button>}
        {role === 'NGO' && isMine && ['NGO_ACCEPTED', 'VOLUNTEER_ASSIGNED', 'PICKED_UP'].includes(d.status) &&
          <button className="ghost" onClick={showQr}>Show delivery QR</button>}

        {role === 'VOLUNTEER' && d.status === 'NGO_ACCEPTED' && <button onClick={() => act('assign')}>Take this pickup</button>}
        {role === 'VOLUNTEER' && ['VOLUNTEER_ASSIGNED', 'PICKED_UP'].includes(d.status) && <>
          <input placeholder={d.status === 'VOLUNTEER_ASSIGNED' ? 'Scan or paste donor pickup code' : 'Scan or paste NGO delivery code'}
                 value={code} onChange={(e) => setCode(e.target.value)} style={{ minWidth: 260 }} />
          <button onClick={() => act(d.status === 'VOLUNTEER_ASSIGNED' ? 'pickup' : 'deliver', { token: code })}>
            {d.status === 'VOLUNTEER_ASSIGNED' ? 'Confirm pickup' : 'Confirm delivery'}</button></>}
      </div>

      {msg && <p className="err">{msg}</p>}
      {qr && <div className="qr"><img src={qr.qr} alt={`${qr.type} QR code`} width="150" height="150" />
        <div><b>{qr.type === 'PICKUP' ? 'Show this to the volunteer at pickup' : 'Show this to the volunteer at delivery'}</b>
          <p className="muted">Or read out the code:</p><code>{qr.token}</code></div></div>}
      {matches && <div className="qr" style={{ display: 'block' }}><b>Best NGO matches</b>
        {matches.length === 0 && <p className="muted">No NGOs registered yet.</p>}
        {matches.map((m) => <div key={m.ngoId} className="row between"><span>{m.ngoName}{m.distanceKm != null ? ` · ${m.distanceKm} km` : ''}</span><b>{Math.round(m.score * 100)}% match</b></div>)}</div>}
    </div>
  );
}

/* ---------------- donor: new donation + surplus prediction ---------------- */
function NewDonation({ token, user, onDone }) {
  const [f, setF] = useState({ foodCategory: 'Cooked meals', quantityKg: 10, vegetarian: 'true', preparedAt: localIso(), expiresAt: localIso(5), pickupAddress: '', description: '' });
  const [err, setErr] = useState(''); const [pred, setPred] = useState(null); const [prepared, setPrepared] = useState(100);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault(); setErr('');
    try {
      await api('/donations', { method: 'POST', token, body: { ...f, quantityKg: +f.quantityKg, vegetarian: f.vegetarian === 'true' } });
      onDone();
    } catch (x) { setErr(x.message); }
  };
  const predict = async () => setPred(await api(`/predict/surplus?preparedQty=${prepared}`, { token }));

  return (
    <div className="grid">
      <form className="card form" onSubmit={submit} style={{ gridColumn: 'span 2' }}>
        <h2 className="full">Add surplus food</h2>
        <label>Food category<select value={f.foodCategory} onChange={set('foodCategory')}>
          {['Cooked meals', 'Bakery', 'Fruits & vegetables', 'Packaged food', 'Sweets', 'Beverages'].map((c) => <option key={c}>{c}</option>)}</select></label>
        <label>Quantity (kg)<input type="number" min="0.5" step="0.5" required value={f.quantityKg} onChange={set('quantityKg')} /></label>
        <label>Type<select value={f.vegetarian} onChange={set('vegetarian')}><option value="true">Vegetarian</option><option value="false">Non-vegetarian</option></select></label>
        <label>Prepared at<input type="datetime-local" value={f.preparedAt} onChange={set('preparedAt')} /></label>
        <label>Safe to distribute until<input type="datetime-local" required value={f.expiresAt} onChange={set('expiresAt')} /></label>
        <label>Pickup address<input placeholder={user.address || 'Uses your profile address if blank'} value={f.pickupAddress} onChange={set('pickupAddress')} /></label>
        <label className="full">Notes<textarea rows="2" value={f.description} onChange={set('description')} /></label>
        {err && <div className="err full">{err}</div>}
        <button className="full">Post donation</button>
      </form>
      <div className="card">
        <h3>Surplus predictor</h3>
        <p className="muted">Enter how much you plan to cook today and the model estimates the leftover.</p>
        <label>Planned quantity (kg)<input type="number" value={prepared} onChange={(e) => setPrepared(e.target.value)} /></label>
        <p><button className="ghost" onClick={predict}>Predict surplus</button></p>
        {pred && <p><span className="ok">About {pred.predictedSurplusKg} kg</span><br /><span className="muted">Source: {pred.source}</span></p>}
      </div>
    </div>
  );
}

/* ---------------- dashboard ---------------- */
function Dashboard({ session, onLogout }) {
  const { token, user } = session;
  const [items, setItems] = useState([]); const [tab, setTab] = useState('list'); const [err, setErr] = useState('');
  const reload = useCallback(() => api('/donations', { token }).then(setItems).catch((e) => setErr(e.message)), [token]);
  useEffect(() => { reload(); const t = setInterval(reload, 30000); return () => clearInterval(t); }, [reload]);

  const heading = { DONOR: 'Your donations', NGO: 'Donations you can accept', VOLUNTEER: 'Pickups and deliveries', ADMIN: 'All donations' }[user.role];
  const empty = { DONOR: 'Nothing posted yet. Add surplus food and an NGO can claim it.', NGO: 'No open donations right now.',
                  VOLUNTEER: 'No pickups waiting. Check back soon.', ADMIN: 'No donations yet.' }[user.role];

  return (
    <>
      <div className="top"><span className="logo">FoodLoop</span>
        <div className="row"><span className="muted">{user.orgName || user.name} · {user.role.toLowerCase()}</span><button className="ghost" onClick={onLogout}>Sign out</button></div></div>
      <div className="wrap">
        <Impact token={token} />
        {user.role === 'DONOR' && <div className="tabs">
          <button className={tab === 'list' ? '' : 'ghost'} onClick={() => setTab('list')}>My donations</button>
          <button className={tab === 'new' ? '' : 'ghost'} onClick={() => setTab('new')}>Add surplus food</button></div>}
        {tab === 'new' && user.role === 'DONOR'
          ? <NewDonation token={token} user={user} onDone={() => { reload(); setTab('list'); }} />
          : <>
              <h2>{heading}</h2>
              {err && <p className="err">{err}</p>}
              {items.length === 0 && <div className="card empty">{empty}</div>}
              {items.map((d) => <DonationCard key={d.id} d={d} user={user} token={token} reload={reload} />)}
            </>}
      </div>
    </>
  );
}

export default function App() {
  const [session, setSession] = useState(() => { try { return JSON.parse(localStorage.getItem('foodloop')); } catch { return null; } });
  const login = (s) => { localStorage.setItem('foodloop', JSON.stringify(s)); setSession(s); };
  const logout = () => { localStorage.removeItem('foodloop'); setSession(null); };
  return session ? <Dashboard session={session} onLogout={logout} /> : <Auth onAuth={login} />;
}
