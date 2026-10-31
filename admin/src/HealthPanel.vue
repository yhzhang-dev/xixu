<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { createBody, createSleep, listBody, listSleep } from './api/wellness'

function localDate(date = new Date()) {
  return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0')
}
function daysAgo(days) { const d = new Date(); d.setDate(d.getDate() - days); return localDate(d) }
function optionalNumber(value) { return String(value).trim() === '' ? null : Number(value) }
function readableMinutes(minutes) { return Math.floor(minutes / 60) + ' 小时 ' + minutes % 60 + ' 分钟' }

const tab = ref('body')
const period = ref(30)
const body = ref([])
const sleep = ref([])
const loading = ref(false)
const error = ref('')
const saving = ref(false)
const formError = ref('')
const success = ref('')
const bodyForm = ref({ observedOn: localDate(), weightKg: '', heightCm: '', bodyFatPct: '', muscleMassKg: '' })
const sleepForm = ref({ sleepOn: localDate(), sessionKind: 'NIGHT', hours: '', minutes: '', deepMinutes: '', lightMinutes: '', remMinutes: '', awakeMinutes: '' })
let requestId = 0

const selectedItems = computed(() => tab.value === 'body' ? body.value : sleep.value)
const latestWeight = computed(() => body.value.find(item => item.weightKg != null))
const latestSleep = computed(() => sleep.value[0])
const chart = computed(() => {
  const rows = [...selectedItems.value].reverse().filter(item =>
    tab.value === 'body' ? item.weightKg != null : item.durationMinutes != null)
  if (rows.length < 4) return null
  const values = rows.map(item => tab.value === 'body' ? Number(item.weightKg) : Number(item.durationMinutes) / 60)
  const min = Math.min(...values)
  const max = Math.max(...values)
  const pad = Math.max((max - min) * .16, tab.value === 'body' ? .5 : .25)
  const lo = min - pad
  const hi = max + pad
  const points = rows.map((item, index) => ({
    x: 28 + index * 604 / (rows.length - 1),
    y: 172 - (values[index] - lo) * 144 / (hi - lo),
    date: tab.value === 'body' ? item.observedOn : item.sleepOn,
    value: values[index],
  }))
  return { points, line: points.map(point => point.x + ',' + point.y).join(' '),
    first: points[0].date, last: points.at(-1).date,
    low: lo.toFixed(1), high: hi.toFixed(1) }
})

async function load() {
  const id = ++requestId
  loading.value = true
  error.value = ''
  const from = daysAgo(period.value - 1)
  const to = localDate()
  const results = await Promise.allSettled([listBody(from, to), listSleep(from, to)])
  if (id !== requestId) return
  if (results[0].status === 'fulfilled') body.value = results[0].value
  if (results[1].status === 'fulfilled') sleep.value = results[1].value
  if (results.some(result => result.status === 'rejected')) error.value = '部分健康记录暂时无法加载，请稍后重试。'
  loading.value = false
}

async function saveBody() {
  if (saving.value) return
  formError.value = ''; success.value = ''
  const form = bodyForm.value
  const values = ['weightKg', 'bodyFatPct', 'muscleMassKg', 'heightCm'].map(key => optionalNumber(form[key]))
  if (!form.observedOn || values.slice(0, 3).every(value => value === null)) {
    formError.value = '请选择日期，并至少填写体重、体脂率或肌肉量。'; return
  }
  if (values.some(value => value !== null && (!Number.isFinite(value) || value < 0)) ||
      (values[0] !== null && values[0] <= 0) || (values[3] !== null && values[3] <= 0) ||
      (values[1] !== null && values[1] > 100)) {
    formError.value = '请检查数值；体重和身高须大于 0，体脂率为 0–100%。'; return
  }
  saving.value = true
  try {
    await createBody({ observedOn: form.observedOn, weightKg: values[0], bodyFatPct: values[1],
      muscleMassKg: values[2], heightCm: values[3] })
    bodyForm.value = { observedOn: form.observedOn, weightKg: '', heightCm: '', bodyFatPct: '', muscleMassKg: '' }
    success.value = '身体测量已保存。'
    await load()
  } catch (cause) { formError.value = cause.message || '保存失败。' }
  finally { saving.value = false }
}

async function saveSleep() {
  if (saving.value) return
  formError.value = ''; success.value = ''
  const form = sleepForm.value
  const hours = optionalNumber(form.hours) ?? 0
  const minutes = optionalNumber(form.minutes) ?? 0
  const duration = hours * 60 + minutes
  const stages = ['deepMinutes', 'lightMinutes', 'remMinutes', 'awakeMinutes'].map(key => optionalNumber(form[key]))
  if (!form.sleepOn || !Number.isInteger(hours) || !Number.isInteger(minutes) || minutes < 0 || minutes > 59 || duration < 1 || duration > 65535 ||
      stages.some(value => value !== null && (!Number.isInteger(value) || value < 0 || value > 65535))) {
    formError.value = '请填写有效的睡眠时长；分钟为 0–59，阶段时长为非负整数。'; return
  }
  if (stages.slice(0, 3).reduce((sum, value) => sum + (value ?? 0), 0) > duration + 5) {
    formError.value = '深睡、浅睡和快速眼动合计不能超过总睡眠时长。'; return
  }
  saving.value = true
  try {
    await createSleep({ sleepOn: form.sleepOn, sessionKind: form.sessionKind, durationMinutes: duration,
      deepMinutes: stages[0], lightMinutes: stages[1], remMinutes: stages[2], awakeMinutes: stages[3] })
    sleepForm.value = { sleepOn: form.sleepOn, sessionKind: form.sessionKind, hours: '', minutes: '',
      deepMinutes: '', lightMinutes: '', remMinutes: '', awakeMinutes: '' }
    success.value = '睡眠记录已保存。'
    await load()
  } catch (cause) { formError.value = cause.message || '保存失败。' }
  finally { saving.value = false }
}

watch(period, load)
watch(tab, () => { formError.value = ''; success.value = '' })
onMounted(load)
</script>

<template>
  <div class="health-page">
    <div class="page-heading"><div><span class="page-kicker">XIXU / WELLNESS</span><h2>身体与训练</h2>
      <p>先从体重与睡眠开始，按发生日期回看自己的变化。</p></div></div>
    <div class="health-intro"><div><span>YOUR BODY, YOUR PACE</span><h3>认真看见身体的每一次变化。</h3>
      <p>记录真实测量与睡眠时长。原始日志仍保留在日志模块，不会自动生成测量值。</p></div>
      <div class="health-intro-mark" aria-hidden="true">汐</div></div>
    <div class="health-highlights">
      <button type="button" class="health-highlight" @click="tab = 'body'"><span>最近体重</span><strong>{{ latestWeight ? latestWeight.weightKg + ' kg' : '尚未记录' }}</strong><small>{{ latestWeight?.observedOn || '从第一次测量开始' }}</small></button>
      <button type="button" class="health-highlight" @click="tab = 'sleep'"><span>最近睡眠</span><strong>{{ latestSleep ? readableMinutes(latestSleep.durationMinutes) : '尚未记录' }}</strong><small>{{ latestSleep?.sleepOn || '从第一次睡眠记录开始' }}</small></button>
    </div>
    <div class="health-toolbar"><div class="health-tabs" role="tablist" aria-label="健康记录类型">
      <button type="button" role="tab" :aria-selected="tab === 'body'" :class="{ selected: tab === 'body' }" @click="tab = 'body'">体重与体成分</button>
      <button type="button" role="tab" :aria-selected="tab === 'sleep'" :class="{ selected: tab === 'sleep' }" @click="tab = 'sleep'">睡眠</button>
    </div><label>查看范围 <select v-model.number="period"><option :value="30">近 30 天</option><option :value="90">近 90 天</option><option :value="365">近一年</option></select></label></div>
    <p v-if="error" class="health-error" role="alert">{{ error }} <button type="button" @click="load">重试</button></p>
    <div class="health-layout">
      <section class="health-card health-compose"><span class="section-kicker">NEW RECORD</span><h3>{{ tab === 'body' ? '记录一次测量' : '记下这段睡眠' }}</h3>
        <p>未测量的项目可以留空；请按实际记录填写。</p>
        <form v-if="tab === 'body'" @submit.prevent="saveBody">
          <label>测量日期 <input v-model="bodyForm.observedOn" type="date" required /></label>
          <div class="health-form-grid"><label>体重（kg）<input v-model="bodyForm.weightKg" type="number" inputmode="decimal" step="0.01" min="0.01" placeholder="选填" /></label>
            <label>身高（cm）<input v-model="bodyForm.heightCm" type="number" inputmode="decimal" step="0.01" min="0.01" placeholder="选填" /></label>
            <label>体脂率（%）<input v-model="bodyForm.bodyFatPct" type="number" inputmode="decimal" step="0.01" min="0" max="100" placeholder="选填" /></label>
            <label>肌肉量（kg）<input v-model="bodyForm.muscleMassKg" type="number" inputmode="decimal" step="0.01" min="0" placeholder="选填" /></label></div>
          <p class="health-hint">至少填写体重、体脂率或肌肉量中的一项。</p>
          <p v-if="formError" class="health-error" role="alert">{{ formError }}</p><p v-if="success" class="health-success" role="status">{{ success }}</p>
          <button type="submit" class="health-save" :disabled="saving">{{ saving ? '保存中…' : '保存测量' }}</button>
        </form>
        <form v-else @submit.prevent="saveSleep">
          <label>醒来日期 <input v-model="sleepForm.sleepOn" type="date" required /></label>
          <label>睡眠类型 <select v-model="sleepForm.sessionKind"><option value="NIGHT">夜间睡眠</option><option value="NAP">午睡</option></select></label>
          <div class="health-form-grid"><label>睡眠小时 <input v-model="sleepForm.hours" type="number" inputmode="numeric" min="0" step="1" placeholder="如 7" /></label>
            <label>剩余分钟 <input v-model="sleepForm.minutes" type="number" inputmode="numeric" min="0" max="59" step="1" placeholder="如 30" /></label></div>
          <details class="health-optional"><summary>补充睡眠阶段（选填）</summary><div class="health-form-grid">
            <label>深睡（分钟）<input v-model="sleepForm.deepMinutes" type="number" min="0" step="1" placeholder="未知可留空" /></label>
            <label>浅睡（分钟）<input v-model="sleepForm.lightMinutes" type="number" min="0" step="1" placeholder="未知可留空" /></label>
            <label>快速眼动（分钟）<input v-model="sleepForm.remMinutes" type="number" min="0" step="1" placeholder="未知可留空" /></label>
            <label>清醒（分钟）<input v-model="sleepForm.awakeMinutes" type="number" min="0" step="1" placeholder="未知可留空" /></label>
          </div></details>
          <p v-if="formError" class="health-error" role="alert">{{ formError }}</p><p v-if="success" class="health-success" role="status">{{ success }}</p>
          <button type="submit" class="health-save" :disabled="saving">{{ saving ? '保存中…' : '保存睡眠' }}</button>
        </form>
      </section>
      <section class="health-card health-trend"><div class="health-section-head"><div><span class="section-kicker">TREND</span><h3>{{ tab === 'body' ? '体重趋势' : '睡眠时长趋势' }}</h3></div><span>{{ period }} 天</span></div>
        <p v-if="loading" class="health-empty">正在加载记录…</p>
        <template v-else-if="chart"><svg viewBox="0 0 660 210" role="img" :aria-label="tab === 'body' ? '体重随日期变化的折线图，下方有明细' : '睡眠时长随日期变化的折线图，下方有明细'">
            <line x1="28" y1="28" x2="632" y2="28"/><line x1="28" y1="100" x2="632" y2="100"/><line x1="28" y1="172" x2="632" y2="172"/>
            <polyline :points="chart.line" fill="none" stroke="#17675f" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            <circle v-for="(point, index) in chart.points" :key="index" :cx="point.x" :cy="point.y" r="4" fill="#17675f"><title>{{ point.date }} · {{ point.value.toFixed(2) }} {{ tab === 'body' ? 'kg' : '小时' }}</title></circle>
          </svg><div class="chart-axis"><span>{{ chart.first }}</span><span>{{ chart.last }}</span></div></template>
        <div v-else class="health-empty"><strong>{{ selectedItems.length ? '再记录几次，就能看到趋势' : '这段时间还没有记录' }}</strong><p>至少 4 个有效数据点后显示折线；下方始终保留日期和数值。</p></div>
        <div class="health-section-head recent-head"><div><span class="section-kicker">HISTORY</span><h3>最近记录</h3></div><span>{{ selectedItems.length }} 条</span></div>
        <div v-if="selectedItems.length" class="health-history">
          <div v-for="item in selectedItems.slice(0, 12)" :key="item.id" class="health-history-row">
            <time>{{ tab === 'body' ? item.observedOn : item.sleepOn }}</time>
            <div v-if="tab === 'body'"><strong>{{ item.weightKg != null ? item.weightKg + ' kg' : '体重未填' }}</strong><small>{{ item.bodyFatPct != null ? '体脂 ' + item.bodyFatPct + '%' : '' }}{{ item.muscleMassKg != null ? ' · 肌肉量 ' + item.muscleMassKg + ' kg' : '' }}</small></div>
            <div v-else><strong>{{ readableMinutes(item.durationMinutes) }}</strong><small>{{ item.sessionKind === 'NAP' ? '午睡' : '夜间睡眠' }}{{ item.deepMinutes != null ? ' · 深睡 ' + item.deepMinutes + ' 分钟' : '' }}</small></div>
          </div>
        </div>
        <p v-else-if="!loading" class="health-list-empty">暂无记录。左侧表单可以写下第一条。</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.health-intro { display:flex; justify-content:space-between; align-items:center; padding:30px 34px; border-radius:20px; background:#103e48; color:#fff; }
.health-intro span { color:#a8d6cc; font-size:11px; letter-spacing:2px; }
.health-intro h3 { margin:9px 0; font-size:27px; }
.health-intro p { margin:0; color:#d2e5e0; font-size:14px; line-height:1.6; }
.health-intro-mark { color:rgba(235,250,240,.18); font-size:90px; }
.health-highlights { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; margin:16px 0 28px; }
.health-highlight { min-height:122px; padding:20px 24px; border:1px solid #dae6e1; border-radius:16px; background:#fff; text-align:left; }
.health-highlight span,.health-highlight strong,.health-highlight small { display:block; }
.health-highlight span { color:#567077; font-size:13px; }.health-highlight strong { margin:9px 0 5px; color:#173d46; font-size:26px; }.health-highlight small { color:#567077; font-size:12px; }
.health-highlight:hover { border-color:#8fbbae; }
.health-toolbar { display:flex; justify-content:space-between; align-items:center; gap:16px; margin-bottom:16px; }.health-tabs { display:flex; gap:8px; }
.health-tabs button { min-height:44px; padding:0 16px; border:1px solid #dae6e1; border-radius:10px; background:#fff; color:#34585d; font-size:14px; }.health-tabs button.selected { background:#17675f; border-color:#17675f; color:#fff; }
.health-toolbar label { display:flex; align-items:center; gap:9px; color:#567077; font-size:13px; }.health-toolbar select { height:44px; padding:0 10px; border:1px solid #dae6e1; border-radius:10px; background:#fff; color:#173d46; }
.health-layout { display:grid; grid-template-columns:minmax(300px,.86fr) minmax(0,1.14fr); gap:16px; align-items:start; }
.health-card { padding:27px 29px; border:1px solid #dae6e1; border-radius:17px; background:#fff; }.health-card h3 { margin:7px 0; color:#173d46; font-size:21px; }.health-card > p { margin:7px 0 18px; color:#567077; font-size:13px; line-height:1.6; }
.health-compose form { display:flex; flex-direction:column; gap:15px; }.health-compose label { display:flex; flex-direction:column; gap:8px; color:#34585d; font-size:13px; font-weight:600; }
.health-compose input,.health-compose select { width:100%; height:46px; padding:0 13px; border:1px solid #dae6e1; border-radius:10px; background:#fff; color:#173d46; font-size:15px; }.health-compose input:focus,.health-compose select:focus { border-color:#17675f; }
.health-form-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; }.health-hint { margin:0; color:#567077; font-size:12px; }.health-optional { padding:12px 0; border-top:1px solid #e6efea; }.health-optional summary { cursor:pointer; color:#17675f; font-size:13px; }.health-optional .health-form-grid { margin-top:16px; }
.health-save { min-height:46px; border:0; border-radius:10px; background:#17675f; color:#fff; font-size:14px; font-weight:650; }.health-save:disabled { opacity:.6; }
.health-error { color:#a75543 !important; font-size:13px !important; }.health-error button { min-height:36px; margin-left:10px; border:0; background:transparent; color:#17675f; text-decoration:underline; }.health-success { color:#17675f !important; font-size:13px !important; }
.health-section-head { display:flex; justify-content:space-between; align-items:flex-end; }.health-section-head > span { color:#567077; font-size:12px; }.health-trend svg { display:block; width:100%; margin-top:20px; }.health-trend line { stroke:#e1eae5; stroke-width:1; }.chart-axis { display:flex; justify-content:space-between; color:#567077; font-size:11px; }
.health-empty { min-height:175px; display:flex; flex-direction:column; justify-content:center; align-items:center; text-align:center; color:#567077; }.health-empty strong { color:#173d46; font-size:17px; }.health-empty p { max-width:300px; font-size:13px; line-height:1.6; }
.recent-head { margin:28px 0 10px; padding-top:20px; border-top:1px solid #e6efea; }.health-history-row { display:flex; gap:20px; padding:13px 0; border-bottom:1px solid #edf2ee; }.health-history-row time { flex:0 0 95px; color:#567077; font-size:12px; }.health-history-row strong,.health-history-row small { display:block; }.health-history-row strong { color:#173d46; font-size:14px; }.health-history-row small { margin-top:4px; color:#567077; font-size:12px; }.health-list-empty { color:#567077; font-size:13px; }
@media(max-width:980px) { .health-layout { grid-template-columns:1fr; } }
@media(max-width:560px) { .health-intro { padding:24px; }.health-intro h3 { font-size:22px; }.health-intro-mark { display:none; }.health-highlights { grid-template-columns:1fr; }.health-toolbar { align-items:stretch; flex-direction:column; }.health-tabs button { flex:1; }.health-form-grid { grid-template-columns:1fr 1fr; }.health-card { padding:22px 18px; } }
</style>
