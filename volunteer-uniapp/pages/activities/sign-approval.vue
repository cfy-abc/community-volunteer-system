<template>
  <view class="page">
    <view class="top-bar">
      <text class="back" @tap="goBack">← 返回</text>
      <text class="title">签到审批</text>
      <text></text>
    </view>
    <view class="list">
      <view class="card" v-for="r in records" :key="r.id || r.recordId">
        <text class="name">{{ r.realName || r.userName || '志愿者' }}</text>
        <text class="detail">签到: {{ fmt(r.checkinTime) }}</text>
        <text class="detail">签退: {{ fmt(r.checkoutTime) }}</text>
        <text class="detail">时长: {{ r.hoursEarned || 0 }}h</text>
        <text class="detail" v-if="r.checkinLocation">位置: {{ r.checkinLocation }}</text>
        <view class="actions" v-if="r.approvalStatus === 0 || r.approvalStatus == null">
          <button class="btn pass" @tap="approve(r)">通过</button>
          <button class="btn reject" @tap="reject(r.id || r.recordId)">拒绝</button>
        </view>
        <text class="status-done" v-else>{{ r.approvalStatus === 1 ? '已通过' : '已拒绝' }}</text>
      </view>
      <view v-if="records.length === 0" class="empty">暂无待审批记录</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/utils/request'

const activityId = ref(null)
const records = ref([])

const loadRecords = async () => {
  // 验证 activityId
  if (!activityId.value || isNaN(activityId.value)) {
    uni.showToast({
      title: '活动ID无效',
      icon: 'none'
    });
    return;
  }
  
  try {
    const res = await request.get(`/api/activities/${activityId.value}/sign-approvals`)
    if (res.code === 200 && res.data) {
      records.value = res.data || []
    }
  } catch (e) {
    console.error('加载审批列表失败', e)
    uni.showToast({
      title: e.message || '加载失败',
      icon: 'none'
    })
  }
}


const approve = async (record) => {
  try {
    await request.put(`/api/activities/sign-approvals/${record.id || record.recordId}/approve`)
    uni.showToast({ title: '已通过', icon: 'success' })
    loadRecords()
  } catch (e) {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

const reject = async (id) => {
  try {
    await request.put(`/api/activities/sign-approvals/${id}/reject`)
    uni.showToast({ title: '已拒绝', icon: 'success' })
    loadRecords()
  } catch (e) {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

const fmt = (d) => {
  if (!d) return '--'
  return new Date(d).toLocaleString()
}
const goBack = () => uni.navigateBack()

onLoad((options) => {
  // 1. 获取 activityId 并验证
  const id = options.activityId;
  
  if (!id || id === 'undefined' || id === 'null') {
    uni.showToast({
      title: '缺少活动ID参数',
      icon: 'none'
    });
    setTimeout(() => {
      uni.navigateBack();
    }, 1500);
    return;
  }
  
  // 2. 转换为整数并验证
  const parsedId = parseInt(id);
  
  if (isNaN(parsedId) || parsedId <= 0) {
    uni.showToast({
      title: '无效的活动ID',
      icon: 'none'
    });
    setTimeout(() => {
      uni.navigateBack();
    }, 1500);
    return;
  }
  
  // 3. 赋值并加载数据
  activityId.value = parsedId;
  loadRecords();
})

</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f0f2f5; }
.top-bar { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 24rpx; background: #fff;
  .back { font-size: 28rpx; color: #667eea; }
  .title { font-size: 34rpx; font-weight: bold; color: #333; }
}
.card { background: #fff; border-radius: 12rpx; padding: 24rpx; margin: 16rpx 24rpx;
  .name { font-size: 30rpx; font-weight: bold; color: #333; display: block; margin-bottom: 8rpx; }
  .detail { font-size: 24rpx; color: #666; display: block; margin-bottom: 4rpx; }
  .actions { display: flex; gap: 12rpx; margin-top: 12rpx; }
  .btn { flex: 1; height: 64rpx; border: none; border-radius: 10rpx; font-size: 26rpx; color: #fff;
    &.pass { background: #4caf50; }
    &.reject { background: #f44336; }
  }
  .status-done { font-size: 24rpx; color: #999; margin-top: 8rpx; display: block; }
}
.empty { text-align: center; padding: 80rpx; color: #999; }
</style>
