const TOKEN_KEY = 'xixu_web_token'

export function getToken() { return sessionStorage.getItem(TOKEN_KEY) }
export function saveToken(token) { sessionStorage.setItem(TOKEN_KEY, token) }
export function clearToken() { sessionStorage.removeItem(TOKEN_KEY) }

async function request(path, options = {}, token = null) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 10000)
  try {
    const response = await fetch(`/api${path}`, {
      ...options,
      signal: controller.signal,
      headers: {
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    })
    const data = response.status === 204 ? null : await response.json().catch(() => null)
    if (!response.ok) {
      const error = new Error(data?.message || `请求失败（${response.status}）`)
      error.status = response.status
      throw error
    }
    return data
  } finally {
    clearTimeout(timeout)
  }
}

export function login(username, password) {
  return request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function getCurrentUser(token) { return request('/auth/me', {}, token) }
export function logout(token) { return request('/auth/logout', { method: 'POST' }, token) }
