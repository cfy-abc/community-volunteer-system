<template>
  <view class="page">
    <view class="top-bar">
      <text class="back" @tap="goBack">←返回</text>
      <text class="title">签到审批</text>
      <text></text>
    </view>
    <view class="list">
      <view class="card" v-for="r in records" :key="r.id">
        <text class="name">{{ r.realName }} — {{ r.activityTitle }}</text>
        <text class="detail">签到: {{ fmt(r.checkinTime) }} | 签退: {{ fmt(r.checkoutTime) }}</text>
        <text class="detail">时长: {{ r.hoursEarned || 0 }}h | {{ approvalText(r.approvalStatus) }}</text>
        <view class="actions" v-if="r.approvalStatus === 0">
          <button class="btn pass" @tap="approve(r)">通过</button>
          <button class="btn reject" @tap="reject(r.id)">拒绝</button>
        </view>
      </view>
      <view v-if="records.length === 0" class="empty">暂无待审批记录</view>
    </view>
  </view>
</template>

<script setup>
import { ref,onMounted } from 'vue'
import request from '@/utils/request'

const records = ref([])

const loadData = async () => {
  try {
    const res = await request.get('/admin/sign-approvals', { page: 1, size: 100 })
    if (res.code === 200 && res.data) records.value = res.data.list || []
  } catch (e) { console.error('加载审批列表失败', e) }
}

const approve = async (r) => {
	// 添加 id 验证
	  if (!r.id) {
	    uni.showToast({ title: '记录ID无效', icon: 'none' })
	    return
	  }
  try {
    await request.post(`/admin/sign-records/${r.id}/approve`, {
      userId: r.userId, 
	  activityId: r.activityId, 
	  hours: r.hoursEarned
    })
    uni.showToast({ title: '已通过', icon: 'success' })
    loadData()
  } catch (e) { uni.showToast({ title: '操作失败', icon: 'none' }) }
}

const reject = async (id) => {
  try {
    await request.put(`/admin/sign-approvals/${id}/reject`)
    uni.showToast({ title: '已拒绝', icon: 'success' })
    loadData()
  } catch (e) { uni.showToast({ title: '操作失败', icon: 'none' }) }
}

const approvalText = (s) => ({ 0: '待审批', 1: '已通过', 2: '已拒绝' }[s] || '')
const fmt = (d) => d ? new Date(d).toLocaleString() : '--'
const goBack = () => uni.navigateBack()

onMounted(loadData)
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f0f2f5; }
.top-bar { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 24rpx; background: #fff; }
.back { font-size: 28rpx; color: #667eea; }
.title { font-size: 34rpx; font-weight: bold; color: #333; }
.card { background: #fff; border-radius: 12rpx; padding: 24rpx; margin: 16rpx 24rpx; }
.name { font-size: 28rpx; font-weight: bold; color: #333; display: block; margin-bottom: 8rpx; }
.detail { font-size: 24rpx; color: #666; display: block; margin-bottom: 4rpx; }
.actions { display: flex; gap: 12rpx; margin-top: 12rpx; }
.btn { flex: 1; height: 64rpx; border: none; border-radius: 10rpx; font-size: 26rpx; color: #fff;
  &.pass { background: #4caf50; }
  &.reject { background: #f44336; }
}
.empty { text-align: center; padding: 80rpx; color: #999; }
</style>
