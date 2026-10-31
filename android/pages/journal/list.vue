<template>
  <view class="journal-page">
    <view class="status-bar"></view>
    <view class="top-bar"><text class="back" @tap="goBack">‹ 返回</text><text class="top-title">日志与训练</text><text class="spacer"></text></view>

    <view class="heading"><text class="eyebrow">XIXU / JOURNAL</text><text class="title">把经历，留在当天。</text><text class="subtitle">日常片段和训练组次，按发生日期回看。</text></view>

    <view class="month-bar"><text class="month-arrow" @tap="shiftMonth(-1)">‹</text><view class="month-label"><text>{{ monthLabel }}</text><text>按发生日期查看</text></view><text class="month-arrow" @tap="shiftMonth(1)">›</text></view>

    <view class="filter-row">
      <view v-for="filter in filters" :key="filter.value" :class="['filter-chip', { active: type === filter.value }]" @tap="selectType(filter.value)">{{ filter.label }}</view>
    </view>

    <view class="list-heading"><view><text class="eyebrow">RECORDS</text><text class="list-title">这个月的记录</text></view><text class="count">共 {{ total }} 条</text></view>
    <view v-if="loading && items.length === 0" class="empty"><text>正在加载记录…</text></view>
    <view v-else-if="error" class="empty"><text>记录暂时没能加载</text><text class="empty-note">{{ error }}</text><button @tap="loadLogs(1)">重试</button></view>
    <view v-else-if="items.length === 0" class="empty"><text>这个月还没有{{ type === 'WORKOUT' ? '训练' : type === 'NOTE' ? '日志' : '记录' }}</text><text class="empty-note">写下第一条，再慢慢回看。</text><button @tap="goRecord">写一条 →</button></view>
    <view v-else class="entry-list">
      <view v-for="item in items" :key="item.id" class="entry">
        <view class="entry-meta"><text>{{ item.occurredOn }}</text><text v-if="item.timeLabel">{{ item.timeLabel }}</text><text :class="['type-tag', item.entryType === 'WORKOUT' ? 'workout' : 'note']">{{ item.entryType === 'WORKOUT' ? '训练' : item.categoryName || '日志' }}</text><text v-if="item.sourceType === 'IMPORT'" class="source-tag">原始日志</text></view>
        <text v-if="item.sourceType !== 'IMPORT'" class="entry-title">{{ item.title }}</text>
        <text v-if="item.content" class="entry-content">{{ item.content }}</text>
        <text v-if="item.remark" class="entry-remark">备注：{{ item.remark }}</text>
        <view v-if="item.sets && item.sets.length" class="set-preview">
          <text v-for="set in item.sets.slice(0, 2)" :key="set.id" class="set-pill">{{ formatSet(set) }}</text>
          <text v-if="item.sets.length > 2" class="set-more">另有 {{ item.sets.length - 2 }} 组</text>
        </view>
      </view>
      <text v-if="loading" class="load-note">正在加载更多…</text>
      <button v-else-if="items.length < total" class="more-button" @tap="loadLogs(page + 1)">加载更多</button>
      <text v-else class="load-note">已显示全部 {{ total }} 条</text>
    </view>
    <button class="record-button" @tap="goRecord">＋ 写一条记录</button>
  </view>
</template>

<script>
import { API_BASE_URL, TOKEN_KEY } from '../../common/api.js'

export default {
  data() {
    const now = new Date()
    return {
      year: now.getFullYear(), month: now.getMonth() + 1, type: '',
      filters: [{ label: '全部', value: '' }, { label: '日常日志', value: 'NOTE' }, { label: '健身训练', value: 'WORKOUT' }],
      items: [], total: 0, page: 1, loading: false, error: '', requestId: 0
    }
  },
  computed: {
    monthLabel() { return this.year + '年' + String(this.month).padStart(2, '0') + '月' },
    from() { return this.year + '-' + String(this.month).padStart(2, '0') + '-01' },
    to() { return this.year + '-' + String(this.month).padStart(2, '0') + '-' + String(new Date(this.year, this.month, 0).getDate()).padStart(2, '0') }
  },
  onShow() { this.loadLogs(1) },
  onReachBottom() { if (!this.loading && !this.error && this.items.length < this.total) this.loadLogs(this.page + 1) },
  methods: {
    goBack() { if (getCurrentPages().length > 1) uni.navigateBack(); else uni.reLaunch({ url: '/pages/index/index' }) },
    goRecord() { uni.navigateTo({ url: '/pages/journal/record' }) },
    expireSession() { uni.removeStorageSync(TOKEN_KEY); uni.reLaunch({ url: '/pages/login/index' }) },
    shiftMonth(delta) {
      const date = new Date(this.year, this.month - 1 + delta, 1)
      this.year = date.getFullYear()
      this.month = date.getMonth() + 1
      this.loadLogs(1)
    },
    selectType(value) {
      if (this.type === value) return
      this.type = value
      this.loadLogs(1)
    },
    formatSet(set) {
      const parts = [set.exerciseName]
      if (set.repetitions != null) parts.push(set.repetitions + ' 次')
      if (set.weightKg != null) parts.push(set.weightKg + ' kg')
      if (set.durationSeconds != null) parts.push(set.durationSeconds + ' 秒')
      return parts.join(' · ')
    },
    loadLogs(page) {
      if (this.loading && page !== 1) return
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) { this.expireSession(); return }
      const id = ++this.requestId
      this.loading = true
      this.error = ''
      if (page === 1) { this.items = []; this.total = 0; this.page = 1 }
      const params = 'from=' + this.from + '&to=' + this.to + '&page=' + page + '&size=20' + (this.type ? '&type=' + this.type : '')
      uni.request({
        url: API_BASE_URL + '/api/logs?' + params,
        header: { Authorization: 'Bearer ' + token },
        success: (res) => {
          if (id !== this.requestId) return
          if (res.statusCode === 200 && res.data) {
            this.items = page === 1 ? (res.data.items || []) : this.items.concat(res.data.items || [])
            this.total = res.data.total || 0
            this.page = page
          } else if (res.statusCode === 401) this.expireSession()
          else this.error = (res.data && res.data.message) || '服务端暂时无法读取日志'
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
.journal-page { padding: 0 30rpx 145rpx; color: #1d414b; }
.status-bar { height: var(--status-bar-height); }
.top-bar { display: flex; align-items: center; justify-content: space-between; margin: 24rpx 0 28rpx; }
.back, .spacer { width: 110rpx; }
.back { color: #4e7b80; font-size: 26rpx; }
.top-title { color: #22444d; font-size: 30rpx; font-weight: 650; }
.heading { display: flex; flex-direction: column; margin: 8rpx 3rpx 30rpx; }
.eyebrow { color: #6aa59a; font-size: 17rpx; font-weight: 600; letter-spacing: 4rpx; }
.title { margin-top: 10rpx; color: #1f454d; font-size: 40rpx; font-weight: 650; }
.subtitle { margin-top: 9rpx; color: #8fa6a5; font-size: 21rpx; line-height: 1.6; }
.month-bar { display: flex; align-items: center; justify-content: space-between; padding: 13rpx 22rpx; border-radius: 22rpx; background: #e9f2ed; }
.month-arrow { width: 62rpx; line-height: 70rpx; color: #357b73; font-size: 48rpx; text-align: center; }
.month-label { display: flex; flex-direction: column; align-items: center; color: #24545a; font-size: 27rpx; font-weight: 600; }
.month-label text:last-child { margin-top: 3rpx; color: #82a09d; font-size: 17rpx; font-weight: 400; }
.filter-row { display: flex; gap: 12rpx; margin-top: 21rpx; }
.filter-chip { padding: 12rpx 24rpx; border-radius: 30rpx; background: #fff; color: #688884; font-size: 22rpx; }
.filter-chip.active { background: #154e59; color: #fff; }
.list-heading { display: flex; justify-content: space-between; align-items: flex-end; margin: 42rpx 3rpx 16rpx; }
.list-heading > view { display: flex; flex-direction: column; }
.list-title { margin-top: 5rpx; font-size: 31rpx; font-weight: 650; }
.count { color: #90a6a4; font-size: 21rpx; }
.entry-list { border-radius: 24rpx; overflow: hidden; background: #fff; }
.entry { display: flex; flex-direction: column; padding: 24rpx 26rpx; border-bottom: 1rpx solid #edf2ef; }
.entry-meta { display: flex; align-items: center; gap: 13rpx; color: #8ca5a2; font-size: 19rpx; }
.type-tag { padding: 4rpx 13rpx; border-radius: 20rpx; font-size: 17rpx; }
.type-tag.note { color: #997b58; background: #f9f0e5; }
.type-tag.workout { color: #377a70; background: #e7f4ed; }
.source-tag { color: #8d9e98; font-size: 17rpx; }
.entry-title { margin-top: 11rpx; color: #234850; font-size: 28rpx; font-weight: 650; }
.entry-content { margin-top: 8rpx; color: #718c8a; font-size: 21rpx; line-height: 1.55; white-space: pre-wrap; }
.entry-remark { margin-top: 8rpx; color: #91a49e; font-size: 19rpx; line-height: 1.5; white-space: pre-wrap; }
.set-preview { display: flex; flex-wrap: wrap; gap: 9rpx; margin-top: 16rpx; }
.set-pill, .set-more { padding: 7rpx 13rpx; border-radius: 12rpx; background: #f0f7f3; color: #598178; font-size: 18rpx; }
.set-more { color: #859e9a; background: #f5f7f5; }
.empty { display: flex; flex-direction: column; align-items: center; padding: 75rpx 26rpx; border-radius: 25rpx; background: #fff; color: #365c61; font-size: 27rpx; }
.empty-note { margin-top: 11rpx; color: #9aaead; font-size: 21rpx; text-align: center; }
.empty button, .more-button { margin-top: 26rpx; padding: 0 30rpx; border-radius: 30rpx; background: #e5f2ec; color: #27786e; font-size: 22rpx; }
.load-note { display: block; padding: 22rpx; color: #a0b2b0; font-size: 19rpx; text-align: center; }
.more-button { margin-bottom: 25rpx; }
.record-button { position: fixed; bottom: 30rpx; left: 30rpx; right: 30rpx; z-index: 10; height: 88rpx; line-height: 88rpx; border-radius: 22rpx; background: linear-gradient(110deg, #154b58, #2b837a); color: #fff; font-size: 27rpx; font-weight: 600; box-shadow: 0 14rpx 26rpx rgba(24,83,82,.2); }
</style>
