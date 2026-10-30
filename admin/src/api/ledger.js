import { getToken, request } from './auth'

export function listEntries({ from, to, page = 1, size = 20, includeVoided = false } = {}) {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  params.set('page', String(page))
  params.set('size', String(size))
  if (includeVoided) params.set('includeVoided', 'true')
  return request('/ledger/entries?' + params.toString(), {}, getToken())
}

export function createEntry(payload) {
  return request('/ledger/entries', { method: 'POST', body: JSON.stringify(payload) }, getToken())
}

export function getSummary({ from, to } = {}) {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  return request('/ledger/summary?' + params.toString(), {}, getToken())
}

export function voidEntry(id, reason = null) {
  return request('/ledger/entries/' + id + '/void', {
    method: 'POST',
    body: JSON.stringify({ reason }),
  }, getToken())
}
