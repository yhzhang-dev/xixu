import { getToken, request } from './auth'

function rangeQuery(from, to) {
  const params = new URLSearchParams({ from, to })
  return '?' + params.toString()
}

export function listBody(from, to) {
  return request('/wellness/body' + rangeQuery(from, to), {}, getToken())
}

export function listSleep(from, to) {
  return request('/wellness/sleep' + rangeQuery(from, to), {}, getToken())
}

export function createBody(payload) {
  return request('/wellness/body', { method: 'POST', body: JSON.stringify(payload) }, getToken())
}

export function createSleep(payload) {
  return request('/wellness/sleep', { method: 'POST', body: JSON.stringify(payload) }, getToken())
}
