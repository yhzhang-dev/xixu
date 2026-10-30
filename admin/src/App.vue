<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { clearToken, getCurrentUser, getToken, login, logout, saveToken } from './api/auth'
import { createEntry, getSummary, listEntries, voidEntry } from './api/ledger'

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

const dateText = new Intl.DateTimeFormat('zh-CN', {
  month: 'long', day: 'numeric', weekday: 'long',
}).format(new Date())

const sections = [
  { key: 'overview', icon: '⌂', label: '总览', detail: '生活的脉络，从这里开始。' },
  { key: 'ledger', icon: '¥', label: '记账', detail: '记录收入与支出，按日期和分类查找。' },
  { key: 'schedule', icon: '▦', label: '日程课表', detail: '安排课程、计划和提醒。' },
  { key: 'journal', icon: '✎', label: '每日日志', detail: '保存每天值得回看的片段。' },
  { key: 'attachments', icon: '▧', label: '附件归档', detail: '整理截图、票据和文件。' },
]

const currentSection = computed(() => sections.find(item => item.key === activeSection.value))
const displayName = computed(() => user.value?.displayName || user.value?.username || '')

async function restoreSession() {
  const token = getToken()
  if (!token) { state.value = 'login'; return }
  try {
    user.value = await getCurrentUser(token)
    state.value = 'ready'
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
    ledger.value.items = []
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

watch(activeSection, (section) => { if (section === 'ledger') loadLedger(1) })

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
        <p>账目、日程、日志与生活里的片段，<br />安放在同一个地方。</p>
        <div class="story-line"><span>01 / 04</span><i></i><span>从记录开始</span></div>
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
          <span class="nav-icon" aria-hidden="true">{{ section.icon }}</span><span>{{ section.label }}</span>
          <span v-if="activeSection === section.key" class="nav-current"></span>
        </button>
        <button v-if="user?.roleCode === 'ADMIN'" type="button"
          :class="['nav-item', { active: activeSection === 'accounts' }]"
          @click="activeSection = 'accounts'">
          <span class="nav-icon" aria-hidden="true">♙</span><span>账号管理</span>
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
          <div class="page-heading"><div><span class="page-kicker">YOUR SPACE</span><h2>你好，{{ displayName }}。</h2><p>把生活放在眼前，也留一点余地给明天。</p></div>
            <div class="account-chip"><span class="chip-dot"></span>{{ user?.roleCode === 'ADMIN' ? '管理员账号' : '普通用户账号' }}</div></div>
          <section class="welcome-card">
            <div class="welcome-glow"></div><div class="welcome-rings"></div>
            <div class="welcome-content"><span>汐序 · 生活总览</span><h3>让记录慢慢汇成<br />自己的生活地图。</h3>
              <p>手机端负责随手记下，Web 端负责安静地整理与回看。</p></div>
            <div class="welcome-seal" aria-hidden="true">汐</div>
          </section>
          <div class="section-title-row"><div><span class="section-kicker">MODULES</span><h3>从这里开始整理</h3></div><span>4 个生活模块</span></div>
          <div class="module-grid">
            <button v-for="section in sections.filter(item => item.key !== 'overview')" :key="section.key"
              type="button" class="module-card" @click="activeSection = section.key">
              <span :class="['module-icon', section.key]">{{ section.icon }}</span>
              <span class="module-title">{{ section.label }} <span aria-hidden="true">↗</span></span>
              <span class="module-detail">{{ section.detail }}</span>
              <span class="module-status"><i></i> {{ section.key === 'ledger' ? '已接入真实数据' : '功能接入中' }}</span>
            </button>
          </div>
          <div class="status-strip"><span class="status-mark">✓</span><div><strong>账号连接正常</strong><p>记账已接入真实数据；日程、日志与附件仍在建设中。</p></div></div>
        </template>
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
        <template v-else>
          <div class="page-heading"><div><span class="page-kicker">XIXU / WORKSPACE</span>
            <h2>{{ activeSection === 'accounts' ? '账号管理' : currentSection?.label }}</h2>
            <p>{{ activeSection === 'accounts' ? '管理账号状态与角色。' : currentSection?.detail }}</p></div></div>
          <div class="empty-state"><span class="empty-symbol">{{ activeSection === 'accounts' ? '♙' : currentSection?.icon }}</span>
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
.form-field input { width: 100%; min-width: 0; height: 44px; padding: 0 13px; border: 1px solid #deebe7; border-radius: 10px; outline: none; background: #fbfdfc; color: #204149; font-size: 14px; font-weight: 400; transition: border-color .18s, box-shadow .18s; }
.form-field input:focus, .filter input:focus { border-color: #57a49a; box-shadow: 0 0 0 3px rgba(69,151,137,.11); }
.form-field input::placeholder { color: #b2c2c0; }
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
</style>
