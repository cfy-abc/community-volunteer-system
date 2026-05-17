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

    <!-- Tab切换 -->
    <view class="tabs">
      <view class="tab" :class="{ active: currentTab === 'detail' }" @tap="currentTab = 'detail'">活动详情</view>
      <view class="tab" :class="{ active: currentTab === 'applicants' }"
        v-if="isActivityCreator" @tap="currentTab = 'applicants'; loadApplicants()">报名名单</view>
    </view>

    <view class="activity-info" v-if="currentTab === 'detail'">
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

      <!-- 留言板区域 -->
      <CommentSection
        :activityId="activityId"
        :isActivityCreator="isActivityCreator"
        :hasParticipated="hasRegistered"
      />
    </view>

    <!-- 报名名单Tab内容 -->
    <view class="applicants-section" v-if="currentTab === 'applicants'">
      <view class="export-bar">
        <button class="export-btn" @tap="exportApplicants">导出Excel</button>
      </view>
      <view class="applicant-card" v-for="(a, i) in applicants" :key="i">
        <text class="app-name">{{ a.applicantName }}</text>
        <text class="app-info">{{ a.applicantPhone }} | {{ a.applicantEmail }}</text>
        <text class="app-info">紧急联系人: {{ a.emergencyContact }} {{ a.emergencyPhone }}</text>
        <text class="app-info" v-if="a.remarks">备注: {{ a.remarks }}</text>
      </view>
      <view v-if="applicants.length === 0" class="empty-applicants">暂无报名者</view>
    </view>

    <view class="bottom-action" v-if="isLoggedIn">
      <view class="action-btn favorite-btn" @tap="toggleFavorite">
        <text class="iconfont" :class="{ 'icon-favorite-fill': isFavorite, 'icon-favorite': !isFavorite }"></text>
        <text class="btn-text">{{ isFavorite ? '已收藏' : '收藏' }}</text>
      </view>
      <button class="apply-btn" :class="{ disabled: !canApply }" @tap="goToApply" :disabled="!canApply">{{ applyButtonText }}</button>
      <button v-if="showSignBtn" class="sign-btn" @tap="goToSignPage">{{ signStatus === 'checked_in' ? '签退' : '签到' }}</button>
    </view>

    <view class="login-tip" v-else><text>请先登录后再报名</text><button class="login-btn" @tap="goToLogin">去登录</button></view>
    <view class="loading-container" v-if="loading"><text class="loading-text">加载中...</text></view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request, { decodeJwt } from '@/utils/request'
import CommentSection from '@/components/activity/comment-section.vue'

const getToken = () => uni.getStorageSync('token') || ''
const isLoggedIn = computed(() => !!getToken())

const activityId = ref('')
const activity = ref({})
const isFavorite = ref(false)
const signStatus = ref('not_start')
const loading = ref(true)
const currentTab = ref('detail')
const applicants = ref([])

const isActivityOpen = computed(() => activity.value.status === 1)
const hasRegistered = computed(() => signStatus.value !== 'not_start')
const isActivityCreator = computed(() => {
  const token = uni.getStorageSync('token')
  if (!token) return false
  const decoded = decodeJwt(token)
  return decoded && decoded.userId === activity.value.creatorId
})
const canApply = computed(() =>
  isActivityOpen.value && !hasRegistered.value && !isActivityCreator.value &&
  (activity.value.appliedCount || 0) < (activity.value.maxParticipants || 0) && !loading.value
)
const applyButtonText = computed(() => {
  if (!isActivityOpen.value) return '活动已结束'
  if (isActivityCreator.value) return '您发布的活动'
  if (hasRegistered.value) return '已报名'
  if ((activity.value.appliedCount || 0) >= (activity.value.maxParticipants || 0)) return '名额已满'
  return '立即报名'
})
const showSignBtn = computed(() => isLoggedIn.value && isActivityOpen.value && hasRegistered.value && signStatus.value !== 'checked_out')

const loadActivity = async () => {
  try {
    const res = await request.get(`/activities/${activityId.value}`)
    if (res.code === 200) {
      activity.value = res.data
      // Parse conditions if it's a JSON string
      if (typeof activity.value.conditions === 'string') {
        try {
          activity.value.conditions = JSON.parse(activity.value.conditions)
        } catch (e) {
          activity.value.conditions = []
        }
      }
    } else uni.showToast({ title: res.msg || '加载活动失败', icon: 'none' })
  } catch (err) { uni.showToast({ title: '网络错误', icon: 'none' }) }
}

const loadSignStatus = async (retry = true) => {
  if (!isLoggedIn.value) return
  try {
    const res = await request.get(`/activities/${activityId.value}/sign-status`)
    if (res.code === 200) signStatus.value = res.data.status
  } catch (err) {
    if (retry) {
      setTimeout(() => loadSignStatus(false), 300)
    }
  }
}

const goToApply = () => {
  if (!canApply.value) {
    uni.showToast({ title: applyButtonText.value, icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/activities/apply?id=${activityId.value}` })
}

const loadApplicants = async () => {
  try {
    const res = await request.get(`/activities/${activityId.value}/applicants`)
    if (res.code === 200) applicants.value = res.data || []
  } catch (e) {}
}

const exportApplicants = async () => {
  try {
    uni.showLoading({ title: '导出中...' })
    const res = await uni.request({
      url: request.config.baseUrl + `/api/activities/${activityId.value}/applicants/export`,
      method: 'GET',
      responseType: 'arraybuffer',
      header: { Authorization: 'Bearer ' + uni.getStorageSync('token') }
    })
    // #ifdef H5
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = '报名名单.xlsx'; a.click()
    window.URL.revokeObjectURL(url)
    // #endif
    // #ifndef H5
    uni.saveFile({ tempFilePath: res.tempFilePath, success: () => uni.showToast({ title: '导出成功', icon: 'success' }) })
    // #endif
    uni.hideLoading()
  } catch (e) { uni.hideLoading(); uni.showToast({ title: '导出失败', icon: 'none' }) }
}

const toggleFavorite = () => { isFavorite.value = !isFavorite.value; uni.showToast({ title: isFavorite.value ? '已收藏' : '已取消收藏', icon: 'success' }) }
const goToSignPage = () => { const type = signStatus.value === 'not_start' ? 'checkin' : 'checkout'; uni.navigateTo({ url: `/pages/activities/sign?id=${activityId.value}&type=${type}` }) }
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

.tabs { display: flex; margin: 20rpx 30rpx; background: #fff; border-radius: 12rpx; overflow: hidden;
  .tab { flex: 1; text-align: center; padding: 20rpx 0; font-size: 28rpx; color: #888;
    &.active { color: #667eea; font-weight: bold; border-bottom: 4rpx solid #667eea; }
  }
}
.applicants-section { padding: 0 30rpx;
  .export-bar { text-align: right; margin-bottom: 16rpx;
    .export-btn { padding: 10rpx 24rpx; background: #4caf50; color: #fff; border-radius: 20rpx; font-size: 24rpx; border: none; }
  }
  .applicant-card { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 12rpx;
    .app-name { font-size: 28rpx; font-weight: bold; color: #333; display: block; }
    .app-info { font-size: 24rpx; color: #666; display: block; margin-top: 6rpx; }
  }
  .empty-applicants { text-align: center; padding: 60rpx 0; color: #999; }
}
</style>
