import React, { useEffect, useMemo, useRef, useState } from 'react'
import {
  Activity, ArrowUpRight, Banknote, Building2, Calculator, CheckCircle2,
  ChevronLeft, ChevronRight, CircleDollarSign, Edit3, LayoutDashboard,
  Menu, MoreHorizontal, Plus, Search, Settings, Trash2, TrendingUp,
  Users, X, Wallet, AlertCircle, RefreshCw, History, ShieldCheck, BarChart3, FileText, Check, Play, CircleCheck
} from 'lucide-react'
import { api, auth } from './api'

const emptyForm = {
  name: '', email: '', department: '', basicSalary: '', allowances: '', deductions: ''
}

const money = value => new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: 'INR', maximumFractionDigits: 0
}).format(Number(value || 0))

const initials = (name = '') =>
  name.split(' ').filter(Boolean).slice(0, 2).map(x => x[0]).join('').toUpperCase() || 'NA'

function parseSalarySearch(value) {
  if (!value) return null
  const normalized = value.toLowerCase().replace(/₹/g, '').replace(/,/g, '').trim()
  const match = normalized.match(/^(\d+(?:\.\d+)?)\s*(k|thousand)?(?:\s*(?:salary|salaried))?$/)
  if (!match) return null
  const amount = Number(match[1])
  if (!Number.isFinite(amount) || amount < 0) return null
  return match[2] === 'k' || match[2] === 'thousand' ? amount * 1000 : amount
}

/* ---------------- Login ---------------- */

function Login({ onLogin, notice }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async e => {
    e.preventDefault()
    if (loading) return
    setLoading(true); setError('')
    try {
      const result = await api.login({ username, password })
      auth.save(result)
      onLogin()
    } catch (err) {
      setError(err.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-screen">
      <form onSubmit={submit} className="auth-card">
        <h1>PayFlow</h1>
        <p className="muted">Sign in with your payroll account.</p>
        {notice && <div className="alert notice"><AlertCircle size={16} /><span>{notice}</span></div>}
        {error && <div className="alert error"><AlertCircle size={16} /><span>{error}</span></div>}
        <label className="field">
          <span>Username</span>
          <input value={username} onChange={e => setUsername(e.target.value)} autoComplete="username" required />
        </label>
        <label className="field">
          <span>Password</span>
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete="current-password" required />
        </label>
        <button className="btn primary block" disabled={loading} type="submit">
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </div>
  )
}

/* ---------------- Error boundary ---------------- */

class ErrorBoundary extends React.Component {
  constructor(props) { super(props); this.state = { error: null } }
  static getDerivedStateFromError(error) { return { error } }
  componentDidCatch(error, info) { console.error('App crashed:', error, info) }
  render() {
    if (this.state.error) {
      return (
        <div className="auth-screen">
          <div className="auth-card">
            <h2>Something went wrong</h2>
            <p className="muted">{this.state.error.message || 'The app hit an unexpected error.'}</p>
            <button className="btn primary block" onClick={() => window.location.reload()}>Reload</button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}

/* ---------------- App shell ---------------- */

function App() {
  const [loggedIn, setLoggedIn] = useState(Boolean(auth.token))
  const [sessionNotice, setSessionNotice] = useState('')
  useEffect(() => {
    const handleExpired = () => {
      setSessionNotice('Your session has expired. Please log in again.')
      setLoggedIn(false)
    }
    window.addEventListener('auth-expired', handleExpired)
    return () => window.removeEventListener('auth-expired', handleExpired)
  }, [])
  return (
    <ErrorBoundary>
      {!loggedIn
        ? <Login notice={sessionNotice} onLogin={() => { setSessionNotice(''); setLoggedIn(true) }} />
        : <Dashboard onLogout={() => { auth.clear(); setLoggedIn(false) }} />}
    </ErrorBoundary>
  )
}

function Dashboard({ onLogout }) {
  const [employees, setEmployees] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState('dashboard')
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [department, setDepartment] = useState('All departments')
  const [modal, setModal] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [payroll, setPayroll] = useState(null)
  const [analytics, setAnalytics] = useState(null)
  const [history, setHistory] = useState([])
  const [monthlyRuns, setMonthlyRuns] = useState([])
  const [auditLogs, setAuditLogs] = useState([])
  const [selectedMonth, setSelectedMonth] = useState(new Date().toISOString().slice(0,7))
  const [toast, setToast] = useState('')
  const [searchPage, setSearchPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [sortBy, setSortBy] = useState('id')
  const [direction, setDirection] = useState('asc')
  const [minSalary, setMinSalary] = useState('')
  const [maxSalary, setMaxSalary] = useState('')
  const [pageMeta, setPageMeta] = useState({ totalElements: 0, totalPages: 0, number: 0, size: 10 })

  // Guards against out-of-order responses (e.g. a slow request finishing
  // after a newer one) so the UI never gets stuck showing stale data/loading.
  const requestId = useRef(0)
  const currentUser = auth.user || { username: '', role: 'USER' }
  const isAdmin = currentUser.role === 'ADMIN'
  const isEmployee = currentUser.role === 'EMPLOYEE'

  const loadEmployees = async () => {
    const id = ++requestId.current
    setLoading(true); setError('')
    try {
      const result = await api.getEmployees(0, 100)
      if (id !== requestId.current) return
      setEmployees(result?.content || [])
      setPageMeta(result || {})
    } catch (e) {
      if (id !== requestId.current) return
      setError(e.message)
    } finally {
      if (id === requestId.current) setLoading(false)
    }
  }

  const loadSearchResults = async targetPage => {
    const id = ++requestId.current
    setLoading(true); setError('')
    try {
      const cleanQuery = query.trim()
      const salaryFromSearch = parseSalarySearch(cleanQuery)
      const result = await api.searchEmployees({
        name: salaryFromSearch === null && cleanQuery && !cleanQuery.includes('@') ? cleanQuery : '',
        email: salaryFromSearch === null && cleanQuery.includes('@') ? cleanQuery : '',
        department: department === 'All departments' ? '' : department,
        minBasicSalary: salaryFromSearch === null ? minSalary : '',
        maxBasicSalary: salaryFromSearch === null ? maxSalary : '',
        salary: salaryFromSearch,
        page: targetPage, size: pageSize, sortBy, direction
      })
      if (id !== requestId.current) return
      setEmployees(result?.content || [])
      setPageMeta(result || {})
    } catch (e) {
      if (id !== requestId.current) return
      setError(e.message)
    } finally {
      if (id === requestId.current) setLoading(false)
    }
  }

  // Initial load
  useEffect(() => { if (!isEmployee) loadEmployees() }, [isEmployee])

  // Debounced search/filter/sort/pagination, only while on the Employees page
  useEffect(() => {
    if (page !== 'employees') return
    const timer = setTimeout(() => loadSearchResults(searchPage), 300)
    return () => clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, query, department, minSalary, maxSalary, searchPage, pageSize, sortBy, direction])

  useEffect(() => {
    if (!toast) return
    const t = setTimeout(() => setToast(''), 2600)
    return () => clearTimeout(t)
  }, [toast])

  const loadAnalytics = async () => {
    try { setAnalytics(await api.analytics()) } catch (e) { setToast(e.message) }
  }
  const loadHistory = async () => {
    try { setHistory(isEmployee ? await api.myPayrollHistory() : await api.payrollHistoryAll()) } catch (e) { setToast(e.message) }
  }
  const loadRuns = async () => {
    try { setMonthlyRuns(await api.payrollMonths()) } catch (e) { setToast(e.message) }
  }
  const loadAudit = async () => {
    try { setAuditLogs((await api.auditLogs(0, 50))?.content || []) } catch (e) { setToast(e.message) }
  }
  useEffect(() => {
    if (page === 'analytics') loadAnalytics()
    if (page === 'history') loadHistory()
    if (page === 'monthly') loadRuns()
    if (page === 'audit' && isAdmin) loadAudit()
  }, [page, isAdmin, isEmployee])

  const departments = useMemo(
    () => ['All departments', ...new Set(employees.map(e => e.department).filter(Boolean))],
    [employees]
  )

  const stats = useMemo(() => {
    const basic = employees.reduce((s, e) => s + Number(e.basicSalary || 0), 0)
    const allowances = employees.reduce((s, e) => s + Number(e.allowances || 0), 0)
    const deductions = employees.reduce((s, e) => s + Number(e.deductions || 0), 0)
    return { employees: employees.length, payroll: basic + allowances - deductions, gross: basic + allowances, deductions }
  }, [employees])

  const goTo = id => { setPage(id); setSidebarOpen(false) }

  const openCreate = () => { setForm(emptyForm); setModal({ type: 'employee', title: 'Add employee' }) }

  const openEdit = employee => {
    setForm({
      name: employee.name || '', email: employee.email || '', department: employee.department || '',
      basicSalary: employee.basicSalary ?? '', allowances: employee.allowances ?? '', deductions: employee.deductions ?? ''
    })
    setModal({ type: 'employee', title: 'Edit employee', id: employee.id })
  }

  const submitEmployee = async e => {
    e.preventDefault()
    const payload = {
      ...form,
      basicSalary: Number(form.basicSalary), allowances: Number(form.allowances), deductions: Number(form.deductions)
    }
    try {
      if (modal.id) await api.updateEmployee(modal.id, payload)
      else await api.createEmployee(payload)
      setModal(null); setToast(modal.id ? 'Employee updated' : 'Employee added')
      page === 'employees' ? loadSearchResults(searchPage) : loadEmployees()
    } catch (e) { setToast(e.message) }
  }

  const remove = async id => {
    if (!confirm('Delete this employee?')) return
    try {
      await api.deleteEmployee(id)
      setToast('Employee deleted')
      page === 'employees' ? loadSearchResults(searchPage) : loadEmployees()
    } catch (e) { setToast(e.message) }
  }

  const showPayroll = async employee => {
    try {
      setPayroll(await api.payroll(employee.id))
      setModal({ type: 'payroll', title: 'Payroll summary' })
    } catch (e) { setToast(e.message) }
  }

  const nav = isEmployee ? [
    { id: 'dashboard', label: 'My Dashboard', icon: LayoutDashboard },
    { id: 'history', label: 'Payroll History', icon: History }
  ] : [
    { id: 'dashboard', label: 'Overview', icon: LayoutDashboard },
    { id: 'employees', label: 'Employees', icon: Users },
    { id: 'payroll', label: 'Payroll', icon: Wallet },
    { id: 'monthly', label: 'Monthly Run', icon: Play },
    { id: 'history', label: 'Payroll History', icon: History },
    { id: 'analytics', label: 'Analytics', icon: BarChart3 },
    ...(isAdmin ? [{ id: 'audit', label: 'Audit Log', icon: ShieldCheck }] : [])
  ]

  return (
    <div className="app-shell">
      {sidebarOpen && <div className="scrim" onClick={() => setSidebarOpen(false)} />}
      <aside className={`sidebar ${sidebarOpen ? 'open' : ''}`}>
        <div className="brand">
          <div className="brand-mark"><CircleDollarSign size={20} /></div>
          <div><strong>PayFlow</strong><span>Payroll platform</span></div>
        </div>
        <nav>
          {nav.map(({ id, label, icon: Icon }) => (
            <button key={id} className={page === id ? 'nav-item active' : 'nav-item'} onClick={() => goTo(id)}>
              <Icon size={17} /><span>{label}</span>
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <button className="nav-item"><Settings size={17} /><span>Settings</span></button>
          <button className="nav-item" onClick={onLogout}><X size={17} /><span>Log out</span></button>
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <button className="mobile-menu" onClick={() => setSidebarOpen(v => !v)}><Menu size={20} /></button>
          <h1>{({
            dashboard: isEmployee ? 'My Dashboard' : 'Overview', employees:'Employees', payroll:'Payroll',
            monthly:'Monthly Payroll Run', history:'Payroll History', analytics:'Dashboard Analytics', audit:'Audit Log'
          })[page] || 'Payroll'}</h1>
          <button className="icon-button" onClick={() => (page === 'employees' ? loadSearchResults(searchPage) : loadEmployees())} title="Refresh">
            <RefreshCw size={17} />
          </button>
        </header>

        {error && (
          <div className="alert error page-alert">
            <AlertCircle size={16} /><span>{error}</span>
            <button onClick={() => (page === 'employees' ? loadSearchResults(searchPage) : loadEmployees())}>Retry</button>
          </div>
        )}

        {page === 'dashboard' && (
          isEmployee
            ? <EmployeeDashboard username={currentUser.username} onHistory={() => goTo('history')} />
            : <Overview employees={employees} stats={stats} loading={loading}
                onEmployees={() => goTo('employees')} onPayroll={showPayroll} onAdd={openCreate} />
        )}

        {!isEmployee && page === 'employees' && (
          <Employees employees={employees} departments={departments} loading={loading}
            query={query} setQuery={v => { setQuery(v); setSearchPage(0) }}
            department={department} setDepartment={v => { setDepartment(v); setSearchPage(0) }}
            minSalary={minSalary} setMinSalary={v => { setMinSalary(v); setSearchPage(0) }}
            maxSalary={maxSalary} setMaxSalary={v => { setMaxSalary(v); setSearchPage(0) }}
            sortBy={sortBy} setSortBy={v => { setSortBy(v); setSearchPage(0) }}
            direction={direction} setDirection={v => { setDirection(v); setSearchPage(0) }}
            pageSize={pageSize} setPageSize={v => { setPageSize(Number(v)); setSearchPage(0) }}
            currentPage={pageMeta.number ?? searchPage} totalPages={pageMeta.totalPages ?? 0}
            totalElements={pageMeta.totalElements ?? 0} setSearchPage={setSearchPage}
            onAdd={openCreate} onEdit={openEdit} onDelete={remove} onPayroll={showPayroll} />
        )}

        {page === 'payroll' && <Payroll employees={employees} onPayroll={showPayroll} onAdd={openCreate} />}

        {page === 'monthly' && (
          <MonthlyPayroll month={selectedMonth} setMonth={setSelectedMonth} runs={monthlyRuns}
            onRun={async () => { try { await api.runPayroll(selectedMonth); setToast('Payroll calculated'); loadRuns() } catch(e){setToast(e.message)} }}
            onApprove={async () => { try { await api.approvePayroll(selectedMonth); setToast('Payroll approved'); loadRuns() } catch(e){setToast(e.message)} }}
            onPay={async () => { try { await api.payPayroll(selectedMonth); setToast('Payroll marked paid'); loadRuns() } catch(e){setToast(e.message)} }} />
        )}
        {page === 'history' && <PayrollHistory history={history} onRefresh={loadHistory} onPayslip={id => api.downloadPayslip(id)} />}
        {page === 'analytics' && <Analytics data={analytics} />}
        {page === 'audit' && isAdmin && <AuditLogs logs={auditLogs} />}
      </main>

      {modal?.type === 'employee' && (
        <Modal title={modal.title} onClose={() => setModal(null)}>
          <form onSubmit={submitEmployee} className="form-grid">
            <Field label="Full name"><input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="e.g. Priya Sharma" required /></Field>
            <Field label="Email"><input type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} placeholder="name@company.com" required /></Field>
            <Field label="Department"><input value={form.department} onChange={e => setForm({ ...form, department: e.target.value })} placeholder="Engineering" required /></Field>
            <Field label="Basic salary"><input type="number" min="0" value={form.basicSalary} onChange={e => setForm({ ...form, basicSalary: e.target.value })} placeholder="0" required /></Field>
            <Field label="Allowances"><input type="number" min="0" value={form.allowances} onChange={e => setForm({ ...form, allowances: e.target.value })} placeholder="0" /></Field>
            <Field label="Deductions"><input type="number" min="0" value={form.deductions} onChange={e => setForm({ ...form, deductions: e.target.value })} placeholder="0" /></Field>
            <div className="modal-actions">
              <button type="button" className="btn secondary" onClick={() => setModal(null)}>Cancel</button>
              <button className="btn primary">{modal.id ? 'Save changes' : 'Add employee'}</button>
            </div>
          </form>
        </Modal>
      )}

      {modal?.type === 'payroll' && payroll && (
        <Modal title={modal.title} onClose={() => setModal(null)}>
          <div className="payroll-head">
            <div className="avatar large">{initials(payroll.employeeName)}</div>
            <div><h3>{payroll.employeeName}</h3><p className="muted">Employee #{payroll.employeeId}</p></div>
          </div>
          <div className="payroll-total"><span>Net salary</span><strong>{money(payroll.netSalary)}</strong></div>
          <div className="breakdown">
            <Row label="Basic salary" value={payroll.basicSalary} />
            <Row label="Allowances" value={payroll.allowances} positive />
            <Row label="Gross salary" value={payroll.grossSalary} strong />
            <Row label="Deductions" value={payroll.deductions} negative />
            <Row label="Net salary" value={payroll.netSalary} strong />
          </div>
        </Modal>
      )}

      {toast && <div className="toast"><CheckCircle2 size={17} />{toast}</div>}
    </div>
  )
}

/* ---------------- Overview ---------------- */

function Overview({ employees, stats, loading, onEmployees, onPayroll, onAdd }) {
  const departmentStats = useMemo(() => {
    const map = new Map()
    employees.forEach(e => {
      const key = e.department || 'Unassigned'
      const entry = map.get(key) || { name: key, count: 0, total: 0 }
      entry.count += 1
      entry.total += Number(e.basicSalary || 0) + Number(e.allowances || 0) - Number(e.deductions || 0)
      map.set(key, entry)
    })
    return [...map.values()].sort((a, b) => b.total - a.total)
  }, [employees])

  return (
    <section className="content">
      <div className="hero">
        <div>
          <span className="pill">Payroll overview</span>
          <h2>Welcome back</h2>
          <p>Track headcount, payroll spend and departments at a glance.</p>
        </div>
        <button className="btn primary" onClick={onAdd}><Plus size={16} /> Add employee</button>
      </div>

      {loading && !employees.length ? (
        <div className="loading">Loading dashboard…</div>
      ) : (
        <>
          <div className="stats-grid">
            <Stat icon={Users} label="Employees" value={stats.employees} note="Active headcount" />
            <Stat icon={Banknote} label="Net payroll" value={money(stats.payroll)} note="Estimated take-home" />
            <Stat icon={TrendingUp} label="Gross payroll" value={money(stats.gross)} note="Before deductions" />
            <Stat icon={Activity} label="Deductions" value={money(stats.deductions)} note="Total withheld" />
          </div>
          <div className="two-col">
            <div className="panel">
              <div className="panel-head">
                <div><h3>Recent employees</h3><p className="muted">Latest additions to payroll</p></div>
                <button className="text-btn" onClick={onEmployees}>View all <ArrowUpRight size={14} /></button>
              </div>
              <EmployeeList employees={employees.slice(0, 6)} onPayroll={onPayroll} />
            </div>
            <div className="panel">
              <div className="panel-head"><div><h3>Departments</h3><p className="muted">Headcount and payroll by team</p></div></div>
              <div className="department-list">
                {departmentStats.length
                  ? departmentStats.map(d => (
                      <div className="dept-row" key={d.name}>
                        <div className="dept-icon"><Building2 size={15} /></div>
                        <div className="dept-info"><b>{d.name}</b><span>{d.count} employee{d.count === 1 ? '' : 's'}</span></div>
                        <strong>{money(d.total)}</strong>
                      </div>
                    ))
                  : <Empty text="No departments yet." />}
              </div>
            </div>
          </div>
        </>
      )}
    </section>
  )
}

/* ---------------- Employees ---------------- */

function Employees({ employees, departments, query, setQuery, department, setDepartment, minSalary, setMinSalary,
  maxSalary, setMaxSalary, sortBy, setSortBy, direction, setDirection, pageSize, setPageSize, currentPage,
  totalPages, totalElements, setSearchPage, loading, onAdd, onEdit, onDelete, onPayroll }) {
  return (
    <section className="content">
      <div className="page-title">
        <div><h2>Employee directory</h2><p className="muted">Search, filter, sort and paginate.</p></div>
        <button className="btn primary" onClick={onAdd}><Plus size={16} /> Add employee</button>
      </div>
      <div className="panel">
        <div className="toolbar">
          <div className="search"><Search size={16} /><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search name, email or salary (e.g. 50k)…" /></div>
          <select value={department} onChange={e => setDepartment(e.target.value)}>{departments.map(d => <option key={d}>{d}</option>)}</select>
          <input className="filter-input" type="number" min="0" value={minSalary} onChange={e => setMinSalary(e.target.value)} placeholder="Min salary" />
          <input className="filter-input" type="number" min="0" value={maxSalary} onChange={e => setMaxSalary(e.target.value)} placeholder="Max salary" />
          <select value={sortBy} onChange={e => setSortBy(e.target.value)}>
            <option value="id">Sort: ID</option><option value="name">Sort: Name</option><option value="email">Sort: Email</option>
            <option value="department">Sort: Department</option><option value="basicSalary">Sort: Basic salary</option>
            <option value="allowances">Sort: Allowances</option><option value="deductions">Sort: Deductions</option>
          </select>
          <select value={direction} onChange={e => setDirection(e.target.value)}><option value="asc">Ascending</option><option value="desc">Descending</option></select>
        </div>
        <div className="table-wrap">
          {loading
            ? <div className="loading">Loading employees…</div>
            : employees.length
              ? (
                <table>
                  <thead><tr><th>Employee</th><th>Department</th><th>Basic salary</th><th>Allowances</th><th>Net estimate</th><th /></tr></thead>
                  <tbody>
                    {employees.map(e => (
                      <tr key={e.id}>
                        <td><div className="person"><div className="avatar">{initials(e.name)}</div><div><b>{e.name}</b><small>{e.email}</small></div></div></td>
                        <td><span className="badge">{e.department}</span></td>
                        <td>{money(e.basicSalary)}</td>
                        <td>{money(e.allowances)}</td>
                        <td><b>{money(Number(e.basicSalary) + Number(e.allowances) - Number(e.deductions))}</b></td>
                        <td>
                          <div className="actions">
                            <button title="Calculate payroll" onClick={() => onPayroll(e)}><Calculator size={15} /></button>
                            <button title="Edit" onClick={() => onEdit(e)}><Edit3 size={15} /></button>
                            <button title="Delete" className="danger" onClick={() => onDelete(e.id)}><Trash2 size={15} /></button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )
              : <Empty text="No employees match your filters." />}
        </div>
        <div className="pagination">
          <span>{totalElements} employee{totalElements === 1 ? '' : 's'} found</span>
          <div className="pagination-actions">
            <select value={pageSize} onChange={e => setPageSize(e.target.value)} title="Rows per page">
              <option value="5">5 / page</option><option value="10">10 / page</option><option value="25">25 / page</option>
              <option value="50">50 / page</option><option value="100">100 / page</option>
            </select>
            <button disabled={currentPage <= 0 || loading} onClick={() => setSearchPage(currentPage - 1)}><ChevronLeft size={15} /></button>
            <span>Page {(currentPage ?? 0) + 1} of {Math.max(totalPages || 1, 1)}</span>
            <button disabled={currentPage + 1 >= totalPages || loading} onClick={() => setSearchPage(currentPage + 1)}><ChevronRight size={15} /></button>
          </div>
        </div>
      </div>
    </section>
  )
}

/* ---------------- Payroll ---------------- */

function Payroll({ employees, onPayroll, onAdd }) {
  const total = employees.reduce((s, e) => s + Number(e.basicSalary || 0) + Number(e.allowances || 0) - Number(e.deductions || 0), 0)
  return (
    <section className="content">
      <div className="page-title">
        <div><h2>Payroll center</h2><p className="muted">Calculate and review employee take-home pay.</p></div>
        <button className="btn primary" onClick={onAdd}><Plus size={16} /> Add employee</button>
      </div>
      <div className="payroll-banner">
        <div><div className="mini-icon"><Wallet size={18} /></div><div><span>Estimated net payroll</span><strong>{money(total)}</strong></div></div>
        <span className="payroll-count">{employees.length} employees</span>
      </div>
      <div className="payroll-grid">
        {employees.length ? employees.map(e => {
          const net = Number(e.basicSalary || 0) + Number(e.allowances || 0) - Number(e.deductions || 0)
          return (
            <div className="salary-card" key={e.id}>
              <div className="salary-top">
                <div className="person"><div className="avatar">{initials(e.name)}</div><div><b>{e.name}</b><small>{e.department}</small></div></div>
                <button className="more" onClick={() => onPayroll(e)}><MoreHorizontal size={18} /></button>
              </div>
              <div className="salary-main"><span>Net salary</span><strong>{money(net)}</strong></div>
              <div className="salary-meta"><span>Gross {money(Number(e.basicSalary || 0) + Number(e.allowances || 0))}</span><span>Deducted {money(e.deductions)}</span></div>
              <button className="salary-btn" onClick={() => onPayroll(e)}>View breakdown <ChevronRight size={14} /></button>
            </div>
          )
        }) : <Empty text="No employees yet." />}
      </div>
    </section>
  )
}


/* ---------------- Enhancement pages ---------------- */

function EmployeeDashboard({ username, onHistory }) {
  const [rows,setRows]=useState([]); const [loading,setLoading]=useState(true)
  useEffect(()=>{api.myPayrollHistory().then(setRows).catch(()=>setRows([])).finally(()=>setLoading(false))},[])
  const latest=rows[0]; const total=rows.reduce((s,r)=>s+Number(r.netSalary||0),0)
  return <section className="content"><div className="hero"><div><span className="pill">Employee portal</span><h2>Welcome, {username}</h2><p>View your payroll history and payslips.</p></div><button className="btn primary" onClick={onHistory}><History size={15}/> Payroll history</button></div>
    <div className="stats-grid"><Stat icon={Wallet} label="Latest net pay" value={latest?money(latest.netSalary):'—'} note={latest?.payrollMonth||'No payroll yet'}/><Stat icon={History} label="Payroll records" value={rows.length} note="Historical records"/><Stat icon={CheckCircle2} label="Latest status" value={latest?.status||'—'} note="Payroll workflow"/><Stat icon={Banknote} label="Total received" value={money(total)} note="All recorded payroll"/></div>
    <div className="panel"><div className="panel-head"><div><h3>Latest payroll</h3><p className="muted">Your most recent payroll record</p></div></div>{loading?<div className="loading">Loading…</div>:latest?<div className="salary-card"><div className="salary-main"><span>{latest.payrollMonth}</span><strong>{money(latest.netSalary)}</strong></div><div className="salary-meta"><span>Gross {money(latest.grossSalary)}</span><span>Status {latest.status}</span></div></div>:<Empty text="No payroll record linked to this account yet."/>}</div>
  </section>
}

function MonthlyPayroll({ month, setMonth, runs, onRun, onApprove, onPay }) {
  const current = runs.find(r => r.month === month)
  return (
    <section className="content">
      <div className="page-title"><div><h2>Monthly payroll run</h2><p className="muted">Calculate → approve → pay payroll for every employee.</p></div></div>
      <div className="panel workflow-panel">
        <div className="workflow-controls"><label className="field"><span>Payroll month</span><input type="month" value={month} onChange={e=>setMonth(e.target.value)} /></label>
          <div className="workflow-actions"><button className="btn primary" onClick={onRun}><Play size={15}/> Calculate</button><button className="btn" disabled={!current || current.status==='PAID'} onClick={onApprove}><Check size={15}/> Approve</button><button className="btn" disabled={!current || current.status!=='APPROVED'} onClick={onPay}><CircleCheck size={15}/> Mark paid</button></div>
        </div>
        <div className="workflow-steps">
          {['CALCULATED','APPROVED','PAID'].map((s,i)=><div key={s} className={`workflow-step ${current?.status===s || (s==='CALCULATED' && current?.status==='APPROVED') || (s!=='PAID' && current?.status==='PAID')?'done':''}`}><span>{i+1}</span><b>{s}</b></div>)}
        </div>
        {current ? <div className="stats-grid"><Stat icon={Users} label="Employees" value={current.employeeCount} note="In this run"/><Stat icon={Banknote} label="Gross" value={money(current.grossTotal)} note="Total gross"/><Stat icon={Wallet} label="Net" value={money(current.netTotal)} note="Total take-home"/><Stat icon={CheckCircle2} label="Status" value={current.status} note="Workflow state"/></div> : <Empty text="No payroll run exists for this month. Click Calculate to create it."/>}
      </div>
      <div className="panel"><div className="panel-head"><div><h3>Previous runs</h3><p className="muted">Payroll processing history by month</p></div></div>
        {runs.length ? <div className="table-wrap"><table><thead><tr><th>Month</th><th>Employees</th><th>Gross</th><th>Net</th><th>Status</th></tr></thead><tbody>{runs.map(r=><tr key={r.month}><td>{r.month}</td><td>{r.employeeCount}</td><td>{money(r.grossTotal)}</td><td>{money(r.netTotal)}</td><td><span className={`badge status-${String(r.status).toLowerCase()}`}>{r.status}</span></td></tr>)}</tbody></table></div>:<Empty text="No previous payroll runs."/>}
      </div>
    </section>
  )
}

function PayrollHistory({ history, onRefresh, onPayslip }) {
  return <section className="content">
    <div className="page-title"><div><h2>Payroll history</h2><p className="muted">Previous payroll records and downloadable payslips.</p></div><button className="btn" onClick={onRefresh}><RefreshCw size={15}/> Refresh</button></div>
    <div className="panel table-wrap">{history.length ? <table><thead><tr><th>Employee</th><th>Month</th><th>Gross</th><th>Net</th><th>Status</th><th>Payslip</th></tr></thead><tbody>{history.map(r=><tr key={r.id}><td><b>{r.employeeName}</b><small>{r.department}</small></td><td>{r.payrollMonth}</td><td>{money(r.grossSalary)}</td><td><b>{money(r.netSalary)}</b></td><td><span className={`badge status-${String(r.status).toLowerCase()}`}>{r.status}</span></td><td><button className="text-btn" onClick={()=>onPayslip(r.id)}><FileText size={14}/> PDF</button></td></tr>)}</tbody></table>:<Empty text="No payroll history yet. Run monthly payroll first."/>}</div>
  </section>
}

function Analytics({ data }) {
  if (!data) return <section className="content"><div className="loading">Loading analytics…</div></section>
  const max = Math.max(...(data.departments || []).map(d=>d.payroll),1)
  return <section className="content">
    <div className="page-title"><div><h2>Dashboard analytics</h2><p className="muted">Payroll trends and department statistics.</p></div></div>
    <div className="stats-grid"><Stat icon={Users} label="Employees" value={data.employeeCount} note="Current headcount"/><Stat icon={Banknote} label="Net payroll" value={money(data.totalPayroll)} note="Current estimate"/><Stat icon={TrendingUp} label="Average net" value={money(data.averageNetSalary)} note="Per employee"/><Stat icon={BarChart3} label="Departments" value={data.departments?.length || 0} note="Teams represented"/></div>
    <div className="two-col"><div className="panel"><div className="panel-head"><div><h3>Department payroll</h3><p className="muted">Net payroll by department</p></div></div><div className="chart-list">{(data.departments||[]).map(d=><div className="bar-row" key={d.department}><div><b>{d.department}</b><span>{d.employees} employees</span></div><div className="bar-track"><div className="bar-fill" style={{width:`${Math.max(4,d.payroll/max*100)}%`}}/></div><strong>{money(d.payroll)}</strong></div>)}</div></div>
      <div className="panel"><div className="panel-head"><div><h3>Payroll trend</h3><p className="muted">Historical net payroll</p></div></div><div className="trend-list">{(data.trends||[]).map(t=><div className="trend-row" key={t.month}><span>{t.month}</span><b>{money(t.payroll)}</b></div>)}</div></div></div>
  </section>
}

function AuditLogs({ logs }) {
 return <section className="content"><div className="page-title"><div><h2>Audit log</h2><p className="muted">Who changed what and when.</p></div></div><div className="panel table-wrap">{logs.length?<table><thead><tr><th>Time</th><th>User</th><th>Action</th><th>Entity</th><th>Details</th></tr></thead><tbody>{logs.map(x=><tr key={x.id}><td>{new Date(x.createdAt).toLocaleString()}</td><td><b>{x.username}</b></td><td><span className="badge">{x.action}</span></td><td>{x.entityType} {x.entityId||''}</td><td>{x.details}</td></tr>)}</tbody></table>:<Empty text="No audit events yet."/>}</div></section>
}

/* ---------------- Small pieces ---------------- */

function EmployeeList({ employees, onPayroll }) {
  return employees.length ? (
    <div className="compact-list">
      {employees.map(e => (
        <div className="compact-row" key={e.id}>
          <div className="person"><div className="avatar">{initials(e.name)}</div><div><b>{e.name}</b><small>{e.department}</small></div></div>
          <div className="compact-right">
            <b>{money(Number(e.basicSalary) + Number(e.allowances) - Number(e.deductions))}</b>
            <button onClick={() => onPayroll(e)}><ChevronRight size={15} /></button>
          </div>
        </div>
      ))}
    </div>
  ) : <Empty text="No employees yet." />
}

function Stat({ icon: Icon, label, value, note }) {
  return <div className="stat"><div className="stat-icon"><Icon size={18} /></div><div><span>{label}</span><strong>{value}</strong><small>{note}</small></div></div>
}
function Field({ label, children }) { return <label className="field"><span>{label}</span>{children}</label> }
function Row({ label, value, positive, negative, strong }) {
  return <div className={`break-row ${strong ? 'strong' : ''}`}><span>{label}</span><b className={positive ? 'positive' : negative ? 'negative' : ''}>{money(value)}</b></div>
}
function Modal({ title, onClose, children }) {
  return (
    <div className="overlay" onMouseDown={e => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <div className="modal-head"><h3>{title}</h3><button onClick={onClose}><X size={18} /></button></div>
        {children}
      </div>
    </div>
  )
}
function Empty({ text }) { return <div className="empty"><Users size={22} /><p>{text}</p></div> }

export default App
