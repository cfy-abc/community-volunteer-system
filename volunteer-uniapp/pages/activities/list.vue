<template>
  <view class="activities-container">
    <view class="search-bar">
      <view class="search-input" v-if="!showSearchInput" @tap="searchActivity">
        <text class="search-icon">&#128269;</text>
        <text class="placeholder">搜索活动</text>
      </view>
      <view class="search-input active" v-else>
        <text class="search-icon">&#128269;</text>
        <input v-model="searchKeyword" class="search-field" placeholder="输入关键词搜索" confirm-type="search" @confirm="onSearchConfirm" />
        <text class="search-cancel" @tap="searchActivity">取消</text>
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
              <view class="header-badges">
                <text class="publisher-tag" :class="activity.orgId > 0 ? 'tag-org' : 'tag-personal'">
                  {{ activity.orgId > 0 ? '组织' : '个人' }}
                </text>
                <view class="activity-status" :class="getStatusClass(activity.status)">
                  {{ getStatusText(activity.status) }}
                </view>
              </view>
            </view>
            <view class="activity-meta">
              <view class="meta-item">
                <text class="meta-icon">&#128197;</text>
                <text class="meta-text">{{ formatDate(activity.startTime) }}</text>
              </view>
              <view class="meta-item">
                <text class="meta-icon">&#128205;</text>
                <text class="meta-text">{{ activity.location }}</text>
              </view>
              <view class="meta-item">
                <text class="meta-icon">&#128101;</text>
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
const registeredActivityIds = ref(new Set())
const currentCategory = ref('all')
const loadingMore = ref(false)
const hasMore = ref(true)
const page = ref(1)
const pageSize = 10

const categories = [
  { label: '全部', value: 'all' },
  { label: '环保', value: '环保' },
  { label: '敬老', value: '敬老' },
  { label: '教育', value: '教育' },
  { label: '医疗', value: '医疗' },
  { label: '社区', value: '社区' },
  { label: '文化', value: '文化' }
]

const filteredActivities = computed(() => {
  if (currentCategory.value === 'all') return activities.value
  return activities.value.filter(activity => {
    const tags = activity.tags || []
    return tags.some(tag => tag.includes(currentCategory.value))
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
    const params = { page: page.value, size: pageSize }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    const res = await request.get('/activities', params)
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

const applyActivity = (activity) => {
  const id = activity.id || activity.activityId
  if (registeredActivityIds.value.has(id)) {
    uni.showToast({ title: '已报名', icon: 'none' })
    return
  }
  if (!canApply(activity)) {
    uni.showToast({ title: getApplyButtonText(activity), icon: 'none' })
    return
  }
  if (!uni.getStorageSync('token')) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => uni.navigateTo({ url: '/pages/auth/login' }), 1000)
    return
  }
  // 检查实名认证
  request.get('/api/users/verify-status').then(verifyRes => {
    if (verifyRes.code === 200 && verifyRes.data) {
      const rns = verifyRes.data.realNameStatus
      if (rns === 0 || rns == null) {
        uni.showModal({
          title: '需要实名认证',
          content: '报名活动需要先完成实名认证，是否前往认证？',
          success: (modalRes) => {
            if (modalRes.confirm) uni.navigateTo({ url: '/pages/auth/verify-identity' })
          }
        })
        return
      } else if (rns === 2) {
        uni.showToast({ title: '实名认证审核中，请等待通过后再报名', icon: 'none', duration: 2500 })
        return
      }
    }
    // 跳转到报名申请表
    uni.navigateTo({ url: `/pages/activities/apply?id=${id}` })
  }).catch(() => {
    uni.navigateTo({ url: `/pages/activities/apply?id=${id}` })
  })
}

const canApply = (activity) => {
  const id = activity.id || activity.activityId
  return activity.status === 1
    && activity.appliedCount < activity.maxParticipants
    && !registeredActivityIds.value.has(id)
}

const getApplyButtonText = (activity) => {
  const id = activity.id || activity.activityId
  if (activity.status !== 1) return '已结束'
  if (registeredActivityIds.value.has(id)) return '已报名'
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

const searchKeyword = ref('')
const showSearchInput = ref(false)

const searchActivity = () => {
  showSearchInput.value = !showSearchInput.value
  if (!showSearchInput.value) {
    searchKeyword.value = ''
    page.value = 1
    loadData()
  }
}

const onSearchConfirm = () => {
  page.value = 1
  loadData()
}

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

// 加载已报名的活动ID集合
const loadMyRegistrations = async () => {
  if (!uni.getStorageSync('token')) return
  try {
    const res = await request.get('/api/users/applications', { page: 1, size: 500 })
    if (res.code === 200 && res.data) {
      (res.data.list || []).forEach(item => {
        registeredActivityIds.value.add(item.activityId)
      })
    }
  } catch (e) {}
}

onMounted(() => { loadData(); loadMyRegistrations() })
</script>

<style lang="scss" scoped>
.activities-container {
  min-height: 100vh;
  background: #f0f2f5;
  padding-bottom: 40rpx;
}

// 搜索栏
.search-bar {
  padding: 20rpx 24rpx;
  background: linear-gradient(160deg, #5b6abf, #7b5ea7);
  .search-input {
    display: flex;
    align-items: center;
    height: 72rpx;
    padding: 0 20rpx;
    background: rgba(255,255,255,0.2);
    border-radius: 36rpx;
    backdrop-filter: blur(10rpx);
    .search-icon { font-size: 30rpx; color: rgba(255,255,255,0.7); margin-right: 10rpx; }
    .placeholder { font-size: 26rpx; color: rgba(255,255,255,0.6); }
    &.active {
      background: #fff;
      .search-icon { color: #999; }
    }
    .search-field { flex: 1; font-size: 26rpx; color: #333; }
    .search-cancel { font-size: 26rpx; color: #fff; padding-left: 16rpx; flex-shrink: 0; }
  }
}

// 分类筛选
.filter-bar {
  background: #fff;
  padding: 16rpx 0;
  .filter-scroll { white-space: nowrap; padding: 0 24rpx; }
  .filter-tags { display: inline-flex; gap: 16rpx; }
  .filter-tag {
    padding: 10rpx 24rpx;
    border-radius: 24rpx;
    font-size: 24rpx;
    color: #888;
    background: #f5f5f5;
    transition: all 0.2s;
    &.active {
      background: linear-gradient(135deg, #667eea, #7b5ea7);
      color: #fff;
    }
  }
}

// 活动列表
.activities-list {
  padding: 16rpx 24rpx;
  .activity-item { margin-bottom: 20rpx; }
  .activity-card {
    background: #fff;
    border-radius: 20rpx;
    overflow: hidden;
    box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.04);
  }
  .activity-cover { width: 100%; height: 220rpx; }
  .activity-content { padding: 20rpx 24rpx 24rpx; }
  .activity-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12rpx;
    .activity-title {
      font-size: 32rpx;
      font-weight: 700;
      color: #222;
      flex: 1;
      margin-right: 16rpx;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .header-badges { display: flex; gap: 8rpx; align-items: center; flex-shrink: 0; }
    .publisher-tag {
      font-size: 20rpx;
      padding: 4rpx 12rpx;
      border-radius: 12rpx;
      &.tag-org { background: #e3f2fd; color: #1976d2; }
      &.tag-personal { background: #f3e5f5; color: #7b1fa2; }
    }
    .activity-status {
      font-size: 22rpx;
      padding: 4rpx 14rpx;
      border-radius: 16rpx;
      flex-shrink: 0;
      &.status-open { background: #e8f5e9; color: #388e3c; }
      &.status-closed { background: #ffebee; color: #d32f2f; }
      &.status-coming { background: #fff3e0; color: #f57c00; }
    }
  }
  .activity-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
    margin-bottom: 12rpx;
    .meta-item {
      display: flex;
      align-items: center;
      font-size: 24rpx;
      color: #888;
      .meta-icon { margin-right: 6rpx; font-size: 22rpx; }
    }
  }
  .activity-desc {
    font-size: 26rpx;
    color: #888;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin-bottom: 16rpx;
  }
  .activity-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .tags { display: flex; gap: 10rpx; }
    .tag {
      font-size: 20rpx;
      color: #667eea;
      background: #f0f0ff;
      padding: 4rpx 14rpx;
      border-radius: 14rpx;
    }
    .apply-btn {
      padding: 12rpx 28rpx;
      background: linear-gradient(135deg, #667eea, #7b5ea7);
      color: #fff;
      border: none;
      border-radius: 24rpx;
      font-size: 26rpx;
      font-weight: 500;
      &.disabled { background: #ddd; color: #999; }
    }
  }
}

.load-more {
  text-align: center;
  padding: 24rpx;
  font-size: 26rpx;
  color: #667eea;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 30rpx;
  .empty-img { width: 200rpx; height: 200rpx; margin-bottom: 30rpx; opacity: 0.5; }
  .empty-text { font-size: 32rpx; color: #999; margin-bottom: 10rpx; }
  .empty-subtext { font-size: 24rpx; color: #ccc; }
}
</style>
