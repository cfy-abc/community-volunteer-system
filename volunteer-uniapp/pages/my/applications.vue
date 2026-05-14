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

// 标签配置
const tabs = ref([
  { key: 'all', label: '全部', count: 0 },
  { key: 'applied', label: '待参加', count: 0 },
  { key: 'completed', label: '已完成', count: 0 },
  { key: 'cancelled', label: '已取消', count: 0 }
])

// 计算属性
const filteredApplications = computed(() => {
  if (currentTab.value === 'all') return applications.value
  return applications.value.filter(app => app.status === currentTab.value)
})

// 从后端获取报名列表
const fetchApplications = async () => {
  loading.value = true
  try {
    // 假设后端接口为 GET /user/applications
    const res = await request.get('/user/applications')
    if (res.code === 200) {
      applications.value = res.data || []
      updateTabCounts()
    } else {
      uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
      // 降级使用模拟数据（仅开发测试）
      if (process.env.NODE_ENV === 'development') loadMockData()
    }
  } catch (err) {
    console.error(err)
    uni.showToast({ title: '网络错误，使用模拟数据', icon: 'none' })
    if (process.env.NODE_ENV === 'development') loadMockData()
  } finally {
    loading.value = false
  }
}


// 例如带查询参数
//const res = await request.get('/user/applications', { page: 1, size: 10 })

// 模拟数据（仅供开发）
const loadMockData = () => {
  applications.value = [
    {
      id: 1,
      activityId: 1,
      activityTitle: '社区环保清洁活动',
      activityTime: '2024-01-15 09:00',
      applyTime: '2024-01-10 14:30',
      status: 'completed',
      duration: 3,
      certificateId: 'cert_001',
      activityCover: '/static/images/activity1.jpg',
      activityStatus: 2,       // 活动状态: 1进行中 2已结束
      signStatus: 'checked_out' // 签到状态: not_start, checked_in, checked_out
    },
    {
      id: 2,
      activityId: 2,
      activityTitle: '敬老院慰问活动',
      activityTime: '2024-01-16 14:00',
      applyTime: '2024-01-11 10:15',
      status: 'applied',
      duration: 2,
      activityCover: '/static/images/activity2.jpg',
      activityStatus: 1,
      signStatus: 'not_start'
    },
    {
      id: 3,
      activityId: 3,
      activityTitle: '儿童图书馆助教',
      activityTime: '2024-01-17 10:00',
      applyTime: '2024-01-12 16:45',
      status: 'completed',
      duration: 4,
      certificateId: 'cert_002',
      activityCover: '/static/images/activity3.jpg',
      activityStatus: 2,
      signStatus: 'checked_out'
    },
    {
      id: 4,
      activityId: 4,
      activityTitle: '医院导诊服务',
      activityTime: '2024-01-18 08:00',
      applyTime: '2024-01-13 09:20',
      status: 'applied',
      duration: 3,
      activityCover: '/static/images/activity4.jpg',
      activityStatus: 1,
      signStatus: 'checked_in'   // 已签到但未签退
    },
    {
      id: 5,
      activityId: 5,
      activityTitle: '文化广场文艺演出',
      activityTime: '2024-01-19 19:00',
      applyTime: '2024-01-14 13:10',
      status: 'cancelled',
      duration: 0,
      activityCover: '/static/images/activity5.jpg',
      activityStatus: 2,
      signStatus: 'not_start'
    }
  ]
  updateTabCounts()
}

// 更新各标签页的数量统计
const updateTabCounts = () => {
  const counts = {
    all: applications.value.length,
    applied: applications.value.filter(app => app.status === 'applied').length,
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

// 状态文本和样式
const getStatusText = (status) => {
  const statusMap = {
    'applied': '待参加',
    'completed': '已完成',
    'cancelled': '已取消',
    'processing': '处理中'
  }
  return statusMap[status] || '未知'
}

const getStatusClass = (status) => {
  const classMap = {
    'applied': 'status-applied',
    'completed': 'status-completed',
    'cancelled': 'status-cancelled',
    'processing': 'status-processing'
  }
  return classMap[status] || 'status-default'
}

// 按钮显示控制
const canViewDetails = (status) => status === 'applied' || status === 'completed'
const canCancel = (status) => status === 'applied'
const canDownloadCertificate = (status) => status === 'completed'

// 签到按钮显示条件：活动进行中（activityStatus === 1）且报名状态为待参加（applied）且未签到（signStatus !== 'checked_in'）
const showSignButton = (app) => {
  return app.status === 'applied' && app.activityStatus === 1 && app.signStatus !== 'checked_in'
}

// 签退按钮显示条件：活动进行中（activityStatus === 1）且报名状态为待参加（applied）且已签到（signStatus === 'checked_in'）
const showCheckoutButton = (app) => {
  return app.status === 'applied' && app.activityStatus === 1 && app.signStatus === 'checked_in'
}

// 跳转到签到/签退页面
const goToSign = (activityId, type) => {
  uni.navigateTo({ url: `/pages/activity/sign?id=${activityId}&type=${type}` })
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
          const result = await request.post(`/applications/${applicationId}/cancel`)
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