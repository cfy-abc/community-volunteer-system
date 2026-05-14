<template>
  <view class="container">
    <view class="top-bar">
      <text class="back" @tap="goBack">&#x2190; 返回</text>
      <text class="title">用户管理</text>
      <text class="spacer"></text>
    </view>

    <view class="filter-tabs">
      <view class="tab" :class="{ active: filterStatus === null }" @tap="filterBy(null)">全部</view>
      <view class="tab" :class="{ active: filterStatus === 0 }" @tap="filterBy(0)">待审核</view>
      <view class="tab" :class="{ active: filterStatus === 1 }" @tap="filterBy(1)">已通过</view>
      <view class="tab" :class="{ active: filterStatus === 2 }" @tap="filterBy(2)">已拒绝</view>
    </view>

    <view class="list" v-if="!loading">
      <view class="list-item" v-for="u in users" :key="u.userId">
        <view class="item-info">
          <text class="name">{{ u.realName }}</text>
          <text class="detail">账号: {{ u.username }} | 手机: {{ u.phone }}</text>
          <text class="detail">志愿时长: {{ u.volunteerHours || 0 }}h | 累计: {{ u.totalEarnedHours || 0 }}h</text>
        </view>
        <view class="item-status">
          <text class="status-tag" :class="getStatusClass(u.status)">{{ getStatusText(u.status) }}</text>
          <view class="actions" v-if="u.status === 0">
            <button class="btn pass" @tap="auditUser(u.userId, 1)">通过</button>
            <button class="btn reject" @tap="auditUser(u.userId, 2)">拒绝</button>
          </view>
        </view>
      </view>
      <view class="empty" v-if="users.length === 0">
        <text>暂无数据</text>
      </view>
    </view>

    <view class="loading-box" v-else><text>加载中...</text></view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const users = ref([])
const filterStatus = ref(null)
const loading = ref(true)

const loadUsers = async () => {
  loading.value = true
  try {
    const params = { page: 1, size: 50 }
    if (filterStatus.value !== null) params.status = filterStatus.value
    const res = await request.get('/admin/users', params)
    if (res.code === 200) {
      users.value = res.data.list || []
    } else {
      uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
    }
  } catch (err) {
    uni.showToast({ title: '网络连接失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const filterBy = (status) => {
  filterStatus.value = status
  loadUsers()
}

const auditUser = async (userId, status) => {
  try {
    const res = await request.put(`/admin/users/${userId}/status`, { status })
    if (res.code === 200) {
      uni.showToast({ title: '审核成功', icon: 'success' })
      loadUsers()
    } else {
      uni.showToast({ title: res.msg || '操作失败', icon: 'none' })
    }
  } catch (err) {
    uni.showToast({ title: '网络错误', icon: 'none' })
  }
}

const getStatusText = (s) => ({ 0: '待审核', 1: '已通过', 2: '已拒绝' })[s] || '未知'
const getStatusClass = (s) => ({ 0: 'pending', 1: 'pass', 2: 'reject' })[s] || ''
const goBack = () => uni.navigateBack()

onMounted(() => loadUsers())
</script>

<style lang="scss" scoped>
.container { min-height: 100vh; background: #f0f2f5; }
.top-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx 30rpx; background: #1a1a2e;
  .back, .spacer { width: 120rpx; font-size: 28rpx; color: rgba(255,255,255,0.7); }
  .title { font-size: 34rpx; font-weight: bold; color: #fff; }
}
.filter-tabs {
  display: flex; gap: 16rpx; padding: 24rpx; background: #fff;
  .tab { padding: 12rpx 24rpx; border-radius: 20rpx; font-size: 26rpx; color: #666; background: #f0f0f0; }
  .tab.active { background: #1a1a2e; color: #fff; }
}
.list { padding: 24rpx; }
.list-item {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx;
  .item-info { flex: 1; .name { font-size: 30rpx; font-weight: bold; color: #333; } .detail { display: block; font-size: 24rpx; color: #999; margin-top: 4rpx; } }
  .item-status { display: flex; flex-direction: column; align-items: flex-end; gap: 12rpx; }
  .status-tag { font-size: 22rpx; padding: 4rpx 12rpx; border-radius: 10rpx; }
  .status-tag.pending { background: #fff3e0; color: #ff9800; }
  .status-tag.pass { background: #e8f5e8; color: #4caf50; }
  .status-tag.reject { background: #ffebee; color: #f44336; }
  .actions { display: flex; gap: 8rpx; }
  .btn { padding: 8rpx 20rpx; border-radius: 12rpx; font-size: 24rpx; border: none; }
  .btn.pass { background: #4caf50; color: #fff; }
  .btn.reject { background: #f44336; color: #fff; }
}
.empty, .loading-box { text-align: center; padding: 80rpx 0; color: #999; font-size: 28rpx; }
</style>
