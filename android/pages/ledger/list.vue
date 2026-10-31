<template>
  <view class="ledger-page">
    <view class="status-bar"></view>
    <view class="top-bar"><text class="back" @tap="goBack">‹ 返回</text><text class="top-title">我的账本</text><text class="spacer"></text></view>
    <view class="heading"><text class="eyebrow">XIXU / LEDGER</text><text class="title">日子有迹，收支有数。</text><text class="subtitle">这里显示当前账号的有效记录。</text></view>

    <view class="month-bar"><text class="month-arrow" @tap="shiftMonth(-1)">‹</text><view class="month-label"><text>{{ monthLabel }}</text><text>按发生日期统计</text></view><text class="month-arrow" @tap="shiftMonth(1)">›</text></view>

    <view class="summary"><view><text class="summary-label">本月支出</text><text class="summary-value expense">¥{{ formatAmount(summary.expense) }}</text></view><view class="summary-line"></view><view><text class="summary-label">本月收入</text><text class="summary-value income">¥{{ formatAmount(summary.income) }}</text></view></view>

    <view class="list-heading"><view><text class="eyebrow">RECORDS</text><text class="list-title">账目明细</text></view><text class="count">共 {{ summary.count }} 笔</text></view>
    <view v-if="error" class="empty"><text>账本暂时没能加载</text><text class="empty-note">{{ error }}</text><button @tap="loadLedger(1)">重试</button></view>
    <view v-else-if="!loading && items.length === 0" class="empty"><text>这个月还没有账目</text><text class="empty-note">记下第一笔，就从这里开始回看。</text><button @tap="goRecord">记一笔 →</button></view>
    <view v-else class="entry-list">
      <view v-for="item in items" :key="item.id" class="entry"><view :class="['entry-icon', item.entryType === 'EXPENSE' ? 'icon-expense' : 'icon-income']">{{ item.entryType === 'EXPENSE' ? '−' : '+' }}</view><view class="entry-main"><text class="entry-title">{{ item.categoryName }}</text><text class="entry-detail">{{ item.occurredOn }}{{ item.occurredTime ? ' ' + item.occurredTime.slice(0, 5) : '' }} · {{ item.channelName || '未填渠道' }}</text><text v-if="item.merchant || item.note" class="entry-note">{{ item.merchant || item.note }}</text></view><text :class="['entry-money', item.entryType === 'EXPENSE' ? 'expense' : 'income']">{{ item.entryType === 'EXPENSE' ? '−' : '+' }}¥{{ formatAmount(item.amount) }}</text></view>
      <text v-if="loading" class="load-note">正在加载…</text>
      <button v-else-if="items.length < total" class="more-button" @tap="loadLedger(page + 1)">加载更多</button>
      <text v-else class="load-note">已显示本月全部 {{ total }} 笔</text>
    </view>
    <button class="record-button" @tap="goRecord">＋ 记一笔</button>
  </view>
</template>

<script>
import { API_BASE_URL, TOKEN_KEY } from '../../common/api.js'

export default {
  data() {
    const now = new Date()
    return { year: now.getFullYear(), month: now.getMonth() + 1, items: [], total: 0, page: 1,
      summary: { count: 0, expense: '0.00', income: '0.00' }, loading: false, error: '', requestId: 0 }
  },
  computed: {
    monthLabel() { return this.year + '年' + String(this.month).padStart(2, '0') + '月' },
    from() { return this.year + '-' + String(this.month).padStart(2, '0') + '-01' },
    to() { return this.year + '-' + String(this.month).padStart(2, '0') + '-' + String(new Date(this.year, this.month, 0).getDate()).padStart(2, '0') }
  },
  onShow() { this.loadLedger(1) },
  onReachBottom() { if (!this.loading && !this.error && this.items.length < this.total) this.loadLedger(this.page + 1) },
  methods: {
    goBack() { if (getCurrentPages().length > 1) uni.navigateBack(); else uni.reLaunch({ url: '/pages/index/index' }) },
    goRecord() { uni.navigateTo({ url: '/pages/ledger/record' }) },
    shiftMonth(delta) {
      const date = new Date(this.year, this.month - 1 + delta, 1)
      this.year = date.getFullYear()
      this.month = date.getMonth() + 1
      this.loadLedger(1)
    },
    formatAmount(value) { const n = Number(value); return Number.isFinite(n) ? n.toFixed(2) : '0.00' },
    expireSession() { uni.removeStorageSync(TOKEN_KEY); uni.reLaunch({ url: '/pages/login/index' }) },
    loadLedger(page) {
      if (this.loading && page !== 1) return
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) { this.expireSession(); return }
      const id = ++this.requestId
      const range = 'from=' + this.from + '&to=' + this.to
      this.loading = true
      this.error = ''
      if (page === 1) {
        this.items = []
        this.total = 0
        this.summary = { count: 0, expense: '0.00', income: '0.00' }
        uni.request({ url: API_BASE_URL + '/api/ledger/summary?' + range,
          header: { Authorization: 'Bearer ' + token },
          success: (res) => {
            if (id !== this.requestId) return
            if (res.statusCode === 200 && res.data) this.summary = res.data
            else if (res.statusCode === 401) this.expireSession()
            else this.error = (res.data && res.data.message) || '汇总查询失败'
          },
          fail: () => { if (id === this.requestId) this.error = '无法连接服务端' }
        })
      }
      uni.request({ url: API_BASE_URL + '/api/ledger/entries?' + range + '&page=' + page + '&size=20',
        header: { Authorization: 'Bearer ' + token },
        success: (res) => {
          if (id !== this.requestId) return
          if (res.statusCode === 200 && res.data) {
            this.items = page === 1 ? (res.data.items || []) : this.items.concat(res.data.items || [])
            this.total = res.data.total || 0
            this.page = page
          } else if (res.statusCode === 401) this.expireSession()
          else this.error = (res.data && res.data.message) || '账目查询失败'
        },
        fail: () => { if (id === this.requestId) this.error = '无法连接服务端' },
        complete: () => { if (id === this.requestId) this.loading = false }
      })
    }
  }
}
</script>

<style>
page { background: #f5f7f5; }
button::after { border: 0; }
.ledger-page { padding: 0 30rpx 130rpx; color: #1d414b; }
.status-bar { height: var(--status-bar-height); }
.top-bar { display: flex; align-items: center; justify-content: space-between; margin: 24rpx 0 28rpx; }
.back { width: 105rpx; color: #4e7b80; font-size: 26rpx; }
.top-title { color: #22444d; font-size: 30rpx; font-weight: 650; }
.spacer { width: 105rpx; }
.heading { display: flex; flex-direction: column; margin: 8rpx 3rpx 30rpx; }
.eyebrow { color: #6aa59a; font-size: 17rpx; font-weight: 600; letter-spacing: 4rpx; }
.title { margin-top: 10rpx; color: #1f454d; font-size: 40rpx; font-weight: 650; }
.subtitle { margin-top: 9rpx; color: #8fa6a5; font-size: 21rpx; }
.month-bar { display: flex; align-items: center; justify-content: space-between; padding: 13rpx 22rpx; border-radius: 22rpx; background: #e9f2ed; }
.month-arrow { width: 62rpx; line-height: 70rpx; color: #357b73; font-size: 48rpx; text-align: center; }
.month-label { display: flex; flex-direction: column; align-items: center; color: #24545a; font-size: 27rpx; font-weight: 600; }
.month-label text:last-child { margin-top: 3rpx; color: #82a09d; font-size: 17rpx; font-weight: 400; }
.summary { display: flex; align-items: center; margin-top: 17rpx; padding: 31rpx 25rpx; border-radius: 27rpx; background: linear-gradient(125deg, #174b58, #27746f); box-shadow: 0 12rpx 26rpx rgba(24,83,82,.12); }
.summary > view:not(.summary-line) { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.summary > view:last-child { padding-left: 26rpx; }
.summary-label { color: #c0e2db; font-size: 20rpx; }
.summary-value { margin-top: 10rpx; color: #fff; font-size: 33rpx; font-weight: 650; white-space: nowrap; }
.summary-value.income { color: #d8f2e3; }
.summary-line { width: 1rpx; height: 65rpx; background: rgba(255,255,255,.18); }
.list-heading { display: flex; justify-content: space-between; align-items: flex-end; margin: 39rpx 3rpx 16rpx; }
.list-heading > view { display: flex; flex-direction: column; }
.list-title { margin-top: 5rpx; font-size: 31rpx; font-weight: 650; }
.count { color: #90a6a4; font-size: 21rpx; }
.entry-list { overflow: hidden; border-radius: 25rpx; background: #fff; }
.entry { display: flex; align-items: center; gap: 17rpx; min-height: 124rpx; padding: 16rpx 21rpx; border-bottom: 1rpx solid #edf2ef; }
.entry-icon { display: flex; align-items: center; justify-content: center; flex-shrink: 0; width: 60rpx; height: 60rpx; border-radius: 18rpx; font-size: 36rpx; }
.icon-expense { color: #b86752; background: #faf0e9; }
.icon-income { color: #287c70; background: #e9f5ee; }
.entry-main { display: flex; flex: 1; flex-direction: column; min-width: 0; }
.entry-title { color: #234850; font-size: 25rpx; font-weight: 600; }
.entry-detail, .entry-note { margin-top: 5rpx; overflow: hidden; color: #95a9a8; font-size: 18rpx; text-overflow: ellipsis; white-space: nowrap; }
.entry-money { color: #b86752; font-size: 24rpx; font-weight: 650; white-space: nowrap; }
.entry-money.income { color: #287c70; }
.empty { display: flex; flex-direction: column; align-items: center; padding: 72rpx 26rpx; border-radius: 25rpx; background: #fff; color: #365c61; font-size: 28rpx; }
.empty-note { margin-top: 11rpx; color: #9aaead; font-size: 21rpx; text-align: center; }
.empty button, .more-button { margin-top: 26rpx; padding: 0 30rpx; border-radius: 30rpx; background: #e5f2ec; color: #27786e; font-size: 22rpx; }
.load-note { display: block; padding: 20rpx; color: #a0b2b0; font-size: 19rpx; text-align: center; }
.more-button { margin-bottom: 25rpx; }
.record-button { position: fixed; bottom: 30rpx; left: 30rpx; right: 30rpx; z-index: 10; height: 88rpx; line-height: 88rpx; border-radius: 22rpx; background: linear-gradient(110deg, #154b58, #2b837a); color: #fff; font-size: 27rpx; font-weight: 600; box-shadow: 0 14rpx 26rpx rgba(24,83,82,.2); }

/* 汐序 v1：月度概览与明细 */
page { background: #f5f7f4; }
.ledger-page { padding-bottom: calc(170rpx + env(safe-area-inset-bottom)); color: #173d46; }
.top-bar { min-height: 96rpx; margin: 0 0 20rpx; }
.back { display: flex; align-items: center; min-height: 96rpx; width: 124rpx; color: #17675f; font-size: 27rpx; }
.spacer { width: 124rpx; }
.top-title { color: #173d46; }
.heading { margin-bottom: 24rpx; }
.eyebrow { color: #397e72; font-size: 20rpx; letter-spacing: 3rpx; }
.subtitle { color: #567077; font-size: 24rpx; }
.month-bar { background: #e7f2ed; border: 1rpx solid #d5e5de; }
.month-arrow { width: 96rpx; min-height: 96rpx; line-height: 96rpx; color: #17675f; }
.month-label { color: #173d46; font-size: 29rpx; }
.month-label text:last-child { color: #567077; font-size: 21rpx; }
.summary { background: #103e48; box-shadow: none; border-radius: 24rpx; }
.summary-label { color: #d4e6df; font-size: 23rpx; }
.summary-value { font-size: 36rpx; }
.count { color: #567077; font-size: 23rpx; }
.entry-list, .empty { border: 1rpx solid #dae6e1; border-radius: 22rpx; }
.entry { min-height: 136rpx; border-bottom-color: #e8efeb; }
.entry-title { color: #173d46; font-size: 27rpx; }
.entry-detail, .entry-note { color: #567077; font-size: 21rpx; }
.entry-money { font-size: 27rpx; }
.empty-note, .load-note { color: #567077; font-size: 23rpx; }
.empty button, .more-button { min-height: 88rpx; line-height: 88rpx; color: #17675f; font-size: 25rpx; }
.record-button { bottom: calc(24rpx + env(safe-area-inset-bottom)); height: 100rpx; line-height: 100rpx; border-radius: 18rpx; background: #17675f; box-shadow: 0 8rpx 20rpx rgba(16,62,72,.12); font-size: 29rpx; }
</style>
