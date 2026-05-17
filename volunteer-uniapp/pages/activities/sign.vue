<template>
  <view class="page">
    <view class="card" v-if="!loading">
      <text class="act-title">{{ activity.title || '加载中...' }}</text>
      <text class="act-meta" v-if="activity.startTime">{{ fmt(activity.startTime) }} ~ {{ fmt(activity.endTime) }}</text>
    </view>

    <!-- 错误提示 -->
    <view class="card warn" v-if="signError">
      <text>{{ signError }}</text>
      <button class="btn sm" v-if="signError.includes('报名')" @tap="goRegister">去报名</button>
    </view>

    <!-- 签到/签退操作区域 -->
    <view v-if="!signError && !loading">
      <view class="card">
        <text class="sec-label">{{ isCheckin ? '签 到' : '签 退' }}</text>
        <text class="sec-sub">请选择一种方式进行{{ isCheckin ? '签到' : '签退' }}</text>

        <!-- GPS定位签到 -->
        <view class="method-card" v-if="allowMethod('gps')">
          <view class="method-header"><text class="method-icon">📍</text><text class="method-name">GPS定位{{ isCheckin ? '签到' : '签退' }}</text></view>
          <text class="loc-text" v-if="gpsLocation">{{ gpsLocation }}</text>
          <text class="loc-text dim" v-else>点击按钮自动获取真实位置</text>
          <button class="btn block" @tap="signWithGps" :disabled="signing">
            {{ gpsLocation ? '确认' + (isCheckin ? '签到' : '签退') : '获取位置并' + (isCheckin ? '签到' : '签退') }}
          </button>
        </view>

        <!-- 扫码签到 -->
        <view class="method-card" v-if="allowMethod('scan')">
          <view class="method-header"><text class="method-icon">📷</text><text class="method-name">扫码{{ isCheckin ? '签到' : '签退' }}</text></view>
          <button class="btn outline block" @tap="signWithScan">{{ isCheckin ? '签到' : '签退' }}（扫码）</button>
        </view>

        <!-- 拍照签到 -->
        <view class="method-card" v-if="allowMethod('photo')">
          <view class="method-header"><text class="method-icon">🤳</text><text class="method-name">拍照{{ isCheckin ? '签到' : '签退' }}</text></view>
          <image v-if="photoPath" :src="photoPath" class="photo-preview" mode="aspectFit"></image>
          <button class="btn outline block" @tap="takePhoto">{{ photoPath ? '重新拍照' : '拍照' }}</button>
          <button v-if="photoPath" class="btn block" @tap="signWithPhoto" :disabled="signing" style="margin-top:12rpx">确认{{ isCheckin ? '签到' : '签退' }}</button>
        </view>

        <!-- 反扫码（仅活动发布方可见） -->
        <view class="method-card" v-if="isCreator && isCheckin">
          <view class="method-header"><text class="method-icon">🔄</text><text class="method-name">反扫码签到（组织者用）</text></view>
          <input class="inp" v-model="scanVolunteerInput" placeholder="粘贴扫描志愿者的二维码数据" />
          <button class="btn outline block" @tap="organizerScan" :disabled="signing" style="margin-top:12rpx">确认签到</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/utils/request'
import { decodeJwt } from '@/utils/request'

const activityId = ref(null)
const type = ref('checkin')
const activity = reactive({})
const signing = ref(false)
const loading = ref(true)
const signError = ref('')
const photoPath = ref('')
const gpsLocation = ref('')
const scanVolunteerInput = ref('')
const signMethod = ref('gps,scan,photo')

const isCheckin = computed(() => type.value === 'checkin')

const isCreator = computed(() => {
  const token = uni.getStorageSync('token')
  if (!token) return false
  const decoded = decodeJwt(token)
  return decoded && decoded.userId === activity.creatorId
})

const allowMethod = (method) => signMethod.value.split(',').map(s => s.trim()).includes(method)

// 自动获取GPS位置
const getLocation = () => {
  return new Promise((resolve, reject) => {
    // #ifdef H5
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        pos => resolve(`${pos.coords.latitude},${pos.coords.longitude}`),
        err => reject(new Error('GPS定位失败，请允许位置权限')),
        { timeout: 10000, enableHighAccuracy: true }
      )
    } else {
      reject(new Error('浏览器不支持GPS定位'))
    }
    // #endif
    // #ifndef H5
    uni.getLocation({
      type: 'gcj02',
      success: r => resolve(`${r.latitude},${r.longitude}`),
      fail: err => reject(new Error('GPS定位失败'))
    })
    // #endif
  })
}

// GPS签到签退
const signWithGps = async () => {
  try {
    if (!gpsLocation.value) {
      uni.showLoading({ title: '获取位置中...' })
      gpsLocation.value = await getLocation()
      uni.hideLoading()
    }
    await doSign({ location: gpsLocation.value })
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '获取位置失败', icon: 'none' })
  }
}

// 拍照
const takePhoto = () => {
  uni.chooseImage({
    count: 1, sourceType: ['camera'],
    success: (res) => { photoPath.value = res.tempFilePaths[0] }
  })
}

// 拍照签到签退
const signWithPhoto = async () => {
  try {
    let location = gpsLocation.value
    if (!location) {
      location = await getLocation()
      gpsLocation.value = location
    }
    await doSign({ location, photo: photoPath.value })
  } catch (e) {
    uni.showToast({ title: '拍照签到失败', icon: 'none' })
  }
}

// 扫码签到签退
const signWithScan = () => {
  // #ifdef H5
  uni.showToast({ title: 'H5暂不支持扫码，请使用GPS或拍照', icon: 'none' })
  return
  // #endif
  // #ifndef H5
  uni.scanCode({
    scanType: ['qrCode'],
    success: async (res) => {
      await doSign({ qrToken: res.result })
    },
    fail: () => uni.showToast({ title: '扫码取消', icon: 'none' })
  })
  // #endif
}

// 统一签到签退方法
const doSign = async (data) => {
  signing.value = true
  try {
    const endpoint = isCheckin.value ? 'checkin' : 'checkout'
    if (!data.location) {
      data.location = gpsLocation.value || 'auto'
    }
    const res = await request.post(`/activities/${activityId.value}/${endpoint}`, data)
    if (res.code === 200) {
      uni.showToast({ title: (isCheckin.value ? '签到' : '签退') + '成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1200)
    } else {
      uni.showToast({ title: res.msg || '操作失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  } finally { signing.value = false }
}

// 反扫码（组织者）
const organizerScan = async () => {
  if (!scanVolunteerInput.value.trim()) {
    uni.showToast({ title: '请粘贴或扫描志愿者二维码数据', icon: 'none' })
    return
  }
  try {
    const qrData = JSON.parse(scanVolunteerInput.value)
    signing.value = true
    const res = await request.post(`/activities/${activityId.value}/organizer-checkin`, {
      volunteerUserId: qrData.userId,
      qrToken: qrData.qrToken
    })
    if (res.code === 200) {
      uni.showToast({ title: '签到成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1200)
    } else {
      uni.showToast({ title: res.msg || '签到失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: '数据格式错误', icon: 'none' })
  } finally { signing.value = false }
}

// 检查签到状态
const checkSignStatus = async () => {
  try {
    const res = await request.get(`/activities/${activityId.value}/sign-status`)
    if (res.code === 200 && res.data) {
      const s = res.data
      if (s.status === 'not_start') {
        signError.value = '请先报名该活动后再' + (isCheckin.value ? '签到' : '签退')
      } else if (s.status === 'checked_in' && isCheckin.value) {
        signError.value = '您已签到，无需重复签到'
      } else if (s.status === 'checked_out') {
        signError.value = '您已完成签到签退'
      }
    } else {
      signError.value = '请先报名该活动后再' + (isCheckin.value ? '签到' : '签退')
    }
  } catch (e) {
    signError.value = '网络错误，请稍后重试'
  }
}

const loadActivity = async () => {
  try {
    const res = await request.get(`/activities/${activityId.value}`)
    if (res.code === 200 && res.data) {
      Object.assign(activity, res.data)
      signMethod.value = res.data.signMethod || 'gps,scan,photo'
    }
  } catch (e) {}
}

const goRegister = () => uni.navigateTo({ url: `/pages/activities/detail?id=${activityId.value}` })
const fmt = (d) => {
  if (!d) return ''
  const dt = new Date(d)
  return `${dt.getMonth()+1}/${dt.getDate()} ${String(dt.getHours()).padStart(2,'0')}:${String(dt.getMinutes()).padStart(2,'0')}`
}

onLoad(async (options) => {
  activityId.value = parseInt(options.id)
  type.value = options.type || 'checkin'
  loading.value = true
  await loadActivity()
  await checkSignStatus()
  loading.value = false
})
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f0f2f5; padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx; }
.card.warn { background: #fff3e0; }
.act-title { font-size: 34rpx; font-weight: 700; color: #222; display: block; margin-bottom: 12rpx; }
.act-meta { font-size: 26rpx; color: #888; display: block; margin-bottom: 6rpx; }
.sec-label { font-size: 32rpx; font-weight: 700; color: #333; display: block; margin-bottom: 8rpx; }
.sec-sub { font-size: 24rpx; color: #999; display: block; margin-bottom: 16rpx; }

.method-card {
  background: #f8f9fa; border-radius: 12rpx; padding: 20rpx; margin-bottom: 16rpx;
  .method-header { display: flex; align-items: center; margin-bottom: 12rpx;
    .method-icon { font-size: 40rpx; margin-right: 12rpx; }
    .method-name { font-size: 28rpx; font-weight: 600; color: #333; }
  }
  .loc-text { font-size: 26rpx; color: #333; margin-bottom: 12rpx; display: block;
    &.dim { color: #999; font-style: italic; }
  }
  .photo-preview { width: 100%; height: 360rpx; border-radius: 8rpx; margin-bottom: 12rpx; }
  .inp { width: 100%; height: 72rpx; padding: 0 16rpx; background: #f0f0f0; border: 1rpx solid #eee; border-radius: 10rpx; font-size: 24rpx; }
}
.btn {
  height: 72rpx; padding: 0 24rpx; border: none; border-radius: 10rpx; font-size: 26rpx; color: #fff;
  background: linear-gradient(135deg, #667eea, #7b5ea7);
  white-space: nowrap; flex-shrink: 0;
  &.outline { background: #fff; color: #667eea; border: 1rpx solid #667eea; }
  &.block { width: 100%; margin-top: 8rpx; }
  &.sm { height: 56rpx; font-size: 24rpx; padding: 0 20rpx; margin-top: 10rpx; width: auto; }
}
</style>
