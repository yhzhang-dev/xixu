<template>
  <view class="journal-record-page">
    <view class="status-bar"></view>
    <view class="top-bar"><text class="back" @tap="goBack">‹ 返回</text><text class="top-title">写一条记录</text><text class="spacer"></text></view>

    <view class="intro"><text class="intro-kicker">A DAY, IN YOUR WORDS</text><text class="intro-title">记下今天的一小段。</text><text class="intro-note">可以写日常，也可以把训练逐组记清楚。</text></view>

    <view class="type-switch">
      <view :class="['type-item', { active: entryType === 'NOTE' }]" @tap="entryType = 'NOTE'">日常日志</view>
      <view :class="['type-item', { active: entryType === 'WORKOUT' }]" @tap="entryType = 'WORKOUT'">健身训练</view>
    </view>

    <view class="form-card">
      <view class="card-heading"><text>基本信息</text><text>日期与标题必填</text></view>
      <view class="field"><text class="field-label">发生日期</text><picker mode="date" :value="occurredOn" @change="onDateChange"><view class="picker-value">{{ occurredOn }}</view></picker></view>
      <view class="field"><text class="field-label">标题</text><input v-model="title" class="field-input" type="text" maxlength="100" :placeholder="entryType === 'WORKOUT' ? '如：胸部训练' : '今天想记下什么？'" placeholder-class="placeholder" /></view>
      <view v-if="entryType === 'NOTE'" class="field"><text class="field-label">正文</text><textarea v-model="content" class="field-textarea" maxlength="2000" placeholder="选填，写下更完整的经过" placeholder-class="placeholder" /></view>
    </view>

    <view v-if="entryType === 'WORKOUT'" class="form-card sets-card">
      <view class="card-heading"><text>训练组次</text><text>{{ sets.length }} / 30 组</text></view>
      <text class="card-note">每组填写动作名称，以及次数或时长。重量没记下时可以留空。</text>
      <view v-for="(set, index) in sets" :key="set._key" class="set-card">
        <view class="set-head"><text>第 {{ index + 1 }} 组</text><view><text v-if="sets.length < 30" @tap="copySet(index)">复制此组</text><text v-if="sets.length > 1" class="remove" @tap="removeSet(index)">移除</text></view></view>
        <view class="field"><text class="field-label">训练项目</text><input v-model="set.exerciseName" class="field-input" type="text" maxlength="80" placeholder="如：卧推、跑步" placeholder-class="placeholder" /></view>
        <view class="metric-row">
          <view class="metric-field"><text class="field-label">次数</text><view class="metric-input"><input v-model="set.repetitions" type="number" placeholder="选填" placeholder-class="placeholder" /><text>次</text></view></view>
          <view class="metric-field"><text class="field-label">重量</text><view class="metric-input"><input v-model="set.weightKg" type="digit" placeholder="选填" placeholder-class="placeholder" /><text>kg</text></view></view>
          <view class="metric-field"><text class="field-label">时长</text><view class="metric-input"><input v-model="set.durationSeconds" type="number" placeholder="选填" placeholder-class="placeholder" /><text>秒</text></view></view>
        </view>
      </view>
      <button class="add-set" :disabled="sets.length >= 30" @tap="addSet">＋ 添加空白组</button>
    </view>

    <view v-if="entryType === 'WORKOUT'" class="form-card"><view class="card-heading"><text>训练感受</text><text>选填</text></view>
      <textarea v-model="content" class="field-textarea" maxlength="2000" placeholder="补充当天的状态、背景或感受" placeholder-class="placeholder" /></view>

    <text class="save-hint">记录会保存到当前账号，之后可以在手机和 Web 端查看。</text>
    <button class="save-button" :disabled="saving || saved" @tap="save">{{ saving ? '正在保存…' : saved ? '已保存' : '保存这条记录' }}</button>
  </view>
</template>

<script>
import { API_BASE_URL, TOKEN_KEY } from '../../common/api.js'

function today() {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate())
}
let nextSetKey = 0
function emptySet() { return { _key: ++nextSetKey, exerciseName: '', repetitions: '', weightKg: '', durationSeconds: '' } }

export default {
  data() {
    return { entryType: 'NOTE', occurredOn: today(), title: '', content: '', sets: [emptySet()], saving: false, saved: false }
  },
  onLoad() { if (!uni.getStorageSync(TOKEN_KEY)) uni.reLaunch({ url: '/pages/login/index' }) },
  methods: {
    goBack() { if (getCurrentPages().length > 1) uni.navigateBack(); else uni.reLaunch({ url: '/pages/index/index' }) },
    onDateChange(event) { this.occurredOn = event.detail.value },
    addSet() { if (this.sets.length < 30) this.sets.push(emptySet()) },
    copySet(index) {
      if (this.sets.length >= 30) return
      this.sets.splice(index + 1, 0, { ...this.sets[index], _key: ++nextSetKey })
    },
    removeSet(index) { if (this.sets.length > 1) this.sets.splice(index, 1) },
    expireSession() { uni.removeStorageSync(TOKEN_KEY); uni.reLaunch({ url: '/pages/login/index' }) },
    showError(message) { uni.showToast({ title: message, icon: 'none', duration: 2500 }) },
    validatedSets() {
      if (this.entryType !== 'WORKOUT') return []
      if (!this.sets.length || this.sets.length > 30) { this.showError('一次训练需要 1–30 组'); return null }
      const result = []
      for (let index = 0; index < this.sets.length; index++) {
        const set = this.sets[index]
        const name = String(set.exerciseName).trim()
        const reps = String(set.repetitions).trim()
        const weight = String(set.weightKg).trim()
        const duration = String(set.durationSeconds).trim()
        if (!name || (!reps && !duration)) { this.showError('第 ' + (index + 1) + ' 组需填项目及次数或时长'); return null }
        if ((reps && (!/^\d+$/.test(reps) || Number(reps) < 1 || Number(reps) > 10000)) ||
            (duration && (!/^\d+$/.test(duration) || Number(duration) < 1 || Number(duration) > 86400)) ||
            (weight && !/^(?:0|[1-9]\d{0,4})(?:\.\d{1,2})?$/.test(weight))) {
          this.showError('第 ' + (index + 1) + ' 组数字格式不正确')
          return null
        }
        result.push({ exerciseName: name, repetitions: reps ? Number(reps) : null,
          weightKg: weight || null, durationSeconds: duration ? Number(duration) : null })
      }
      return result
    },
    save() {
      if (this.saving || this.saved) return
      const token = uni.getStorageSync(TOKEN_KEY)
      if (!token) { this.expireSession(); return }
      const title = this.title.trim()
      if (!this.occurredOn || !title) { this.showError('请填写日期和标题'); return }
      const sets = this.validatedSets()
      if (sets === null) return
      this.saving = true
      uni.request({
        url: API_BASE_URL + '/api/logs',
        method: 'POST',
        header: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
        data: { entryType: this.entryType, occurredOn: this.occurredOn, title,
          content: this.content.trim() || null, sets },
        success: (res) => {
          if (res.statusCode === 201) {
            this.saved = true
            uni.showToast({ title: '已记下', icon: 'success' })
            setTimeout(() => this.goBack(), 650)
          } else if (res.statusCode === 401) this.expireSession()
          else this.showError((res.data && res.data.message) || '保存失败')
        },
        fail: () => this.showError('无法连接服务端'),
        complete: () => { this.saving = false }
      })
    }
  }
}
</script>

<style>
page { background: #f5f7f5; }
button::after { border: 0; }
.journal-record-page { padding: 0 32rpx 90rpx; color: #1d414b; }
.status-bar { height: var(--status-bar-height); }
.top-bar { display: flex; align-items: center; justify-content: space-between; margin: 24rpx 0 30rpx; }
.back, .spacer { width: 110rpx; }
.back { color: #4e7b80; font-size: 26rpx; }
.top-title { color: #22444d; font-size: 30rpx; font-weight: 650; }
.intro { display: flex; flex-direction: column; margin: 14rpx 2rpx 30rpx; }
.intro-kicker { color: #66a49a; font-size: 18rpx; font-weight: 600; letter-spacing: 4rpx; }
.intro-title { margin-top: 13rpx; color: #1b414b; font-size: 42rpx; line-height: 1.35; font-weight: 650; }
.intro-note { margin-top: 9rpx; color: #90a5a6; font-size: 22rpx; line-height: 1.6; }
.type-switch { display: flex; padding: 6rpx; background: #e8f0ee; border-radius: 22rpx; }
.type-item { flex: 1; height: 72rpx; line-height: 72rpx; text-align: center; border-radius: 18rpx; color: #6b878a; font-size: 25rpx; }
.type-item.active { background: #123f4a; color: #fff; font-weight: 600; }
.form-card { margin-top: 20rpx; padding: 28rpx 24rpx 30rpx; border: 1rpx solid #e5eeea; border-radius: 28rpx; background: #fff; box-shadow: 0 8rpx 26rpx rgba(25,75,77,.035); }
.card-heading { display: flex; justify-content: space-between; align-items: center; }
.card-heading text:first-child { color: #24505a; font-size: 27rpx; font-weight: 650; }
.card-heading text:last-child { color: #9aafad; font-size: 19rpx; }
.card-note { display: block; margin-top: 11rpx; color: #90a5a1; font-size: 20rpx; line-height: 1.55; }
.field { margin-top: 25rpx; }
.field-label { color: #3d5a62; font-size: 21rpx; font-weight: 600; }
.picker-value, .field-input { box-sizing: border-box; width: 100%; height: 76rpx; margin-top: 10rpx; padding: 0 20rpx; background: #f6f9f7; border-radius: 16rpx; color: #21444d; font-size: 25rpx; }
.picker-value { line-height: 76rpx; }
.field-textarea { box-sizing: border-box; width: 100%; min-height: 190rpx; margin-top: 10rpx; padding: 18rpx 20rpx; background: #f6f9f7; border-radius: 16rpx; color: #21444d; font-size: 24rpx; line-height: 1.55; }
.placeholder { color: #aab9ba; }
.set-card { margin-top: 22rpx; padding: 20rpx; border: 1rpx solid #e4eee8; border-radius: 21rpx; background: #f9fcfa; }
.set-head { display: flex; justify-content: space-between; align-items: center; color: #2c605c; font-size: 23rpx; font-weight: 600; }
.set-head > view { display: flex; gap: 23rpx; color: #4f8b80; font-size: 19rpx; font-weight: 400; }
.set-head .remove { color: #b57668; }
.set-card .field { margin-top: 16rpx; }
.metric-row { display: flex; gap: 10rpx; margin-top: 18rpx; }
.metric-field { flex: 1; min-width: 0; }
.metric-input { display: flex; align-items: center; height: 70rpx; margin-top: 9rpx; padding: 0 9rpx; background: #fff; border: 1rpx solid #e0ebe5; border-radius: 13rpx; }
.metric-input input { flex: 1; min-width: 0; color: #21444d; font-size: 22rpx; }
.metric-input text { color: #8da5a1; font-size: 18rpx; }
.add-set { margin-top: 22rpx; height: 76rpx; line-height: 76rpx; border: 1rpx dashed #a9cfc2; border-radius: 17rpx; background: #f2faf5; color: #3f8476; font-size: 23rpx; }
.save-hint { display: block; margin: 27rpx 6rpx 0; color: #9aadaa; font-size: 19rpx; line-height: 1.6; text-align: center; }
.save-button { margin-top: 23rpx; height: 92rpx; line-height: 92rpx; border-radius: 22rpx; background: linear-gradient(110deg, #104b5b, #287b78); color: #fff; font-size: 28rpx; font-weight: 600; }
.save-button:disabled { opacity: .65; }

/* 汐序 v1：让日志易写，训练组次易核对 */
page { background: #f5f7f4; }
.journal-record-page { padding-bottom: calc(80rpx + env(safe-area-inset-bottom)); color: #173d46; }
.top-bar { min-height: 96rpx; margin: 0 0 20rpx; }
.back { display: flex; align-items: center; width: 124rpx; min-height: 96rpx; color: #17675f; font-size: 27rpx; }
.spacer { width: 124rpx; }
.top-title { color: #173d46; }
.intro { margin-bottom: 26rpx; }
.intro-kicker { color: #397e72; font-size: 20rpx; letter-spacing: 3rpx; }
.intro-note { color: #567077; font-size: 24rpx; }
.type-switch { background: #e7f2ed; }
.type-item { height: 88rpx; line-height: 88rpx; color: #345f60; }
.type-item.active { background: #17675f; }
.form-card { border-color: #dae6e1; box-shadow: none; }
.card-heading text:first-child { color: #173d46; font-size: 28rpx; }
.card-heading text:last-child, .card-note, .save-hint { color: #567077; font-size: 22rpx; }
.field-label { color: #34555b; font-size: 24rpx; }
.picker-value, .field-input { height: 94rpx; line-height: 94rpx; background: #f8faf8; border: 1rpx solid #dae6e1; color: #173d46; font-size: 27rpx; }
.field-textarea { min-height: 220rpx; background: #f8faf8; border: 1rpx solid #dae6e1; color: #173d46; font-size: 26rpx; }
.placeholder { color: #819a97; }
.set-card { background: #f7faf7; border-color: #dae6e1; }
.set-head { color: #173d46; font-size: 25rpx; }
.set-head > view { gap: 10rpx; color: #17675f; font-size: 22rpx; }
.set-head > view text { display: flex; align-items: center; min-height: 88rpx; padding: 0 8rpx; }
.metric-row { flex-wrap: wrap; }
.metric-field { flex-basis: 30%; }
.metric-input { height: 88rpx; border-color: #dae6e1; }
.metric-input input { font-size: 25rpx; }
.metric-input text { color: #567077; font-size: 21rpx; }
.add-set { height: 96rpx; line-height: 96rpx; border-color: #9bc6af; color: #17675f; font-size: 26rpx; }
.save-button { height: 100rpx; line-height: 100rpx; border-radius: 18rpx; background: #17675f; font-size: 29rpx; }
</style>
