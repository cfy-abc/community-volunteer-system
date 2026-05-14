<template>
  <view class="login-container">
    <!-- 自定义导航栏 -->
    <view class="custom-nav">
      <view class="nav-back" @tap="goBack">
        <text class="iconfont icon-back"></text>
      </view>
      <view class="nav-title">登录</view>
      <view class="nav-right"></view>
    </view>

    <view class="login-content">
      <!-- Logo -->
      <view class="logo-container">
        <image src="/static/images/logo.png" class="logo-img"></image>
        <text class="app-title">社区服务志愿者管理系统</text>
        <text class="app-subtitle">欢迎使用志愿者管理平台</text>
      </view>

      <!-- 登录表单 -->
      <view class="form-container">
        <view class="input-group">
          <view class="input-item">
            <text class="input-label">用户名</text>
            <input
              v-model="formData.username"
              class="input-field"
              placeholder="请输入用户名"
              placeholder-class="input-placeholder"
              @input="onUsernameInput"
            />
          </view>
          
          <view class="input-item">
            <text class="input-label">密码</text>
            <input
              v-model="formData.password"
              type="password"
              class="input-field"
              placeholder="请输入密码"
              placeholder-class="input-placeholder"
              @input="onPasswordInput"
            />
          </view>
        </view>

        <button 
          class="login-btn" 
          :disabled="!canSubmit || loading"
          :class="{ 'btn-loading': loading }"
          @tap="handleLogin"
        >
          <text v-if="!loading">登录</text>
          <view v-else class="loading-spinner">
            <text class="spinner-dot"></text>
            <text class="spinner-dot"></text>
            <text class="spinner-dot"></text>
          </view>
        </button>

        <view class="form-footer">
          <text class="footer-text">还没有账号？</text>
          <text class="register-link" @tap="toRegister">立即注册</text>
        </view>
      </view>

      <!-- 其他登录方式 -->
      <view class="other-login">
        <view class="divider">
          <text class="divider-text">其他登录方式</text>
        </view>
        <view class="social-login">
          <view class="social-item" @tap="wechatLogin">
            <text class="social-icon wechat-icon">微信</text>
          </view>
          <view class="social-item" @tap="qqLogin">
            <text class="social-icon qq-icon">QQ</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import request from '@/utils/request'

const formData = reactive({
  username: '',
  password: ''
})

const loading = ref(false)
const canSubmit = computed(() => {
  return formData.username.trim().length >= 3 && formData.password.trim().length >= 6
})

const handleLogin = async () => {
  if (!canSubmit.value) {
    uni.showToast({ title: '请填写正确的用户名和密码', icon: 'none' })
    return
  }
  if (loading.value) return
  loading.value = true

  try {
    const res = await request.post('/api/users/login', {
      username: formData.username.trim(),
      password: formData.password.trim()
    })

    if (res.code === 200) {
      const token = res.data
      uni.setStorageSync('token', token)
      // 存储用户名用于显示
      uni.setStorageSync('userInfo', { username: formData.username.trim() })
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/home/home' })
      }, 1000)
    } else {
      uni.showToast({ title: res.msg || '登录失败', icon: 'none' })
    }
  } catch (error) {
    console.error('登录异常:', error)
    uni.showToast({ title: '网络错误', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const goBack = () => uni.navigateBack()
const toRegister = () => uni.navigateTo({ url: '/pages/auth/register' })

const wechatLogin = () => {
  uni.showToast({ title: '微信登录功能开发中', icon: 'none' })
}

const qqLogin = () => {
  uni.showToast({ title: 'QQ登录功能开发中', icon: 'none' })
}
</script>

<style lang="scss">
page {
  background-color: #f5f5f5;
  height: 100%;
}

.login-container {
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

// 背景装饰
.login-container::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
  transform: rotate(30deg);
}

.custom-nav {
  display: flex;
  align-items: center;
  height: 88rpx;
  padding: 0 30rpx;
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20rpx);
  position: relative;
  z-index: 10;
  
  .nav-back {
    width: 60rpx;
    height: 60rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background-color: #f0f0f0;
    
    .icon-back {
      font-size: 32rpx;
      color: #666;
    }
  }
  
  .nav-title {
    flex: 1;
    text-align: center;
    font-size: 36rpx;
    font-weight: bold;
    color: #333;
  }
  
  .nav-right {
    width: 60rpx;
  }
}

.login-content {
  padding: 40rpx;
  margin-top: 88rpx;
  position: relative;
  z-index: 2;
}

.logo-container {
  text-align: center;
  margin-bottom: 80rpx;
  
  .logo-img {
    width: 120rpx;
    height: 120rpx;
    border-radius: 20rpx;
    margin-bottom: 20rpx;
    box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.2);
  }
  
  .app-title {
    display: block;
    font-size: 40rpx;
    font-weight: bold;
    color: #fff;
    margin-bottom: 10rpx;
    text-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.3);
  }
  
  .app-subtitle {
    display: block;
    font-size: 28rpx;
    color: rgba(255, 255, 255, 0.8);
  }
}

.form-container {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20rpx;
  padding: 40rpx;
  box-shadow: 0 20rpx 40rpx rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(20rpx);
}

.input-group {
  margin-bottom: 40rpx;
}

.input-item {
  margin-bottom: 30rpx;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.input-label {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 10rpx;
  font-weight: 500;
}

.input-field {
  width: 100%;
  height: 80rpx;
  padding: 0 20rpx;
  border: 2rpx solid #e0e0e0;
  border-radius: 12rpx;
  font-size: 30rpx;
  background-color: #fafafa;
  
  &:focus {
    border-color: #667eea;
    outline: none;
  }
}

.input-placeholder {
  color: #ccc;
  font-size: 28rpx;
}

.login-btn {
  width: 100%;
  height: 80rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 40rpx;
  color: #fff;
  font-size: 32rpx;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
  transition: all 0.3s ease;
  
  &:active {
    transform: scale(0.98);
  }
  
  &.btn-loading {
    background: #ccc;
  }
}

.loading-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
  
  .spinner-dot {
    width: 8rpx;
    height: 8rpx;
    background-color: #fff;
    border-radius: 50%;
    margin: 0 4rpx;
    animation: spin 1.4s infinite ease-in-out both;
    
    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

@keyframes spin {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1.0); }
}

.form-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  
  .footer-text {
    font-size: 26rpx;
    color: #666;
    margin-right: 10rpx;
  }
  
  .register-link {
    font-size: 26rpx;
    color: #667eea;
    text-decoration: underline;
  }
}

.other-login {
  margin-top: 60rpx;
  text-align: center;
}

.divider {
  display: flex;
  align-items: center;
  margin-bottom: 30rpx;
  
  &::before,
  &::after {
    content: '';
    flex: 1;
    height: 1rpx;
    background-color: rgba(255, 255, 255, 0.3);
  }
  
  .divider-text {
    padding: 0 20rpx;
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.8);
  }
}

.social-login {
  display: flex;
  justify-content: center;
  gap: 40rpx;
}

.social-item {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10rpx);
  transition: all 0.3s ease;
  
  &:active {
    transform: scale(0.95);
  }
}

.social-icon {
  font-size: 24rpx;
  color: #fff;
  font-weight: bold;
}

.wechat-icon {
  background: linear-gradient(135deg, #07c160, #06ae56);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.qq-icon {
  background: linear-gradient(135deg, #12b7f5, #0ba2f3);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

// 响应式适配
@media screen and (max-width: 375px) {
  .login-content {
    padding: 20rpx;
  }
  
  .form-container {
    padding: 30rpx;
  }
}
</style>