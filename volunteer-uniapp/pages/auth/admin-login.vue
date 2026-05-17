<template>
  <view class="admin-login-container">
    <view class="login-card">
      <view class="card-header">
        <text class="shield-icon">&#x1f6e1;</text>
        <text class="title">管理员登录</text>
        <text class="subtitle">社区志愿服务管理系统</text>
      </view>
      <view class="card-body">
        <view class="input-item">
          <text class="label">账号</text>
          <input v-model="form.username" placeholder="请输入管理员账号" />
        </view>
        <view class="input-item">
          <text class="label">密码</text>
          <input v-model="form.password" type="password" placeholder="请输入密码" />
        </view>
        <button class="login-btn" :disabled="!canSubmit || submitting" @tap="handleLogin">
          {{ submitting ? '登录中...' : '登录' }}
        </button>
      </view>
      <view class="card-footer">
        <text class="back-link" @tap="goBack">返回用户登录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useStore } from 'vuex'
import request from '@/utils/request'

const store = useStore()

const form = reactive({ username: '', password: '' })
const submitting = ref(false)

const canSubmit = computed(() => {
  return form.username.trim().length >= 3 && form.password.trim().length >= 6
})

const handleLogin = async () => {
  if (!canSubmit.value) {
    uni.showToast({ title: '请填写正确的账号和密码', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const res = await request.post('/admin/login', {
      username: form.username.trim(),
      password: form.password
    })
    if (res.code === 200) {
      uni.setStorageSync('adminToken', res.data)
      store.commit('auth/SET_ADMIN_TOKEN', res.data)
      uni.setStorageSync('adminUsername', form.username.trim())
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => uni.redirectTo({ url: '/pages/admin/dashboard' }), 1000)
    } else {
      uni.showToast({ title: res.msg || '登录失败', icon: 'none' })
    }
  } catch (err) {
    uni.showToast({ title: '网络错误', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

const goBack = () => uni.navigateTo({ url: '/pages/auth/login' })
</script>

<style lang="scss" scoped>
.admin-login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}
.login-card {
  width: 100%;
  max-width: 600rpx;
  background: rgba(255,255,255,0.95);
  border-radius: 24rpx;
  padding: 60rpx 50rpx;
  box-shadow: 0 20rpx 60rpx rgba(0,0,0,0.3);
}
.card-header {
  text-align: center;
  margin-bottom: 50rpx;
  .shield-icon { font-size: 64rpx; }
  .title {
    display: block;
    font-size: 40rpx;
    font-weight: bold;
    color: #1a1a2e;
    margin: 16rpx 0 8rpx;
  }
  .subtitle { font-size: 26rpx; color: #999; }
}
.card-body {
  .input-item {
    margin-bottom: 30rpx;
    .label {
      display: block;
      font-size: 28rpx;
      color: #333;
      margin-bottom: 12rpx;
      font-weight: 500;
    }
    input {
      width: 100%;
      height: 80rpx;
      padding: 0 24rpx;
      border: 2rpx solid #e0e0e0;
      border-radius: 12rpx;
      font-size: 28rpx;
      background: #fafafa;
    }
  }
  .login-btn {
    width: 100%;
    height: 88rpx;
    background: linear-gradient(135deg, #1a1a2e, #0f3460);
    color: #fff;
    border: none;
    border-radius: 44rpx;
    font-size: 32rpx;
    font-weight: 500;
    margin-top: 40rpx;
  }
}
.card-footer {
  text-align: center;
  margin-top: 40rpx;
  .back-link { font-size: 26rpx; color: #667eea; }
}
</style>
