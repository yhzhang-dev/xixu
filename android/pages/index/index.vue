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
      <view class="hero-circle circle-one"></view>
      <view class="hero-circle circle-two"></view>
      <view class="hero-inner">
        <text class="hero-top">{{ greeting }}{{ accountName ? '，' + accountName : '' }} · {{ weekday }}</text>
        <text class="hero-title">把今天，</text>
        <text class="hero-title hero-title-next">慢慢收好。</text>
        <view class="hero-subtitle"><text>每一笔开销、每一段日常，</text><text>都能在这里找到自己的位置。</text></view>
        <button class="hero-button" @tap="openAction('记一笔')">＋　开始记录</button>
      </view>
    </view>

    <view class="sample-label"><view class="sample-dot"></view><text>界面原型 · 以下为示例数据</text></view>

    <view class="section-head">
      <view><text class="section-en">OVERVIEW</text><text class="section-title">今日一览</text></view>
      <text class="section-side">{{ month }}月{{ day }}日</text>
    </view>
    <view class="stats">
      <view class="stat">
        <text class="stat-name">今日支出</text>
        <view class="stat-number"><text class="unit">¥</text><text>36.50</text></view>
        <text class="stat-note">示例账目</text>
      </view>
      <view class="stat">
        <text class="stat-name">待办日程</text>
        <view class="stat-number"><text>02</text><text class="suffix">项</text></view>
        <text class="stat-note">示例安排</text>
      </view>
      <view class="stat">
        <text class="stat-name">生活记录</text>
        <view class="stat-number"><text>01</text><text class="suffix">条</text></view>
        <text class="stat-note">示例随记</text>
      </view>
    </view>

    <view class="section-head later">
      <view><text class="section-en">QUICK ACCESS</text><text class="section-title">随手记下</text></view>
      <text class="section-side">从此刻开始</text>
    </view>
    <view class="actions">
      <button class="action" @tap="openAction('记一笔')">
        <text class="action-icon icon-green">¥</text><view class="action-text"><text class="action-title">记一笔</text><text class="action-desc">收支一目了然</text></view><text class="arrow">›</text>
      </button>
      <button class="action" @tap="openAction('看日程')">
        <text class="action-icon icon-blue">日</text><view class="action-text"><text class="action-title">看日程</text><text class="action-desc">课表与安排</text></view><text class="arrow">›</text>
      </button>
      <button class="action" @tap="openAction('写日志')">
        <text class="action-icon icon-sand">✎</text><view class="action-text"><text class="action-title">写日志</text><text class="action-desc">留住日常片段</text></view><text class="arrow">›</text>
      </button>
      <button class="action" @tap="openAction('存附件')">
        <text class="action-icon icon-purple">▧</text><view class="action-text"><text class="action-title">存附件</text><text class="action-desc">图片与凭据</text></view><text class="arrow">›</text>
      </button>
    </view>

    <view class="section-head later">
      <view><text class="section-en">TODAY'S FLOW</text><text class="section-title">今日片段</text></view>
      <text class="section-side">示例</text>
    </view>
    <view class="timeline">
      <view class="event"><text class="event-time">09:00</text><view class="event-info"><text class="event-type">课表</text><text class="event-title">课程安排示例</text><text class="event-detail">接入课表后显示真实课程</text></view></view>
      <view class="event last"><text class="event-time">12:30</text><view class="event-info"><text class="event-type">账本</text><text class="event-title">午餐 · ¥25.00</text><text class="event-detail">以后可以从这里回看当天记录</text></view></view>
    </view>
    <text class="footer">汐序 · 收好生活的每一天</text>
    <button v-if="isLoggedIn" class="logout-button" @tap="logout">退出登录</button>
  </view>
</template>

<script>
import { API_BASE_URL, TOKEN_KEY } from '../../common/api.js'

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
      isLoggedIn: false
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
  methods: {
    openAction(name) {
      uni.showToast({ title: name + '页面正在制作', icon: 'none' })
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
</style>
