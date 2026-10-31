<template>
  <view class="home">
    <view class="status-bar"></view>
    <view class="header">
      <view>
        <text class="eyebrow">X I X U</text>
        <view class="brand"><text class="brand-name">汐序</text><text class="brand-note">生活自有节奏</text></view>
      </view>
      <view class="date"><text class="date-number">{{ day }}</text><text class="date-month">{{ month }}月</text></view>
    </view>

    <view class="hero">
      <view class="hero-inner">
        <text class="hero-top">{{ greeting }}{{ accountName ? '，' + accountName : '' }}</text>
        <text class="hero-title">先照顾身体，再记录生活。</text>
        <text class="hero-subtitle">体重与睡眠，从真实测量开始。</text>
        <button class="hero-button" @tap="goHealth()">身体与训练 <text aria-hidden="true">→</text></button>
      </view>
    </view>

    <view class="sample-label"><view class="sample-dot"></view><text>身体、日志与账目来自同一账号</text></view>

    <view class="section-head"><view><text class="section-en">WELLNESS</text><text class="section-title">身体与训练</text></view><text class="section-side" @tap="goHealth()">查看记录 ›</text></view>
    <view class="health-preview" @tap="goHealth()"><view><text>最近体重</text><text class="health-value">{{ healthError ? '—' : latestWeight ? latestWeight.weightKg + ' kg' : '尚未记录' }}</text><text class="health-date">{{ latestWeight ? latestWeight.observedOn : '点击开始记录' }}</text></view><view><text>最近睡眠</text><text class="health-value">{{ healthError ? '—' : latestSleep ? durationText(latestSleep.durationMinutes) : '尚未记录' }}</text><text class="health-date">{{ latestSleep ? latestSleep.sleepOn : '点击开始记录' }}</text></view></view>

    <view class="section-head">
      <view><text class="section-en">OVERVIEW</text><text class="section-title">今日一览</text></view>
      <text class="section-side">{{ month }}月{{ day }}日</text>
    </view>
    <view class="stats">
      <view class="stat" @tap="goLedger">
        <text class="stat-name">今日支出</text>
        <view class="stat-number"><text v-if="!todayLedgerError" class="unit">¥</text><text>{{ todayLedgerError ? '—' : todayExpense }}</text></view>
        <text class="stat-note">{{ todayLedgerError ? '账目暂未同步' : todayCount ? '今日真实支出' : '暂无支出' }}</text>
      </view>
      <view class="stat" @tap="goJournalList">
        <text class="stat-name">生活记录</text>
        <view class="stat-number"><text>{{ todayLogError ? '—' : todayLogCount }}</text><text v-if="!todayLogError" class="suffix">条</text></view>
        <text class="stat-note">{{ todayLogError ? '日志暂未同步' : '点此查看日志' }}</text>
      </view>
    </view>

    <view class="section-head later">
      <view><text class="section-en">QUICK ACCESS</text><text class="section-title">随手记下</text></view>
      <text class="section-side">从此刻开始</text>
    </view>
    <view class="actions">
      <button class="action" @tap="goHealthRecord('body')">
        <text class="action-icon icon-green">01</text><view class="action-text"><text class="action-title">记体重</text><text class="action-desc">记录真实测量</text></view><text class="arrow">›</text>
      </button>
      <button class="action" @tap="goHealthRecord('sleep')">
        <text class="action-icon icon-blue">02</text><view class="action-text"><text class="action-title">记睡眠</text><text class="action-desc">按醒来日期</text></view><text class="arrow">›</text>
      </button>
      <button class="action" @tap="goRecord">
        <text class="action-icon icon-sand">03</text><view class="action-text"><text class="action-title">记一笔</text><text class="action-desc">录入收支</text></view><text class="arrow">›</text>
      </button>
      <button class="action" @tap="goJournalRecord">
        <text class="action-icon icon-purple">04</text><view class="action-text"><text class="action-title">写日志</text><text class="action-desc">日常片段</text></view><text class="arrow">›</text>
      </button>
    </view>

    <view class="section-head later">
      <view><text class="section-en">LEDGER TODAY</text><text class="section-title">今日账目</text></view>
      <text class="section-side" @tap="goLedger">查看账本 ›</text>
    </view>
    <view class="timeline" v-if="!todayLedgerError && todayEntries.length">
      <view v-for="(item, idx) in todayEntries" :key="item.id" :class="['event', { last: idx === todayEntries.length - 1 }]">
        <text class="event-time">{{ item.occurredTime || '全天' }}</text>
        <view class="event-info">
          <text class="event-type">{{ item.entryType === 'EXPENSE' ? '支出' : '收入' }}</text>
          <text class="event-title">{{ item.categoryName }} · ¥{{ formatAmount(item.amount) }}</text>
          <text class="event-detail">{{ item.channelName || '手动' }}{{ item.note ? ' · ' + item.note : '' }}</text>
        </view>
      </view>
    </view>
    <view class="timeline" v-else-if="todayLedgerError" @tap="loadTodayLedger">
      <view class="event last"><text class="event-time">—</text><view class="event-info"><text class="event-type">账本</text><text class="event-title">账目暂未同步</text><text class="event-detail">检查网络和服务端后，点这里重试</text></view></view>
    </view>
    <view class="timeline" v-else>
      <view class="event last"><text class="event-time">—</text><view class="event-info"><text class="event-type">账本</text><text class="event-title">今天还没有记录</text><text class="event-detail">点上方「开始记录」记下第一笔</text></view></view>
    </view>
    <button v-if="todayCount > todayEntries.length" class="ledger-more" @tap="goLedger">还有 {{ todayCount - todayEntries.length }} 笔，查看全部账目 →</button>
    <text class="footer">汐序 · 收好生活的每一天</text>
    <button v-if="isLoggedIn" class="logout-button" @tap="logout">退出登录</button>
  </view>
</template>

<script>
import { API_BASE_URL, TOKEN_KEY } from '../../common/api.js'
import { listWellness } from '../../common/wellness.js'

const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
export default {
  data() {
    const now = new Date()
    const hour = now.getHours()
    return {
      day: String(now.getDate()).padStart(2, '0'),
      month: now.getMonth() + 1,
      weekday: weekdays[now.getDay()],
      greeting: hour < 11 ? '早上好' : hour < 18 ? '下午好' : '晚上好',
      accountName: '',
      isLoggedIn: false,
      todayExpense: '0.00',
      todayEntries: [],
      todayCount: 0,
      todayLedgerError: false,
      todayRequestId: 0,
      todayLogCount: 0,
      todayLogError: false,
      todayLogRequestId: 0,
      latestWeight: null,
      latestSleep: null,
      healthError: false,
      healthRequestId: 0
    }
  },
  onLoad() {
    const token = uni.getStorageSync(TOKEN_KEY)
    if (!token) return
    uni.request({
      url: API_BASE_URL + '/api/auth/me',
      header: { Authorization: 'Bearer ' + token },
      success: (res) => {
        if (res.statusCode === 200 && res.data) {
          this.accountName = res.data.displayName || res.data.username || ''
          this.isLoggedIn = true
        } else if (res.statusCode === 401) {
          uni.removeStorageSync(TOKEN_KEY)
          uni.reLaunch({ url: '/pages/login/index' })
        }
      }
    })
  },
  onShow() {
    const now = new Date()
    this.day = String(now.getDate()).padStart(2, '0')
    this.month = now.getMonth() + 1
    this.weekday = weekdays[now.getDay()]
    this.loadTodayLedger()
    this.loadTodayLogs()
    this.loadHealth()
  },
  methods: {
    goHealth(tab) {
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) { this.expireSession(); return }
      uni.navigateTo({ url: '/pages/health/index' + (tab ? '?tab=' + tab : '') })
    },
    goHealthRecord(tab) {
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) { this.expireSession(); return }
      uni.navigateTo({ url: '/pages/health/record?tab=' + tab })
    },
    durationText(minutes) { return Math.floor(minutes / 60) + '小时' + minutes % 60 + '分' },
    async loadHealth() {
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) return
      const id = ++this.healthRequestId
      const end = new Date(), start = new Date()
      start.setDate(start.getDate() - 364)
      const date = d => d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
      this.healthError = false
      const results = await Promise.allSettled([listWellness('body', date(start), date(end)), listWellness('sleep', date(start), date(end))])
      if (id !== this.healthRequestId) return
      if (results.some(result => result.status === 'rejected' && result.reason.status === 401)) { this.expireSession(); return }
      if (results[0].status === 'fulfilled') this.latestWeight = (results[0].value || []).find(item => item.weightKg != null) || null
      if (results[1].status === 'fulfilled') this.latestSleep = (results[1].value || [])[0] || null
      this.healthError = results.some(result => result.status === 'rejected')
    },
    goRecord() {
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        setTimeout(() => uni.reLaunch({ url: '/pages/login/index' }), 600)
        return
      }
      uni.navigateTo({ url: '/pages/ledger/record' })
    },
    goLedger() {
      uni.navigateTo({ url: '/pages/ledger/list' })
    },
    goJournalRecord() {
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) { this.expireSession(); return }
      uni.navigateTo({ url: '/pages/journal/record' })
    },
    goJournalList() {
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) { this.expireSession(); return }
      uni.navigateTo({ url: '/pages/journal/list' })
    },
    loadTodayLogs() {
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) return
      const requestId = ++this.todayLogRequestId
      const now = new Date()
      const pad = (n) => String(n).padStart(2, '0')
      const today = now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate())
      this.todayLogError = false
      uni.request({
        url: API_BASE_URL + '/api/logs?from=' + today + '&to=' + today + '&page=1&size=1',
        header: { Authorization: 'Bearer ' + token },
        success: (res) => {
          if (requestId !== this.todayLogRequestId) return
          if (res.statusCode === 200 && res.data) this.todayLogCount = res.data.total || 0
          else if (res.statusCode === 401) this.expireSession()
          else this.todayLogError = true
        },
        fail: () => { if (requestId === this.todayLogRequestId) this.todayLogError = true }
      })
    },
    loadTodayLedger() {
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) return
      const requestId = ++this.todayRequestId
      this.todayEntries = []
      const now = new Date()
      const pad = (n) => String(n).padStart(2, '0')
      const today = now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate())
      this.todayLedgerError = false
      uni.request({
        url: API_BASE_URL + '/api/ledger/summary?from=' + today + '&to=' + today,
        header: { Authorization: 'Bearer ' + token },
        success: (res) => {
          if (requestId !== this.todayRequestId) return
          if (res.statusCode === 200 && res.data) {
            this.todayExpense = this.formatAmount(res.data.expense)
            this.todayCount = res.data.count || 0
          } else {
            this.todayLedgerError = true
            if (res.statusCode === 401) this.expireSession()
          }
        },
        fail: () => { if (requestId === this.todayRequestId) this.todayLedgerError = true }
      })
      uni.request({
        url: API_BASE_URL + '/api/ledger/entries?from=' + today + '&to=' + today + '&size=5',
        header: { Authorization: 'Bearer ' + token },
        success: (res) => {
          if (requestId !== this.todayRequestId) return
          if (res.statusCode === 200 && res.data) {
            this.todayEntries = res.data.items || []
          } else if (res.statusCode === 401) {
            this.expireSession()
          } else {
            this.todayLedgerError = true
          }
        },
        fail: () => { if (requestId === this.todayRequestId) { this.todayLedgerError = true; this.todayEntries = [] } }
      })
    },
    expireSession() {
      uni.removeStorageSync(TOKEN_KEY)
      uni.reLaunch({ url: '/pages/login/index' })
    },
    formatAmount(value) {
      const n = Number(value)
      return Number.isFinite(n) ? n.toFixed(2) : '0.00'
    },
    logout() {
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) return
      uni.request({
        url: API_BASE_URL + '/api/auth/logout',
        method: 'POST',
        header: { Authorization: 'Bearer ' + token },
        complete: () => {
          uni.removeStorageSync(TOKEN_KEY)
          uni.reLaunch({ url: '/pages/login/index' })
        }
      })
    }
  }
}
</script>

<style>
page { background: #f5f7f5; }
button::after { border: 0; }
.logout-button { margin: 20rpx auto 0; width: 180rpx; background: transparent; color: #789398; font-size: 20rpx; }
.home { padding: 0 30rpx 60rpx; color: #1d414b; }
.status-bar { height: var(--status-bar-height); }
.header { display: flex; justify-content: space-between; align-items: center; margin: 28rpx 3rpx 28rpx; }
.eyebrow { font-size: 18rpx; letter-spacing: 6rpx; color: #7b9da0; }
.brand { display: flex; align-items: center; margin-top: 3rpx; }
.brand-name { font-size: 46rpx; font-weight: 700; letter-spacing: 4rpx; }
.brand-note { margin-left: 18rpx; padding-left: 18rpx; border-left: 1rpx solid #a9c4c2; color: #80979c; font-size: 22rpx; }
.date { display: flex; align-items: baseline; }
.date-number { font-size: 42rpx; font-weight: 600; }
.date-month { font-size: 21rpx; color: #7b969b; margin-left: 7rpx; }
.hero { position: relative; overflow: hidden; min-height: 390rpx; border-radius: 32rpx; background: linear-gradient(135deg, #103a4b, #165a67 55%, #3a918b); box-shadow: 0 18rpx 32rpx rgba(17, 68, 73, .14); }
.hero-inner { position: relative; z-index: 1; display: flex; flex-direction: column; align-items: flex-start; padding: 36rpx 38rpx; }
.hero-top { font-size: 22rpx; color: #c9e8e2; letter-spacing: 2rpx; }
.hero-title { margin-top: 20rpx; font-size: 50rpx; line-height: 1.25; font-weight: 600; color: #fff; letter-spacing: 2rpx; }
.hero-title-next { margin-top: 0; }
.hero-subtitle { display: flex; flex-direction: column; margin-top: 12rpx; font-size: 21rpx; line-height: 1.6; color: #c7e2df; }
.hero-button { margin: 24rpx 0 0; padding: 0 24rpx; height: 62rpx; line-height: 62rpx; background: #e4f3ee; color: #174d56; border-radius: 35rpx; font-size: 22rpx; font-weight: 600; }
.hero-circle { position: absolute; border-radius: 50%; border: 1rpx solid rgba(226, 250, 242, .24); }
.circle-one { width: 440rpx; height: 440rpx; right: -155rpx; top: -120rpx; background: rgba(190, 234, 222, .07); }
.circle-two { width: 280rpx; height: 280rpx; right: 0; bottom: -100rpx; }
.sample-label { display: flex; align-items: center; justify-content: center; margin: 24rpx 0 30rpx; font-size: 20rpx; color: #92a5a7; }
.sample-dot { width: 9rpx; height: 9rpx; border-radius: 50%; background: #8eaaa7; margin-right: 10rpx; }
.section-head { display: flex; justify-content: space-between; align-items: flex-end; margin: 0 4rpx 18rpx; }
.section-head > view { display: flex; flex-direction: column; }
.section-en { color: #91abad; font-size: 17rpx; letter-spacing: 4rpx; font-weight: 600; }
.section-title { color: #22444d; font-size: 31rpx; font-weight: 650; margin-top: 7rpx; }
.section-side { color: #97a9ab; font-size: 20rpx; padding-bottom: 3rpx; }
.later { margin-top: 42rpx; }
.stats { display: flex; gap: 13rpx; }
.stat { box-sizing: border-box; flex: 1; min-width: 0; min-height: 145rpx; padding: 20rpx 15rpx 15rpx; background: #fff; border-radius: 22rpx; box-shadow: 0 4rpx 18rpx rgba(25, 75, 77, .035); }
.stat-name { color: #81979b; font-size: 19rpx; }
.stat-number { display: flex; align-items: baseline; margin-top: 12rpx; color: #1d424b; font-size: 35rpx; line-height: 1; font-weight: 650; white-space: nowrap; }
.unit, .suffix { font-size: 19rpx; font-weight: 400; }
.unit { margin-right: 3rpx; }
.suffix { margin-left: 5rpx; color: #749198; }
.stat-note { display: block; margin-top: 12rpx; color: #a5b3b3; font-size: 17rpx; }
.actions { display: flex; flex-wrap: wrap; justify-content: space-between; row-gap: 14rpx; }
.action { box-sizing: border-box; display: flex; align-items: center; width: 49%; height: 130rpx; margin: 0; padding: 18rpx 14rpx; background: #fff; border-radius: 22rpx; text-align: left; line-height: normal; box-shadow: 0 4rpx 18rpx rgba(25, 75, 77, .035); }
.action-icon { display: flex; align-items: center; justify-content: center; flex-shrink: 0; width: 61rpx; height: 61rpx; border-radius: 18rpx; font-size: 29rpx; font-weight: 600; }
.icon-green { color: #2a8075; background: #e2f3ee; }
.icon-blue { color: #3a7292; background: #e8f2f5; }
.icon-sand { color: #ae7b51; background: #f8efe5; }
.icon-purple { color: #7775a0; background: #f0eef7; }
.action-text { display: flex; flex-direction: column; flex: 1; min-width: 0; margin-left: 11rpx; }
.action-title { color: #294a52; font-size: 22rpx; font-weight: 600; white-space: nowrap; }
.action-desc { color: #9aabaa; font-size: 17rpx; margin-top: 6rpx; white-space: nowrap; }
.arrow { color: #b5c5c4; font-size: 30rpx; }
.timeline { padding: 26rpx 28rpx 5rpx; background: #fff; border-radius: 24rpx; box-shadow: 0 4rpx 18rpx rgba(25, 75, 77, .035); }
.event { display: flex; min-height: 120rpx; }
.event-time { width: 88rpx; flex-shrink: 0; color: #6c8d94; font-size: 20rpx; font-weight: 600; }
.event-info { display: flex; flex-direction: column; padding: 0 0 23rpx 14rpx; border-left: 2rpx solid #dce9e6; }
.event-type { color: #3a8a80; font-size: 17rpx; letter-spacing: 2rpx; }
.event-title { color: #2c4c54; font-size: 24rpx; font-weight: 600; margin-top: 7rpx; }
.event-detail { color: #9aadae; font-size: 19rpx; margin-top: 7rpx; }
.last { min-height: 95rpx; }
.last .event-info { border-left-color: transparent; }
.footer { display: block; text-align: center; margin-top: 38rpx; font-size: 18rpx; color: #a1b2b3; letter-spacing: 1rpx; }
.ledger-more { margin-top: 14rpx; background: #eaf4ef; border-radius: 18rpx; color: #2c756d; font-size: 22rpx; }
/* 汐序 v1：以真实数据和快速记录为中心 */
page { background: var(--xi-bg); }
.home { padding: 0 34rpx calc(74rpx + env(safe-area-inset-bottom)); color: var(--xi-ink); }
.header { margin: 28rpx 2rpx 25rpx; }
.eyebrow { color: var(--xi-brand); font-size: 19rpx; letter-spacing: 5rpx; }
.brand-name { color: var(--xi-ink); font-size: 45rpx; }
.brand-note { color: var(--xi-body); font-size: 23rpx; }
.date-number { color: var(--xi-ink); font-size: 42rpx; }
.date-month { color: var(--xi-body); font-size: 22rpx; }
.hero { min-height: 0; border-radius: 30rpx; background: var(--xi-deep); box-shadow: none; }
.hero-inner { padding: 39rpx 38rpx 37rpx; }
.hero-top { color: #c6e5dc; font-size: 23rpx; letter-spacing: 0; }
.hero-title { margin-top: 18rpx; font-size: 43rpx; line-height: 1.3; letter-spacing: 0; }
.hero-subtitle { margin-top: 12rpx; color: #d4e7e0; font-size: 24rpx; line-height: 1.55; }
.hero-button { display: flex; align-items: center; justify-content: space-between; min-width: 210rpx; height: 82rpx; line-height: 82rpx; margin: 28rpx 0 0; padding: 0 28rpx; border-radius: 17rpx; background: #eaf5ee; color: var(--xi-deep); font-size: 25rpx; }
.hero-button text { margin-left: 24rpx; }
.sample-label { justify-content: flex-start; margin: 21rpx 2rpx 34rpx; color: var(--xi-body); font-size: 21rpx; }
.sample-dot { background: var(--xi-brand); }
.section-head { margin-bottom: 17rpx; }
.section-en { color: var(--xi-brand); font-size: 19rpx; letter-spacing: 3rpx; }
.section-title { color: var(--xi-ink); font-size: 34rpx; }
.section-side { color: var(--xi-body); font-size: 22rpx; }
.later { margin-top: 42rpx; }
.stats { gap: 15rpx; }
.stat { min-height: 166rpx; padding: 23rpx 22rpx; border: 1rpx solid var(--xi-border); border-radius: 24rpx; box-shadow: none; }
.stat-name { color: var(--xi-body); font-size: 22rpx; }
.stat-number { margin-top: 13rpx; color: var(--xi-ink); font-size: 42rpx; }
.unit, .suffix { font-size: 22rpx; }
.stat-note { color: var(--xi-body); font-size: 20rpx; }
.actions { row-gap: 15rpx; }
.action { width: 49%; min-height: 130rpx; padding: 17rpx 16rpx; border: 1rpx solid var(--xi-border); border-radius: 23rpx; box-shadow: none; }
.action-icon { width: 50rpx; height: 50rpx; border-radius: 13rpx; font-size: 20rpx; letter-spacing: 1rpx; }
.action-title { color: var(--xi-ink); font-size: 24rpx; }
.action-desc { color: var(--xi-body); font-size: 19rpx; }
.arrow { color: var(--xi-brand); }
.timeline { border: 1rpx solid var(--xi-border); box-shadow: none; }
.event-time, .event-detail { color: var(--xi-body); font-size: 21rpx; }
.event-title { color: var(--xi-ink); font-size: 25rpx; }
.event-type { color: var(--xi-brand); font-size: 20rpx; }
.footer { color: var(--xi-body); font-size: 20rpx; }
.logout-button { min-height: 82rpx; color: var(--xi-body); font-size: 22rpx; }
.health-preview { display:flex; gap:15rpx; margin-bottom:38rpx; }.health-preview > view { box-sizing:border-box; flex:1; min-width:0; display:flex; flex-direction:column; min-height:170rpx; padding:24rpx 21rpx; border:1rpx solid var(--xi-border); border-radius:24rpx; background:#fff; }.health-preview text:first-child { color:var(--xi-body); font-size:22rpx; }.health-value { margin-top:14rpx; color:var(--xi-ink); font-size:31rpx; font-weight:650; white-space:nowrap; }.health-date { margin-top:8rpx; color:var(--xi-body); font-size:20rpx; }
button:active, .stat:active { opacity: .78; }
</style>
