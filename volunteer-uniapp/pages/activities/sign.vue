<template>
  <view class="sign-page">
    <!-- 活动信息卡片 -->
    <view class="activity-card">
      <text class="title">{{ activity.title }}</text>
      <text class="time">{{ formatTime(activity.startTime) }} 至 {{ formatTime(activity.endTime) }}</text>
      <text class="location">📍 {{ activity.location }}</text>
    </view>

    <!-- 签到/签退按钮 -->
    <view class="action-area">
      <button 
        v-if="type === 'checkin'" 
        class="main-btn checkin" 
        @click="handleCheckin"
        :loading="loading"
      >
        {{ loading ? '签到中...' : '签到' }}
      </button>
      <button 
        v-if="type === 'checkout'" 
        class="main-btn checkout" 
        @click="handleCheckout"
        :loading="loading"
      >
        {{ loading ? '签退中...' : '签退' }}
      </button>
    </view>

    <!-- 提示信息 -->
    <view class="tips">
      <text>📌 签到将记录您的位置，请确保在活动范围内</text>
      <text v-if="type === 'checkin'">📷 如有需要，请扫描现场二维码进行签到</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      activityId: null,
      type: '',        // 'checkin' 或 'checkout'
      activity: {},
      loading: false,
      baseUrl: 'http://localhost:8080' // 请修改为你的后端地址
    }
  },
  onLoad(options) {
    this.activityId = parseInt(options.id)
    this.type = options.type
    this.loadActivity()
  },
  methods: {
    formatTime(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getMonth()+1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2,'0')}`
    },
    getToken() {
      return uni.getStorageSync('token') || ''
    },
    async loadActivity() {
      try {
        const res = await new Promise((resolve, reject) => {
          uni.request({
            url: `${this.baseUrl}/activities/${this.activityId}`,
            method: 'GET',
            header: { 'Authorization': `Bearer ${this.getToken()}` },
            success: resolve,
            fail: reject
          })
        })
        if (res.statusCode === 200 && res.data.code === 200) {
          this.activity = res.data.data
        } else {
          uni.showToast({ title: res.data?.msg || '获取活动失败', icon: 'none' })
        }
      } catch (err) {
        console.error(err)
        uni.showToast({ title: '网络错误', icon: 'none' })
      }
    },
    getLocation() {
      return new Promise((resolve, reject) => {
        uni.getLocation({
          type: 'gcj02',
          success: res => resolve(`${res.latitude},${res.longitude}`),
          fail: err => {
            uni.showToast({ title: '获取位置失败，请检查GPS', icon: 'none' })
            reject(err)
          }
        })
      })
    },
    scanQrCode() {
      return new Promise((resolve, reject) => {
        uni.scanCode({
          scanType: ['qrCode'],
          success: res => resolve(res.result),
          fail: () => reject(new Error('扫码取消'))
        })
      })
    },
    async handleCheckin() {
      uni.showActionSheet({
        itemList: ['位置签到', '扫码签到'],
        success: async (res) => {
          let location = null
          let qrToken = null
          if (res.tapIndex === 0) {
            try {
              location = await this.getLocation()
            } catch { return }
          } else if (res.tapIndex === 1) {
            try {
              qrToken = await this.scanQrCode()
            } catch { return }
          } else {
            return
          }
          this.submitCheckin(location, qrToken)
        }
      })
    },
    async submitCheckin(location, qrToken) {
      this.loading = true
      try {
        const res = await new Promise((resolve, reject) => {
          uni.request({
            url: `${this.baseUrl}/activities/${this.activityId}/checkin`,
            method: 'POST',
            header: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${this.getToken()}`
            },
            data: { location, qrToken },
            success: resolve,
            fail: reject
          })
        })
        if (res.statusCode === 200 && (res.data.code === 200 || res.data.code === 0)) {
          uni.showToast({ title: '签到成功', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 1500)
        } else {
          uni.showToast({ title: res.data?.msg || '签到失败', icon: 'none' })
        }
      } catch (err) {
        uni.showToast({ title: '网络错误', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    async handleCheckout() {
      let location = null
      try {
        location = await this.getLocation()
      } catch { return }
      this.loading = true
      try {
        const res = await new Promise((resolve, reject) => {
          uni.request({
            url: `${this.baseUrl}/activities/${this.activityId}/checkout`,
            method: 'POST',
            header: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${this.getToken()}`
            },
            data: { location },
            success: resolve,
            fail: reject
          })
        })
        if (res.statusCode === 200 && (res.data.code === 200 || res.data.code === 0)) {
          uni.showToast({ title: '签退成功', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 1500)
        } else {
          uni.showToast({ title: res.data?.msg || '签退失败', icon: 'none' })
        }
      } catch (err) {
        uni.showToast({ title: '网络错误', icon: 'none' })
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.sign-page {
  padding: 30rpx;
  background-color: #f5f5f5;
  min-height: 100vh;
}
.activity-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 50rpx;
}
.title {
  font-size: 36rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 15rpx;
}
.time, .location {
  font-size: 28rpx;
  color: #666;
  display: block;
  margin-top: 10rpx;
}
.action-area {
  margin: 40rpx 0;
}
.main-btn {
  width: 100%;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 34rpx;
  color: #fff;
}
.checkin { background-color: #1989fa; }
.checkout { background-color: #ff976a; }
.tips {
  text-align: center;
  color: #999;
  font-size: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 15rpx;
}
</style>