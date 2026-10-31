<template>
  <view class="health-page">
    <view class="status-bar"></view>
    <view class="top-bar"><text class="back" @tap="goBack">‹ 返回</text><text class="top-title">身体与训练</text><text class="spacer"></text></view>
    <view class="intro"><text class="intro-kicker">YOUR BODY, YOUR PACE</text><text class="intro-title">从真实记录，看见变化。</text><text class="intro-note">先记录体重与睡眠，未测量的项目保持空白。</text></view>
    <view class="summary-row">
      <view class="summary-card" @tap="selectTab('body')"><text>最近体重</text><text class="summary-value">{{ latestWeight ? latestWeight.weightKg + ' kg' : '尚未记录' }}</text><text class="summary-date">{{ latestWeight ? latestWeight.observedOn : '记录第一次测量' }}</text></view>
      <view class="summary-card" @tap="selectTab('sleep')"><text>最近睡眠</text><text class="summary-value">{{ latestSleep ? durationText(latestSleep.durationMinutes) : '尚未记录' }}</text><text class="summary-date">{{ latestSleep ? latestSleep.sleepOn : '记录第一次睡眠' }}</text></view>
    </view>
    <view class="switch" role="tablist">
      <view :class="['switch-item', { active: tab === 'body' }]" @tap="selectTab('body')">体重与体成分</view>
      <view :class="['switch-item', { active: tab === 'sleep' }]" @tap="selectTab('sleep')">睡眠</view>
    </view>
    <view class="period-row"><text>查看范围</text><view><text v-for="option in periods" :key="option" :class="['period', { active: period === option }]" @tap="setPeriod(option)">{{ option === 365 ? '一年' : option + '天' }}</text></view></view>
    <view v-if="error" class="message"><text>{{ error }}</text><button @tap="load">重试</button></view>
    <view class="card trend-card"><view class="card-heading"><view><text class="eyebrow">TREND</text><text class="card-title">{{ tab === 'body' ? '体重趋势' : '睡眠时长趋势' }}</text></view><text>最近 {{ period === 365 ? '一年' : period + '天' }}</text></view>
      <view v-if="loading" class="empty-trend">正在加载…</view>
      <view v-else-if="trend.length >= 4" class="bar-chart">
        <view v-for="item in trend" :key="item.id" class="bar-column"><text class="bar-value">{{ tab === 'body' ? Number(item.weightKg).toFixed(1) : (item.durationMinutes / 60).toFixed(1) }}</text><view class="bar-track"><view class="bar-fill" :style="{ height: barHeight(item) + '%' }"></view></view><text class="bar-date">{{ tab === 'body' ? item.observedOn.slice(5) : item.sleepOn.slice(5) }}</text></view>
      </view>
      <view v-else class="empty-trend"><text>{{ selectedItems.length ? '再记录几次，就能看到变化' : '这里还没有记录' }}</text><text class="empty-note">至少 4 个有效数据点后显示趋势；下方始终保留原始数值。</text></view>
    </view>
    <view class="list-heading"><view><text class="eyebrow">HISTORY</text><text class="list-title">最近记录</text></view><text>{{ selectedItems.length }} 条</text></view>
    <view v-if="selectedItems.length" class="card history">
      <view v-for="item in selectedItems.slice(0, 20)" :key="item.id" class="history-row"><view><text class="history-date">{{ tab === 'body' ? item.observedOn : item.sleepOn }}</text><text class="history-meta">{{ tab === 'body' ? bodyMeta(item) : sleepMeta(item) }}</text></view><text class="history-value">{{ tab === 'body' ? item.weightKg != null ? item.weightKg + ' kg' : '体重未填' : durationText(item.durationMinutes) }}</text></view>
    </view>
    <view v-else-if="!loading" class="card empty-list">记录会按日期出现在这里。</view>
    <button class="record-button" @tap="goRecord">＋ {{ tab === 'body' ? '记录身体测量' : '记录睡眠' }}</button>
  </view>
</template>

<script>
import { TOKEN_KEY } from '../../common/api.js'
import { listWellness } from '../../common/wellness.js'

function dateText(date) { return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0') }

export default {
  data() { return { tab: 'body', period: 30, periods: [30, 90, 365], body: [], sleep: [], loading: false, error: '', requestId: 0 } },
  computed: {
    selectedItems() { return this.tab === 'body' ? this.body : this.sleep },
    latestWeight() { return this.body.find(item => item.weightKg != null) },
    latestSleep() { return this.sleep[0] },
    trend() { return this.selectedItems.filter(item => this.tab === 'body' ? item.weightKg != null : item.durationMinutes != null).slice(0, 7).reverse() }
  },
  onLoad(options) { if (options && options.tab === 'sleep') this.tab = 'sleep' },
  onShow() { this.load() },
  methods: {
    goBack() { if (getCurrentPages().length > 1) uni.navigateBack(); else uni.reLaunch({ url: '/pages/index/index' }) },
    selectTab(tab) { this.tab = tab },
    setPeriod(period) { if (period !== this.period) { this.period = period; this.load() } },
    goRecord() { uni.navigateTo({ url: '/pages/health/record?tab=' + this.tab }) },
    durationText(minutes) { return Math.floor(minutes / 60) + '小时' + minutes % 60 + '分' },
    bodyMeta(item) { return [item.bodyFatPct != null ? '体脂 ' + item.bodyFatPct + '%' : '', item.muscleMassKg != null ? '肌肉量 ' + item.muscleMassKg + ' kg' : ''].filter(Boolean).join(' · ') || '手动记录' },
    sleepMeta(item) { return (item.sessionKind === 'NAP' ? '午睡' : '夜间睡眠') + (item.deepMinutes != null ? ' · 深睡 ' + item.deepMinutes + ' 分' : '') },
    barHeight(item) {
      const values = this.trend.map(row => this.tab === 'body' ? Number(row.weightKg) : Number(row.durationMinutes) / 60)
      const value = this.tab === 'body' ? Number(item.weightKg) : Number(item.durationMinutes) / 60
      const min = Math.min(...values), max = Math.max(...values)
      return max === min ? 60 : 25 + (value - min) / (max - min) * 70
    },
    async load() {
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) { uni.reLaunch({ url: '/pages/login/index' }); return }
      const id = ++this.requestId
      const end = new Date(), start = new Date()
      start.setDate(start.getDate() - this.period + 1)
      this.loading = true; this.error = ''
      const results = await Promise.allSettled([listWellness('body', dateText(start), dateText(end)), listWellness('sleep', dateText(start), dateText(end))])
      if (id !== this.requestId) return
      if (results.some(result => result.status === 'rejected' && result.reason.status === 401)) { uni.removeStorageSync(TOKEN_KEY); uni.reLaunch({ url: '/pages/login/index' }); return }
      if (results[0].status === 'fulfilled') this.body = results[0].value || []
      if (results[1].status === 'fulfilled') this.sleep = results[1].value || []
      if (results.some(result => result.status === 'rejected')) this.error = '部分记录暂未同步，请检查服务端。'
      this.loading = false
    }
  }
}
</script>

<style>
page { background:#f5f7f4; } button::after { border:0; }
.health-page { padding:0 30rpx calc(170rpx + env(safe-area-inset-bottom)); color:#173d46; }.status-bar { height:var(--status-bar-height); }
.top-bar { display:flex; justify-content:space-between; align-items:center; min-height:96rpx; margin-bottom:20rpx; }.back { display:flex; align-items:center; width:124rpx; min-height:96rpx; color:#17675f; font-size:27rpx; }.top-title { font-size:30rpx; font-weight:650; }.spacer { width:124rpx; }
.intro { display:flex; flex-direction:column; margin:5rpx 3rpx 28rpx; }.intro-kicker,.eyebrow { color:#397e72; font-size:20rpx; font-weight:600; letter-spacing:3rpx; }.intro-title { margin-top:10rpx; font-size:40rpx; font-weight:650; }.intro-note { margin-top:9rpx; color:#567077; font-size:24rpx; line-height:1.5; }
.summary-row { display:flex; gap:14rpx; }.summary-card { flex:1; min-width:0; min-height:170rpx; display:flex; flex-direction:column; padding:23rpx 20rpx; border:1rpx solid #dae6e1; border-radius:23rpx; background:#fff; }.summary-card text { color:#567077; font-size:22rpx; }.summary-card .summary-value { margin-top:15rpx; color:#173d46; font-size:32rpx; white-space:nowrap; font-weight:650; }.summary-card .summary-date { margin-top:10rpx; color:#567077; font-size:20rpx; }
.switch { display:flex; margin-top:30rpx; padding:6rpx; border-radius:20rpx; background:#e7f2ed; }.switch-item { flex:1; min-height:88rpx; display:flex; align-items:center; justify-content:center; border-radius:16rpx; color:#345f60; font-size:24rpx; }.switch-item.active { background:#17675f; color:#fff; font-weight:600; }
.period-row { display:flex; justify-content:space-between; align-items:center; margin:22rpx 3rpx; color:#567077; font-size:22rpx; }.period-row > view { display:flex; gap:6rpx; }.period { min-width:78rpx; min-height:76rpx; display:flex; align-items:center; justify-content:center; border-radius:14rpx; color:#345f60; font-size:22rpx; }.period.active { background:#d9eee5; color:#17675f; font-weight:600; }
.card { border:1rpx solid #dae6e1; border-radius:24rpx; background:#fff; }.trend-card { padding:28rpx 24rpx; }.card-heading { display:flex; justify-content:space-between; align-items:flex-end; }.card-heading > view { display:flex; flex-direction:column; }.card-heading > text { color:#567077; font-size:21rpx; }.card-title { margin-top:6rpx; color:#173d46; font-size:28rpx; font-weight:650; }.empty-trend { display:flex; flex-direction:column; align-items:center; justify-content:center; min-height:240rpx; color:#34585d; font-size:26rpx; text-align:center; }.empty-note { max-width:430rpx; margin-top:10rpx; color:#567077; font-size:21rpx; line-height:1.6; }
.bar-chart { display:flex; gap:7rpx; align-items:end; justify-content:space-around; height:260rpx; margin-top:25rpx; }.bar-column { flex:1; min-width:0; display:flex; flex-direction:column; align-items:center; height:100%; }.bar-value { color:#34585d; font-size:19rpx; }.bar-track { flex:1; width:100%; max-width:55rpx; display:flex; align-items:end; margin:8rpx 0; background:#f0f5f1; border-radius:9rpx; overflow:hidden; }.bar-fill { width:100%; background:#17675f; border-radius:9rpx; }.bar-date { color:#567077; font-size:18rpx; white-space:nowrap; }
.list-heading { display:flex; justify-content:space-between; align-items:end; margin:38rpx 3rpx 17rpx; }.list-heading > view { display:flex; flex-direction:column; }.list-title { margin-top:6rpx; font-size:30rpx; font-weight:650; }.list-heading > text { color:#567077; font-size:22rpx; }.history { padding:0 22rpx; }.history-row { display:flex; justify-content:space-between; align-items:center; gap:15rpx; min-height:116rpx; border-bottom:1rpx solid #edf2ee; }.history-row:last-child { border-bottom:0; }.history-row > view { display:flex; flex-direction:column; min-width:0; }.history-date { color:#173d46; font-size:24rpx; }.history-meta { margin-top:7rpx; color:#567077; font-size:20rpx; }.history-value { flex-shrink:0; color:#173d46; font-size:26rpx; font-weight:650; }.empty-list { padding:55rpx 20rpx; color:#567077; text-align:center; font-size:24rpx; }.message { display:flex; justify-content:space-between; align-items:center; margin:12rpx 0; padding:16rpx; border-radius:14rpx; background:#fff2ed; color:#a75543; font-size:22rpx; }.message button { min-height:76rpx; margin:0; color:#17675f; background:transparent; font-size:22rpx; }
.record-button { position:fixed; left:30rpx; right:30rpx; bottom:calc(24rpx + env(safe-area-inset-bottom)); height:100rpx; line-height:100rpx; border-radius:18rpx; background:#17675f; color:#fff; font-size:28rpx; font-weight:600; }
</style>
