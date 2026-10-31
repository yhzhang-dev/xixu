import { getToken, request } from './auth'

export function listLogs({ from, to, type, page = 1, size = 20 } = {}) {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  if (type) params.set('type', type)
  params.set('page', String(page))
  params.set('size', String(size))
  return request('/logs?' + params.toString(), {}, getToken())
}

export function createLog(payload) {
  return request('/logs', { method: 'POST', body: JSON.stringify(payload) }, getToken())
}
