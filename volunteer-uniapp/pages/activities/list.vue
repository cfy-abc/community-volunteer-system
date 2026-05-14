<template>
  <view class="activities-container">
    <view class="search-bar">
      <view class="search-input" @tap="searchActivity">
        <text class="iconfont icon-search"></text>
        <text class="placeholder">搜索活动</text>
      </view>
    </view>

    <view class="filter-bar">
      <scroll-view scroll-x class="filter-scroll">
        <view class="filter-tags">
          <view
            class="filter-tag"
            :class="{ active: currentCategory === tag.value }"
            v-for="tag in categories"
            :key="tag.value"
            @tap="selectCategory(tag.value)"
          >
            {{ tag.label }}
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="activities-list">
      <view
        class="activity-item"
        v-for="activity in filteredActivities"
        :key="activity.id"
        @tap="viewActivity(activity.id)"
      >
        <view class="activity-card">
          <image :src="activity.coverImage || '/static/images/default-activity.jpg'" class="activity-cover" mode="aspectFill"></image>
          <view class="activity-content">
            <view class="activity-header">
              <text class="activity-title">{{ activity.title }}</text>
              <view class="activity-status" :class="getStatusClass(activity.status)">
                {{ getStatusText(activity.status) }}
              </view>
            </view>
            <view class="activity-meta">
              <view class="meta-item">
                <text class="iconfont icon-time"></text>
                <text class="meta-text">{{ formatDate(activity.startTime) }}</text>
              </view>
              <view class="meta-item">
                <text class="iconfont icon-location"></text>
                <text class="meta-text">{{ activity.location }}</text>
              </view>
              <view class="meta-item">
                <text class="iconfont icon-users"></text>
                <text class="meta-text">{{ activity.appliedCount }}/{{ activity.maxParticipants }}人</text>
              </view>
            </view>
            <view class="activity-desc">
              <text>{{ activity.description }}</text>
            </view>
            <view class="activity-footer">
              <view class="tags">
                <view class="tag" v-for="tag in activity.tags.slice(0, 3)" :key="tag">{{ tag }}</view>
              </view>
              <button
                class="apply-btn"
                :class="{ disabled: !canApply(activity) }"
                @tap.stop="applyActivity(activity)"
              >
                {{ getApplyButtonText(activity) }}
              </button>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="load-more" v-if="hasMore">
      <text class="loading-text" v-if="loadingMore">加载中...</text>
      <text class="load-text" v-else @tap="loadMore">点击加载更多</text>
    </view>

    <view class="empty-state" v-if="filteredActivities.length === 0">
      <image src="/static/images/empty-activities.png" class="empty-img"></image>
      <text class="empty-text">暂无活动</text>
      <text class="empty-subtext">当前筛选条件下没有找到活动</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'

const activities = ref([])
const currentCategory = ref('all')
const loadingMore = ref(false)
const hasMore = ref(true)
const page = ref(1)
const pageSize = 10

const categories = [
  { label: '全部', value: 'all' },
  { label: '环保', value: 'environment' },
  { label: '教育', value: 'education' },
  { label: '医疗', value: 'medical' },
  { label: '社区', value: 'community' },
  { label: '文化', value: 'culture' }
]

const filteredActivities = computed(() => {
  if (currentCategory.value === 'all') return activities.value
  return activities.value.filter(activity => {
    const tags = activity.tags || []
    return tags.some(tag => tag.toLowerCase().includes(currentCategory.value.toLowerCase()))
  })
})

function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  try { return JSON.parse(tags) } catch (e) { return [] }
}

const mapActivity = (a) => ({
  ...a,
  id: a.activityId,
  coverImage: a.poster || '/static/images/default-activity.jpg',
  appliedCount: a.currentParticipants || 0,
  tags: parseTags(a.tags)
})

const loadData = async () => {
  try {
    const res = await request.get('/activities', { page: page.value, size: pageSize })
    if (res.code === 200 && res.data) {
      activities.value = (res.data.list || []).map(mapActivity)
      hasMore.value = activities.value.length < res.data.total
    }
  } catch (err) {
    uni.showToast({ title: '加载活动失败', icon: 'none' })
  }
}

const selectCategory = (category) => { currentCategory.value = category }

const viewActivity = (id) => uni.navigateTo({ url: `/pages/activities/detail?id=${id}` })

const applyActivity = async (activity) => {
  if (!canApply(activity)) {
    uni.showToast({ title: getApplyButtonText(activity), icon: 'none' })
    return
  }
  if (!uni.getStorageSync('token')) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => uni.navigateTo({ url: '/pages/auth/login' }), 1000)
    return
  }
  uni.showModal({
    title: '确认报名',
    content: `确定要报名参加"${activity.title}"活动吗？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        const apiRes = await request.post(`/activities/${activity.id}/register`)
        if (apiRes.code === 200) {
          activity.appliedCount++
          uni.showToast({ title: '报名成功', icon: 'success' })
        } else {
          uni.showToast({ title: apiRes.msg || '报名失败', icon: 'none' })
        }
      } catch (err) { uni.showToast({ title: '网络错误', icon: 'none' }) }
    }
  })
}

const canApply = (activity) => activity.status === 1 && activity.appliedCount < activity.maxParticipants

const getApplyButtonText = (activity) => {
  if (activity.status !== 1) return '已结束'
  if (activity.appliedCount >= activity.maxParticipants) return '已满员'
  return '报名参加'
}

const getStatusText = (status) => {
  const statusMap = { 1: '招募中', 2: '进行中', 3: '已结束', 0: '已取消', 4: '审核中' }
  return statusMap[status] || '未知'
}

const getStatusClass = (status) => {
  const classMap = { 1: 'status-open', 2: 'status-open', 3: 'status-closed', 0: 'status-closed', 4: 'status-coming' }
  return classMap[status] || 'status-default'
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`
}

const searchActivity = () => uni.navigateTo({ url: '/pages/activities/search' })

const loadMore = async () => {
  loadingMore.value = true
  page.value++
  try {
    const res = await request.get('/activities', { page: page.value, size: pageSize })
    if (res.code === 200 && res.data) {
      const newItems = (res.data.list || []).map(mapActivity)
      activities.value = [...activities.value, ...newItems]
      hasMore.value = activities.value.length < res.data.total
    }
  } catch (err) {
    hasMore.value = false
  } finally {
    loadingMore.value = false
  }
}

onMounted(() => { loadData() })
</script>

<style lang="scss">
.activities-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 20rpx;
}

.search-bar {
  padding: 20rpx 30rpx;
  background-color: #fff;

  .search-input {
    display: flex;
    align-items: center;
    padding: 15rpx 20rpx;
    background-color: #f0f0f0;
    border-radius: 30rpx;

    .icon-search {
      font-size: 32rpx;
      color: #666;
      margin-right: 10rpx;
    }

    .placeholder {
      font-size: 26rpx;
      color: #999;
    }
  }
}

.filter-bar {
  background-color: #fff;
  padding: 20rpx 0;

  .filter-scroll {
    white-space: nowrap;
    padding: 0 30rpx;
  }

  .filter-tags {
    display: inline-flex;
    gap: 20rpx;
  }

  .filter-tag {
    padding: 10rpx 20rpx;
    border-radius: 20rpx;
    font-size: 26rpx;
    color: #666;
    border: 1rpx solid #ddd;

    &.active {
      background: #667eea;
      color: #fff;
      border-color: #667eea;
    }
  }
}

.activities-list {
  padding: 0 20rpx;

  .activity-item {
    margin-bottom: 20rpx;

    .activity-card {
      background: #fff;
      border-radius: 20rpx;
      overflow: hidden;
    }

    .activity-cover {
      width: 100%;
      height: 200rpx;
    }

    .activity-content {
      padding: 20rpx;
    }

    .activity-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 15rpx;

      .activity-title {
        font-size: 32rpx;
        font-weight: bold;
        color: #333;
        flex: 1;
        margin-right: 15rpx;
      }

      .activity-status {
        font-size: 22rpx;
        padding: 4rpx 12rpx;
        border-radius: 12rpx;

        &.status-open {
          background-color: #e8f5e8;
          color: #4caf50;
        }

        &.status-closed {
          background-color: #ffebee;
          color: #f44336;
        }

        &.status-coming {
          background-color: #fff3e0;
          color: #ff9800;
        }
      }
    }

    .activity-meta {
      display: flex;
      flex-direction: column;
      gap: 10rpx;
      margin-bottom: 15rpx;

      .meta-item {
        display: flex;
        align-items: center;
        font-size: 24rpx;
        color: #666;

        .iconfont {
          margin-right: 8rpx;
          font-size: 22rpx;
        }
      }
    }

    .activity-desc {
      font-size: 26rpx;
      color: #666;
      line-height: 1.5;
      margin-bottom: 15rpx;
    }

    .activity-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .tags {
        display: flex;
        gap: 10rpx;

        .tag {
          font-size: 20rpx;
          color: #667eea;
          background-color: #f0f0ff;
          padding: 4rpx 12rpx;
          border-radius: 12rpx;
        }
      }

      .apply-btn {
        padding: 10rpx 20rpx;
        background: #667eea;
        color: #fff;
        border: none;
        border-radius: 20rpx;
        font-size: 24rpx;

        &.disabled {
          background-color: #ccc;
        }
      }
    }
  }
}

.load-more {
  text-align: center;
  padding: 30rpx;

  .loading-text {
    font-size: 26rpx;
    color: #666;
  }

  .load-text {
    font-size: 26rpx;
    color: #667eea;
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
  }
}
</style>
