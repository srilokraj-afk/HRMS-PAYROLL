const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const auth = {
  get token() { return localStorage.getItem('payflow_token') },
  get user() {
    try { return JSON.parse(localStorage.getItem('payflow_user') || 'null') } catch { return null }
  },
  save(data) {
    localStorage.setItem('payflow_token', data.token)
    localStorage.setItem('payflow_user', JSON.stringify({ username: data.username, role: data.role }))
  },
  clear() {
    localStorage.removeItem('payflow_token')
    localStorage.removeItem('payflow_user')
  }
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  const token = auth.token
  if (token) headers.Authorization = `Bearer ${token}`

  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 15000)

  let response
  try {
    response = await fetch(`${API_BASE}${path}`, { ...options, headers, signal: controller.signal })
  } catch (err) {
    if (err.name === 'AbortError') {
      throw new Error(`Request timed out. Is the backend reachable at ${API_BASE}?`)
    }
    throw new Error(`Could not reach the server at ${API_BASE}. Check that the backend is running and CORS allows this origin.`)
  } finally {
    clearTimeout(timeout)
  }

  let body = null
  try { body = await response.json() } catch {}

  // Spring Security has no formLogin()/httpBasic() configured, so its default
  // entry point returns 403 (not 401) for a missing/expired/invalid token on a
  // protected route — 403 here means "please log in again", same as 401,
  // except when it's the login endpoint itself rejecting bad credentials.
  const isAuthFailure = (response.status === 401 || response.status === 403) && path !== '/auth/login'
  if (isAuthFailure) {
    auth.clear()
    window.dispatchEvent(new Event('auth-expired'))
  }

  if (!response.ok) {
    let message = `Request failed (${response.status})`
    if (body) {
      message = body.message || body.error || message
      if (body.errors) message = Object.values(body.errors).join(', ')
    }
    if (isAuthFailure) message = 'Your session has expired. Please log in again.'
    throw new Error(message)
  }

  if (response.status === 204) return null
  return body?.data !== undefined ? body.data : body
}

const queryString = params => {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, value)
  })
  return search.toString()
}

export const api = {
  login: credentials => request('/auth/login', { method: 'POST', body: JSON.stringify(credentials) }),
  searchEmployees: (params = {}) => {
    const query = queryString({
      name: params.name, email: params.email, department: params.department,
      minBasicSalary: params.minBasicSalary, maxBasicSalary: params.maxBasicSalary,
      salary: params.salary, page: params.page ?? 0, size: params.size ?? 10,
      sortBy: params.sortBy ?? 'id', direction: params.direction ?? 'asc'
    })
    return request(`/employees?${query}`)
  },
  getEmployees: (page = 0, size = 100) => api.searchEmployees({ page, size, sortBy: 'id', direction: 'asc' }),
  getEmployee: id => request(`/employees/${id}`),
  createEmployee: data => request('/employees', { method: 'POST', body: JSON.stringify(data) }),
  updateEmployee: (id, data) => request(`/employees/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteEmployee: id => request(`/employees/${id}`, { method: 'DELETE' }),
  payroll: id => request(`/payroll/${id}`),
  payrollHistory: id => request(`/payroll/history/${id}`),
  myPayrollHistory: () => request('/payroll/history/me'),
  payrollHistoryAll: async () => {
    const employees = await request('/employees?page=0&size=100')
    const all = []
    for (const e of (employees?.content || [])) {
      const rows = await request(`/payroll/history/${e.id}`)
      all.push(...(rows || []))
    }
    return all
  },
  payrollMonths: () => request('/payroll/months'),
  runPayroll: month => request(`/payroll/run/${month}`, { method:'POST' }),
  approvePayroll: month => request(`/payroll/approve/${month}`, { method:'POST' }),
  payPayroll: month => request(`/payroll/pay/${month}`, { method:'POST' }),
  analytics: () => request('/analytics'),
  auditLogs: (page=0,size=20) => request(`/audit?page=${page}&size=${size}`),
  downloadPayslip: async id => {
    const response = await fetch(`${API_BASE}/payroll/payslip/${id}`, { headers: { Authorization: `Bearer ${auth.token}` } })
    if (!response.ok) throw new Error('Unable to download payslip')
    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href=url; a.download=`payslip-${id}.pdf`; a.click(); URL.revokeObjectURL(url)
  }
}
