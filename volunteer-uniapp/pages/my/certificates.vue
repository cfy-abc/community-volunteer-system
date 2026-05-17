<template>
  <view class="certificates-container">
    <!-- 页面标题 -->
    <view class="page-header">
      <view class="header-left">
        <text class="title">我的证书</text>
      </view>
      <view class="header-right">
        <text class="total-count">共{{ certificates.length }}张</text>
      </view>
    </view>

    <!-- 证书分类 -->
    <view class="category-section">
      <view class="category-list">
        <view 
          class="category-item" 
          :class="{ active: currentCategory === item.key }"
          v-for="item in categories" 
          :key="item.key"
          @tap="selectCategory(item.key)"
        >
          <text class="category-icon" :class="item.icon"></text>
          <text class="category-name">{{ item.name }}</text>
          <text class="category-count">{{ getCategoryCount(item.key) }}</text>
        </view>
      </view>
    </view>

    <!-- 证书列表 -->
    <view class="certificates-list">
      <view 
        class="certificate-item" 
        v-for="cert in filteredCertificates" 
        :key="cert.id"
        @tap="viewCertificate(cert.id)"
      >
        <view class="certificate-card">
          <view class="certificate-header">
            <view class="cert-type">
              <text class="cert-type-text">{{ cert.type }}</text>
            </view>
            <text class="cert-status" :class="getStatusClass(cert.status)">{{ getStatusText(cert.status) }}</text>
          </view>
          
          <view class="certificate-body">
            <view class="cert-title">{{ cert.title }}</view>
            <view class="cert-meta">
              <text class="meta-item">颁发日期：{{ formatDate(cert.issueDate) }}</text>
              <text class="meta-item">服务时长：{{ cert.duration }}小时</text>
            </view>
            <view class="cert-description">{{ cert.description }}</view>
          </view>
          
          <view class="certificate-footer">
            <text class="issue-org">颁发机构：{{ cert.issuer }}</text>
            <view class="cert-actions">
              <button class="action-btn" @tap.stop="previewCertificate(cert.id)">预览</button>
              <button class="action-btn" @tap.stop="downloadCertificate(cert.id)">下载</button>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-if="filteredCertificates.length === 0">
      <image src="/static/images/empty-certificates.png" class="empty-img"></image>
      <text class="empty-text">暂无证书</text>
      <text class="empty-subtext">您还没有获得任何证书</text>
      <button class="go-activity-btn" @tap="goToActivities">去参加活动</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'

const certificates = ref([])
const currentCategory = ref('all')

const categories = [
  { key: 'all', name: '全部', icon: 'icon-all' },
  { key: 'volunteer', name: '志愿证书', icon: 'icon-volunteer' },
  { key: 'training', name: '培训证书', icon: 'icon-training' },
  { key: 'achievement', name: '成就证书', icon: 'icon-achievement' }
]

const filteredCertificates = computed(() => {
  if (currentCategory.value === 'all') return certificates.value
  return certificates.value.filter(cert => cert.category === currentCategory.value)
})

const loadData = async () => {
  try {
    const res = await request.get('/api/users/applications', { status: 'completed', page: 1, size: 20 })
    if (res.code === 200 && res.data) {
      certificates.value = (res.data.list || []).map(item => ({
        id: item.recordId,
        type: '志愿证书',
        title: (item.activityTitle || '活动') + '证书',
        description: '完成志愿服务',
        issueDate: item.checkOutTime || item.registerTime || '',
        issuer: '社区志愿服务管理系统',
        duration: item.hoursEarned || 0,
        status: 'valid',
        category: 'volunteer'
      }))
    }
  } catch (err) {
    console.error(err)
  }
}

const selectCategory = (category) => {
  currentCategory.value = category
}

const getCategoryCount = (category) => {
  if (category === 'all') {
    return certificates.value.length
  }
  return certificates.value.filter(cert => cert.category === category).length
}

const getStatusText = (status) => {
  const statusMap = {
    'valid': '有效',
    'expired': '已过期',
    'revoked': '已撤销'
  }
  return statusMap[status] || '未知'
}

const getStatusClass = (status) => {
  const classMap = {
    'valid': 'status-valid',
    'expired': 'status-expired',
    'revoked': 'status-revoked'
  }
  return classMap[status] || 'status-default'
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

const viewCertificate = (id) => {
  uni.showToast({ title: '证书详情功能开发中', icon: 'none' })
}

const previewCertificate = (id) => {
  uni.showModal({
    title: '证书预览',
    content: '证书预览功能开发中',
    showCancel: false
  })
}

const downloadCertificate = (id) => {
  uni.showLoading({
    title: '下载中...'
  })
  
  // 模拟下载过程
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
  uni.switchTab({
    url: '/pages/activities/list'
  })
}

// 页面生命周期
onMounted(() => {
  loadData()
})
</script>

<style lang="scss">
.certificates-container {
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
  
  .header-left {
    .title {
      font-size: 36rpx;
      font-weight: bold;
      color: #333;
    }
  }
  
  .header-right {
    .total-count {
      font-size: 26rpx;
      color: #666;
    }
  }
}

.category-section {
  background-color: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
  
  .category-list {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20rpx;
  }
  
  .category-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20rpx 10rpx;
    border-radius: 12rpx;
    border: 2rpx solid #f0f0f0;
    
    &.active {
      border-color: #667eea;
      background-color: #f0f0ff;
    }
    
    .category-icon {
      font-size: 36rpx;
      color: #667eea;
      margin-bottom: 10rpx;
    }
    
    .category-name {
      font-size: 24rpx;
      color: #333;
      margin-bottom: 5rpx;
    }
    
    .category-count {
      font-size: 22rpx;
      color: #666;
      background-color: #f0f0f0;
      padding: 2rpx 10rpx;
      border-radius: 10rpx;
    }
  }
}

.certificates-list {
  padding: 0 20rpx;
  
  .certificate-item {
    margin-bottom: 20rpx;
    
    .certificate-card {
      background: #fff;
      border-radius: 20rpx;
      padding: 30rpx;
      border: 2rpx solid #f0f0f0;
    }
    
    .certificate-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20rpx;
      
      .cert-type {
        .cert-type-text {
          font-size: 24rpx;
          color: #667eea;
          background-color: #f0f0ff;
          padding: 4rpx 12rpx;
          border-radius: 12rpx;
        }
      }
      
      .cert-status {
        font-size: 22rpx;
        padding: 4rpx 12rpx;
        border-radius: 12rpx;
        
        &.status-valid {
          background-color: #e8f5e8;
          color: #4caf50;
        }
        
        &.status-expired {
          background-color: #ffebee;
          color: #f44336;
        }
        
        &.status-revoked {
          background-color: #ffcdd2;
          color: #d32f2f;
        }
      }
    }
    
    .certificate-body {
      margin-bottom: 20rpx;
      
      .cert-title {
        font-size: 32rpx;
        font-weight: bold;
        color: #333;
        margin-bottom: 15rpx;
      }
      
      .cert-meta {
        display: flex;
        flex-direction: column;
        gap: 8rpx;
        margin-bottom: 15rpx;
        
        .meta-item {
          font-size: 24rpx;
          color: #666;
        }
      }
      
      .cert-description {
        font-size: 26rpx;
        color: #666;
        line-height: 1.5;
      }
    }
    
    .certificate-footer {
      border-top: 1rpx solid #f0f0f0;
      padding-top: 20rpx;
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .issue-org {
        font-size: 24rpx;
        color: #999;
      }
      
      .cert-actions {
        display: flex;
        gap: 15rpx;
        
        .action-btn {
          padding: 10rpx 20rpx;
          background-color: #f0f0f0;
          color: #666;
          border: none;
          border-radius: 20rpx;
          font-size: 24rpx;
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