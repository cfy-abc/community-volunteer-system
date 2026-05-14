<template>
  <view class="dashboard-container">
    <view class="top-bar">
      <text class="title">管理后台</text>
      <text class="logout" @tap="handleLogout">退出</text>
    </view>

    <view class="loading-box" v-if="loading">
      <text>加载中...</text>
    </view>

    <template v-else-if="!error">
      <view class="stats-row">
        <view class="stat-card primary">
          <text class="stat-num">{{ data.totalUsers || 0 }}</text>
          <text class="stat-label">注册用户</text>
        </view>
        <view class="stat-card success">
          <text class="stat-num">{{ data.totalActivities || 0 }}</text>
          <text class="stat-label">活动总数</text>
        </view>
        <view class="stat-card warning">
          <text class="stat-num">{{ data.totalOrganizations || 0 }}</text>
          <text class="stat-label">志愿组织</text>
        </view>
        <view class="stat-card info">
          <text class="stat-num">{{ data.totalVolunteerHours || 0 }}</text>
          <text class="stat-label">总志愿时长(h)</text>
        </view>
      </view>

      <view class="quick-actions">
        <view class="action-item" @tap="goTo('users')">
          <text class="action-icon">&#x1f465;</text>
          <text class="action-text">用户管理</text>
        </view>
        <view class="action-item" @tap="goTo('activities')">
          <text class="action-icon">&#x1f4cb;</text>
          <text class="action-text">活动管理</text>
        </view>
        <view class="action-item" @tap="goTo('organizations')">
          <text class="action-icon">&#x1f3e2;</text>
          <text class="action-text">组织管理</text>
        </view>
      </view>

      <view class="section" v-if="topList.length > 0">
        <text class="section-title">志愿时长排行 TOP10</text>
        <view class="rank-list">
          <view class="rank-item" v-for="(u, idx) in topList" :key="u.userId || idx">
            <view class="rank-num" :class="{ top: idx < 3 }">{{ idx + 1 }}</view>
            <view class="rank-info">
              <text class="rank-name">{{ u.realName }}</text>
              <text class="rank-phone">{{ u.phone }}</text>
            </view>
            <text class="rank-hours">{{ u.volunteerHours || 0 }}h</text>
          </view>
        </view>
      </view>

      <view class="section" v-if="data.topOrganizations && data.topOrganizations.length > 0">
        <text class="section-title">组织活跃度 TOP10</text>
        <view class="rank-list">
          <view class="rank-item" v-for="(o, idx) in data.topOrganizations" :key="idx">
            <view class="rank-num" :class="{ top: idx < 3 }">{{ idx + 1 }}</view>
            <text class="rank-name flex-1">{{ o.name }}</text>
            <text class="rank-hours">{{ o.total || 0 }}h</text>
          </view>
        </view>
      </view>
    </template>

    <view class="error-box" v-if="error">
      <text>{{ error }}</text>
      <button class="retry-btn" @tap="loadData">重新加载</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'

const data = ref({
  totalUsers: 0, totalActivities: 0, totalOrganizations: 0, totalVolunteerHours: 0,
  topVolunteers: [], topOrganizations: [], activityTypeDistribution: []
})
const loading = ref(true)
const error = ref('')

const topList = computed(() => data.value.topVolunteers || [])

const goTo = (page) => {
  const urls = {
    users: '/pages/admin/users',
    activities: '/pages/admin/activities',
    organizations: '/pages/admin/organizations'
  }
  uni.navigateTo({ url: urls[page] })
}

const loadData = async () => {
  loading.value = true
  error.value = ''
  try {
    const adminToken = uni.getStorageSync('adminToken')
    if (!adminToken) {
      uni.redirectTo({ url: '/pages/auth/admin-login' })
      return
    }
    const res = await request.get('/admin/dashboard')
    if (res.code === 200) {
      data.value = {
        totalUsers: res.data.totalUsers || 0,
        totalActivities: res.data.totalActivities || 0,
        totalOrganizations: res.data.totalOrganizations || 0,
        totalVolunteerHours: res.data.totalVolunteerHours || 0,
        topVolunteers: res.data.topVolunteers || [],
        topOrganizations: res.data.topOrganizations || [],
        activityTypeDistribution: res.data.activityTypeDistribution || []
      }
    } else {
      error.value = res.msg || '加载失败'
    }
  } catch (err) {
    error.value = '网络连接失败，请检查后端服务是否启动'
    console.error('Dashboard load error:', err)
  } finally {
    loading.value = false
  }
}

const handleLogout = () => {
  uni.removeStorageSync('adminToken')
  uni.removeStorageSync('adminUsername')
  uni.redirectTo({ url: '/pages/auth/admin-login' })
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.dashboard-container { min-height: 100vh; background: #f0f2f5; padding-bottom: 40rpx; }
.top-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx 30rpx; background: linear-gradient(135deg, #1a1a2e, #16213e);
  .title { font-size: 36rpx; font-weight: bold; color: #fff; }
  .logout { font-size: 28rpx; color: rgba(255,255,255,0.7); padding: 8rpx 16rpx; }
}
.loading-box, .error-box {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 100rpx 0; color: #999; font-size: 28rpx;
  .retry-btn { margin-top: 30rpx; padding: 12rpx 40rpx; background: #667eea; color: #fff; border-radius: 40rpx; border: none; font-size: 28rpx; }
}
.stats-row {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 16rpx; padding: 24rpx;
  .stat-card {
    background: #fff; border-radius: 16rpx; padding: 30rpx; text-align: center;
    &.primary { border-left: 6rpx solid #667eea; }
    &.success { border-left: 6rpx solid #4caf50; }
    &.warning { border-left: 6rpx solid #ff9800; }
    &.info { border-left: 6rpx solid #2196f3; }
    .stat-num { display: block; font-size: 48rpx; font-weight: bold; color: #333; }
    .stat-label { display: block; font-size: 24rpx; color: #999; margin-top: 8rpx; }
  }
}
.quick-actions {
  display: flex; gap: 16rpx; padding: 0 24rpx 24rpx;
  .action-item {
    flex: 1; background: #fff; border-radius: 16rpx; padding: 30rpx 20rpx; text-align: center;
    .action-icon { font-size: 40rpx; }
    .action-text { display: block; font-size: 24rpx; color: #666; margin-top: 8rpx; }
  }
}
.section {
  background: #fff; border-radius: 16rpx; margin: 0 24rpx 24rpx; padding: 30rpx;
  .section-title { font-size: 30rpx; font-weight: bold; color: #333; margin-bottom: 24rpx; }
}
.rank-list {
  .rank-item {
    display: flex; align-items: center; padding: 16rpx 0; border-bottom: 1rpx solid #f0f0f0;
    &:last-child { border-bottom: none; }
    .rank-num {
      width: 48rpx; height: 48rpx; border-radius: 50%; background: #eee; display: flex;
      align-items: center; justify-content: center; font-size: 24rpx; color: #666; margin-right: 16rpx;
      &.top { background: #ff9800; color: #fff; }
    }
    .rank-info { flex: 1; .rank-name { display: block; font-size: 28rpx; color: #333; } .rank-phone { display: block; font-size: 22rpx; color: #999; } }
    .rank-name.flex-1 { flex: 1; }
    .rank-hours { font-size: 28rpx; font-weight: bold; color: #667eea; }
  }
}
</style>
