<template>
  <view class="container">
    <!-- 基本信息 -->
    <view class="section">
      <view class="section-title">基本信息</view>
      <view class="form-item">
        <text class="label">活动标题<text class="required">*</text></text>
        <input v-model="form.title" placeholder="请输入活动标题" maxlength="50" />
      </view>
      <view class="form-item">
        <text class="label">活动描述<text class="required">*</text></text>
        <textarea v-model="form.description" placeholder="请详细描述活动内容、要求等" maxlength="500" auto-height />
      </view>
      <view class="form-item">
        <text class="label">活动地点<text class="required">*</text></text>
        <view class="location-picker" @click="chooseLocation">
          <text :class="{ placeholder: !form.location }">{{ form.location || '点击选择地点' }}</text>
          <text class="iconfont icon-location"></text>
        </view>
        <view v-if="form.latitude && form.longitude" class="location-note">
          已获取坐标：{{ form.latitude }}, {{ form.longitude }}
        </view>
      </view>
      <view class="form-row">
        <view class="form-item half">
          <text class="label">开始时间<text class="required">*</text></text>
          <picker mode="datetime" :value="form.startTime" @change="onStartTimeChange">
            <view class="picker">{{ form.startTime || '请选择' }}</view>
          </picker>
        </view>
        <view class="form-item half">
          <text class="label">结束时间<text class="required">*</text></text>
          <picker mode="datetime" :value="form.endTime" @change="onEndTimeChange">
            <view class="picker">{{ form.endTime || '请选择' }}</view>
          </picker>
        </view>
      </view>
    </view>

    <!-- 活动设置 -->
    <view class="section">
      <view class="section-title">活动设置</view>
      <view class="form-row">
        <view class="form-item half">
          <text class="label">最大参与人数<text class="required">*</text></text>
          <input v-model="form.maxParticipants" type="number" placeholder="人数" />
        </view>
        <view class="form-item half">
          <text class="label">奖励志愿时长(小时)<text class="required">*</text></text>
          <input v-model="form.rewardHours" type="digit" placeholder="时长" />
        </view>
      </view>
      <view class="form-item">
        <text class="label">联系电话<text class="required">*</text></text>
        <input v-model="form.contactPhone" type="number" placeholder="活动联系电话" />
      </view>
      <view class="form-item">
        <text class="label">活动类型</text>
        <picker mode="selector" :range="activityTypes" @change="onTypeChange">
          <view class="picker">{{ form.type || '请选择活动类型' }}</view>
        </picker>
      </view>
      <view class="form-item">
        <text class="label">活动标签（可多选，用逗号分隔）</text>
        <input v-model="form.tagsStr" placeholder="例如：环保,社区,户外" />
      </view>
      <view class="form-item">
        <text class="label">报名条件（每行一条）</text>
        <textarea v-model="form.conditionsStr" placeholder="每行一个条件，例如：&#10;年满18周岁&#10;身体健康" rows="4" />
      </view>
      <view class="form-item">
        <text class="label">活动海报</text>
        <view class="upload-box" @click="uploadPoster">
          <image v-if="form.poster" :src="form.poster" mode="aspectFill" class="poster-img" />
          <view v-else class="upload-placeholder">
            <text class="iconfont icon-add"></text>
            <text>上传海报</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 按钮 -->
    <button class="publish-btn" :loading="submitting" @click="publish">发布活动</button>
  </view>
</template>

<script>
import request from '@/utils/request'

export default {
  data() {
    return {
      submitting: false,
      form: {
        title: '',
        description: '',
        location: '',
        longitude: null,
        latitude: null,
        startTime: '',
        endTime: '',
        maxParticipants: '',
        rewardHours: '',
        contactPhone: '',
        type: '',
        tagsStr: '',
        conditionsStr: '',
        poster: '',
        orgId: null
      },
      activityTypes: ['环保公益', '敬老助残', '教育助学', '医疗健康', '社区服务', '文化文艺', '其他']
    }
  },
  onLoad() {
    // 可获取组织ID
    // this.form.orgId = uni.getStorageSync('orgId') || null
  },
  methods: {
    onStartTimeChange(e) { this.form.startTime = e.detail.value },
    onEndTimeChange(e) { this.form.endTime = e.detail.value },
    onTypeChange(e) { this.form.type = this.activityTypes[e.detail.value] },
    chooseLocation() {
      uni.chooseLocation({
        success: (res) => {
          this.form.location = res.name || res.address
          this.form.longitude = res.longitude
          this.form.latitude = res.latitude
        },
        fail: (err) => {
          if (err.errMsg.indexOf('auth deny') > -1) {
            uni.showModal({
              title: '位置权限',
              content: '需要获取您的位置信息，请在设置中开启',
              confirmText: '去设置',
              success: (modalRes) => {
                if (modalRes.confirm) uni.openAppAuthorizeSetting()
              }
            })
          } else {
            uni.showToast({ title: '选择位置失败', icon: 'none' })
          }
        }
      })
    },
    async uploadPoster() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: async (res) => {
          const tempFilePath = res.tempFilePaths[0]
          uni.showLoading({ title: '上传中...' })
          try {
            const uploadRes = await this.uploadFile(tempFilePath)
            if (uploadRes.code === 200) {
              this.form.poster = uploadRes.data.url
              uni.showToast({ title: '上传成功', icon: 'success' })
            } else {
              uni.showToast({ title: uploadRes.msg || '上传失败', icon: 'none' })
            }
          } catch (err) {
            console.error(err)
            uni.showToast({ title: '上传失败', icon: 'none' })
          } finally {
            uni.hideLoading()
          }
        }
      })
    },
    uploadFile(filePath) {
      return new Promise((resolve, reject) => {
        uni.uploadFile({
          url: 'http://localhost:8080/upload',
          filePath: filePath,
          name: 'file',
          header: { 'Authorization': `Bearer ${uni.getStorageSync('token')}` },
          success: (res) => {
            try { resolve(JSON.parse(res.data)) } catch (e) { reject(e) }
          },
          fail: reject
        })
      })
    },
    async publish() {
      // 表单校验
      if (!this.form.title.trim()) return uni.showToast({ title: '请填写活动标题', icon: 'none' })
      if (!this.form.description.trim()) return uni.showToast({ title: '请填写活动描述', icon: 'none' })
      if (!this.form.location) return uni.showToast({ title: '请选择活动地点', icon: 'none' })
      if (!this.form.startTime || !this.form.endTime) return uni.showToast({ title: '请选择活动时间', icon: 'none' })
      if (new Date(this.form.startTime) >= new Date(this.form.endTime)) return uni.showToast({ title: '结束时间必须晚于开始时间', icon: 'none' })
      if (!this.form.maxParticipants || this.form.maxParticipants <= 0) return uni.showToast({ title: '请填写最大参与人数', icon: 'none' })
      if (!this.form.rewardHours || this.form.rewardHours <= 0) return uni.showToast({ title: '请填写奖励时长', icon: 'none' })
      if (!this.form.contactPhone) return uni.showToast({ title: '请填写联系电话', icon: 'none' })

      // 处理标签和报名条件
      const tags = this.form.tagsStr.split(',').map(t => t.trim()).filter(t => t)
      const conditions = this.form.conditionsStr.split('\n').filter(c => c.trim())

      const requestData = {
        title: this.form.title,
        description: this.form.description,
        location: this.form.location,
        longitude: this.form.longitude,
        latitude: this.form.latitude,
        startTime: this.form.startTime,
        endTime: this.form.endTime,
        maxParticipants: parseInt(this.form.maxParticipants),
        rewardHours: parseFloat(this.form.rewardHours),
        contactPhone: this.form.contactPhone,
        type: this.form.type,
        tags: JSON.stringify(tags),
        conditions: JSON.stringify(conditions),
        poster: this.form.poster,
        orgId: this.form.orgId
      }

      this.submitting = true
      try {
        const res = await request({ url: '/activities', method: 'POST', data: requestData })
        if (res.code === 200) {
          uni.showToast({ title: '发布成功', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 1500)
        } else {
          uni.showToast({ title: res.msg || '发布失败', icon: 'none' })
        }
      } catch (err) {
        console.error(err)
        uni.showToast({ title: '网络错误', icon: 'none' })
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.container {
  padding: 30rpx;
  background-color: #f5f5f5;
  min-height: 100vh;
}
.section {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
}
.section-title {
  font-size: 34rpx;
  font-weight: bold;
  margin-bottom: 30rpx;
  position: relative;
  padding-left: 20rpx;
}
.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6rpx;
  height: 30rpx;
  background-color: #1989fa;
  border-radius: 3rpx;
}
.form-item {
  margin-bottom: 30rpx;
}
.form-row {
  display: flex;
  justify-content: space-between;
}
.half {
  width: 48%;
}
.label {
  display: block;
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
  margin-bottom: 15rpx;
}
.required {
  color: #f44336;
  margin-left: 5rpx;
}
input, textarea, .picker, .location-picker {
  width: 100%;
  padding: 20rpx;
  background-color: #f9f9f9;
  border-radius: 12rpx;
  font-size: 28rpx;
  border: 1rpx solid #eee;
  /* 修复输入框无法点击的核心样式 */
  pointer-events: auto !important;
  -webkit-user-select: text !important;
  user-select: text !important;
}
/* 单独确保 input 和 textarea 可点击 */
.input-fix, .textarea-fix {
  pointer-events: auto !important;
  -webkit-appearance: none;
}
.picker, .location-picker {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.placeholder {
  color: #aaa;
}
.location-note {
  font-size: 24rpx;
  color: #999;
  margin-top: 10rpx;
}
.upload-box {
  width: 200rpx;
  height: 200rpx;
  background-color: #f9f9f9;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1rpx dashed #ccc;
}
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #aaa;
  font-size: 24rpx;
}
.upload-placeholder .iconfont {
  font-size: 50rpx;
  margin-bottom: 10rpx;
}
.poster-img {
  width: 100%;
  height: 100%;
}
.publish-btn {
  background: linear-gradient(135deg, #1989fa, #0a7ae5);
  color: #fff;
  border-radius: 44rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
  font-weight: 500;
  margin-top: 30rpx;
}
</style>