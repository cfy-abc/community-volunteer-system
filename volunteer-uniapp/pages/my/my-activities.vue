<template>
  <view class="page">
    <!-- 标签切换 -->
    <view class="tabs">
      <view class="tab" :class="{ active: tab === 'published' }" @tap="tab = 'published'">我发布的</view>
      <view class="tab" :class="{ active: tab === 'joined' }" @tap="tab = 'joined'">我参与的</view>
    </view>

    <!-- 我发布的活动 -->
    <view class="list" v-if="tab === 'published'">
      <view class="empty" v-if="!loading1 && published.length === 0">
        <text>暂无发布的活动</text>
        <button class="action-btn" @tap="goPublish">去发布活动</button>
      </view>
      <view class="card" v-for="a in published" :key="a.activityId" @tap="viewDetail(a.activityId)">
        <view class="card-top">
          <text class="card-title">{{ a.title }}</text>
          <view class="card-badges">
            <text class="pub-tag" :class="a.orgId > 0 ? 't-org' : 't-personal'">{{ a.orgId > 0 ? '组织' : '个人' }}</text>
            <text class="card-status" :class="'s' + a.status">{{ st(a.status) }}</text>
          </view>
        </view>
        <view class="card-meta">
          <text>{{ fmt(a.startTime) }} ~ {{ fmt(a.endTime) }}</text>
          <text>{{ a.location }}</text>
          <text>报名 {{ a.currentParticipants }}/{{ a.maxParticipants }} 人</text>
        </view>
        <view class="pending-badge" v-if="a.pendingCount > 0" @tap.stop="goApproval(a.activityId)">
          待审批: {{ a.pendingCount }}人
        </view>
      </view>
    </view>

    <!-- 我参与的活动（含签到签退操作） -->
    <view class="list" v-if="tab === 'joined'">
      <view class="empty" v-if="!loading2 && joined.length === 0">
        <text>暂无参与的活动</text>
        <button class="action-btn" @tap="goToList">去浏览活动</button>
      </view>
      <view class="card" v-for="item in joined" :key="item.recordId || item.activityId">
        <view class="card-top">
          <text class="card-title">{{ item.title || item.activityTitle || '未知活动' }}</text>
          <text class="card-status" :class="signClass(item)">{{ signLabel(item) }}</text>
        </view>
        <view class="card-meta" v-if="item.startTime">
          <text>{{ fmt(item.startTime) }} ~ {{ fmt(item.endTime) }}</text>
        </view>
        <!-- 签到签退操作 -->
        <view class="sign-actions" v-if="item.status !== 'cancelled'">
          <button class="sign-btn checkin" v-if="item.signStatus === 0 || item.signStatus === 'pending'"
            @tap.stop="goSign(item.activityId, 'checkin')">签 到</button>
          <button class="sign-btn checkout" v-if="item.signStatus === 1 || item.signStatus === 'checked_in'"
            @tap.stop="goSign(item.activityId, 'checkout')">签 退</button>
          <text class="sign-done" v-if="item.signStatus === 2 || item.signStatus === 'checked_out'">已签退 ✓</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const tab = ref('published')
const published = ref([])
const joined = ref([])
const loading1 = ref(true)
const loading2 = ref(true)

const st = (s) => ({ 0: '已取消', 1: '招募中', 2: '进行中', 3: '已结束', 4: '审核中' }[s] || '')
const fmt = (d) => {
  if (!d) return ''
  const dt = new Date(d)
  return `${dt.getMonth() + 1}/${dt.getDate()} ${String(dt.getHours()).padStart(2, '0')}:${String(dt.getMinutes()).padStart(2, '0')}`
}

const signLabel = (item) => {
  const ss = item.signStatus
  if (ss === 1 || ss === 'checked_in') return '已签到'
  if (ss === 2 || ss === 'checked_out') return '已完成'
  if (item.status === 'cancelled') return '已取消'
  return '待签到'
}
const signClass = (item) => {
  const ss = item.signStatus
  if (ss === 1 || ss === 'checked_in') return 's2'
  if (ss === 2 || ss === 'checked_out') return 's3'
  return 's4'
}

// 加载我发布的活动
const loadPublished = async () => {
  try {
    const info = await request.get('/api/users/info')
    if (info.code !== 200 || !info.data) return
    const userId = info.data.userId
    const res = await request.get('/activities', { page: 1, size: 100 })
    if (res.code === 200 && res.data) {
      // 只显示个人发布的活动（orgId === 0），组织活动在其他入口管理
      published.value = (res.data.list || []).filter(a => a.creatorId === userId && (a.orgId === 0 || a.orgId == null))
    }
    // 加载每个活动的待审批数（占位 — 后端接口待添加）
    for (const a of published.value) {
      a.pendingCount = 0
    }
  } catch (e) { console.error(e) } finally { loading1.value = false }
}

// 加载我参与的活动
const loadJoined = async () => {
  try {
    const res = await request.get('/api/users/applications', { page: 1, size: 100 })
    if (res.code === 200 && res.data) {
      joined.value = (res.data.list || [])
    }
  } catch (e) { console.error(e) } finally { loading2.value = false }
}

const viewDetail = (id) => uni.navigateTo({ url: `/pages/activities/detail?id=${id}` })
const goSign = (id, type) => uni.navigateTo({ url: `/pages/activities/sign?id=${id}&type=${type}` })
const goPublish = () => uni.navigateTo({ url: '/pages/activities/publish' })
const goToList = () => uni.switchTab({ url: '/pages/activities/list' })
const goApproval = (activityId) => {
  uni.navigateTo({ url: `/pages/activities/sign-approval?activityId=${activityId}` })
}

onMounted(() => { loadPublished(); loadJoined() })
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f0f2f5; padding: 24rpx; }
.tabs {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 8rpx;
  margin-bottom: 20rpx;
  .tab {
    flex: 1; text-align: center; padding: 16rpx 0; font-size: 28rpx; color: #888; border-radius: 12rpx;
    &.active { background: linear-gradient(135deg, #667eea, #7b5ea7); color: #fff; font-weight: 600; }
  }
}
.card {
  background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
}
.card-top {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx;
  .card-title { font-size: 30rpx; font-weight: 600; color: #222; flex: 1; }
  .card-badges { display: flex; gap: 8rpx; align-items: center; flex-shrink: 0; }
  .pub-tag {
    font-size: 20rpx; padding: 4rpx 10rpx; border-radius: 10rpx;
    &.t-org { background: #e3f2fd; color: #1976d2; }
    &.t-personal { background: #f3e5f5; color: #7b1fa2; }
  }
  .card-status {
    font-size: 22rpx; padding: 4rpx 14rpx; border-radius: 14rpx;
    &.s1 { background: #e8f5e9; color: #388e3c; }
    &.s2 { background: #e3f2fd; color: #1976d2; }
    &.s3 { background: #ffebee; color: #d32f2f; }
    &.s4 { background: #fff3e0; color: #f57c00; }
    &.s0 { background: #f5f5f5; color: #999; }
  }
}
.card-meta { display: flex; flex-direction: column; gap: 6rpx; font-size: 24rpx; color: #888; margin-bottom: 12rpx; }
.pending-badge {
  margin-top: 12rpx; padding: 10rpx 20rpx; background: #fff3e0; color: #ff9800;
  font-size: 24rpx; border-radius: 20rpx; display: inline-block;
}
.sign-actions {
  display: flex; justify-content: flex-end; gap: 16rpx; align-items: center;
  .sign-btn {
    padding: 10rpx 28rpx; border-radius: 24rpx; font-size: 24rpx; border: none; color: #fff;
    &.checkin { background: #4caf50; }
    &.checkout { background: #ff9800; }
  }
  .sign-done { font-size: 24rpx; color: #999; }
}
.empty { text-align: center; padding: 100rpx 0; color: #999; font-size: 28rpx; }
.action-btn {
  margin-top: 30rpx; width: 300rpx; height: 76rpx;
  background: linear-gradient(135deg, #667eea, #7b5ea7);
  color: #fff; border: none; border-radius: 38rpx; font-size: 28rpx;
}
</style>
