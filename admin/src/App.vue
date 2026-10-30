<script setup>
import { computed, onMounted, ref } from 'vue'
import { clearToken, getCurrentUser, getToken, login, logout, saveToken } from './api/auth'

const state = ref('checking')
const username = ref('')
const password = ref('')
const showPassword = ref(false)
const busy = ref(false)
const error = ref('')
const user = ref(null)
const activeSection = ref('overview')

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
    password.value = ''
    state.value = 'login'
    busy.value = false
  }
}

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
              <span class="module-status"><i></i> 功能接入中</span>
            </button>
          </div>
          <div class="status-strip"><span class="status-mark">✓</span><div><strong>账号连接正常</strong><p>你已通过与手机端相同的账号登录。业务数据接入后会在此显示。</p></div></div>
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
