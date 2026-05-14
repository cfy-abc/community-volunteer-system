<template>
  <view class="activity-detail-container" v-if="!loading">
    <view class="back-header">
      <view class="back-btn" @tap="goBack"><text class="iconfont icon-back"></text></view>
      <text class="title">活动详情</text>
      <view class="right-space"></view>
    </view>

    <view class="activity-cover">
      <image :src="activity.poster || '/static/images/default-activity.jpg'" class="cover-img" mode="aspectFill"></image>
      <view class="cover-overlay"></view>
      <view class="cover-content">
        <text class="activity-title">{{ activity.title }}</text>
        <view class="activity-stats">
          <text class="stat-item">{{ activity.appliedCount || 0 }}人报名</text>
          <text class="stat-item">{{ activity.maxParticipants }}人上限</text>
          <text class="stat-item">{{ activity.rewardHours }}小时</text>
        </view>
      </view>
    </view>

    <view class="activity-info">
      <view class="info-section">
        <view class="info-item"><text class="label">活动时间</text><text class="value">{{ formatDate(activity.startTime) }} - {{ formatDate(activity.endTime) }}</text></view>
        <view class="info-item"><text class="label">活动地点</text><text class="value">{{ activity.location }}</text></view>
        <view class="info-item"><text class="label">联系电话</text><text class="value">{{ activity.contactPhone || '暂无' }}</text></view>
        <view class="info-item"><text class="label">活动类型</text><text class="value">{{ activity.type || '公益活动' }}</text></view>
        <view class="info-item" v-if="activity.tags && activity.tags.length">
          <text class="label">活动标签</text>
          <view class="tags"><text class="tag" v-for="tag in activity.tags" :key="tag">{{ tag }}</text></view>
        </view>
      </view>

      <view class="desc-section"><text class="section-title">活动介绍</text><text class="desc-content">{{ activity.description }}</text></view>

      <view class="conditions-section" v-if="activity.conditions && activity.conditions.length">
        <text class="section-title">报名条件</text>
        <view class="condition-list"><view class="condition-item" v-for="(cond, idx) in activity.conditions" :key="idx"><text class="condition-dot">•</text><text class="condition-text">{{ cond }}</text></view></view>
      </view>

      <view class="feedback-section" v-if="activity.feedbacks && activity.feedbacks.length">
        <text class="section-title">志愿者反馈</text>
        <view class="feedback-list">
          <view class="feedback-item" v-for="fb in activity.feedbacks" :key="fb.id">
            <view class="feedback-user"><image :src="fb.avatar || '/static/images/default-avatar.png'" class="user-avatar" mode="aspectFill"></image><text class="user-name">{{ fb.name }}</text></view>
            <text class="feedback-content">{{ fb.content }}</text><text class="feedback-time">{{ fb.time }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-action" v-if="isLoggedIn">
      <view class="action-btn favorite-btn" @tap="toggleFavorite">
        <text class="iconfont" :class="{ 'icon-favorite-fill': isFavorite, 'icon-favorite': !isFavorite }"></text>
        <text class="btn-text">{{ isFavorite ? '已收藏' : '收藏' }}</text>
      </view>
      <button class="apply-btn" :class="{ disabled: !canApply }" @tap="applyActivity" :disabled="!canApply">{{ applyButtonText }}</button>
      <button v-if="showSignBtn" class="sign-btn" @tap="goToSignPage">{{ signStatus === 'checked_in' ? '签退' : '签到' }}</button>
    </view>

    <view class="login-tip" v-else><text>请先登录后再报名</text><button class="login-btn" @tap="goToLogin">去登录</button></view>
    <view class="loading-container" v-if="loading"><text class="loading-text">加载中...</text></view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/utils/request'

const getToken = () => uni.getStorageSync('token') || ''
const isLoggedIn = computed(() => !!getToken())

const activityId = ref('')
const activity = ref({})
const isFavorite = ref(false)
const signStatus = ref('not_start')
const loading = ref(true)

const isActivityOpen = computed(() => activity.value.status === 1)
const hasRegistered = computed(() => signStatus.value !== 'not_start')
const canApply = computed(() => isActivityOpen.value && !hasRegistered.value && (activity.value.appliedCount || 0) < (activity.value.maxParticipants || 0))
const applyButtonText = computed(() => {
  if (!isActivityOpen.value) return '活动已结束'
  if (hasRegistered.value) return '已报名'
  if ((activity.value.appliedCount || 0) >= (activity.value.maxParticipants || 0)) return '名额已满'
  return '立即报名'
})
const showSignBtn = computed(() => isLoggedIn.value && isActivityOpen.value && hasRegistered.value && signStatus.value !== 'checked_out')

const loadActivity = async () => {
  try {
    const res = await request.get(`/activities/${activityId.value}`)
    if (res.code === 200) activity.value = res.data
    else uni.showToast({ title: res.msg || '加载活动失败', icon: 'none' })
  } catch (err) { uni.showToast({ title: '网络错误', icon: 'none' }) }
}

const loadSignStatus = async () => {
  if (!isLoggedIn.value) return
  try {
    const res = await request.get(`/activities/${activityId.value}/sign-status`)
    if (res.code === 200) signStatus.value = res.data.status
  } catch (err) {}
}

const applyActivity = async () => {
  if (!canApply.value) return uni.showToast({ title: applyButtonText.value, icon: 'none' })
  uni.showModal({
    title: '确认报名',
    content: `确定要报名参加"${activity.value.title}"吗？`,
    success: async (modalRes) => {
      if (!modalRes.confirm) return
      try {
        const res = await request.post(`/activities/${activityId.value}/register`)
        if (res.code === 200) {
          uni.showToast({ title: '报名成功', icon: 'success' })
          activity.value.appliedCount = (activity.value.appliedCount || 0) + 1
          await loadSignStatus()
        } else uni.showToast({ title: res.msg || '报名失败', icon: 'none' })
      } catch (err) { uni.showToast({ title: '网络错误', icon: 'none' }) }
    }
  })
}

const toggleFavorite = () => { isFavorite.value = !isFavorite.value; uni.showToast({ title: isFavorite.value ? '已收藏' : '已取消收藏', icon: 'success' }) }
const goToSignPage = () => { const type = signStatus.value === 'not_start' ? 'checkin' : 'checkout'; uni.navigateTo({ url: `/pages/activity/sign?id=${activityId.value}&type=${type}` }) }
const goBack = () => uni.navigateBack()
const goToLogin = () => uni.navigateTo({ url: '/pages/auth/login' })
const formatDate = (dateStr) => dateStr ? new Date(dateStr).toLocaleString() : ''

onLoad(async (options) => {
  if (!options.id) {
    uni.showToast({ title: '参数错误', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1500)
    return
  }
  activityId.value = options.id
  loading.value = true
  await Promise.all([loadActivity(), loadSignStatus()])
  loading.value = false
})
</script>

<style lang="scss" scoped>
.activity-detail-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 140rpx;
}

.back-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  background-color: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
  .back-btn {
    width: 60rpx;
    height: 60rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background-color: #f0f0f0;
  }
  .title {
    font-size: 36rpx;
    font-weight: bold;
    color: #333;
  }
  .right-space { width: 60rpx; }
}

.activity-cover {
  position: relative;
  height: 400rpx;
  .cover-img {
    width: 100%;
    height: 100%;
    position: absolute;
    top: 0;
    left: 0;
  }
  .cover-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(transparent, rgba(0,0,0,0.6));
  }
  .cover-content {
    position: absolute;
    bottom: 40rpx;
    left: 30rpx;
    right: 30rpx;
    .activity-title {
      display: block;
      font-size: 40rpx;
      font-weight: bold;
      color: #fff;
      margin-bottom: 20rpx;
    }
    .activity-stats {
      display: flex;
      gap: 30rpx;
      .stat-item {
        font-size: 26rpx;
        color: rgba(255,255,255,0.9);
        background: rgba(255,255,255,0.2);
        padding: 6rpx 16rpx;
        border-radius: 20rpx;
      }
    }
  }
}

.activity-info {
  padding: 30rpx;
  .info-section {
    background: #fff;
    border-radius: 20rpx;
    padding: 30rpx;
    margin-bottom: 20rpx;
    .info-item {
      display: flex;
      flex-direction: column;
      margin-bottom: 30rpx;
      &:last-child { margin-bottom: 0; }
      .label { font-size: 28rpx; color: #666; margin-bottom: 10rpx; font-weight: 500; }
      .value { font-size: 30rpx; color: #333; }
      .tags {
        display: flex;
        flex-wrap: wrap;
        gap: 10rpx;
        .tag {
          font-size: 24rpx;
          color: #667eea;
          background-color: #f0f0ff;
          padding: 6rpx 16rpx;
          border-radius: 16rpx;
        }
      }
    }
  }
  .desc-section, .conditions-section, .feedback-section {
    background: #fff;
    border-radius: 20rpx;
    padding: 30rpx;
    margin-bottom: 20rpx;
    .section-title {
      display: block;
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
      margin-bottom: 20rpx;
    }
    .desc-content {
      font-size: 28rpx;
      color: #666;
      line-height: 1.6;
    }
  }
  .condition-list {
    .condition-item {
      display: flex;
      margin-bottom: 15rpx;
      .condition-dot { font-size: 32rpx; color: #667eea; margin-right: 15rpx; }
      .condition-text { font-size: 28rpx; color: #666; flex: 1; }
    }
  }
  .feedback-list {
    .feedback-item {
      margin-bottom: 30rpx;
      .feedback-user {
        display: flex;
        align-items: center;
        margin-bottom: 15rpx;
        .user-avatar { width: 50rpx; height: 50rpx; border-radius: 50%; margin-right: 15rpx; }
        .user-name { font-size: 26rpx; color: #333; font-weight: 500; }
      }
      .feedback-content { font-size: 26rpx; color: #666; line-height: 1.5; margin-bottom: 10rpx; }
      .feedback-time { font-size: 22rpx; color: #999; }
    }
  }
}

.bottom-action {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 20rpx;
  padding: 20rpx 30rpx;
  background-color: #fff;
  border-top: 1rpx solid #f0f0f0;
  .action-btn {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20rpx;
    border-radius: 12rpx;
    border: 1rpx solid #e0e0e0;
    background: #fff;
    .iconfont { font-size: 36rpx; margin-right: 10rpx; color: #666; }
    .icon-favorite-fill { color: #ff4757; }
    .btn-text { font-size: 28rpx; color: #666; }
  }
  .apply-btn {
    flex: 2;
    height: 80rpx;
    background: #667eea;
    color: #fff;
    border: none;
    border-radius: 40rpx;
    font-size: 32rpx;
    font-weight: 500;
    &.disabled { background-color: #ccc; }
  }
  .sign-btn {
    flex: 1;
    height: 80rpx;
    background: #ff976a;
    color: #fff;
    border-radius: 40rpx;
    font-size: 28rpx;
    font-weight: 500;
  }
}

.login-tip {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 30rpx;
  text-align: center;
  border-top: 1rpx solid #eee;
  .login-btn {
    margin-top: 20rpx;
    background: #667eea;
    color: #fff;
    width: 60%;
    border-radius: 40rpx;
  }
}

.loading-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255,255,255,0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  .loading-text { font-size: 32rpx; color: #666; }
}
</style>
