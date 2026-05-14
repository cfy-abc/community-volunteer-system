<template>
  <view class="sign-container">
    <!-- 活动信息 -->
    <view class="activity-card">
      <view class="title">{{ activityInfo.title }}</view>
      <view class="location">地点：{{ activityInfo.location }}</view>
      <view class="time">时间：{{ formatTime(activityInfo.startTime) }} 至 {{ formatTime(activityInfo.endTime) }}</view>
      <view class="status" :class="statusClass">状态：{{ statusText }}</view>
    </view>

    <!-- 操作按钮组 -->
    <view class="button-group">
      <button class="btn checkin-btn" :disabled="!canCheckin" @click="handleCheckin">签到</button>
      <button class="btn checkout-btn" :disabled="!canCheckout" @click="handleCheckout">签退</button>
    </view>

    <!-- 提示信息 -->
    <view v-if="signRecord.checkinTime" class="info-card">
      <text>✅ 签到时间：{{ signRecord.checkinTime }}</text>
      <text v-if="signRecord.checkoutTime">🏁 签退时间：{{ signRecord.checkoutTime }}</text>
      <text v-if="signRecord.checkinLocation">📍 签到位置：{{ signRecord.checkinLocation }}</text>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request'

export default {
  data() {
    return {
      activityId: 0,
      activityInfo: {},
      signRecord: {
        status: 'not_start',
        checkinTime: null,
        checkoutTime: null,
        checkinLocation: ''
      },
      loading: false
    }
  },
  computed: {
    statusText() {
      const map = {
        not_start: '未签到',
        checked_in: '已签到，待签退',
        checked_out: '已完成签退'
      }
      return map[this.signRecord.status] || '未知'
    },
    statusClass() {
      const map = {
        not_start: 'status-none',
        checked_in: 'status-checkedin',
        checked_out: 'status-checkedout'
      }
      return map[this.signRecord.status]
    },
    canCheckin() {
      return this.signRecord.status === 'not_start' && !this.loading
    },
    canCheckout() {
      return this.signRecord.status === 'checked_in' && !this.loading
    }
  },
  onLoad(options) {
    if (options.id) {
      this.activityId = parseInt(options.id)
      this.loadActivityInfo()
      this.loadSignStatus()
    } else {
      uni.showToast({ title: '参数错误', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 1500)
    }
  },
  methods: {
    formatTime(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getMonth()+1}/${date.getDate()} ${date.getHours()}:${date.getMinutes()}`
    },
    async loadActivityInfo() {
      try {
        const res = await request({ url: `/activities/${this.activityId}`, method: 'GET' })
        if (res.code === 200) this.activityInfo = res.data
        else uni.showToast({ title: '获取活动失败', icon: 'none' })
      } catch (err) {
        console.error(err)
      }
    },
    async loadSignStatus() {
      try {
        const res = await request({ url: `/activities/${this.activityId}/sign-status`, method: 'GET' })
        if (res.code === 200) {
          this.signRecord = res.data
        }
      } catch (err) {
        console.error(err)
      }
    },
    getLocation() {
      return new Promise((resolve, reject) => {
        uni.getLocation({
          type: 'gcj02',
          success: res => resolve(`${res.latitude},${res.longitude}`),
          fail: err => {
            uni.showToast({ title: '获取位置失败', icon: 'none' })
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
      if (!this.canCheckin) return
      uni.showModal({
        title: '签到方式',
        content: '请选择签到方式',
        confirmText: '扫码签到',
        cancelText: '位置签到',
        success: async (res) => {
          let qrToken = null
          let location = null
          if (res.confirm) {
            // 扫码
            try {
              qrToken = await this.scanQrCode()
            } catch (err) {
              return
            }
          } else if (res.cancel) {
            // 位置签到
            try {
              location = await this.getLocation()
            } catch (err) {
              return
            }
          } else {
            return
          }
          await this.submitCheckin(location, qrToken)
        }
      })
    },
    async submitCheckin(location, qrToken) {
      this.loading = true
      try {
        const data = { location, qrToken }
        const res = await request({
          url: `/activities/${this.activityId}/checkin`,
          method: 'POST',
          data
        })
        if (res.code === 200) {
          uni.showToast({ title: '签到成功', icon: 'success' })
          await this.loadSignStatus()
        } else {
          uni.showToast({ title: res.msg || '签到失败', icon: 'none' })
        }
      } catch (err) {
        uni.showToast({ title: '请求失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    async handleCheckout() {
      if (!this.canCheckout) return
      uni.showModal({
        title: '确认签退',
        content: '确定要签退吗？',
        success: async (modalRes) => {
          if (!modalRes.confirm) return
          let location = null
          try {
            location = await this.getLocation()
          } catch (err) {
            return
          }
          this.loading = true
          try {
            const res = await request({
              url: `/activities/${this.activityId}/checkout`,
              method: 'POST',
              data: { location }
            })
            if (res.code === 200) {
              uni.showToast({ title: '签退成功', icon: 'success' })
              await this.loadSignStatus()
            } else {
              uni.showToast({ title: res.msg || '签退失败', icon: 'none' })
            }
          } catch (err) {
            uni.showToast({ title: '请求失败', icon: 'none' })
          } finally {
            this.loading = false
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.sign-container {
  padding: 30rpx;
  background-color: #f5f5f5;
  min-height: 100vh;
}
.activity-card {
  background: white;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 40rpx;
}
.title {
  font-size: 36rpx;
  font-weight: bold;
}
.location, .time {
  font-size: 28rpx;
  color: #666;
  margin-top: 10rpx;
}
.status {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #eee;
}
.status-checkedin { color: #ff9800; }
.status-checkedout { color: #4caf50; }
.status-none { color: #f44336; }
.button-group {
  display: flex;
  gap: 30rpx;
  margin-bottom: 40rpx;
}
.btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 44rpx;
  font-size: 32rpx;
  color: white;
}
.checkin-btn { background-color: #1989fa; }
.checkout-btn { background-color: #ff976a; }
.btn[disabled] { background-color: #ccc; }
.info-card {
  background: white;
  border-radius: 20rpx;
  padding: 30rpx;
  font-size: 28rpx;
  color: #333;
}
.info-card text {
  display: block;
  margin-top: 10rpx;
}
</style>