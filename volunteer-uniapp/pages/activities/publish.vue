<template>
  <view class="publish-page">
    <!-- 基本信息 -->
    <view class="card">
      <view class="card-title">
        <text class="title-icon">&#9997;</text>
        <text>基本信息</text>
      </view>

      <view class="field">
        <text class="field-label">活动标题<text class="star">*</text></text>
        <input
          class="field-input"
          v-model="title"
          placeholder="请输入活动标题"
          maxlength="50"
        />
      </view>

      <view class="field">
        <text class="field-label">活动描述<text class="star">*</text></text>
        <textarea
          class="field-textarea"
          v-model="description"
          placeholder="请详细描述活动内容、要求等"
          maxlength="500"
          :auto-height="true"
        />
      </view>

      <view class="field">
        <text class="field-label">活动地点<text class="star">*</text></text>
        <!-- #ifdef H5 -->
        <input class="field-input" v-model="location" placeholder="请输入活动详细地址" />
        <!-- #endif -->
        <!-- #ifndef H5 -->
        <view class="field-picker" @click="chooseLocation">
          <text :class="{ 'ph': !location }">{{ location || '点击选择地点' }}</text>
          <text class="pick-arrow">&#8250;</text>
        </view>
        <!-- #endif -->
        <view v-if="latitude && longitude" class="loc-note">
          坐标：{{ latitude }}, {{ longitude }}
        </view>
      </view>

      <view class="field">
        <text class="field-label">开始时间<text class="star">*</text></text>
        <view class="row-2">
          <picker class="half" mode="date" :value="startDate" @change="(e) => { startDate = e.detail.value; mergeStart() }">
            <view class="field-picker">{{ startDate || '选择日期' }}</view>
          </picker>
          <picker class="half" mode="time" :value="startTime" @change="(e) => { startTime = e.detail.value; mergeStart() }">
            <view class="field-picker">{{ startTime || '选择时间' }}</view>
          </picker>
        </view>
      </view>
      <view class="field">
        <text class="field-label">结束时间<text class="star">*</text></text>
        <view class="row-2">
          <picker class="half" mode="date" :value="endDate" @change="(e) => { endDate = e.detail.value; mergeEnd() }">
            <view class="field-picker">{{ endDate || '选择日期' }}</view>
          </picker>
          <picker class="half" mode="time" :value="endTime" @change="(e) => { endTime = e.detail.value; mergeEnd() }">
            <view class="field-picker">{{ endTime || '选择时间' }}</view>
          </picker>
        </view>
      </view>
    </view>

    <!-- 活动设置 -->
    <view class="card">
      <view class="card-title">
        <text class="title-icon">&#9881;</text>
        <text>活动设置</text>
      </view>

      <view class="row-2">
        <view class="field half">
          <text class="field-label">最大人数<text class="star">*</text></text>
          <input class="field-input" v-model="maxParticipants" type="number" placeholder="人数" />
        </view>
        <view class="field half">
          <text class="field-label">奖励时长(h)<text class="star">*</text></text>
          <input class="field-input" v-model="rewardHours" type="digit" placeholder="时长" />
        </view>
      </view>

      <view class="field">
        <text class="field-label">联系电话<text class="star">*</text></text>
        <input class="field-input" v-model="contactPhone" type="number" placeholder="活动联系电话" maxlength="11" />
      </view>

      <view class="field">
        <text class="field-label">活动类型</text>
        <picker mode="selector" :range="activityTypes" @change="onType">
          <view class="field-picker">{{ formType || '请选择活动类型' }}</view>
        </picker>
      </view>

      <view class="field">
        <text class="field-label">活动标签（逗号分隔）</text>
        <input class="field-input" v-model="tagsStr" placeholder="例如：环保,社区,户外" />
      </view>

      <view class="field">
        <text class="field-label">报名条件（每行一条）</text>
        <textarea class="field-textarea" v-model="conditionsStr" placeholder="每行一个条件，例如：&#10;年满18周岁&#10;身体健康" :auto-height="true" />
      </view>

      <view class="field">
        <text class="field-label">活动海报</text>
        <view class="poster-box" @click="uploadPoster">
          <image v-if="poster" :src="poster" class="poster-img" mode="aspectFill" />
          <view v-else class="poster-empty">
            <text class="poster-plus">+</text>
            <text>上传海报</text>
          </view>
        </view>
      </view>
    </view>

    <view class="form-section">
      <text class="section-title">签到方式设置（多选）</text>
      <view class="checkbox-group">
        <view class="checkbox-item" @tap="toggleSignMethod('gps')">
          <text class="checkbox-icon" :class="{ checked: signMethods.includes('gps') }">{{ signMethods.includes('gps') ? '☑' : '☐' }}</text>
          <text>GPS定位签到</text>
        </view>
        <view class="checkbox-item" @tap="toggleSignMethod('scan')">
          <text class="checkbox-icon" :class="{ checked: signMethods.includes('scan') }">{{ signMethods.includes('scan') ? '☑' : '☐' }}</text>
          <text>扫码签到</text>
        </view>
        <view class="checkbox-item" @tap="toggleSignMethod('photo')">
          <text class="checkbox-icon" :class="{ checked: signMethods.includes('photo') }">{{ signMethods.includes('photo') ? '☑' : '☐' }}</text>
          <text>拍照签到</text>
        </view>
      </view>
    </view>

    <!-- 发布按钮 -->
    <button class="submit-btn" :loading="submitting" @click="publish">发 布 活 动</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'

// 每个字段单独 ref，确保 v-model 可靠绑定
const title = ref('')
const description = ref('')
const location = ref('')
const longitude = ref(null)
const latitude = ref(null)
const startDate = ref('')
const startTime = ref('')
const startFull = ref('')  // 合并后的完整开始时间
const endDate = ref('')
const endTime = ref('')
const endFull = ref('')    // 合并后的完整结束时间
const mergeStart = () => { if (startDate.value && startTime.value) startFull.value = startDate.value + ' ' + startTime.value + ':00' }
const mergeEnd = () => { if (endDate.value && endTime.value) endFull.value = endDate.value + ' ' + endTime.value + ':00' }
const maxParticipants = ref('')
const rewardHours = ref('')
const contactPhone = ref('')
const formType = ref('')
const tagsStr = ref('')
const conditionsStr = ref('')
const poster = ref('')
const orgId = ref(null)
const submitting = ref(false)

const signMethods = ref(['gps', 'scan', 'photo'])

const toggleSignMethod = (method) => {
  const idx = signMethods.value.indexOf(method)
  if (idx >= 0) signMethods.value.splice(idx, 1)
  else signMethods.value.push(method)
}

const activityTypes = ['环保公益', '敬老助残', '教育助学', '医疗健康', '社区服务', '文化文艺', '其他']

const onType = (e) => { formType.value = activityTypes[e.detail.value] }

const chooseLocation = () => {
  // #ifdef H5
  uni.showToast({ title: '请在App或小程序中选择位置', icon: 'none' })
  return
  // #endif
  // #ifndef H5
  uni.chooseLocation({
    success: (res) => {
      location.value = res.name || res.address
      longitude.value = res.longitude
      latitude.value = res.latitude
    },
    fail: (err) => {
      if (err.errMsg && err.errMsg.includes('auth deny')) {
        uni.showModal({
          title: '位置权限',
          content: '需要获取您的位置信息，请在设置中开启',
          confirmText: '去设置',
          success: (m) => { if (m.confirm) uni.openAppAuthorizeSetting() }
        })
      } else {
        uni.showToast({ title: '选择位置失败', icon: 'none' })
      }
    }
  })
  // #endif
}

const uploadPoster = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const fp = res.tempFilePaths[0]
      uni.showLoading({ title: '上传中...' })
      try {
        const r = await new Promise((resolve, reject) => {
          uni.uploadFile({
            url: request.config.baseUrl + '/upload',
            filePath: fp,
            name: 'file',
            header: { Authorization: `Bearer ${uni.getStorageSync('token')}` },
            success: (rr) => { try { resolve(JSON.parse(rr.data)) } catch (e) { reject(e) } },
            fail: reject
          })
        })
        if (r.code === 200) {
          // 非H5模式拼接完整URL
          const url = r.data.url
          poster.value = url.startsWith('http') ? url : (request.config.baseUrl || '') + url
          uni.showToast({ title: '上传成功', icon: 'success' })
        } else {
          uni.showToast({ title: r.msg || '上传失败', icon: 'none' })
        }
      } catch (e) {
        uni.showToast({ title: '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

const publish = async () => {
  const t = title.value.trim()
  if (!t) return uni.showToast({ title: '请填写活动标题', icon: 'none' })
  const d = description.value.trim()
  if (!d) return uni.showToast({ title: '请填写活动描述', icon: 'none' })
  if (!location.value.trim()) return uni.showToast({ title: '请填写活动地点', icon: 'none' })
  if (!startFull.value || !endFull.value) return uni.showToast({ title: '请选择活动开始/结束时间', icon: 'none' })
  if (new Date(startFull.value) >= new Date(endFull.value)) return uni.showToast({ title: '结束时间必须晚于开始时间', icon: 'none' })
  const mp = parseInt(maxParticipants.value)
  if (!mp || mp <= 0) return uni.showToast({ title: '请填写最大参与人数', icon: 'none' })
  const rh = parseFloat(rewardHours.value)
  if (!rh || rh <= 0) return uni.showToast({ title: '请填写奖励时长', icon: 'none' })
  if (!contactPhone.value) return uni.showToast({ title: '请填写联系电话', icon: 'none' })

  if (contactPhone.value && !/^1[3-9]\d{9}$/.test(contactPhone.value.trim())) {
    return uni.showToast({ title: '手机号格式不正确', icon: 'none' })
  }

  const tags = tagsStr.value.split(',').map(s => s.trim()).filter(s => s)
  const conditions = conditionsStr.value.split('\n').filter(s => s.trim())

  submitting.value = true
  try {
    const res = await request.post('/activities', {
      title: t, description: d,
      location: location.value.trim(), longitude: longitude.value, latitude: latitude.value,
      startTime: startFull.value, endTime: endFull.value,
      maxParticipants: mp, rewardHours: rh,
      contactPhone: contactPhone.value, type: formType.value,
      tags: JSON.stringify(tags), conditions: JSON.stringify(conditions),
      poster: poster.value, orgId: orgId.value,
      signMethod: signMethods.value.join(',')
    })
    if (res.code === 200) {
      uni.showToast({ title: '发布成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1500)
    } else {
      uni.showToast({ title: res.msg || '发布失败', icon: 'none' })
    }
  } catch (err) {
    uni.showToast({ title: err.message || '发布失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.publish-page {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 24rpx;
}

.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
}

.card-title {
  display: flex;
  align-items: center;
  font-size: 32rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 28rpx;
  .title-icon { font-size: 34rpx; margin-right: 12rpx; }
}

.field {
  margin-bottom: 24rpx;
}

.field-label {
  display: block;
  font-size: 28rpx;
  font-weight: 500;
  color: #444;
  margin-bottom: 12rpx;
  .star { color: #f44336; margin-left: 4rpx; }
}

.field-input {
  width: 100%;
  height: 80rpx;
  padding: 0 24rpx;
  background: #f8f8f8;
  border: 2rpx solid #eee;
  border-radius: 14rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

.field-textarea {
  width: 100%;
  min-height: 160rpx;
  padding: 20rpx 24rpx;
  background: #f8f8f8;
  border: 2rpx solid #eee;
  border-radius: 14rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

.field-picker {
  width: 100%;
  height: 80rpx;
  padding: 0 24rpx;
  background: #f8f8f8;
  border: 2rpx solid #eee;
  border-radius: 14rpx;
  font-size: 28rpx;
  color: #333;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  .ph { color: #bbb; }
  .pick-arrow { color: #ccc; font-size: 34rpx; }
}

.loc-note { font-size: 22rpx; color: #999; margin-top: 8rpx; }

.row-2 {
  display: flex;
  gap: 20rpx;
  .half { flex: 1; min-width: 0; }
}

.poster-box {
  width: 200rpx;
  height: 200rpx;
  border-radius: 14rpx;
  overflow: hidden;
  border: 2rpx dashed #ddd;
  background: #fafafa;
  .poster-img { width: 100%; height: 100%; }
  .poster-empty {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #bbb;
    font-size: 24rpx;
    .poster-plus { font-size: 60rpx; color: #ccc; margin-bottom: 8rpx; }
  }
}

.submit-btn {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #667eea, #7b5ea7);
  color: #fff;
  border: none;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: 500;
  margin-top: 10rpx;
  letter-spacing: 8rpx;
}

.form-section {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
  .section-title { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 20rpx; display: block; }
}

.checkbox-group { display: flex; flex-direction: column; gap: 16rpx; }
.checkbox-item { display: flex; align-items: center; gap: 12rpx; font-size: 28rpx; color: #333; }
.checkbox-icon { font-size: 36rpx; width: 40rpx;
  &.checked { color: #667eea; }
}
</style>
