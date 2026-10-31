<template>
  <view class="record-page">
    <view class="status-bar"></view>
    <view class="top-bar">
      <text class="back" @tap="goBack">‹ 返回</text>
      <text class="page-title">记一笔</text>
      <text class="spacer"></text>
    </view>

    <view class="intro"><text class="intro-kicker">A SMALL MOMENT, KEPT</text><text class="intro-title">把这一笔，认真记下。</text><text class="intro-note">先填金额、日期和分类，其他信息按需要补充。</text></view>

    <view class="type-switch">
      <view :class="['type-item', { active: entryType === 'EXPENSE' }]" @tap="entryType = 'EXPENSE'">支出</view>
      <view :class="['type-item', { active: entryType === 'INCOME' }]" @tap="entryType = 'INCOME'">收入</view>
    </view>

    <view class="amount-box">
      <text class="amount-unit">¥</text>
      <input v-model="amount" class="amount-input" type="digit" placeholder="0.00" placeholder-class="amount-placeholder" />
    </view>

    <view class="form-card">
      <view class="card-heading"><text>基本信息</text><text>日期与分类必填</text></view>

    <view class="field">
      <text class="field-label">日期</text>
      <picker mode="date" :value="occurredOn" @change="onDateChange">
        <view class="picker-value">{{ occurredOn }}</view>
      </picker>
    </view>

    <view class="field">
      <text class="field-label">时间</text>
      <view class="time-row"><picker mode="time" :value="occurredTime || '12:00'" @change="onTimeChange"><view class="picker-value">{{ occurredTime || '未填写发生时间' }}</view></picker><text v-if="occurredTime" class="clear-time" @tap="occurredTime = ''">清除</text></view>
    </view>

    <view class="field">
      <text class="field-label">分类</text>
      <view class="chips">
        <view v-for="c in categories" :key="c" :class="['chip', { active: categoryName === c }]" @tap="categoryName = c">{{ c }}</view>
      </view>
      <input v-model="categoryName" class="field-input" type="text" maxlength="50" placeholder="或自定义分类" placeholder-class="placeholder" />
    </view>
    </view>

    <view class="form-card optional-card"><view class="card-heading"><text>补充信息</text><text>选填</text></view>

    <view class="field">
      <text class="field-label">渠道</text>
      <view class="chips">
        <view v-for="c in channels" :key="c" :class="['chip', { active: channelName === c }]" @tap="channelName = c">{{ c }}</view>
      </view>
    </view>

    <view class="field"><text class="field-label">商家 / 对方</text><input v-model="merchant" class="field-input" type="text" maxlength="100" placeholder="这笔账与谁有关" placeholder-class="placeholder" /></view>

    <view class="field">
      <text class="field-label">备注</text>
      <input v-model="note" class="field-input" type="text" maxlength="500" placeholder="选填" placeholder-class="placeholder" />
    </view>
    </view>

    <button class="save-button" :disabled="saving || saved" @tap="save">{{ saving ? '保存中…' : saved ? '已保存' : '保存这笔 →' }}</button>
    <text class="save-hint">保存后可在账本中查看；更正或作废可在 Web 端操作。</text>
  </view>
</template>

<script>
import { API_BASE_URL, TOKEN_KEY } from '../../common/api.js'

export default {
  data() {
    const d = new Date()
    const pad = (n) => String(n).padStart(2, '0')
    return {
      entryType: 'EXPENSE',
      amount: '',
      occurredOn: d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()),
      occurredTime: '',
      categoryName: '',
      channelName: '',
      merchant: '',
      note: '',
      categories: ['吃饭', '交通', '购物', '娱乐', '居住', '医疗', '其他'],
      channels: ['微信', '支付宝', '现金', '银行卡'],
      saving: false,
      saved: false
    }
  },
  onLoad() {
    const token = uni.getStorageSync(TOKEN_KEY)
    if (!token) {
      uni.reLaunch({ url: '/pages/login/index' })
    }
  },
  methods: {
    onDateChange(e) {
      this.occurredOn = e.detail.value
    },
    onTimeChange(e) {
      this.occurredTime = e.detail.value
    },
    goBack() {
      if (getCurrentPages().length > 1) uni.navigateBack()
      else uni.reLaunch({ url: '/pages/index/index' })
    },
    save() {
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) {
        uni.reLaunch({ url: '/pages/login/index' })
        return
      }
      const amountText = String(this.amount).trim()
      if (!this.categoryName.trim()) {
        uni.showToast({ title: '请填写分类', icon: 'none' })
        return
      }
      if (!/^(?:0|[1-9]\d{0,9})(?:\.\d{1,2})?$/.test(amountText) || Number(amountText) <= 0) {
        uni.showToast({ title: '金额最多两位小数，且须大于 0', icon: 'none' })
        return
      }
      if (this.saving || this.saved) return
      this.saving = true
      uni.request({
        url: API_BASE_URL + '/api/ledger/entries',
        method: 'POST',
        header: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
        data: {
          entryType: this.entryType,
          amount: amountText,
          occurredOn: this.occurredOn,
          occurredTime: this.occurredTime ? this.occurredTime + ':00' : null,
          categoryName: this.categoryName.trim(),
          channelName: this.channelName || null,
          merchant: this.merchant.trim() || null,
          note: this.note || null
        },
        success: (res) => {
          if (res.statusCode === 201) {
            this.saved = true
            uni.showToast({ title: '已记下', icon: 'success' })
            setTimeout(() => this.goBack(), 600)
          } else if (res.statusCode === 401) {
            uni.removeStorageSync(TOKEN_KEY)
            uni.reLaunch({ url: '/pages/login/index' })
          } else {
            const msg = (res.data && res.data.message) || '保存失败'
            uni.showToast({ title: msg, icon: 'none' })
          }
        },
        fail: () => {
          uni.showToast({ title: '无法连接服务端', icon: 'none' })
        },
        complete: () => { this.saving = false }
      })
    }
  }
}
</script>

<style>
page { background: #f5f7f5; }
button::after { border: 0; }
.record-page { padding: 0 32rpx 60rpx; color: #1d414b; }
.intro { display: flex; flex-direction: column; margin: 14rpx 2rpx 30rpx; }
.intro-kicker { color: #66a49a; font-size: 18rpx; font-weight: 600; letter-spacing: 4rpx; }
.intro-title { margin-top: 13rpx; color: #1b414b; font-size: 42rpx; line-height: 1.35; font-weight: 650; }
.intro-note { margin-top: 9rpx; color: #90a5a6; font-size: 22rpx; line-height: 1.6; }
.form-card { margin-top: 20rpx; padding: 28rpx 24rpx 30rpx; border: 1rpx solid #e5eeea; border-radius: 28rpx; background: #fff; box-shadow: 0 8rpx 26rpx rgba(25,75,77,.035); }
.card-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4rpx; }
.card-heading text:first-child { color: #24505a; font-size: 27rpx; font-weight: 650; }
.card-heading text:last-child { color: #9aafad; font-size: 19rpx; }
.optional-card { margin-top: 18rpx; }
.time-row { display: flex; align-items: center; gap: 15rpx; }
.time-row picker { flex: 1; }
.clear-time { color: #5f908b; font-size: 21rpx; }
.save-hint { display: block; margin: 18rpx 6rpx 0; color: #9aadaa; font-size: 19rpx; line-height: 1.6; text-align: center; }
.status-bar { height: var(--status-bar-height); }
.top-bar { display: flex; align-items: center; justify-content: space-between; margin: 24rpx 0 30rpx; }
.back { color: #4e7b80; font-size: 26rpx; }
.spacer { width: 80rpx; }
.page-title { color: #22444d; font-size: 32rpx; font-weight: 650; }
.type-switch { display: flex; padding: 6rpx; background: #e8f0ee; border-radius: 22rpx; }
.type-item { flex: 1; height: 72rpx; line-height: 72rpx; text-align: center; border-radius: 18rpx; color: #6b878a; font-size: 26rpx; }
.type-item.active { background: #123f4a; color: #fff; font-weight: 600; }
.amount-box { display: flex; align-items: center; margin: 26rpx 0 12rpx; padding: 18rpx 24rpx; border: 1rpx solid #cfe5da; border-radius: 24rpx; background: #ecf6ef; }
.amount-unit { color: #1d424b; font-size: 48rpx; font-weight: 650; margin-right: 12rpx; }
.amount-input { flex: 1; min-width: 0; height: 90rpx; color: #1d424b; font-size: 56rpx; font-weight: 650; }
.amount-placeholder { color: #c3d0cf; }
.field { margin-top: 26rpx; }
.field-label { color: #3d5a62; font-size: 21rpx; font-weight: 600; }
.picker-value { height: 76rpx; line-height: 76rpx; margin-top: 10rpx; padding: 0 20rpx; background: #f6f9f7; border-radius: 16rpx; color: #21444d; font-size: 25rpx; }
.chips { display: flex; flex-wrap: wrap; gap: 14rpx; margin-top: 14rpx; }
.chip { padding: 12rpx 26rpx; background: #f1f6f3; border-radius: 30rpx; color: #6b878a; font-size: 23rpx; }
.chip.active { background: #123f4a; color: #fff; }
.field-input { height: 76rpx; margin-top: 12rpx; padding: 0 20rpx; background: #f6f9f7; border-radius: 16rpx; color: #21444d; font-size: 25rpx; }
.placeholder { color: #aab9ba; }
.save-button { margin-top: 48rpx; height: 92rpx; line-height: 92rpx; border-radius: 22rpx; background: linear-gradient(110deg, #104b5b, #287b78); color: #fff; font-size: 28rpx; font-weight: 600; letter-spacing: 4rpx; }

/* 汐序 v1：金额先行，补充信息按需填写 */
page { background: #f5f7f4; }
.record-page { padding-bottom: calc(70rpx + env(safe-area-inset-bottom)); color: #173d46; }
.top-bar { min-height: 96rpx; margin: 0 0 20rpx; }
.back { display: flex; align-items: center; min-width: 124rpx; min-height: 96rpx; color: #17675f; font-size: 27rpx; }
.spacer { width: 124rpx; }
.page-title { color: #173d46; }
.intro { margin-bottom: 26rpx; }
.intro-kicker { color: #397e72; font-size: 20rpx; letter-spacing: 3rpx; }
.intro-note { color: #567077; font-size: 24rpx; }
.type-switch { background: #e7f2ed; }
.type-item { height: 88rpx; line-height: 88rpx; color: #345f60; }
.type-item.active { background: #17675f; }
.form-card { border-color: #dae6e1; box-shadow: none; }
.card-heading text:first-child { color: #173d46; font-size: 28rpx; }
.card-heading text:last-child, .save-hint { color: #567077; font-size: 22rpx; }
.amount-box { background: #e7f2ed; border-color: #c6ded2; }
.field-label { color: #34555b; font-size: 24rpx; }
.picker-value, .field-input { height: 94rpx; line-height: 94rpx; background: #f8faf8; border: 1rpx solid #dae6e1; color: #173d46; font-size: 27rpx; }
.chip { display: flex; align-items: center; min-height: 88rpx; padding: 0 26rpx; background: #f1f6f3; color: #345f60; font-size: 25rpx; }
.chip.active { background: #17675f; }
.clear-time { display: flex; align-items: center; min-height: 88rpx; color: #17675f; font-size: 24rpx; }
.placeholder { color: #819a97; }
.save-button { height: 100rpx; line-height: 100rpx; border-radius: 18rpx; background: #17675f; font-size: 29rpx; letter-spacing: 1rpx; }
</style>
