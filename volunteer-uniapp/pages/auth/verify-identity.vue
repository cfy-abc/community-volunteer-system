<template>
  <view class="container">
    <view class="header">
      <text class="icon">🔐</text>
      <text class="title">实名认证</text>
      <text class="subtitle">完成实名认证后可使用全部功能</text>
    </view>

    <view class="form-section">
      <view class="form-item">
        <text class="label">真实姓名<text class="required">*</text></text>
        <input v-model="form.realName" class="input" placeholder="请输入身份证上的姓名" />
      </view>
      <view class="form-item">
        <text class="label">身份证号<text class="required">*</text></text>
        <input v-model="form.idCard" class="input" placeholder="请输入18位身份证号" maxlength="18" />
      </view>
    </view>

    <view class="tips">
      <text class="tips-title">温馨提示：</text>
      <text class="tips-text">1. 请确保填写的信息真实有效</text>
      <text class="tips-text">2. 身份信息仅用于实名认证，不会对外公开</text>
      <text class="tips-text">3. 认证通过后不可修改</text>
    </view>

    <button class="submit-btn" :loading="submitting" @tap="submit">提交认证</button>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import request from '@/utils/request'
import { validateIdCard } from '@/utils/validate'

const form = reactive({ realName: '', idCard: '' })
const submitting = ref(false)

const submit = async () => {
  if (!form.realName.trim()) return uni.showToast({ title: '请填写真实姓名', icon: 'none' })
  if (!form.idCard.trim()) return uni.showToast({ title: '请填写身份证号', icon: 'none' })
  if (!validateIdCard(form.idCard)) return uni.showToast({ title: '身份证号格式不正确', icon: 'none' })
  submitting.value = true
  try {
    const res = await request.post('/api/users/verify-identity', {
      realName: form.realName.trim(),
      idCard: form.idCard.trim()
    })
    if (res.code === 200) {
      uni.showToast({ title: '认证信息已提交', icon: 'success' })
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
  .icon { font-size: 80rpx; display: block; margin-bottom: 20rpx; }
  .title { font-size: 40rpx; font-weight: bold; color: #333; display: block; margin-bottom: 15rpx; }
  .subtitle { font-size: 26rpx; color: #999; }
}
.form-section { background: #fff; border-radius: 20rpx; padding: 30rpx; margin-bottom: 30rpx; }
.form-item { margin-bottom: 30rpx; }
.label { font-size: 28rpx; color: #333; font-weight: 500; display: block; margin-bottom: 15rpx; }
.required { color: #f44336; margin-left: 5rpx; }
.input { width: 100%; height: 80rpx; border: 2rpx solid #e0e0e0; border-radius: 12rpx; padding: 0 20rpx; font-size: 28rpx; }
.tips { background: #fff9e6; border-radius: 12rpx; padding: 20rpx; margin-bottom: 30rpx;
  .tips-title { font-size: 26rpx; color: #ff9800; font-weight: 500; display: block; margin-bottom: 10rpx; }
  .tips-text { font-size: 24rpx; color: #999; display: block; line-height: 1.8; }
}
.submit-btn { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border-radius: 44rpx; height: 88rpx; line-height: 88rpx; font-size: 32rpx; border: none; }
</style>
