import { API_BASE_URL, TOKEN_KEY } from './api.js'

function requestWellness(path, method = 'GET', data) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync(TOKEN_KEY)
    if (!token) { reject({ status: 401, message: '请重新登录' }); return }
    uni.request({
      url: API_BASE_URL + '/api/wellness/' + path,
      method,
      header: { Authorization: 'Bearer ' + token, 'Content-Type': 'application/json' },
      data,
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) resolve(res.data)
        else reject({ status: res.statusCode, message: (res.data && res.data.message) || '请求失败' })
      },
      fail: () => reject({ status: 0, message: '无法连接服务端' })
    })
  })
}

export function listWellness(kind, from, to) {
  return requestWellness(kind + '?from=' + from + '&to=' + to)
}

export function createWellness(kind, payload) {
  return requestWellness(kind, 'POST', payload)
}
