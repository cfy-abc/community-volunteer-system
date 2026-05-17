<template>
  <view class="applications-container">
    <!-- 页面标题 -->
    <view class="page-header">
      <view class="header-left">
        <text class="title">我的报名</text>
      </view>
      <view class="header-right">
        <text class="total-count">共{{ applications.length }}个</text>
      </view>
    </view>

    <!-- 筛选标签 -->
    <view class="filter-tabs">
      <scroll-view scroll-x class="tabs-scroll">
        <view class="tab-list">
          <view 
            class="tab-item" 
            :class="{ active: currentTab === tab.key }"
            v-for="tab in tabs" 
            :key="tab.key"
            @tap="switchTab(tab.key)"
          >
            {{ tab.label }}
            <text class="count-badge" v-if="tab.count > 0">{{ tab.count }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 报名列表 -->
    <view class="applications-list">
      <view 
        class="application-item" 
        v-for="application in filteredApplications" 
        :key="application.id"
      >
        <view class="application-card">
          <image :src="application.activityCover || '/static/images/default-activity.jpg'" class="activity-image" mode="aspectFill"></image>
          <view class="application-info">
            <view class="activity-title">{{ application.activityTitle }}</view>
            <view class="activity-time">{{ formatDate(application.activityTime) }}</view>
            <view class="application-status" :class="getStatusClass(application.status)">
              {{ getStatusText(application.status) }}
            </view>
            <view class="application-meta">
              <text class="meta-item">报名时间：{{ formatDate(application.applyTime) }}</text>
              <text class="meta-item">服务时长：{{ application.duration }}小时</text>
            </view>
            <view class="application-actions">
              <!-- 签到/签退按钮（根据活动状态和签到状态） -->
              <button 
                v-if="showSignButton(application)" 
                class="action-btn signin" 
                @tap="goToSign(application.activityId, 'checkin')"
              >
                签到
              </button>
              <button 
                v-if="showCheckoutButton(application)" 
                class="action-btn signout" 
                @tap="goToSign(application.activityId, 'checkout')"
              >
                签退
              </button>
              <!-- 原有按钮 -->
              <button 
                class="action-btn primary" 
                v-if="canViewDetails(application.status)"
                @tap="viewActivity(application.activityId)"
              >
                查看详情
              </button>
              <button 
                class="action-btn secondary" 
                v-if="canCancel(application.status)"
                @tap="cancelApplication(application.id)"
              >
                取消报名
              </button>
              <button 
                class="action-btn success" 
                v-if="canDownloadCertificate(application.status)"
                @tap="downloadCertificate(application.certificateId)"
              >
                下载证书
              </button>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-if="filteredApplications.length === 0">
      <image src="/static/images/empty-applications.png" class="empty-img"></image>
      <text class="empty-text">暂无报名记录</text>
      <text class="empty-subtext">您还没有报名任何活动</text>
      <button class="go-activity-btn" @tap="goToActivities">去报名活动</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'   // ✅ 正确导入 onShow
import request from '@/utils/request'

// 数据
const applications = ref([])
const currentTab = ref('all')
const loading = ref(false)

// 标签配置（与数据库 status 字段一致）
const tabs = ref([
  { key: 'all', label: '全部', count: 0 },
  { key: 'registered', label: '待签到', count: 0 },
  { key: 'attended', label: '已签到', count: 0 },
  { key: 'completed', label: '已完成', count: 0 },
  { key: 'cancelled', label: '已取消', count: 0 }
])

const filteredApplications = computed(() => {
  if (currentTab.value === 'all') return applications.value
  return applications.value.filter(app => app.status === currentTab.value)
})

// 从后端获取报名列表
const fetchApplications = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/users/applications', { page: 1, size: 50 })
    if (res.code === 200 && res.data) {
      applications.value = (res.data.list || []).map(item => ({
        id: item.recordId,
        activityId: item.activityId,
        activityTitle: item.activityTitle || '活动',
        activityTime: item.startTime || '',
        applyTime: item.registerTime || '',
        status: item.status || 'applied',
        duration: item.hoursEarned || 0,
        signStatus: item.signStatus,
        activityCover: '/default_activity_poster.jpg'
      }))
      updateTabCounts()
    }
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

// 更新各标签页的数量统计
const updateTabCounts = () => {
  const counts = {
    all: applications.value.length,
    registered: applications.value.filter(app => app.status === 'registered').length,
    attended: applications.value.filter(app => app.status === 'attended').length,
    completed: applications.value.filter(app => app.status === 'completed').length,
    cancelled: applications.value.filter(app => app.status === 'cancelled').length
  }
  tabs.value.forEach(tab => {
    tab.count = counts[tab.key] || 0
  })
}

// 切换标签
const switchTab = (tabKey) => {
  currentTab.value = tabKey
}

// 状态文本和样式（数据库字段: registered/attended/completed/cancelled）
const getStatusText = (status) => {
  const statusMap = {
    'registered': '待参加',
    'attended': '已签到',
    'completed': '已完成',
    'cancelled': '已取消'
  }
  return statusMap[status] || status || '未知'
}

const getStatusClass = (status) => {
  const classMap = {
    'registered': 'status-applied',
    'attended': 'status-processing',
    'completed': 'status-completed',
    'cancelled': 'status-cancelled'
  }
  return classMap[status] || 'status-default'
}

// 按钮显示控制
const canViewDetails = (status) => true
const canCancel = (status) => status === 'registered'
const canDownloadCertificate = (status) => status === 'completed'

// 签到按钮：已报名未签到 (signStatus===0)
const showSignButton = (app) => {
  return app.status === 'registered' && (app.signStatus === 0 || app.signStatus == null)
}

// 签退按钮：已签到未签退 (signStatus===1)
const showCheckoutButton = (app) => {
  return (app.status === 'registered' || app.status === 'attended') && app.signStatus === 1
}

// 跳转到签到/签退页面
const goToSign = (activityId, type) => {
  uni.navigateTo({ url: `/pages/activities/sign?id=${activityId}&type=${type}` })
}

// 其他方法
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`
}

const viewActivity = (activityId) => {
  uni.navigateTo({ url: `/pages/activities/detail?id=${activityId}` })
}

const cancelApplication = async (applicationId) => {
  uni.showModal({
    title: '确认取消',
    content: '确定要取消报名吗？取消后将无法恢复。',
    success: async (res) => {
      if (res.confirm) {
        try {
          // 调用后端取消接口
          const result = await request.post(`/api/users/applications/${applicationId}/cancel`)
          if (result.code === 200) {
            uni.showToast({ title: '取消成功', icon: 'success' })
            await fetchApplications() // 刷新列表
          } else {
            uni.showToast({ title: result.msg || '取消失败', icon: 'none' })
          }
        } catch (err) {
          console.error(err)
          uni.showToast({ title: '网络错误', icon: 'none' })
        }
      }
    }
  })
}

const downloadCertificate = (certificateId) => {
  uni.showLoading({ title: '下载中...' })
  setTimeout(() => {
    uni.hideLoading()
    uni.showModal({
      title: '证书下载',
      content: '证书下载功能开发中，将在后续版本上线',
      showCancel: false
    })
  }, 2000)
}

const goToActivities = () => {
  uni.switchTab({ url: '/pages/activities/list' })
}

// 页面生命周期
onShow(() => {
  fetchApplications()
})
</script>

<style lang="scss">
.applications-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 20rpx;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  background-color: #fff;
  
  .header-left .title {
    font-size: 36rpx;
    font-weight: bold;
    color: #333;
  }
  .header-right .total-count {
    font-size: 26rpx;
    color: #666;
  }
}

.filter-tabs {
  background-color: #fff;
  padding: 20rpx 0;
  
  .tabs-scroll {
    white-space: nowrap;
    padding: 0 30rpx;
  }
  .tab-list {
    display: inline-flex;
    gap: 30rpx;
  }
  .tab-item {
    position: relative;
    padding: 10rpx 20rpx;
    font-size: 28rpx;
    color: #666;
    
    &.active {
      color: #667eea;
      font-weight: 500;
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 40rpx;
        height: 6rpx;
        background: #667eea;
        border-radius: 3rpx;
      }
    }
    .count-badge {
      position: absolute;
      top: -10rpx;
      right: -15rpx;
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
}

.applications-list {
  padding: 0 20rpx;
  
  .application-item {
    margin-bottom: 20rpx;
    .application-card {
      background: #fff;
      border-radius: 20rpx;
      display: flex;
      overflow: hidden;
    }
    .activity-image {
      width: 180rpx;
      height: 120rpx;
      margin-right: 20rpx;
    }
    .application-info {
      flex: 1;
      padding: 20rpx;
      position: relative;
      
      .activity-title {
        font-size: 30rpx;
        font-weight: bold;
        color: #333;
        margin-bottom: 10rpx;
      }
      .activity-time {
        font-size: 26rpx;
        color: #666;
        margin-bottom: 10rpx;
      }
      .application-status {
        position: absolute;
        top: 20rpx;
        right: 20rpx;
        font-size: 22rpx;
        padding: 4rpx 12rpx;
        border-radius: 12rpx;
        
        &.status-applied { background-color: #fff3e0; color: #ff9800; }
        &.status-completed { background-color: #e8f5e8; color: #4caf50; }
        &.status-cancelled { background-color: #ffebee; color: #f44336; }
        &.status-processing { background-color: #e3f2fd; color: #2196f3; }
      }
      .application-meta {
        display: flex;
        flex-direction: column;
        gap: 8rpx;
        margin-bottom: 15rpx;
        .meta-item {
          font-size: 24rpx;
          color: #999;
        }
      }
      .application-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 15rpx;
        
        .action-btn {
          flex: 1;
          height: 60rpx;
          border: none;
          border-radius: 30rpx;
          font-size: 24rpx;
          padding: 0 20rpx;
          line-height: 60rpx;
          
          &.primary { background-color: #667eea; color: #fff; }
          &.secondary { background-color: #f0f0f0; color: #666; }
          &.success { background-color: #4caf50; color: #fff; }
          &.signin { background-color: #1989fa; color: #fff; }
          &.signout { background-color: #ff976a; color: #fff; }
        }
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 30rpx;
  
  .empty-img {
    width: 200rpx;
    height: 200rpx;
    margin-bottom: 30rpx;
  }
  .empty-text {
    font-size: 32rpx;
    color: #999;
    margin-bottom: 10rpx;
  }
  .empty-subtext {
    font-size: 26rpx;
    color: #ccc;
    margin-bottom: 30rpx;
  }
  .go-activity-btn {
    background: #667eea;
    color: #fff;
    border: none;
    border-radius: 40rpx;
    padding: 20rpx 40rpx;
    font-size: 28rpx;
  }
}
</style>