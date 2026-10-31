<template>
  <view class="record-page">
    <view class="status-bar"></view>
    <view class="top-bar"><text class="back" @tap="goBack">‹ 返回</text><text class="top-title">记录身体</text><text class="spacer"></text></view>
    <view class="intro"><text class="intro-kicker">A SMALL NOTE FOR YOURSELF</text><text class="intro-title">按实际测量，慢慢记下。</text><text class="intro-note">体重与睡眠分别保存；没有数据的项目可以留空。</text></view>
    <view class="switch"><view :class="['switch-item', { active: tab === 'body' }]" @tap="tab = 'body'">体重与体成分</view><view :class="['switch-item', { active: tab === 'sleep' }]" @tap="tab = 'sleep'">睡眠</view></view>
    <view v-if="tab === 'body'" class="form-card">
      <view class="card-heading"><text>一次测量</text><text>至少填一项测量值</text></view>
      <view class="field"><text class="field-label">测量日期</text><picker mode="date" :value="bodyForm.observedOn" @change="bodyForm.observedOn = $event.detail.value"><view class="picker-value">{{ bodyForm.observedOn }}</view></picker></view>
      <view class="field"><text class="field-label">体重（kg）</text><input v-model="bodyForm.weightKg" class="field-input" type="digit" placeholder="选填，如 67.35" placeholder-class="placeholder" /></view>
      <view class="field"><text class="field-label">身高（cm）</text><input v-model="bodyForm.heightCm" class="field-input" type="digit" placeholder="选填，如 175" placeholder-class="placeholder" /></view>
      <view class="field"><text class="field-label">体脂率（%）</text><input v-model="bodyForm.bodyFatPct" class="field-input" type="digit" placeholder="选填，填写测量值" placeholder-class="placeholder" /></view>
      <view class="field"><text class="field-label">肌肉量（kg）</text><input v-model="bodyForm.muscleMassKg" class="field-input" type="digit" placeholder="选填，填写测量值" placeholder-class="placeholder" /></view>
      <text class="form-note">至少填写体重、体脂率或肌肉量中的一项；身高可以单独留空。</text>
    </view>
    <view v-else class="form-card">
      <view class="card-heading"><text>一段睡眠</text><text>按醒来日期保存</text></view>
      <view class="field"><text class="field-label">醒来日期</text><picker mode="date" :value="sleepForm.sleepOn" @change="sleepForm.sleepOn = $event.detail.value"><view class="picker-value">{{ sleepForm.sleepOn }}</view></picker></view>
      <view class="field"><text class="field-label">睡眠类型</text><view class="kind-row"><view :class="['kind', { active: sleepForm.sessionKind === 'NIGHT' }]" @tap="sleepForm.sessionKind = 'NIGHT'">夜间睡眠</view><view :class="['kind', { active: sleepForm.sessionKind === 'NAP' }]" @tap="sleepForm.sessionKind = 'NAP'">午睡</view></view></view>
      <view class="field"><text class="field-label">睡眠时长</text><view class="duration-row"><view><input v-model="sleepForm.hours" type="number" placeholder="0" placeholder-class="placeholder" /><text>小时</text></view><view><input v-model="sleepForm.minutes" type="number" placeholder="0" placeholder-class="placeholder" /><text>分钟</text></view></view></view>
      <view class="optional-toggle" @tap="showStages = !showStages">{{ showStages ? '收起' : '补充' }}睡眠阶段（选填） <text>{{ showStages ? '⌃' : '⌄' }}</text></view>
      <view v-if="showStages" class="stage-fields">
        <view class="field"><text class="field-label">深睡（分钟）</text><input v-model="sleepForm.deepMinutes" class="field-input" type="number" placeholder="未知可留空" placeholder-class="placeholder" /></view>
        <view class="field"><text class="field-label">浅睡（分钟）</text><input v-model="sleepForm.lightMinutes" class="field-input" type="number" placeholder="未知可留空" placeholder-class="placeholder" /></view>
        <view class="field"><text class="field-label">快速眼动（分钟）</text><input v-model="sleepForm.remMinutes" class="field-input" type="number" placeholder="未知可留空" placeholder-class="placeholder" /></view>
        <view class="field"><text class="field-label">清醒（分钟）</text><input v-model="sleepForm.awakeMinutes" class="field-input" type="number" placeholder="未知可留空" placeholder-class="placeholder" /></view>
      </view>
    </view>
    <text class="save-hint">仅保存你明确填写的数值，可在手机和 Web 端查看。</text>
    <button class="save-button" :disabled="saving || saved" @tap="save">{{ saving ? '正在保存…' : saved ? '已保存' : tab === 'body' ? '保存测量' : '保存睡眠' }}</button>
  </view>
</template>

<script>
import { TOKEN_KEY } from '../../common/api.js'
import { createWellness } from '../../common/wellness.js'

function today() { const d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0') }
function decimal(text, maxInteger, allowZero) {
  const value = String(text).trim()
  if (!value) return null
  const pattern = new RegExp('^(?:0|[1-9]\\d{0,' + (maxInteger - 1) + '})(?:\\.\\d{1,2})?$')
  if (!pattern.test(value) || (!allowZero && Number(value) <= 0)) return undefined
  return value
}
function whole(text) {
  const value = String(text).trim()
  if (!value) return null
  return /^\d+$/.test(value) && Number(value) <= 65535 ? Number(value) : undefined
}

export default {
  data() { return { tab: 'body', bodyForm: { observedOn: today(), weightKg: '', heightCm: '', bodyFatPct: '', muscleMassKg: '' },
    sleepForm: { sleepOn: today(), sessionKind: 'NIGHT', hours: '', minutes: '', deepMinutes: '', lightMinutes: '', remMinutes: '', awakeMinutes: '' },
    showStages: false, saving: false, saved: false } },
  onLoad(options) { if (options && options.tab === 'sleep') this.tab = 'sleep'; if (!uni.getStorageSync(TOKEN_KEY)) uni.reLaunch({ url: '/pages/login/index' }) },
  methods: {
    goBack() { if (getCurrentPages().length > 1) uni.navigateBack(); else uni.reLaunch({ url: '/pages/health/index' }) },
    showError(message) { uni.showToast({ title: message, icon: 'none', duration: 2800 }) },
    async save() {
      if (this.saving || this.saved) return
      let payload, kind
      if (this.tab === 'body') {
        kind = 'body'
        const form = this.bodyForm
        const weightKg = decimal(form.weightKg, 4, false)
        const heightCm = decimal(form.heightCm, 3, false)
        const bodyFatPct = decimal(form.bodyFatPct, 3, true)
        const muscleMassKg = decimal(form.muscleMassKg, 4, true)
        if (!form.observedOn || [weightKg, heightCm, bodyFatPct, muscleMassKg].includes(undefined) || (bodyFatPct !== null && Number(bodyFatPct) > 100)) {
          this.showError('请检查日期和测量数值'); return
        }
        if (weightKg === null && bodyFatPct === null && muscleMassKg === null) { this.showError('至少填写一项测量值'); return }
        payload = { observedOn: form.observedOn, weightKg, heightCm, bodyFatPct, muscleMassKg }
      } else {
        kind = 'sleep'
        const form = this.sleepForm
        const hours = whole(form.hours) || 0
        const minutes = whole(form.minutes) || 0
        const durationMinutes = hours * 60 + minutes
        const deepMinutes = whole(form.deepMinutes), lightMinutes = whole(form.lightMinutes)
        const remMinutes = whole(form.remMinutes), awakeMinutes = whole(form.awakeMinutes)
        if (!form.sleepOn || [whole(form.hours), whole(form.minutes), deepMinutes, lightMinutes, remMinutes, awakeMinutes].includes(undefined) ||
            minutes > 59 || durationMinutes < 1 || durationMinutes > 65535) { this.showError('请填写有效的睡眠时长'); return }
        if ((deepMinutes || 0) + (lightMinutes || 0) + (remMinutes || 0) > durationMinutes + 5) { this.showError('睡眠阶段合计超过总时长'); return }
        payload = { sleepOn: form.sleepOn, sessionKind: form.sessionKind, durationMinutes, deepMinutes, lightMinutes, remMinutes, awakeMinutes }
      }
      this.saving = true
      try {
        await createWellness(kind, payload)
        this.saved = true
        uni.showToast({ title: '已保存', icon: 'success' })
        setTimeout(() => this.goBack(), 650)
      } catch (error) {
        if (error.status === 401) { uni.removeStorageSync(TOKEN_KEY); uni.reLaunch({ url: '/pages/login/index' }) }
        else this.showError(error.message || '保存失败')
      } finally { this.saving = false }
    }
  }
}
</script>

<style>
page { background:#f5f7f4; } button::after { border:0; }
.record-page { padding:0 32rpx calc(80rpx + env(safe-area-inset-bottom)); color:#173d46; }.status-bar { height:var(--status-bar-height); }
.top-bar { display:flex; align-items:center; justify-content:space-between; min-height:96rpx; margin-bottom:20rpx; }.back { display:flex; align-items:center; width:124rpx; min-height:96rpx; color:#17675f; font-size:27rpx; }.top-title { color:#173d46; font-size:30rpx; font-weight:650; }.spacer { width:124rpx; }
.intro { display:flex; flex-direction:column; margin:12rpx 2rpx 28rpx; }.intro-kicker { color:#397e72; font-size:20rpx; font-weight:600; letter-spacing:3rpx; }.intro-title { margin-top:12rpx; color:#173d46; font-size:41rpx; font-weight:650; }.intro-note { margin-top:10rpx; color:#567077; font-size:23rpx; line-height:1.6; }
.switch { display:flex; padding:6rpx; border-radius:21rpx; background:#e7f2ed; }.switch-item { flex:1; min-height:88rpx; display:flex; align-items:center; justify-content:center; border-radius:17rpx; color:#345f60; font-size:25rpx; }.switch-item.active { background:#17675f; color:#fff; font-weight:600; }
.form-card { margin-top:20rpx; padding:28rpx 25rpx 32rpx; border:1rpx solid #dae6e1; border-radius:27rpx; background:#fff; }.card-heading { display:flex; justify-content:space-between; align-items:center; }.card-heading text:first-child { color:#173d46; font-size:28rpx; font-weight:650; }.card-heading text:last-child { color:#567077; font-size:21rpx; }.field { margin-top:27rpx; }.field-label { color:#34555b; font-size:24rpx; font-weight:600; }.field-input,.picker-value { box-sizing:border-box; width:100%; height:94rpx; margin-top:11rpx; padding:0 20rpx; border:1rpx solid #dae6e1; border-radius:16rpx; background:#f8faf8; color:#173d46; font-size:27rpx; }.picker-value { line-height:94rpx; }.placeholder { color:#819a97; }.form-note { display:block; margin-top:24rpx; color:#567077; font-size:22rpx; line-height:1.5; }
.kind-row { display:flex; gap:13rpx; margin-top:13rpx; }.kind { flex:1; min-height:88rpx; display:flex; align-items:center; justify-content:center; border:1rpx solid #dae6e1; border-radius:15rpx; color:#345f60; font-size:25rpx; }.kind.active { border-color:#17675f; background:#e7f2ed; color:#17675f; font-weight:600; }
.duration-row { display:flex; gap:13rpx; margin-top:12rpx; }.duration-row > view { flex:1; display:flex; align-items:center; height:94rpx; padding:0 17rpx; border:1rpx solid #dae6e1; border-radius:16rpx; background:#f8faf8; }.duration-row input { flex:1; min-width:0; color:#173d46; font-size:28rpx; }.duration-row text { color:#567077; font-size:23rpx; }
.optional-toggle { display:flex; justify-content:space-between; align-items:center; min-height:90rpx; margin-top:25rpx; border-top:1rpx solid #e5eeea; color:#17675f; font-size:24rpx; }.stage-fields { padding:1rpx 0 10rpx; }.stage-fields .field:first-child { margin-top:4rpx; }
.save-hint { display:block; margin:28rpx 0 15rpx; color:#567077; font-size:21rpx; line-height:1.5; text-align:center; }.save-button { height:100rpx; line-height:100rpx; border-radius:18rpx; background:#17675f; color:#fff; font-size:29rpx; font-weight:600; }.save-button[disabled] { opacity:.65; }
</style>
