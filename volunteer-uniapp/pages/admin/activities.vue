<template>
  <view class="container">
    <view class="top-bar">
      <text class="back" @tap="goBack"> 返回</text>
      <text class="title">活动管理</text>
      <text class="spacer"></text>
    </view>

    <view class="filter-tabs">
      <view class="tab" :class="{ active: filterStatus === null }" @tap="filterBy(null)">全部</view>
      <view class="tab" :class="{ active: filterStatus === 4 }" @tap="filterBy(4)">审核中</view>
      <view class="tab" :class="{ active: filterStatus === 1 }" @tap="filterBy(1)">招募中</view>
      <view class="tab" :class="{ active: filterStatus === 2 }" @tap="filterBy(2)">进行中</view>
      <view class="tab" :class="{ active: filterStatus === 3 }" @tap="filterBy(3)">已结束</view>
    </view>

    <view class="list">
      <view class="list-item" v-for="a in activities" :key="a.activityId" @tap="viewDetail(a.activityId)">
        <view class="item-info">
          <text class="name">{{ a.title }}</text>
          <text class="detail">地点: {{ a.location }} | 人数: {{ a.currentParticipants }}/{{ a.maxParticipants }}</text>
          <text class="detail">奖励: {{ a.rewardHours }}h | 状态: {{ getStatusText(a.status) }}</text>
        </view>
        <view class="item-actions" v-if="a.status === 4">
          <button class="btn pass" @tap="auditActivity(a.activityId, 1)">通过</button>
          <button class="btn reject" @tap="auditActivity(a.activityId, 0)">拒绝</button>
        </view>
      </view>
    </view>

    <view class="empty" v-if="activities.length === 0">
      <text>暂无数据</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const activities = ref([])
const filterStatus = ref(null)

const loadActivities = async () => {
  try {
    const params = { page: 1, size: 100 }
    if (filterStatus.value !== null) params.status = filterStatus.value
    const res = await request.get('/api/activities', params)
    if (res.code === 200) activities.value = res.data.list || []
  } catch (err) { uni.showToast({ title: '加载失败', icon: 'none' }) }
}

const filterBy = (status) => {
  filterStatus.value = status
  loadActivities()
}

const auditActivity = async (activityId, status) => {
  try {
    const res = await request.put(`/admin/activities/${activityId}/status`, { status })
    if (res.code === 200) {
      uni.showToast({ title: '审核成功', icon: 'success' })
      loadActivities()
    } else {
      uni.showToast({ title: res.msg || '操作失败', icon: 'none' })
    }
  } catch (err) { uni.showToast({ title: '网络错误', icon: 'none' }) }
}

const getStatusText = (s) => ({ 0: '已取消', 1: '招募中', 2: '进行中', 3: '已结束', 4: '审核中' })[s] || '未知'
const viewDetail = (id) => uni.navigateTo({ url: `/pages/activities/detail?id=${id}` })
const goBack = () => uni.navigateBack()

onMounted(() => loadActivities())
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
  display: flex; gap: 12rpx; padding: 24rpx; background: #fff; flex-wrap: wrap;
  .tab {
    padding: 12rpx 20rpx; border-radius: 20rpx; font-size: 24rpx;
    color: #666; background: #f0f0f0;
    &.active { background: #1a1a2e; color: #fff; }
  }
}
.list {
  padding: 24rpx;
  .list-item {
    display: flex; justify-content: space-between; align-items: center;
    background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx;
    .item-info {
      flex: 1;
      .name { font-size: 28rpx; font-weight: bold; color: #333; }
      .detail { display: block; font-size: 24rpx; color: #999; margin-top: 4rpx; }
    }
    .item-actions { display: flex; gap: 8rpx; }
    .btn {
      padding: 10rpx 24rpx; border-radius: 12rpx; font-size: 24rpx; border: none;
      &.pass { background: #4caf50; color: #fff; }
      &.reject { background: #f44336; color: #fff; }
    }
  }
}
.empty { text-align: center; padding: 80rpx 0; color: #999; font-size: 28rpx; }
</style>
