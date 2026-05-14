<template>
  <view class="profile-container">
    <view class="profile-header">
      <view class="header-overlay"></view>
      <view class="user-info">
        <view class="avatar-container">
          <image :src="profile.avatar || '/static/images/default-avatar.png'" class="user-avatar" mode="aspectFill"></image>
        </view>
        <text class="user-name">{{ profile.realName || profile.username || '未登录' }}</text>
        <text class="user-role">志愿者</text>
      </view>
    </view>

    <view class="stats-row">
      <view class="stat-item">
        <text class="stat-num">{{ profile.volunteerHours || 0 }}</text>
        <text class="stat-label">可用时长(h)</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ profile.totalEarnedHours || 0 }}</text>
        <text class="stat-label">累计赚取</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ profile.totalSpentHours || 0 }}</text>
        <text class="stat-label">累计支出</text>
      </view>
    </view>

    <view class="menu-section">
      <view class="section-title">基本信息</view>
      <view class="menu-item">
        <text class="item-label">用户名</text>
        <text class="item-value">{{ profile.username }}</text>
      </view>
      <view class="menu-item">
        <text class="item-label">真实姓名</text>
        <text class="item-value">{{ profile.realName }}</text>
      </view>
      <view class="menu-item">
        <text class="item-label">手机号</text>
        <text class="item-value">{{ profile.phone }}</text>
      </view>
    </view>

    <view class="menu-section">
      <view class="section-title">功能</view>
      <view class="menu-item" @tap="goTo('myApplications')">
        <text class="item-label">我的报名</text>
        <text class="arrow">></text>
      </view>
      <view class="menu-item" @tap="goTo('myCertificates')">
        <text class="item-label">我的证书</text>
        <text class="arrow">></text>
      </view>
      <view class="menu-item" @tap="goTo('publishActivity')">
        <text class="item-label">发布活动</text>
        <text class="arrow">></text>
      </view>
      <view class="menu-item" @tap="goTo('adminLogin')">
        <text class="item-label">管理后台</text>
        <text class="arrow">></text>
      </view>
    </view>

    <view class="logout-section">
      <button class="logout-btn" @tap="handleLogout">退出登录</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const profile = ref({})

const loadProfile = async () => {
  try {
    const res = await request.get('/api/users/info')
    if (res.code === 200) profile.value = res.data
  } catch (err) {}
}

const goTo = (page) => {
  const routes = {
    myApplications: '/pages/my/applications',
    myCertificates: '/pages/my/certificates',
    publishActivity: '/pages/activities/publish',
    adminLogin: '/pages/auth/admin-login'
  }
  uni.navigateTo({ url: routes[page] })
}

const handleLogout = () => {
  uni.showModal({
    title: '确认退出',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        uni.removeStorageSync('token')
        uni.removeStorageSync('userInfo')
        uni.reLaunch({ url: '/pages/auth/login' })
      }
    }
  })
}

onMounted(() => loadProfile())
</script>

<style lang="scss" scoped>
.profile-container {
  min-height: 100vh;
  background: #f0f2f5;
}
.profile-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60rpx 0 40rpx;
  position: relative;
  .header-overlay {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    background: radial-gradient(circle at 30% 50%, rgba(255,255,255,0.1) 0%, transparent 60%);
  }
  .user-info {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    .avatar-container {
      margin-bottom: 20rpx;
      .user-avatar {
        width: 120rpx;
        height: 120rpx;
        border-radius: 50%;
        border: 3rpx solid rgba(255,255,255,0.3);
      }
    }
    .user-name { font-size: 36rpx; font-weight: bold; color: #fff; margin-bottom: 8rpx; }
    .user-role {
      font-size: 24rpx;
      color: rgba(255,255,255,0.8);
      background: rgba(255,255,255,0.2);
      padding: 4rpx 20rpx;
      border-radius: 20rpx;
    }
  }
}
.stats-row {
  display: flex;
  margin: -30rpx 30rpx 20rpx;
  position: relative;
  z-index: 2;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.08);
  .stat-item {
    flex: 1;
    text-align: center;
    padding: 24rpx 0;
    .stat-num { display: block; font-size: 40rpx; font-weight: bold; color: #667eea; }
    .stat-label { display: block; font-size: 22rpx; color: #999; margin-top: 4rpx; }
  }
}
.menu-section {
  background: #fff;
  border-radius: 16rpx;
  margin: 0 30rpx 20rpx;
  padding: 30rpx;
  .section-title { font-size: 28rpx; font-weight: bold; color: #333; margin-bottom: 20rpx; }
  .menu-item {
    display: flex; justify-content: space-between; align-items: center;
    padding: 20rpx 0; border-bottom: 1rpx solid #f0f0f0;
    &:last-child { border-bottom: none; }
    .item-label { font-size: 28rpx; color: #333; }
    .item-value { font-size: 26rpx; color: #999; }
    .arrow { font-size: 26rpx; color: #ccc; }
  }
}
.logout-section {
  padding: 40rpx 30rpx;
  .logout-btn {
    width: 100%;
    height: 80rpx;
    background: #ff4757;
    color: #fff;
    border: none;
    border-radius: 40rpx;
    font-size: 32rpx;
  }
}
</style>
