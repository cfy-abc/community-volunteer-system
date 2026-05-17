<template>
  <view class="home-container">
    <!-- 顶部导航 -->
    <view class="home-header">
      <view class="header-left">
        <text class="greeting">你好，{{ userDisplayName }}！</text>
      </view>
      <view class="header-right">
        <view class="notification-icon" @tap="showNotifications">
          <text class="bell-icon">&#128276;</text>
          <view v-if="unreadCount > 0" class="badge">{{ unreadCount }}</view>
        </view>
        <view class="user-avatar" @tap="toProfile">
          <image :src="userAvatar" class="avatar-img" mode="aspectFill"></image>
        </view>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="quick-actions">
      <view class="action-grid">
        <view class="action-item" @tap="goToPublish">
          <view class="action-icon bg-blue">
            <text class="action-icon-text">&#9997;</text>
          </view>
          <text class="action-text">发布活动</text>
        </view>
        <view class="action-item" @tap="goToMyApplications">
          <view class="action-icon bg-green">
            <text class="action-icon-text">&#128203;</text>
          </view>
          <text class="action-text">我的报名</text>
        </view>
        <view class="action-item" @tap="goToCertificates">
          <view class="action-icon bg-orange">
            <text class="action-icon-text">&#127942;</text>
          </view>
          <text class="action-text">我的证书</text>
        </view>
        <view class="action-item" @tap="goToMyActivities">
          <view class="action-icon bg-purple">
            <text class="action-icon-text">&#128188;</text>
          </view>
          <text class="action-text">我的活动</text>
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

    <!-- 服务概览看板 -->
    <view class="overview-section" v-if="hasToken">
      <view class="overview-card">
        <view class="ov-item">
          <text class="ov-num">{{ volunteerHours }}</text>
          <text class="ov-lbl">可用时长 (h)</text>
        </view>
        <view class="ov-item">
          <text class="ov-num">{{ totalEarned }}</text>
          <text class="ov-lbl">累计赚取 (h)</text>
        </view>
        <view class="ov-item">
          <text class="ov-num">{{ currentMonthHours }}</text>
          <text class="ov-lbl">本月服务 (h)</text>
        </view>
      </view>
      <view class="overview-tip" @tap="toProfile">
        <text>查看详细数据看板 ></text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '@/utils/request'

const unreadCount = ref(0)
const recommendActivities = ref([])
const volunteerHours = ref(0)
const totalEarned = ref(0)
const monthHours = ref(0)
const monthlyStats = ref([])
const hasToken = ref(false)
const userInfo = ref({})
const userDisplayName = computed(() => {
  if (userInfo.value.realName) return userInfo.value.realName
  if (userInfo.value.username) return userInfo.value.username
  const cached = uni.getStorageSync('userInfo')
  if (cached && cached.realName) return cached.realName
  if (cached && cached.username) return cached.username
  return uni.getStorageSync('token') ? '志愿者' : '游客'
})
const userAvatar = computed(() => {
  if (userInfo.value.avatar) return userInfo.value.avatar
  const cached = uni.getStorageSync('userInfo')
  if (cached && cached.avatar) return cached.avatar
  return '/static/images/default-avatar.png'
})
const currentMonthHours = computed(() => {
  if (!monthlyStats.value || monthlyStats.value.length === 0) return 0
  const now = new Date()
  const currentMonth = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`
  const found = monthlyStats.value.find(s => s.month === currentMonth)
  return found ? (found.hours || 0) : 0
})

const loadData = async () => {
  // 加载活动列表（无需登录）
  try {
    const res = await request.get('/api/activities', { status: 1, page: 1, size: 3 })
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
    hasToken.value = true
    try {
      const res = await request.get('/api/users/info')
      if (res.code === 200 && res.data) {
        userInfo.value = res.data
        volunteerHours.value = res.data.volunteerHours || 0
        totalEarned.value = res.data.totalEarnedHours || 0
        uni.setStorageSync('userInfo', res.data)
      }
    } catch (err) {
      console.error('加载用户信息失败:', err)
    }
    // 加载月度总计
    try {
      const statsRes = await request.get('/api/users/stats')
      if (statsRes.code === 200 && statsRes.data) {
        const ms = statsRes.data.monthlyStats || []
        monthlyStats.value = ms
        monthHours.value = ms.length > 0 ? (ms[ms.length - 1].hours || 0) : 0
      }
    } catch (err) {
      console.error('加载统计数据失败:', err)
    }
  }
}

// ---------- 跳转方法 ----------
const goToActivityList = () => {
  uni.switchTab({ url: '/pages/activities/list' }) // 活动列表是 tabBar 页面
}
const goToMyApplications = () => {
  if (!hasToken.value) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  uni.navigateTo({ url: '/pages/my/applications' })
}
const goToCertificates = () => {
  if (!hasToken.value) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  uni.navigateTo({ url: '/pages/my/certificates' })
}
const goToMyActivities = () => {
  if (!uni.getStorageSync('token')) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/my/my-activities' })
}
const toProfile = () => {
  uni.switchTab({ url: '/pages/profile/profile' })
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
onShow(() => {
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
      .bell-icon { font-size: 44rpx; line-height: 1; }
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
      .action-icon-text { font-size: 40rpx; line-height: 1; }
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

.chart-section {
  padding: 0 20rpx;
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

// 服务概览看板
.overview-section {
  background: #fff;
  margin: 20rpx;
  border-radius: 20rpx;
  padding: 30rpx 20rpx;
}
.overview-card {
  display: flex;
  .ov-item {
    flex: 1;
    text-align: center;
    .ov-num {
      display: block;
      font-size: 42rpx;
      font-weight: 800;
      background: linear-gradient(135deg, #667eea, #7b5ea7);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .ov-lbl {
      display: block;
      font-size: 22rpx;
      color: #999;
      margin-top: 6rpx;
    }
  }
}
.overview-tip {
  text-align: center;
  margin-top: 20rpx;
  font-size: 24rpx;
  color: #667eea;
}

</style>