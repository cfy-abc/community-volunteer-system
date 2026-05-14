<template>
  <view class="home-container">
    <!-- 顶部导航 -->
    <view class="home-header">
      <view class="header-left">
        <text class="greeting">你好，{{ userDisplayName }}！</text>
      </view>
      <view class="header-right">
        <view class="notification-icon" @tap="showNotifications">
          <text class="iconfont icon-notification"></text>
          <view v-if="unreadCount > 0" class="badge">{{ unreadCount }}</view>
        </view>
        <view class="user-avatar" @tap="toProfile">
          <image :src="userAvatar" class="avatar-img" mode="aspectFill"></image>
        </view>
      </view>
    </view>

    <!-- 快捷入口（将第一个改为“发布活动”） -->
    <view class="quick-actions">
      <view class="action-grid">
        <view class="action-item" @tap="goToPublish">
          <view class="action-icon bg-blue">
            <text class="iconfont icon-publish"></text>
          </view>
          <text class="action-text">发布活动</text>
        </view>
        <view class="action-item" @tap="goToMyApplications">
          <view class="action-icon bg-green">
            <text class="iconfont icon-application"></text>
          </view>
          <text class="action-text">我的报名</text>
        </view>
        <view class="action-item" @tap="goToCertificates">
          <view class="action-icon bg-orange">
            <text class="iconfont icon-certificate"></text>
          </view>
          <text class="action-text">我的证书</text>
        </view>
        <view class="action-item" @tap="toProfile">
          <view class="action-icon bg-purple">
            <text class="iconfont icon-profile"></text>
          </view>
          <text class="action-text">个人中心</text>
        </view>
      </view>
    </view>

    <!-- 活动推荐（保留，查看更多仍跳转活动列表） -->
    <view class="recommend-section">
      <view class="section-header">
        <text class="section-title">热门活动推荐</text>
        <text class="more-text" @tap="goToActivityList">查看更多 ></text>
      </view>
      <view class="recommend-list">
        <view 
          class="activity-card" 
          v-for="activity in recommendActivities" 
          :key="activity.id"
          @tap="viewActivity(activity.id)"
        >
          <image :src="activity.coverImage || '/static/images/default-activity.jpg'" class="activity-image" mode="aspectFill"></image>
          <view class="activity-info">
            <text class="activity-title">{{ activity.title }}</text>
            <text class="activity-time">{{ formatDate(activity.startTime) }}</text>
            <view class="activity-meta">
              <text class="meta-item">{{ activity.location }}</text>
              <text class="meta-item">已报名 {{ activity.appliedCount }}/{{ activity.maxParticipants }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 统计卡片（保持不变） -->
    <view class="stats-section">
      <view class="stat-card">
        <view class="stat-icon bg-blue">
          <text class="iconfont icon-clock"></text>
        </view>
        <view class="stat-info">
          <text class="stat-number">{{ volunteerHours }}</text>
          <text class="stat-label">志愿服务时长</text>
        </view>
      </view>
      <view class="stat-card">
        <view class="stat-icon bg-green">
          <text class="iconfont icon-check"></text>
        </view>
        <view class="stat-info">
          <text class="stat-number">{{ completedActivities }}</text>
          <text class="stat-label">已完成活动</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import store from '@/store'
import request from '@/utils/request'

const unreadCount = ref(0)
const recommendActivities = ref([])
const volunteerHours = ref(0)
const completedActivities = ref(0)
const userDisplayName = computed(() => {
  const ui = uni.getStorageSync('userInfo')
  if (ui && ui.realName) return ui.realName
  if (ui && ui.username) return ui.username
  return uni.getStorageSync('token') ? '志愿者' : '游客'
})
const userAvatar = computed(() => {
  return store.state.auth.userInfo?.avatar || '/static/images/default-avatar.png'
})

const loadData = async () => {
  // 加载活动列表（无需登录）
  try {
    const res = await request.get('/activities', { status: 1, page: 1, size: 3 })
    if (res.code === 200 && res.data) {
      recommendActivities.value = (res.data.list || []).map(a => ({
        ...a,
        id: a.activityId,
        coverImage: a.poster || '/static/images/default-activity.jpg',
        appliedCount: a.currentParticipants || 0,
        tags: typeof a.tags === 'string' ? JSON.parse(a.tags || '[]') : (a.tags || [])
      }))
    }
  } catch (err) {
    console.error('加载活动失败:', err)
  }

  // 加载用户信息（需要登录）
  const token = uni.getStorageSync('token')
  if (token) {
    try {
      const res = await request.get('/api/users/info')
      if (res.code === 200 && res.data) {
        volunteerHours.value = res.data.volunteerHours || 0
        completedActivities.value = Math.floor((res.data.totalEarnedHours || 0) / 2)
      }
    } catch (err) {
      console.error('加载用户信息失败:', err)
    }
  }
}

// ---------- 跳转方法 ----------
const goToActivityList = () => {
  uni.switchTab({ url: '/pages/activities/list' }) // 活动列表是 tabBar 页面
}
const goToMyApplications = () => {
  uni.navigateTo({ url: '/pages/my/applications' })
}
const goToCertificates = () => {
  uni.navigateTo({ url: '/pages/my/certificates' })
}
const toProfile = () => {
  uni.switchTab({ url: '/pages/profile/profile' }) // 个人中心是 tabBar 页面
}
const goToPublish = () => {
  if (!uni.getStorageSync('token')) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => uni.navigateTo({ url: '/pages/auth/login' }), 1000)
    return
  }
  uni.navigateTo({ url: '/pages/activities/publish' })
}
const viewActivity = (id) => {
  uni.navigateTo({ url: `/pages/activities/detail?id=${id}` })
}

// 其他方法
const showNotifications = () => {
  uni.showModal({
    title: '消息通知',
    content: '暂无新消息',
    showCancel: false
  })
}
const formatDate = (dateString) => {
  const date = new Date(dateString)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 生命周期
onMounted(() => {
  loadData()
})
</script>

<style lang="scss">
.home-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 120rpx;
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 30rpx 20rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  
  .header-left .greeting {
    font-size: 36rpx;
    font-weight: bold;
  }
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 30rpx;
    
    .notification-icon {
      position: relative;
      font-size: 40rpx;
      .badge {
        position: absolute;
        top: -10rpx;
        right: -10rpx;
        background-color: #ff4757;
        color: #fff;
        font-size: 20rpx;
        border-radius: 50%;
        width: 30rpx;
        height: 30rpx;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
    .user-avatar {
      width: 60rpx;
      height: 60rpx;
      border-radius: 50%;
      border: 2rpx solid rgba(255, 255, 255, 0.3);
      .avatar-img {
        width: 100%;
        height: 100%;
        border-radius: 50%;
      }
    }
  }
}

.quick-actions {
  padding: 30rpx;
  background-color: #fff;
  
  .action-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20rpx;
  }
  
  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20rpx 10rpx;
    
    .action-icon {
      width: 80rpx;
      height: 80rpx;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 10rpx;
      font-size: 36rpx;
      color: #fff;
    }
    .action-text {
      font-size: 24rpx;
      color: #666;
    }
  }
  
  .bg-blue { background: linear-gradient(135deg, #667eea, #764ba2); }
  .bg-green { background: linear-gradient(135deg, #4facfe, #00f2fe); }
  .bg-orange { background: linear-gradient(135deg, #fa709a, #fee140); }
  .bg-purple { background: linear-gradient(135deg, #a8edea, #fed6e3); }
}

.recommend-section {
  background-color: #fff;
  margin: 20rpx 20rpx 0;
  border-radius: 20rpx;
  padding: 30rpx;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;
    
    .section-title {
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
    }
    .more-text {
      font-size: 26rpx;
      color: #667eea;
    }
  }
  
  .recommend-list {
    .activity-card {
      display: flex;
      margin-bottom: 30rpx;
      
      &:last-child { margin-bottom: 0; }
      
      .activity-image {
        width: 180rpx;
        height: 120rpx;
        border-radius: 12rpx;
        margin-right: 20rpx;
      }
      .activity-info {
        flex: 1;
        
        .activity-title {
          display: block;
          font-size: 28rpx;
          font-weight: 500;
          color: #333;
          margin-bottom: 10rpx;
        }
        .activity-time {
          display: block;
          font-size: 24rpx;
          color: #999;
          margin-bottom: 10rpx;
        }
        .activity-meta {
          display: flex;
          gap: 20rpx;
          .meta-item {
            font-size: 22rpx;
            color: #666;
            background-color: #f0f0f0;
            padding: 4rpx 12rpx;
            border-radius: 12rpx;
          }
        }
      }
    }
  }
}

.stats-section {
  display: flex;
  gap: 20rpx;
  padding: 0 20rpx;
  
  .stat-card {
    flex: 1;
    background: #fff;
    border-radius: 20rpx;
    padding: 30rpx;
    display: flex;
    align-items: center;
    
    .stat-icon {
      width: 60rpx;
      height: 60rpx;
      border-radius: 12rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 20rpx;
      font-size: 32rpx;
      color: #fff;
    }
    .stat-info {
      .stat-number {
        display: block;
        font-size: 36rpx;
        font-weight: bold;
        color: #333;
      }
      .stat-label {
        display: block;
        font-size: 24rpx;
        color: #666;
      }
    }
  }
}

/* 已删除底部悬浮发布按钮，避免冗余 */
</style>