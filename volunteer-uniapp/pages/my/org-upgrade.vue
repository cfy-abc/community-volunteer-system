<template>
  <view class="container">
    <view class="header">
      <text class="title">申请成为组织用户</text>
      <text class="subtitle">认证后可创建组织并发布组织活动</text>
    </view>

    <view class="form-section">
      <view class="form-item">
        <text class="label">职位<text class="required">*</text></text>
        <input v-model="form.position" class="input" placeholder="如：社区志愿者协会会长" />
      </view>
      <view class="form-item">
        <text class="label">所属社区/部门<text class="required">*</text></text>
        <input v-model="form.department" class="input" placeholder="如：阳光社区居委会" />
      </view>
      <view class="form-item">
        <text class="label">申请理由</text>
        <textarea v-model="form.reason" class="textarea" placeholder="请简述申请成为组织用户的理由" maxlength="300" />
      </view>
    </view>

    <button class="submit-btn" :loading="submitting" @tap="submit">提交申请</button>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import request from '@/utils/request'

const form = reactive({ position: '', department: '', reason: '' })
const submitting = ref(false)

const submit = async () => {
  if (!form.position.trim()) return uni.showToast({ title: '请填写职位', icon: 'none' })
  if (!form.department.trim()) return uni.showToast({ title: '请填写所属社区/部门', icon: 'none' })
  submitting.value = true
  try {
    const res = await request.post('/api/users/apply-org-upgrade', {
      position: form.position.trim(),
      department: form.department.trim(),
      reason: form.reason.trim()
    })
    if (res.code === 200) {
      uni.showToast({ title: '申请已提交', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1500)
    } else {
      uni.showToast({ title: res.msg || '提交失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: '网络错误', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss">
.container { min-height: 100vh; background: #f5f5f5; padding: 30rpx; }
.header { text-align: center; padding: 60rpx 0 40rpx;
  .title { font-size: 40rpx; font-weight: bold; color: #333; display: block; margin-bottom: 15rpx; }
  .subtitle { font-size: 26rpx; color: #999; }
}
.form-section { background: #fff; border-radius: 20rpx; padding: 30rpx; margin-bottom: 30rpx; }
.form-item { margin-bottom: 30rpx; }
.label { font-size: 28rpx; color: #333; font-weight: 500; display: block; margin-bottom: 15rpx; }
.required { color: #f44336; margin-left: 5rpx; }
.input { width: 100%; height: 80rpx; border: 2rpx solid #e0e0e0; border-radius: 12rpx; padding: 0 20rpx; font-size: 28rpx; }
.textarea { width: 100%; min-height: 150rpx; border: 2rpx solid #e0e0e0; border-radius: 12rpx; padding: 20rpx; font-size: 28rpx; }
.submit-btn { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border-radius: 44rpx; height: 88rpx; line-height: 88rpx; font-size: 32rpx; border: none; }
</style>
