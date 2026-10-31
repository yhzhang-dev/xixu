<template>
  <view class="login-page">
    <view class="status-bar"></view>

    <view class="brand-area">
      <view class="brand-mark"><text>汐</text></view>
      <text class="brand-en">X I X U</text>
      <text class="brand-title">让每一天，都有迹可循。</text>
      <text class="brand-desc">账目与日常片段，安放在一处。</text>
    </view>

    <view class="form-card">
      <view class="form-heading">
        <text class="form-kicker">WELCOME BACK</text>
        <text class="form-title">欢迎回来</text>
        <text class="form-subtitle">登录后，继续记录你的生活。</text>
      </view>

      <view class="field">
        <text class="field-label">账号</text>
        <view class="input-wrap">
          <input v-model.trim="account" class="field-input" type="text" maxlength="50" placeholder="请输入用户名" placeholder-class="placeholder" autocomplete="off" />
        </view>
      </view>
      <view class="field password-field">
        <text class="field-label">密码</text>
        <view class="input-wrap">
          <input v-model="password" class="field-input" :password="!showPassword" maxlength="128" placeholder="请输入密码" placeholder-class="placeholder" autocomplete="off" />
          <button class="visibility" @tap="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</button>
        </view>
      </view>

      <button class="login-button" :disabled="loading" @tap="onLogin">{{ loading ? '登录中…' : '登 录' }} <text class="login-arrow">→</text></button>
      <view class="prototype-note"><view class="note-dot"></view><text>本地开发版 · 请连接本机服务端</text></view>
    </view>
    <text class="bottom-note">汐序 · 收好生活的每一天</text>
  </view>
</template>

<script>
import { API_BASE_URL, TOKEN_KEY } from '../../common/api.js'

export default {
  data() {
    return {
      account: '',
      password: '',
      showPassword: false,
      loading: false
    }
  },
  onLoad() {
    const token = uni.getStorageSync(TOKEN_KEY)
    if (!token) return
    uni.request({
      url: API_BASE_URL + '/api/auth/me',
      header: { Authorization: 'Bearer ' + token },
      success: (res) => {
        if (res.statusCode === 200) {
          uni.reLaunch({ url: '/pages/index/index' })
        } else if (res.statusCode === 401) {
          uni.removeStorageSync(TOKEN_KEY)
        }
      }
    })
  },
  methods: {
    onLogin() {
      if (!this.account || !this.password) {
        uni.showToast({ title: '请填写账号和密码', icon: 'none' })
        return
      }
      if (this.loading) return
      this.loading = true
      uni.request({
        url: API_BASE_URL + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        data: { username: this.account, password: this.password },
        success: (res) => {
          if (res.statusCode === 200 && res.data && res.data.token) {
            uni.setStorageSync(TOKEN_KEY, res.data.token)
            this.password = ''
            uni.reLaunch({ url: '/pages/index/index' })
          } else if (res.statusCode === 401) {
            uni.showToast({ title: '用户名或密码错误', icon: 'none' })
          } else {
            uni.showToast({ title: '登录失败，请稍后再试', icon: 'none' })
          }
        },
        fail: () => {
          uni.showModal({
            title: '无法连接服务端',
            content: '请确认后端已启动，并已执行 adb reverse tcp:18080 tcp:18080。',
            showCancel: false
          })
        },
        complete: () => { this.loading = false }
      })
    }
  }
}
</script>

<style>
page { background: #f5f7f5; }
button::after { border: 0; }
.login-page { position: relative; overflow: hidden; box-sizing: border-box; min-height: 100vh; padding: 0 35rpx 50rpx; background: linear-gradient(to bottom, #0d394b 0%, #175b68 39%, #f5f7f5 39.1%, #f5f7f5 100%); color: #1d414b; }
.status-bar { height: var(--status-bar-height); }
.brand-area { position: relative; z-index: 1; display: flex; flex-direction: column; align-items: flex-start; padding: 56rpx 18rpx 55rpx; color: #fff; }
.brand-mark { width: 94rpx; height: 94rpx; display: flex; align-items: center; justify-content: center; border-radius: 28rpx; background: rgba(226, 248, 239, .18); border: 1rpx solid rgba(237, 255, 248, .33); font-size: 52rpx; font-weight: 600; }
.brand-en { margin-top: 28rpx; color: #b5ded7; font-size: 18rpx; letter-spacing: 8rpx; font-weight: 600; }
.brand-title { margin-top: 12rpx; font-size: 42rpx; line-height: 1.3; font-weight: 600; letter-spacing: 2rpx; }
.brand-desc { margin-top: 11rpx; color: #c2dedb; font-size: 22rpx; line-height: 1.5; }
.form-card { position: relative; z-index: 1; box-sizing: border-box; padding: 39rpx 34rpx 32rpx; background: #fff; border-radius: 32rpx; box-shadow: 0 19rpx 47rpx rgba(27, 78, 76, .1); }
.form-heading { display: flex; flex-direction: column; margin-bottom: 32rpx; }
.form-kicker { color: #83a5a5; font-size: 17rpx; letter-spacing: 5rpx; font-weight: 600; }
.form-title { margin-top: 9rpx; color: #20434c; font-size: 38rpx; font-weight: 650; }
.form-subtitle { margin-top: 7rpx; color: #91a7a9; font-size: 22rpx; }
.field-label { color: #3d5a62; font-size: 21rpx; font-weight: 600; }
.input-wrap { display: flex; align-items: center; box-sizing: border-box; height: 85rpx; margin-top: 12rpx; padding: 0 21rpx; border: 1rpx solid #e2ebe8; border-radius: 18rpx; background: #f8faf9; }
.field-input { flex: 1; min-width: 0; height: 75rpx; color: #21444d; font-size: 24rpx; }
.placeholder { color: #aab9ba; }
.password-field { margin-top: 26rpx; }
.visibility { margin: 0; padding: 0 3rpx; background: transparent; color: #6c9693; font-size: 20rpx; line-height: 1; }
.login-button { display: flex; align-items: center; justify-content: center; width: 100%; height: 90rpx; margin: 38rpx 0 0; border-radius: 20rpx; background: linear-gradient(110deg, #104b5b, #287b78); color: #fff; font-size: 27rpx; font-weight: 600; letter-spacing: 4rpx; box-shadow: 0 11rpx 25rpx rgba(25, 95, 99, .15); }
.login-arrow { margin-left: 12rpx; font-size: 29rpx; font-weight: 400; }
.prototype-note { display: flex; align-items: center; justify-content: center; margin-top: 10rpx; color: #9caeaf; font-size: 19rpx; }
.note-dot { width: 8rpx; height: 8rpx; margin-right: 10rpx; border-radius: 50%; background: #91aca9; }
.bottom-note { position: relative; z-index: 1; display: block; margin-top: 31rpx; color: #a0b2b3; text-align: center; font-size: 19rpx; letter-spacing: 1rpx; }

/* 汐序 v1：清晰的登录层级与便于触达的表单 */
page { background: #f5f7f4; }
.login-page { min-height: 100vh; padding: 0 32rpx calc(44rpx + env(safe-area-inset-bottom)); background: linear-gradient(to bottom, #103e48 0%, #103e48 38%, #f5f7f4 38.1%); color: #173d46; }
.brand-area { padding: 52rpx 10rpx 58rpx; }
.brand-mark { width: 88rpx; height: 88rpx; border-radius: 24rpx; background: #28746b; border-color: rgba(255,255,255,.2); font-size: 48rpx; }
.brand-en { font-size: 20rpx; letter-spacing: 6rpx; color: #a9d6ca; }
.brand-title { font-size: 44rpx; letter-spacing: 0; }
.brand-desc { color: #d1e4dd; font-size: 25rpx; }
.form-card { padding: 38rpx 30rpx 34rpx; border: 1rpx solid #dae6e1; border-radius: 28rpx; box-shadow: 0 12rpx 30rpx rgba(16,62,72,.07); }
.form-kicker { color: #567e78; font-size: 21rpx; letter-spacing: 3rpx; }
.form-title { color: #173d46; font-size: 40rpx; }
.form-subtitle { color: #567077; font-size: 25rpx; }
.field-label { color: #34555b; font-size: 25rpx; }
.input-wrap { height: 96rpx; border-color: #d6e4de; background: #f8faf8; }
.input-wrap:focus-within { border-color: #17675f; }
.field-input { height: 90rpx; font-size: 28rpx; }
.placeholder { color: #819a97; }
.visibility { min-width: 96rpx; height: 88rpx; line-height: 88rpx; color: #17675f; font-size: 25rpx; }
.login-button { height: 100rpx; margin-top: 34rpx; border-radius: 18rpx; background: #17675f; box-shadow: none; font-size: 29rpx; letter-spacing: 1rpx; }
.login-button[disabled] { opacity: .65; }
.prototype-note { margin-top: 24rpx; color: #567077; font-size: 21rpx; }
.bottom-note { color: #738b88; font-size: 21rpx; }
</style>
