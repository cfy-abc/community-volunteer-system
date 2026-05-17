<template>
  <view class="container">
    <view class="top-bar">
      <text class="back" @tap="goBack">返回</text>
      <text class="title">组织管理</text>
      <text class="spacer"></text>
    </view>

    <view class="filter-tabs">
      <view class="tab" :class="{ active: filterStatus === null }" @tap="filterBy(null)">全部</view>
      <view class="tab" :class="{ active: filterStatus === 0 }" @tap="filterBy(0)">待审核</view>
      <view class="tab" :class="{ active: filterStatus === 1 }" @tap="filterBy(1)">已通过</view>
      <view class="tab" :class="{ active: filterStatus === 2 }" @tap="filterBy(2)">已拒绝</view>
    </view>

    <view class="list">
      <view class="list-item" v-for="o in organizations" :key="o.orgId">
        <view class="item-info">
          <text class="name">{{ o.orgName }}</text>
          <text class="detail">{{ o.description }}</text>
          <text class="detail">电话: {{ o.contactPhone }} | 邮箱: {{ o.contactEmail || '无' }}</text>
        </view>
        <view class="item-status">
          <text class="status-tag" :class="getStatusClass(o.status)">{{ getStatusText(o.status) }}</text>
          <view class="actions" v-if="o.status === 0">
            <button class="btn pass" @tap="auditOrg(o.orgId, 1)">通过</button>
            <button class="btn reject" @tap="auditOrg(o.orgId, 2)">拒绝</button>
          </view>
        </view>
      </view>
    </view>

    <view class="empty" v-if="organizations.length === 0">
      <text>暂无数据</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const organizations = ref([])
const filterStatus = ref(null)

const loadOrganizations = async () => {
  try {
    const res = await request.get('/api/organizations')
    if (res.code === 200) {
      let list = res.data || []
      if (filterStatus.value !== null) {
        list = list.filter(o => o.status === filterStatus.value)
      }
      organizations.value = list
    }
  } catch (err) { uni.showToast({ title: '加载失败', icon: 'none' }) }
}

const filterBy = (status) => {
  filterStatus.value = status
  loadOrganizations()
}

const auditOrg = async (orgId, status) => {
  try {
    const res = await request.put(`/admin/organizations/${orgId}/status`, { status })
    if (res.code === 200) {
      uni.showToast({ title: '审核成功', icon: 'success' })
      loadOrganizations()
    } else {
      uni.showToast({ title: res.msg || '操作失败', icon: 'none' })
    }
  } catch (err) { uni.showToast({ title: '网络错误', icon: 'none' }) }
}

const getStatusText = (s) => ({ 0: '待审核', 1: '已通过', 2: '已拒绝' })[s] || '未知'
const getStatusClass = (s) => ({ 0: 'pending', 1: 'pass', 2: 'reject' })[s] || ''
const goBack = () => uni.navigateBack()

onMounted(() => loadOrganizations())
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
  .tab {
    padding: 12rpx 24rpx; border-radius: 20rpx; font-size: 26rpx;
    color: #666; background: #f0f0f0;
    &.active { background: #1a1a2e; color: #fff; }
  }
}
.list {
  padding: 24rpx;
  .list-item {
    display: flex; justify-content: space-between; align-items: flex-start;
    background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx;
    .item-info {
      flex: 1;
      .name { font-size: 30rpx; font-weight: bold; color: #333; }
      .detail { display: block; font-size: 24rpx; color: #999; margin-top: 6rpx; line-height: 1.5; }
    }
    .item-status {
      display: flex; flex-direction: column; align-items: flex-end; gap: 12rpx;
      .status-tag { font-size: 22rpx; padding: 4rpx 12rpx; border-radius: 10rpx; }
      .status-tag.pending { background: #fff3e0; color: #ff9800; }
      .status-tag.pass { background: #e8f5e8; color: #4caf50; }
      .status-tag.reject { background: #ffebee; color: #f44336; }
      .actions { display: flex; gap: 8rpx; }
      .btn {
        padding: 8rpx 20rpx; border-radius: 12rpx; font-size: 24rpx; border: none;
        &.pass { background: #4caf50; color: #fff; }
        &.reject { background: #f44336; color: #fff; }
      }
    }
  }
}
.empty { text-align: center; padding: 80rpx 0; color: #999; font-size: 28rpx; }
</style>
