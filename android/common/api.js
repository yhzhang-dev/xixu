// 真机 USB 调试：先运行 adb reverse tcp:18080 tcp:18080。
// 云端部署前改为 HTTPS 域名，不要把这个开发地址用于正式包。
export const API_BASE_URL = 'http://127.0.0.1:18080'
export const TOKEN_KEY = 'xixu_access_token'
