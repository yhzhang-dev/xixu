<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { clearToken, getCurrentUser, getToken, login, logout, saveToken } from './api/auth'
import { createEntry, getSummary, listEntries, voidEntry } from './api/ledger'
import { createLog, listLogs } from './api/logs'
import { listBody, listSleep } from './api/wellness'
import HealthPanel from './HealthPanel.vue'

const state = ref('checking')
const username = ref('')
const password = ref('')
const showPassword = ref(false)
const busy = ref(false)
const error = ref('')
const user = ref(null)
const activeSection = ref('overview')

function todayInput() {
  const d = new Date()
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
}

const ledger = ref({ items: [], total: 0, page: 1, size: 20, from: '', to: '', includeVoided: false, loading: false, error: '' })
const ledgerSummary = ref({ count: 0, expense: 0, income: 0 })
const filterError = ref('')
const entryForm = ref({ entryType: 'EXPENSE', amount: '', occurredOn: todayInput(), categoryName: '', channelName: '', merchant: '', note: '' })
const entryBusy = ref(false)
const entryError = ref('')
const entrySuccess = ref('')
const voidBusyId = ref(null)
const voidTarget = ref(null)
const voidReason = ref('')
const voidError = ref('')
let ledgerRequestId = 0
const journal = ref({ items: [], total: 0, page: 1, size: 20, month: todayInput().slice(0, 7), type: '', loading: false, error: '' })
let nextWorkoutSetKey = 0
const journalForm = ref({ entryType: 'NOTE', occurredOn: todayInput(), title: '', content: '', sets: [emptyWorkoutSet()] })
const journalBusy = ref(false)
const journalError = ref('')
const journalSuccess = ref('')
let journalRequestId = 0
const overview = ref({ expense: null, ledgerCount: null, logCount: null, weight: null, weightDate: null, sleepMinutes: null, sleepDate: null, loading: false, error: '' })
let overviewRequestId = 0

function emptyWorkoutSet() { return { _key: ++nextWorkoutSetKey, exerciseName: '', repetitions: '', weightKg: '', durationSeconds: '' } }

const dateText = new Intl.DateTimeFormat('zh-CN', {
  month: 'long', day: 'numeric', weekday: 'long',
}).format(new Date())

const sections = [
  { key: 'overview', index: '00', label: '总览', detail: '从今天开始回看。', ready: true },
  { key: 'health', index: '01', label: '身体与训练', detail: '记录体重、睡眠，观察真实变化。', ready: true },
  { key: 'journal', index: '02', label: '日志与训练', detail: '记录日常片段和每一组训练。', ready: true },
  { key: 'ledger', index: '03', label: '记账', detail: '记录收入与支出，按日期和分类查找。', ready: true },
  { key: 'schedule', index: '04', label: '日程课表', detail: '安排课程、计划和提醒。', ready: false },
  { key: 'attachments', index: '05', label: '附件归档', detail: '整理截图、票据和文件。', ready: false },
]

const currentSection = computed(() => sections.find(item => item.key === activeSection.value))
const displayName = computed(() => user.value?.displayName || user.value?.username || '')

async function restoreSession() {
  const token = getToken()
  if (!token) { state.value = 'login'; return }
  try {
    user.value = await getCurrentUser(token)
    state.value = 'ready'
    loadOverview()
  } catch {
    clearToken()
    state.value = 'login'
  }
}

async function submitLogin() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '请填写用户名和密码。'
    return
  }
  if (busy.value) return
  busy.value = true
  try {
    const result = await login(username.value.trim(), password.value)
    saveToken(result.token)
    user.value = await getCurrentUser(result.token)
    password.value = ''
    state.value = 'ready'
    activeSection.value = 'overview'
    loadOverview()
  } catch (cause) {
    clearToken()
    error.value = cause.status === 401
      ? '用户名或密码错误，请检查后重试。'
      : cause.name === 'AbortError'
        ? '连接超时，请确认服务端正在运行。'
        : '暂时无法登录，请确认服务端正在运行。'
  } finally {
    busy.value = false
  }
}

async function signOut() {
  const token = getToken()
  busy.value = true
  try {
    if (token) await logout(token)
  } catch {
    // 网络中断时仍清除浏览器里的令牌。
  } finally {
    clearToken()
    user.value = null
    ledgerRequestId++
    journalRequestId++
    overviewRequestId++
    ledger.value.items = []
    journal.value.items = []
    ledgerSummary.value = { count: 0, expense: 0, income: 0 }
    voidTarget.value = null
    activeSection.value = 'overview'
    password.value = ''
    state.value = 'login'
    busy.value = false
  }
}

function formatAmount(value) {
  const n = Number(value)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

async function loadOverview() {
  const requestId = ++overviewRequestId
  const today = todayInput()
  const start = new Date()
  start.setDate(start.getDate() - 364)
  const from = start.getFullYear() + '-' + String(start.getMonth() + 1).padStart(2, '0') + '-' + String(start.getDate()).padStart(2, '0')
  overview.value.loading = true
  overview.value.error = ''
  overview.value.expense = null
  overview.value.ledgerCount = null
  overview.value.logCount = null
  overview.value.weight = null
  overview.value.weightDate = null
  overview.value.sleepMinutes = null
  overview.value.sleepDate = null
  const [money, logs, body, sleep] = await Promise.allSettled([
    getSummary({ from: today, to: today }),
    listLogs({ from: today, to: today, page: 1, size: 1 }),
    listBody(from, today),
    listSleep(from, today),
  ])
  if (requestId !== overviewRequestId) return
  if (money.status === 'fulfilled') {
    overview.value.expense = formatAmount(money.value.expense)
    overview.value.ledgerCount = money.value.count
  }
  if (logs.status === 'fulfilled') overview.value.logCount = logs.value.total
  if (body.status === 'fulfilled') {
    const latestWeight = body.value.find(item => item.weightKg != null)
    overview.value.weight = latestWeight?.weightKg ?? null
    overview.value.weightDate = latestWeight?.observedOn ?? null
  }
  if (sleep.status === 'fulfilled' && sleep.value.length) {
    overview.value.sleepMinutes = sleep.value[0].durationMinutes
    overview.value.sleepDate = sleep.value[0].sleepOn
  }
  if ([money, logs, body, sleep].some(result => result.status === 'rejected')) overview.value.error = '部分数据暂未同步，可稍后重试。'
  overview.value.loading = false
}

async function loadLedger(page = ledger.value.page) {
  filterError.value = ''
  if (ledger.value.from && ledger.value.to && ledger.value.from > ledger.value.to) {
    filterError.value = '开始日期不能晚于结束日期。'
    return
  }
  const requestId = ++ledgerRequestId
  ledger.value.loading = true
  ledger.value.error = ''
  try {
    const range = {
      from: ledger.value.from || undefined,
      to: ledger.value.to || undefined,
    }
    const [data, summary] = await Promise.all([
      listEntries({ ...range, page, size: ledger.value.size, includeVoided: ledger.value.includeVoided }),
      getSummary(range),
    ])
    if (requestId !== ledgerRequestId) return
    ledger.value.items = data.items
    ledger.value.total = data.total
    ledger.value.page = data.page
    ledgerSummary.value = summary
  } catch (cause) {
    if (requestId !== ledgerRequestId) return
    ledger.value.error = cause.message || '账目加载失败'
    if (cause.status === 401) { clearToken(); user.value = null; state.value = 'login' }
  } finally {
    if (requestId === ledgerRequestId) ledger.value.loading = false
  }
}

async function submitEntry() {
  if (entryBusy.value) return
  entryError.value = ''
  entrySuccess.value = ''
  const amountText = String(entryForm.value.amount).trim()
  if (!/^(?:0|[1-9]\d{0,9})(?:\.\d{1,2})?$/.test(amountText) || Number(amountText) <= 0) {
    entryError.value = '金额需大于 0，最多 10 位整数和 2 位小数。'
    return
  }
  if (!entryForm.value.categoryName.trim()) { entryError.value = '请填写分类。'; return }
  if (!entryForm.value.occurredOn) { entryError.value = '请选择发生日期。'; return }
  entryBusy.value = true
  try {
    await createEntry({
      entryType: entryForm.value.entryType,
      amount: amountText,
      occurredOn: entryForm.value.occurredOn,
      categoryName: entryForm.value.categoryName.trim(),
      channelName: entryForm.value.channelName.trim() || null,
      merchant: entryForm.value.merchant.trim() || null,
      note: entryForm.value.note.trim() || null,
    })
    entryForm.value.amount = ''
    entryForm.value.categoryName = ''
    entryForm.value.channelName = ''
    entryForm.value.merchant = ''
    entryForm.value.note = ''
    entrySuccess.value = '已保存这笔账。'
    await loadLedger(1)
  } catch (cause) {
    entryError.value = cause.message || '保存失败，请稍后再试'
    if (cause.status === 401) { clearToken(); user.value = null; state.value = 'login' }
  } finally {
    entryBusy.value = false
  }
}

function openVoidDialog(item) {
  if (voidBusyId.value !== null) return
  voidTarget.value = item
  voidReason.value = ''
  voidError.value = ''
}

function closeVoidDialog() {
  if (voidBusyId.value !== null) return
  voidTarget.value = null
}

async function confirmVoid() {
  if (!voidTarget.value || voidBusyId.value !== null) return
  const reason = voidReason.value.trim()
  if (!reason || reason.length > 200) { voidError.value = '请填写 1–200 字的作废原因。'; return }
  voidError.value = ''
  voidBusyId.value = voidTarget.value.id
  try {
    await voidEntry(voidTarget.value.id, reason)
    voidTarget.value = null
    const lastItemOnPage = !ledger.value.includeVoided && ledger.value.items.length === 1 && ledger.value.page > 1
    await loadLedger(lastItemOnPage ? ledger.value.page - 1 : ledger.value.page)
  } catch (cause) {
    voidError.value = cause.message || '作废失败，请稍后重试'
    if (cause.status === 401) { clearToken(); user.value = null; state.value = 'login' }
  } finally {
    voidBusyId.value = null
  }
}

function addWorkoutSet() {
  if (journalForm.value.sets.length < 30) journalForm.value.sets.push(emptyWorkoutSet())
}

function copyWorkoutSet(index) {
  if (journalForm.value.sets.length >= 30) return
  journalForm.value.sets.splice(index + 1, 0, { ...journalForm.value.sets[index], _key: ++nextWorkoutSetKey })
}

function removeWorkoutSet(index) {
  if (journalForm.value.sets.length > 1) journalForm.value.sets.splice(index, 1)
}

async function loadJournal(page = journal.value.page) {
  const requestId = ++journalRequestId
  const month = journal.value.month
  if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(month)) {
    journal.value.error = '请选择查询月份。'
    journal.value.loading = false
    return
  }
  const [year, monthNumber] = month.split('-').map(Number)
  const lastDay = String(new Date(year, monthNumber, 0).getDate()).padStart(2, '0')
  journal.value.loading = true
  journal.value.error = ''
  if (page === 1) { journal.value.items = []; journal.value.total = 0; journal.value.page = 1 }
  try {
    const result = await listLogs({ from: month + '-01', to: month + '-' + lastDay,
      page, size: journal.value.size, type: journal.value.type || undefined })
    if (requestId !== journalRequestId) return
    journal.value.items = result.items
    journal.value.total = result.total
    journal.value.page = result.page
  } catch (cause) {
    if (requestId !== journalRequestId) return
    journal.value.error = cause.message || '日志加载失败'
    if (cause.status === 401) { clearToken(); user.value = null; state.value = 'login' }
  } finally {
    if (requestId === journalRequestId) journal.value.loading = false
  }
}

function shiftJournalMonth(delta) {
  const current = /^\d{4}-(0[1-9]|1[0-2])$/.test(journal.value.month)
    ? journal.value.month : todayInput().slice(0, 7)
  const [year, month] = current.split('-').map(Number)
  const date = new Date(year, month - 1 + delta, 1)
  journal.value.month = date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0')
  loadJournal(1)
}

async function submitJournal() {
  if (journalBusy.value) return
  journalError.value = ''
  journalSuccess.value = ''
  const form = journalForm.value
  if (!form.occurredOn || !form.title.trim()) { journalError.value = '请填写日期和标题。'; return }
  let sets = []
  if (form.entryType === 'WORKOUT') {
    if (!form.sets.length || form.sets.length > 30) { journalError.value = '一次训练需要 1–30 组。'; return }
    for (const [index, set] of form.sets.entries()) {
      const repetitions = String(set.repetitions).trim()
      const duration = String(set.durationSeconds).trim()
      const weight = String(set.weightKg).trim()
      if (!set.exerciseName.trim() || (!repetitions && !duration)) {
        journalError.value = `第 ${index + 1} 组需要项目名称及次数或时长。`
        return
      }
      if ((repetitions && (!/^\d+$/.test(repetitions) || Number(repetitions) < 1 || Number(repetitions) > 10000)) ||
          (duration && (!/^\d+$/.test(duration) || Number(duration) < 1 || Number(duration) > 86400)) ||
          (weight && (!/^(?:0|[1-9]\d{0,4})(?:\.\d{1,2})?$/.test(weight)))) {
        journalError.value = `第 ${index + 1} 组的次数、秒数或重量格式不正确。`
        return
      }
      sets.push({ exerciseName: set.exerciseName.trim(), repetitions: repetitions ? Number(repetitions) : null,
        weightKg: weight ? weight : null, durationSeconds: duration ? Number(duration) : null })
    }
  }
  journalBusy.value = true
  try {
    await createLog({ entryType: form.entryType, occurredOn: form.occurredOn,
      title: form.title.trim(), content: form.content.trim() || null, sets })
    form.title = ''
    form.content = ''
    form.sets = [emptyWorkoutSet()]
    journalSuccess.value = '已保存这条记录。'
    await loadJournal(1)
  } catch (cause) {
    journalError.value = cause.message || '保存失败，请稍后重试'
    if (cause.status === 401) { clearToken(); user.value = null; state.value = 'login' }
  } finally {
    journalBusy.value = false
  }
}

watch(activeSection, (section) => {
  if (section === 'overview') loadOverview()
  if (section === 'ledger') loadLedger(1)
  if (section === 'journal') loadJournal(1)
})

onMounted(restoreSession)
</script>

<template>
  <div v-if="state === 'checking'" class="boot-screen" role="status">
    <span class="brand-icon">汐</span><p>正在准备你的空间…</p>
  </div>

  <main v-else-if="state === 'login'" class="login-shell">
    <section class="login-story" aria-label="汐序介绍">
      <div class="story-brand"><span class="brand-icon">汐</span><span>汐序 <small>X I X U</small></span></div>
      <div class="story-orbit orbit-one"></div><div class="story-orbit orbit-two"></div>
      <div class="story-content">
        <div class="story-eyebrow"><span></span> YOUR DAYS, IN ONE PLACE</div>
        <h1>让每一天，<br /><em>都有迹可循。</em></h1>
        <p>账目、日志与生活里的片段，<br />安放在同一个地方。</p>
        <div class="story-line"><span>01 / 03</span><i></i><span>从记录开始</span></div>
      </div>
      <div class="story-footer">汐序 · 收好生活的每一天</div>
    </section>

    <section class="login-panel" aria-label="登录">
      <div class="panel-top"><span>个人生活管理系统</span><span class="panel-dot"></span></div>
      <div class="login-content">
        <div class="mobile-brand"><span class="brand-icon">汐</span><strong>汐序</strong></div>
        <div class="form-kicker">WELCOME BACK</div><h2>欢迎回来</h2>
        <p class="form-intro">登录后，继续整理你的生活。</p>
        <form @submit.prevent="submitLogin">
          <label for="username">用户名</label>
          <input id="username" v-model="username" type="text" maxlength="50" autocomplete="username"
            placeholder="请输入用户名" :disabled="busy" />
          <div class="password-label"><label for="password">密码</label></div>
          <div class="password-wrap">
            <input id="password" v-model="password" :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password" placeholder="请输入密码" :disabled="busy" />
            <button type="button" class="visibility" :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              @click="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</button>
          </div>
          <p v-if="error" class="form-error" role="alert">{{ error }}</p>
          <button type="submit" class="submit-button" :disabled="busy">
            <span>{{ busy ? '正在登录…' : '进入汐序' }}</span><span aria-hidden="true">↗</span>
          </button>
        </form>
        <p class="form-footnote">与手机端使用同一账号，数据由同一服务端管理。</p>
      </div>
      <div class="panel-bottom">© 2026 汐序 <span>生活自有节奏</span></div>
    </section>
  </main>

  <div v-else class="dashboard-shell">
    <aside class="sidebar">
      <div class="sidebar-brand"><span class="brand-icon">汐</span><span>汐序<small>XIXU</small></span></div>
      <div class="sidebar-label">你的空间</div>
      <nav aria-label="主导航">
        <button v-for="section in sections" :key="section.key" type="button"
          :class="['nav-item', { active: activeSection === section.key }]"
          @click="activeSection = section.key">
          <span class="nav-icon" aria-hidden="true">{{ section.index }}</span><span>{{ section.label }}</span>
          <span v-if="!section.ready" class="nav-soon">规划中</span>
          <span v-if="activeSection === section.key" class="nav-current"></span>
        </button>
        <button v-if="user?.roleCode === 'ADMIN'" type="button"
          :class="['nav-item', { active: activeSection === 'accounts' }]"
          @click="activeSection = 'accounts'">
          <span class="nav-icon" aria-hidden="true">06</span><span>账号管理</span><span class="nav-soon">规划中</span>
          <span v-if="activeSection === 'accounts'" class="nav-current"></span>
        </button>
      </nav>
      <div class="sidebar-bottom">
        <div class="sidebar-caption">一个地方，收好每一天。</div>
        <div class="sidebar-account"><span class="avatar">{{ displayName.slice(0, 1).toUpperCase() }}</span>
          <span><strong>{{ displayName }}</strong><small>{{ user?.roleCode === 'ADMIN' ? '管理员' : '普通用户' }}</small></span></div>
      </div>
    </aside>

    <main class="workspace">
      <header class="workspace-header">
        <div class="breadcrumb">个人空间 <span>/</span> {{ activeSection === 'accounts' ? '账号管理' : currentSection?.label }}</div>
        <div class="header-right"><span class="header-date">{{ dateText }}</span><span class="header-divider"></span>
          <button type="button" class="logout-link" :disabled="busy" @click="signOut">退出登录</button></div>
      </header>
      <div class="workspace-body">
        <template v-if="activeSection === 'overview'">
          <div class="page-heading"><div><span class="page-kicker">TODAY · {{ dateText }}</span><h2>你好，{{ displayName }}。</h2><p>从身体、日常与收支，慢慢看见自己的生活。</p></div>
            <div class="account-chip"><span class="chip-dot"></span>{{ user?.roleCode === 'ADMIN' ? '管理员账号' : '普通用户账号' }}</div></div>
          <section class="welcome-card" aria-label="今日概览">
            <div class="welcome-content"><span>YOUR DAY AT A GLANCE</span><h3>先照顾身体，再记录生活。</h3>
              <p>体重与睡眠从真实测量开始，日志和账目继续留在各自的位置。</p>
              <div class="welcome-actions"><button type="button" @click="activeSection = 'health'">查看身体与训练 <span aria-hidden="true">↗</span></button><button type="button" @click="activeSection = 'journal'">写一条日志 <span aria-hidden="true">↗</span></button><button type="button" @click="activeSection = 'ledger'">记一笔账 <span aria-hidden="true">↗</span></button></div>
            </div>
            <span class="welcome-mark" aria-hidden="true">汐</span>
          </section>
          <div class="section-title-row"><div><span class="section-kicker">TODAY'S RECORD</span><h3>今天的记录</h3></div><button type="button" class="text-link" @click="loadOverview">{{ overview.loading ? '正在同步…' : '刷新数据 ↻' }}</button></div>
          <div class="overview-stats" aria-live="polite">
            <button type="button" class="overview-stat" @click="activeSection = 'health'"><span>最近体重</span><strong>{{ overview.loading ? '…' : overview.weight === null ? '尚未记录' : overview.weight + ' kg' }}</strong><small>{{ overview.weightDate || '查看身体与训练' }} ↗</small></button>
            <button type="button" class="overview-stat" @click="activeSection = 'health'"><span>最近睡眠</span><strong>{{ overview.loading ? '…' : overview.sleepMinutes === null ? '尚未记录' : Math.floor(overview.sleepMinutes / 60) + ' 小时 ' + overview.sleepMinutes % 60 + ' 分钟' }}</strong><small>{{ overview.sleepDate || '查看身体与训练' }} ↗</small></button>
            <button type="button" class="overview-stat" @click="activeSection = 'ledger'"><span>今日支出</span><strong>{{ overview.loading ? '…' : overview.expense === null ? '—' : '¥' + overview.expense }}</strong><small>{{ overview.ledgerCount === null ? '数据暂未同步' : overview.ledgerCount + ' 笔有效账目' }} · 查看账本 ↗</small></button>
            <button type="button" class="overview-stat" @click="activeSection = 'journal'"><span>生活记录</span><strong>{{ overview.loading ? '…' : overview.logCount === null ? '—' : overview.logCount + ' 条' }}</strong><small>按发生日期回看 · 查看日志 ↗</small></button>
          </div>
          <p v-if="overview.error" class="overview-error" role="status">{{ overview.error }}</p>
          <div class="section-title-row"><div><span class="section-kicker">YOUR SPACE</span><h3>管理你的生活</h3></div><span>已开放 3 个模块</span></div>
          <div class="module-grid">
            <button v-for="section in sections.filter(item => item.key !== 'overview' && item.ready)" :key="section.key"
              type="button" class="module-card" @click="activeSection = section.key">
              <span :class="['module-icon', section.key]">{{ section.index }}</span>
              <span class="module-title">{{ section.label }} <span aria-hidden="true">↗</span></span>
              <span class="module-detail">{{ section.detail }}</span>
              <span class="module-status"><i></i> 已接入真实数据</span>
            </button>
          </div>
          <div class="coming-soon"><strong>继续建设中</strong><span>日程课表与附件归档会在后续版本加入；当前不会显示模拟数据。</span></div>
        </template>
        <HealthPanel v-else-if="activeSection === 'health'" />
        <template v-else-if="activeSection === 'ledger'">
          <div class="page-heading"><div><span class="page-kicker">XIXU / LEDGER</span>
            <h2>记账</h2>
            <p>记录每一笔收支，按日期回看。金额单位为人民币。</p></div></div>

          <div v-if="!ledger.loading && !ledger.error && !filterError" class="ledger-summary" aria-label="当前日期范围汇总">
            <div class="summary-card"><span>有效记录</span><strong>{{ ledgerSummary.count }}</strong><small>笔账目</small></div>
            <div class="summary-card expense"><span>支出合计</span><strong>¥{{ formatAmount(ledgerSummary.expense) }}</strong><small>仅计入有效支出</small></div>
            <div class="summary-card income"><span>收入合计</span><strong>¥{{ formatAmount(ledgerSummary.income) }}</strong><small>仅计入有效收入</small></div>
          </div>

          <section class="ledger-compose">
            <div class="ledger-section-head"><div><span class="section-kicker">NEW ENTRY</span><h3>记一笔</h3></div><span>发生日期与录入时间分开保存</span></div>
            <form class="entry-form" @submit.prevent="submitEntry">
              <div class="entry-form-grid">
                <div class="form-field"><span>收支类型</span><div class="type-toggle">
                  <button type="button" :class="{ active: entryForm.entryType === 'EXPENSE' }" :aria-pressed="entryForm.entryType === 'EXPENSE'" @click="entryForm.entryType = 'EXPENSE'">支出</button>
                  <button type="button" :class="{ active: entryForm.entryType === 'INCOME' }" :aria-pressed="entryForm.entryType === 'INCOME'" @click="entryForm.entryType = 'INCOME'">收入</button>
                </div></div>
                <label class="form-field"><span>金额 <b>*</b></span><div class="money-input"><i>¥</i><input v-model="entryForm.amount" type="text" inputmode="decimal" maxlength="13" placeholder="0.00" /></div></label>
                <label class="form-field"><span>发生日期 <b>*</b></span><input v-model="entryForm.occurredOn" type="date" required /></label>
                <label class="form-field"><span>分类 <b>*</b></span><input v-model="entryForm.categoryName" type="text" maxlength="50" placeholder="如：吃饭、工资" required /></label>
                <label class="form-field"><span>支付渠道</span><input v-model="entryForm.channelName" type="text" maxlength="30" placeholder="微信、支付宝、现金等" /></label>
                <label class="form-field"><span>商家 / 对方</span><input v-model="entryForm.merchant" type="text" maxlength="100" placeholder="选填" /></label>
                <label class="form-field form-field-wide"><span>备注</span><input v-model="entryForm.note" type="text" maxlength="500" placeholder="补充这笔账的背景，选填" /></label>
              </div>
              <div class="form-actions">
                <p class="form-feedback" aria-live="polite"><span v-if="entryError" class="form-error">{{ entryError }}</span><span v-else-if="entrySuccess" class="success-text">{{ entrySuccess }}</span></p>
                <button type="submit" class="submit" :disabled="entryBusy">{{ entryBusy ? '保存中…' : '保存这笔 →' }}</button>
                </div>
            </form>
          </section>

          <section class="ledger-list">
            <div class="ledger-toolbar">
              <div><span class="section-kicker">HISTORY</span><h3>账目列表<small>共 {{ ledger.total }} 笔</small></h3></div>
              <div class="filter">
                <label class="void-toggle"><input v-model="ledger.includeVoided" type="checkbox" @change="loadLedger(1)" />显示已作废</label>
                <label>开始日期<input v-model="ledger.from" type="date" /></label>
                <span>至</span>
                <label>结束日期<input v-model="ledger.to" type="date" /></label>
                <button type="button" :disabled="ledger.loading" @click="loadLedger(1)">查询</button>
              </div>
            </div>
            <p v-if="filterError" class="filter-error" role="alert">{{ filterError }}</p>
            <div v-if="filterError" class="ledger-empty">请调整日期后重新查询。</div>
            <div v-else-if="ledger.loading" class="ledger-empty">正在加载…</div>
            <div v-else-if="ledger.error" class="ledger-empty error-state">{{ ledger.error }} <button type="button" @click="loadLedger()">重试</button></div>
            <div v-else-if="ledger.items.length === 0" class="ledger-empty">还没有账目，先在上面记一笔。</div>
            <div v-else class="entry-table-wrap"><table class="entry-table">
              <thead><tr><th>发生日期</th><th>类型</th><th>分类 / 商家</th><th>渠道</th><th>备注</th><th class="num">金额</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="item in ledger.items" :key="item.id" :class="{ voided: item.status === 0 }">
                  <td>{{ item.occurredOn }}</td>
                  <td>{{ item.entryType === 'EXPENSE' ? '支出' : '收入' }}</td>
                  <td><strong>{{ item.categoryName }}</strong><small v-if="item.merchant">{{ item.merchant }}</small><small v-if="item.status === 0">作废：{{ item.voidReason || '未记录原因' }}</small></td>
                  <td>{{ item.channelName || '—' }}</td>
                  <td class="muted">{{ item.note || '—' }}</td>
                  <td class="num" :class="item.entryType === 'EXPENSE' ? 'expense' : 'income'">{{ (item.entryType === 'EXPENSE' ? '-' : '+') + '¥' + formatAmount(item.amount) }}</td>
                  <td>
                    <button v-if="item.status === 1" type="button" class="void-btn" :disabled="voidBusyId !== null" @click="openVoidDialog(item)">{{ voidBusyId === item.id ? '处理中…' : '作废' }}</button>
                    <span v-else class="voided-tag">已作废</span>
                  </td>
                </tr>
              </tbody>
            </table></div>
            <div v-if="ledger.total > ledger.size" class="pager">
              <button type="button" :disabled="ledger.page <= 1" @click="loadLedger(ledger.page - 1)">上一页</button>
              <span>第 {{ ledger.page }} 页 / 共 {{ Math.ceil(ledger.total / ledger.size) }} 页</span>
              <button type="button" :disabled="ledger.page >= Math.ceil(ledger.total / ledger.size)" @click="loadLedger(ledger.page + 1)">下一页</button>
            </div>
          </section>
          <div v-if="voidTarget" class="dialog-backdrop" @click.self="closeVoidDialog">
            <section class="void-dialog" role="dialog" aria-modal="true" aria-labelledby="void-title">
              <span class="section-kicker">KEEP THE TRACE</span><h3 id="void-title">作废这笔账？</h3>
              <p>「{{ voidTarget.categoryName }} · ¥{{ formatAmount(voidTarget.amount) }}」会从有效账目和汇总中移除，原记录仍会保留。</p>
              <label for="void-reason">作废原因 <b>*</b></label>
              <textarea id="void-reason" v-model="voidReason" maxlength="200" rows="3" placeholder="例如：重复录入、金额填错" :disabled="voidBusyId !== null"></textarea>
              <span v-if="voidError" class="form-error" role="alert">{{ voidError }}</span>
              <div class="dialog-actions"><button type="button" class="cancel" :disabled="voidBusyId !== null" @click="closeVoidDialog">取消</button><button type="button" class="confirm" :disabled="voidBusyId !== null" @click="confirmVoid">{{ voidBusyId !== null ? '处理中…' : '确认作废' }}</button></div>
            </section>
          </div>
        </template>
        <template v-else-if="activeSection === 'journal'">
          <div class="page-heading"><div><span class="page-kicker">XIXU / JOURNAL</span><h2>日志与训练</h2>
            <p>文字记录和训练组次按实际发生日期保存。未知的重量、次数或时长可以留空。</p></div></div>
          <section class="ledger-compose">
            <div class="ledger-section-head"><div><span class="section-kicker">NEW LOG</span><h3>写一条记录</h3></div><span>训练项目按组次单独保存</span></div>
            <form class="entry-form" @submit.prevent="submitJournal">
              <div class="entry-form-grid">
                <div class="form-field"><span>记录类型</span><div class="type-toggle">
                  <button type="button" :class="{ active: journalForm.entryType === 'NOTE' }" @click="journalForm.entryType = 'NOTE'">日常日志</button>
                  <button type="button" :class="{ active: journalForm.entryType === 'WORKOUT' }" @click="journalForm.entryType = 'WORKOUT'">健身训练</button>
                </div></div>
                <label class="form-field"><span>发生日期 <b>*</b></span><input v-model="journalForm.occurredOn" type="date" required /></label>
                <label class="form-field"><span>标题 <b>*</b></span><input v-model="journalForm.title" type="text" maxlength="100" placeholder="今天想记下什么？" required /></label>
                <label v-if="journalForm.entryType === 'NOTE'" class="form-field form-field-full"><span>正文</span><textarea v-model="journalForm.content" maxlength="2000" rows="3" placeholder="保留自己的原始描述，选填"></textarea></label>
              </div>
              <div v-if="journalForm.entryType === 'WORKOUT'" class="workout-sets">
                <div class="workout-sets-heading"><strong>训练组次 <small>{{ journalForm.sets.length }} / 30</small></strong><span>填写次数或时长；重量未知可留空</span></div>
                <div v-for="(set, index) in journalForm.sets" :key="set._key" class="workout-set-card">
                  <div class="workout-set-head"><strong>第 {{ index + 1 }} 组</strong><div><button type="button" :disabled="journalForm.sets.length >= 30" @click="copyWorkoutSet(index)">复制此组</button><button type="button" :disabled="journalForm.sets.length === 1" @click="removeWorkoutSet(index)">移除</button></div></div>
                  <div class="workout-set-fields">
                    <label>训练项目<input v-model="set.exerciseName" type="text" maxlength="80" placeholder="如：卧推、跑步" /></label>
                    <label>次数<input v-model="set.repetitions" type="text" inputmode="numeric" maxlength="5" placeholder="次" /></label>
                    <label>重量<input v-model="set.weightKg" type="text" inputmode="decimal" maxlength="8" placeholder="kg" /></label>
                    <label>时长<input v-model="set.durationSeconds" type="text" inputmode="numeric" maxlength="5" placeholder="秒" /></label>
                  </div>
                </div>
                <button type="button" class="set-add" :disabled="journalForm.sets.length >= 30" @click="addWorkoutSet">＋ 添加空白组</button>
              </div>
              <label v-if="journalForm.entryType === 'WORKOUT'" class="form-field journal-followup"><span>训练感受或补充</span><textarea v-model="journalForm.content" maxlength="2000" rows="3" placeholder="记录当天的状态或背景，选填"></textarea></label>
              <div class="form-actions"><p class="form-feedback" aria-live="polite"><span v-if="journalError" class="form-error">{{ journalError }}</span><span v-else-if="journalSuccess" class="success-text">{{ journalSuccess }}</span></p>
                <button type="submit" class="submit" :disabled="journalBusy">{{ journalBusy ? '保存中…' : '保存记录 →' }}</button></div>
            </form>
          </section>
          <section class="ledger-list">
            <div class="ledger-toolbar"><div><span class="section-kicker">HISTORY</span><h3>记录列表<small>共 {{ journal.total }} 条</small></h3></div>
              <div class="filter journal-filter"><button type="button" class="month-step" aria-label="上个月" @click="shiftJournalMonth(-1)">‹</button><label>月份<input v-model="journal.month" type="month" @change="loadJournal(1)" /></label><button type="button" class="month-step" aria-label="下个月" @click="shiftJournalMonth(1)">›</button>
                <label>类型<select v-model="journal.type" @change="loadJournal(1)"><option value="">全部</option><option value="NOTE">日常日志</option><option value="WORKOUT">健身训练</option></select></label></div></div>
            <div v-if="journal.loading" class="ledger-empty">正在加载…</div>
            <div v-else-if="journal.error" class="ledger-empty error-state">{{ journal.error }} <button type="button" @click="loadJournal()">重试</button></div>
            <div v-else-if="journal.items.length === 0" class="ledger-empty">还没有记录，先在上面写一条。</div>
            <div v-else class="log-list"><article v-for="item in journal.items" :key="item.id" class="log-card">
              <div class="log-meta"><span>{{ item.occurredOn }}</span><span v-if="item.timeLabel">{{ item.timeLabel }}</span><span class="log-type">{{ item.entryType === 'WORKOUT' ? '健身训练' : item.categoryName || '日常日志' }}</span><span v-if="item.sourceType === 'IMPORT'" class="log-source">原始日志</span></div>
              <h4 v-if="item.sourceType !== 'IMPORT'">{{ item.title }}</h4><p v-if="item.content">{{ item.content }}</p>
              <p v-if="item.remark" class="log-remark">备注：{{ item.remark }}</p>
              <div v-if="item.sets?.length" class="set-chips"><span v-for="set in item.sets" :key="set.id">{{ set.exerciseName }} · {{ set.repetitions ? set.repetitions + ' 次' : '' }}{{ set.weightKg != null ? ' · ' + set.weightKg + ' kg' : '' }}{{ set.durationSeconds ? ' · ' + set.durationSeconds + ' 秒' : '' }}</span></div>
            </article></div>
            <div v-if="journal.total > journal.size" class="pager"><button type="button" :disabled="journal.page <= 1" @click="loadJournal(journal.page - 1)">上一页</button>
              <span>第 {{ journal.page }} 页 / 共 {{ Math.ceil(journal.total / journal.size) }} 页</span>
              <button type="button" :disabled="journal.page >= Math.ceil(journal.total / journal.size)" @click="loadJournal(journal.page + 1)">下一页</button></div>
          </section>
        </template>
        <template v-else>
          <div class="page-heading"><div><span class="page-kicker">XIXU / WORKSPACE</span>
            <h2>{{ activeSection === 'accounts' ? '账号管理' : currentSection?.label }}</h2>
            <p>{{ activeSection === 'accounts' ? '管理账号状态与角色。' : currentSection?.detail }}</p></div></div>
          <div class="empty-state"><span class="empty-symbol">{{ activeSection === 'accounts' ? '06' : currentSection?.index }}</span>
            <h3>正在准备这个空间</h3><p>当前已完成登录和身份识别。这里的真实数据与操作会在对应模块接入后出现。</p>
            <button type="button" @click="activeSection = 'overview'">返回总览 <span>→</span></button></div>
        </template>
      </div>
    </main>
  </div>
</template>

<style scoped>
.ledger-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; margin-bottom: 18px; }
.summary-card { position: relative; overflow: hidden; min-height: 132px; padding: 21px 23px; border: 1px solid #dcebe5; border-radius: 16px; background: #edf6f1; }
.summary-card::after { content: ''; position: absolute; width: 120px; height: 120px; right: -46px; top: -53px; border-radius: 50%; border: 1px solid rgba(35,108,96,.13); }
.summary-card.expense { background: #fff7f2; border-color: #f3e5db; }
.summary-card.income { background: #f0f7f7; border-color: #dcebea; }
.summary-card span, .summary-card small { display: block; color: #73918f; font-size: 12px; }
.summary-card strong { display: block; margin: 12px 0 3px; color: #173f49; font-size: 27px; letter-spacing: -.4px; font-variant-numeric: tabular-nums; }
.summary-card.expense strong { color: #b26750; }
.summary-card.income strong { color: #227f73; }
.ledger-section-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; margin-bottom: 18px; }
.ledger-section-head h3 { margin: 5px 0 0; font-size: 18px; }
.ledger-section-head > span { color: #9aaead; font-size: 11px; }
.entry-form-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 17px 13px; }
.form-field { display: flex; flex-direction: column; gap: 8px; min-width: 0; color: #547175; font-size: 12px; font-weight: 600; }
.form-field b { color: #bf765c; }
.form-field-wide { grid-column: span 2; }
.form-field-full { grid-column: 1 / -1; }
.form-field input { width: 100%; min-width: 0; height: 44px; padding: 0 13px; border: 1px solid #deebe7; border-radius: 10px; outline: none; background: #fbfdfc; color: #204149; font-size: 14px; font-weight: 400; transition: border-color .18s, box-shadow .18s; }
.form-field input:focus, .filter input:focus { border-color: #57a49a; box-shadow: 0 0 0 3px rgba(69,151,137,.11); }
.form-field input::placeholder { color: #b2c2c0; }
.form-field textarea { width: 100%; min-height: 92px; padding: 12px 13px; resize: vertical; border: 1px solid #deebe7; border-radius: 10px; outline: none; background: #fbfdfc; color: #204149; font: inherit; font-size: 14px; font-weight: 400; }
.form-field textarea:focus, .workout-set-fields input:focus, .filter select:focus { border-color: #57a49a; box-shadow: 0 0 0 3px rgba(69,151,137,.11); outline: none; }
.workout-sets { padding: 18px; border: 1px solid #e3ece8; border-radius: 13px; background: #f9fcfa; }
.workout-sets-heading { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 12px; color: #284f53; font-size: 13px; }
.workout-sets-heading small { margin-left: 7px; color: #8da6a2; font-size: 11px; font-weight: 400; }
.workout-sets-heading span { color: #8da6a2; font-size: 11px; }
.workout-set-card { margin-top: 10px; padding: 14px; border: 1px solid #e2eee7; border-radius: 11px; background: #fff; }
.workout-set-head { display: flex; align-items: center; justify-content: space-between; color: #325d59; font-size: 12px; }
.workout-set-head > div { display: flex; gap: 13px; }
.workout-set-head button { padding: 2px 0; border: 0; background: transparent; color: #4f8e83; font-size: 11px; }
.workout-set-head button:last-child { color: #ad786b; }
.workout-set-head button:disabled { opacity: .4; cursor: default; }
.workout-set-fields { display: grid; grid-template-columns: minmax(150px, 2fr) repeat(3, minmax(76px, 1fr)); gap: 9px; margin-top: 12px; }
.workout-set-fields label { display: flex; flex-direction: column; gap: 5px; min-width: 0; color: #66847f; font-size: 11px; }
.workout-set-fields input { width: 100%; min-width: 0; height: 38px; padding: 0 9px; border: 1px solid #dfe9e4; border-radius: 8px; background: #fbfdfc; color: #204149; font-size: 12px; }
.journal-followup { margin-top: 5px; }
.set-add { width: 100%; margin-top: 12px; padding: 10px; border: 1px dashed #b8d9cc; border-radius: 9px; background: #f3faf6; color: #528d7c; font-size: 12px; }
.set-add:disabled { opacity: .4; cursor: default; }
.journal-filter { align-items: flex-end; }
.journal-filter .month-step { min-width: 31px; width: 31px; padding: 0; font-size: 21px; }
.filter select { width: 130px; height: 38px; padding: 0 10px; border: 1px solid #dfe8e5; border-radius: 9px; background: #f8faf9; color: #204149; }
.log-list { display: grid; gap: 10px; }
.log-card { padding: 17px 18px; border: 1px solid #e6efea; border-radius: 11px; background: #fbfdfc; }
.log-meta { display: flex; align-items: center; gap: 9px; color: #829b98; font-size: 11px; }
.log-type { padding: 3px 8px; border-radius: 15px; background: #e7f2ec; color: #458578; }
.log-source { padding: 3px 8px; border-radius: 15px; background: #f1f3f0; color: #758b87; }
.log-card h4 { margin: 10px 0 5px; color: #234950; font-size: 15px; }
.log-card p { margin: 0; color: #66827f; font-size: 12px; line-height: 1.7; white-space: pre-wrap; }
.log-card p.log-remark { margin-top: 7px; color: #879993; font-size: 11px; }
.set-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
.set-chips span { padding: 6px 9px; border: 1px solid #dfece6; border-radius: 7px; background: #f2f8f4; color: #517c73; font-size: 11px; }
.money-input { position: relative; }
.money-input i { position: absolute; left: 13px; top: 10px; color: #548780; font-size: 18px; font-style: normal; }
.money-input input { padding-left: 34px; font-size: 18px; font-variant-numeric: tabular-nums; }
.type-toggle { height: 44px; }
.type-toggle button { flex: 1; min-width: 75px; height: 44px; }
.form-actions { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-top: 4px; }
.form-feedback { margin: 0; min-height: 20px; font-size: 12px; }
.success-text { color: #328273; }
.submit { min-width: 132px; height: 44px; background: linear-gradient(110deg, #154e5a, #2d857c); box-shadow: 0 8px 18px rgba(25,92,85,.13); }
.ledger-toolbar { align-items: flex-end; }
.ledger-toolbar h3 { margin: 5px 0 0; font-size: 18px; }
.filter label { display: flex; flex-direction: column; gap: 5px; color: #78918f; font-size: 11px; }
.filter > span { align-self: flex-end; padding-bottom: 11px; color: #9db0ad; }
.filter input { width: 150px; height: 38px; outline: none; }
.filter button { align-self: flex-end; height: 38px; }
.filter-error { margin: 0 0 12px; color: #b0574c; font-size: 12px; }
.entry-table-wrap { overflow-x: auto; }
.entry-table { min-width: 760px; }
.entry-table td { height: 58px; }
.entry-table td strong, .entry-table td small { display: block; }
.entry-table td strong { color: #2a4d54; font-weight: 600; }
.entry-table td small { color: #9baead; margin-top: 2px; }
.entry-table .num { white-space: nowrap; font-variant-numeric: tabular-nums; }
.error-state button { margin-left: 9px; padding: 4px 9px; border: 1px solid #b9d8cf; border-radius: 7px; background: #f5faf7; color: #2a756b; }
.void-btn:disabled { opacity: .55; }
@media (max-width: 900px) { .entry-form-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .ledger-summary { gap: 8px; } .summary-card { min-height: 105px; padding: 14px 11px; } .summary-card strong { font-size: 17px; word-break: break-all; } .summary-card span, .summary-card small { font-size: 10px; } .entry-form-grid { grid-template-columns: 1fr; } .form-field-wide { grid-column: auto; } .filter { width: 100%; flex-wrap: wrap; } .filter label { flex: 1; min-width: 120px; } .filter input { width: 100%; } .ledger-section-head > span { display: none; } }
@media (max-width: 760px) { .workout-set-fields { grid-template-columns: repeat(3, minmax(0, 1fr)); } .workout-set-fields label:first-child { grid-column: 1 / -1; } .workout-sets-heading { flex-direction: column; } }
.ledger-compose, .ledger-list { margin-bottom: 26px; background: #fff; border: 1px solid #e7efec; border-radius: 16px; padding: 22px 24px; box-shadow: 0 6px 22px rgba(25, 75, 77, .04); }
.ledger-compose h3, .ledger-list h3 { margin: 0 0 16px; font-size: 15px; color: #204149; }
.ledger-list h3 small { color: #8ba0a3; font-weight: 400; margin-left: 8px; }
.entry-form { display: flex; flex-direction: column; gap: 12px; }
.entry-row { display: flex; gap: 10px; flex-wrap: wrap; }
.entry-row input, .filter input { flex: 1; min-width: 120px; height: 40px; padding: 0 12px; border: 1px solid #dfe8e5; border-radius: 10px; background: #f8faf9; color: #204149; }
.type-toggle { display: flex; overflow: hidden; border: 1px solid #dfe8e5; border-radius: 10px; }
.type-toggle button { padding: 0 18px; height: 40px; border: 0; background: #f8faf9; color: #6b878a; }
.type-toggle button.active { background: #123f4a; color: #fff; }
.submit { padding: 0 22px; height: 40px; border: 0; border-radius: 10px; background: #1f6f6b; color: #fff; font-weight: 600; }
.submit:disabled { opacity: .6; cursor: wait; }
.ledger-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 14px; flex-wrap: wrap; margin-bottom: 16px; }
.ledger-toolbar h3 { margin: 0; }
.filter { display: flex; align-items: center; gap: 8px; }
.filter input { min-width: 0; width: 150px; }
.filter button { height: 40px; padding: 0 16px; border: 0; border-radius: 10px; background: #123f4a; color: #fff; }
.entry-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.entry-table th, .entry-table td { text-align: left; padding: 10px 8px; border-bottom: 1px solid #eef3f1; }
.entry-table th { color: #8ba0a3; font-weight: 600; font-size: 12px; }
.entry-table .num { text-align: right; }
.entry-table .expense { color: #c0564a; font-weight: 600; }
.entry-table .income { color: #1f8a6a; font-weight: 600; }
.entry-table tr.voided td { background: #fafbfa; color: #9aa9a9; }
.entry-table tr.voided td strong { color: #7e9795; }
.void-btn { padding: 4px 12px; border: 1px solid #e3c9c4; border-radius: 8px; background: #fff; color: #b0574c; }
.voided-tag { color: #9aa9a9; font-size: 12px; }
.muted { color: #8ba0a3; }
.ledger-empty { padding: 40px 0; text-align: center; color: #8ba0a3; }
.pager { display: flex; align-items: center; justify-content: center; gap: 14px; margin-top: 16px; color: #6b878a; }
.pager button { padding: 6px 14px; border: 1px solid #dfe8e5; border-radius: 8px; background: #fff; color: #204149; }
.pager button:disabled { opacity: .5; cursor: not-allowed; }
.form-error { color: #b0574c; font-size: 13px; }
.dialog-backdrop { position: fixed; inset: 0; z-index: 30; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(16,43,49,.47); }
.void-dialog { width: min(100%, 440px); padding: 28px; border: 1px solid #e1ece7; border-radius: 19px; background: #fff; box-shadow: 0 25px 70px rgba(10,38,43,.22); }
.void-dialog h3 { margin: 9px 0 8px; color: #1f444b; font-size: 23px; }
.void-dialog p { margin: 0 0 21px; color: #799391; font-size: 13px; line-height: 1.7; }
.void-dialog label { display: block; margin-bottom: 8px; color: #3a5b60; font-size: 12px; font-weight: 600; }
.void-dialog label b { color: #b66751; }
.void-dialog textarea { width: 100%; padding: 11px 13px; border: 1px solid #dce9e5; border-radius: 10px; outline: none; resize: vertical; color: #234a50; background: #fafdFA; font: inherit; font-size: 13px; }
.void-dialog textarea:focus { border-color: #58a097; box-shadow: 0 0 0 3px rgba(69,151,137,.11); }
.void-dialog .form-error { display: block; margin-top: 7px; }
.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 22px; }
.dialog-actions button { min-width: 90px; padding: 10px 16px; border-radius: 9px; font-size: 13px; }
.dialog-actions .cancel { border: 1px solid #dce8e4; background: #fff; color: #607d7c; }
.dialog-actions .confirm { border: 0; background: #aa594d; color: #fff; }
@media (max-width: 640px) { .filter input { width: 100%; } .void-dialog { padding: 23px; } }
.filter .void-toggle { display: flex; align-self: flex-end; align-items: center; flex-direction: row; gap: 7px; height: 38px; padding-right: 7px; white-space: nowrap; color: #617e7d; font-size: 12px; }
.filter .void-toggle input { flex: 0 0 auto; width: 15px; min-width: 15px; height: 15px; margin: 0; accent-color: #27786e; }
.ledger-compose, .ledger-list { padding: 27px 30px; border-color: var(--xi-border); border-radius: 16px; box-shadow: none; }
.summary-card { background: #fff; border-color: var(--xi-border); box-shadow: none; }
.summary-card::after { display: none; }
.summary-card.expense { background: #fff9f5; }
.summary-card.income { background: #f3faf6; }
.summary-card span, .summary-card small { color: var(--xi-body); font-size: 13px; }
.summary-card strong { color: var(--xi-ink); }
.summary-card.expense strong { color: var(--xi-expense); }
.ledger-section-head h3, .ledger-toolbar h3 { color: var(--xi-ink); font-size: 20px; }
.ledger-section-head > span, .workout-sets-heading span { color: var(--xi-body); font-size: 12px; }
.form-field, .filter label { color: var(--xi-ink); font-size: 13px; }
.form-field input, .form-field textarea, .filter input, .filter select, .workout-set-fields input { border-color: var(--xi-border); background: #fff; color: var(--xi-ink); }
.form-field input { height: 46px; }
.form-field input::placeholder, .form-field textarea::placeholder { color: #71898c; }
.submit, .filter button { min-height: 44px; background: var(--xi-brand); box-shadow: none; }
.type-toggle button.active { background: var(--xi-brand-deep); }
.filter input, .filter select, .filter button { height: 44px; }
.journal-filter .month-step { min-width: 44px; width: 44px; }
.entry-table th { color: var(--xi-body); font-size: 12px; }
.entry-table td { color: var(--xi-ink); }
.log-card { padding: 18px 20px; border-color: var(--xi-border); background: #fff; }
.log-meta { flex-wrap: wrap; color: var(--xi-body); font-size: 12px; }
.log-type { color: var(--xi-brand); }
.log-source { color: var(--xi-body); }
.log-card h4 { color: var(--xi-ink); font-size: 17px; }
.log-card p { margin-top: 9px; color: var(--xi-body); font-size: 14px; line-height: 1.7; }
.log-card p.log-remark { font-size: 12px; color: var(--xi-body); }
.pager button, .void-btn, .set-add { min-height: 40px; }
@media (max-width: 640px) {
  .ledger-compose, .ledger-list { padding: 22px 18px; }
  .ledger-summary { grid-template-columns: 1fr 1fr; }
  .summary-card:first-child { grid-column: 1 / -1; }
  .summary-card strong { font-size: 23px; }
  .journal-filter { align-items: flex-end; }
}
</style>
