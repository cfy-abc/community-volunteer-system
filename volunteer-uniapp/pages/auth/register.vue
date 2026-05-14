<template>
  <view class="register-container">
    <!-- 顶部背景 -->
    <view class="top-bg">
      <view class="bg-shape"></view>
      <view class="bg-shape shape-2"></view>
    </view>

    <!-- 注册表单 -->
    <view class="register-form">
      <view class="form-header">
        <text class="title">志愿者注册</text>
        <text class="subtitle">加入我们，一起传递爱心</text>
      </view>

      <view class="form-body">
        <view class="form-item">
          <view class="input-wrapper">
            <text class="iconfont icon-user input-icon"></text>
            <input 
              class="input" 
              type="text" 
              placeholder="请输入用户名" 
              v-model="formData.username"
              @blur="validateField('username')"
            />
          </view>
          <text v-if="errors.username" class="error-msg">{{ errors.username }}</text>
        </view>

        <view class="form-item">
          <view class="input-wrapper">
            <text class="iconfont icon-user input-icon"></text>
            <input 
              class="input" 
              type="text" 
              placeholder="请输入真实姓名" 
              v-model="formData.realName"
              @blur="validateField('realName')"
            />
          </view>
          <text v-if="errors.realName" class="error-msg">{{ errors.realName }}</text>
        </view>

        <view class="form-item">
          <view class="input-wrapper">
            <text class="iconfont icon-phone input-icon"></text>
            <input 
              class="input" 
              type="number" 
              placeholder="请输入手机号" 
              v-model="formData.phone"
              @blur="validateField('phone')"
            />
          </view>
          <text v-if="errors.phone" class="error-msg">{{ errors.phone }}</text>
        </view>

        <view class="form-item">
          <view class="input-wrapper">
            <text class="iconfont icon-email input-icon"></text>
            <input 
              class="input" 
              type="text" 
              placeholder="请输入邮箱" 
              v-model="formData.email"
              @blur="validateField('email')"
            />
          </view>
          <text v-if="errors.email" class="error-msg">{{ errors.email }}</text>
        </view>

        <view class="form-item">
          <view class="input-wrapper">
            <text class="iconfont icon-lock input-icon"></text>
            <input 
              class="input" 
              type="password" 
              placeholder="请输入密码" 
              v-model="formData.password"
              @blur="validateField('password')"
            />
          </view>
          <text v-if="errors.password" class="error-msg">{{ errors.password }}</text>
        </view>

        <view class="form-item">
          <view class="input-wrapper">
            <text class="iconfont icon-lock input-icon"></text>
            <input 
              class="input" 
              type="password" 
              placeholder="请确认密码" 
              v-model="formData.confirmPassword"
              @blur="validateField('confirmPassword')"
            />
          </view>
          <text v-if="errors.confirmPassword" class="error-msg">{{ errors.confirmPassword }}</text>
        </view>

        <view class="form-item checkbox-item">
          <view class="checkbox-wrapper" @tap="toggleAgreement">
            <text class="checkbox" :class="{ checked: formData.agreed }"></text>
            <text class="agreement-text">我已阅读并同意</text>
            <text class="agreement-link" @tap.stop="viewAgreement">《用户协议》</text>
            <text class="agreement-text">和</text>
            <text class="agreement-link" @tap.stop="viewPrivacyPolicy">《隐私政策》</text>
          </view>
          <text v-if="errors.agreed" class="error-msg">{{ errors.agreed }}</text>
        </view>

        <button 
          class="submit-btn" 
          :class="{ disabled: !canSubmit }"
          @tap="handleRegister"
        >
          注册
        </button>

        <view class="login-link">
          <text>已有账号？</text>
          <text class="link-text" @tap="toLogin">立即登录</text>
        </view>
      </view>
    </view>

    <!-- 加载提示 -->
    <view class="loading-overlay" v-if="loading">
      <view class="loading-content">
        <view class="loading-spinner"></view>
        <text class="loading-text">注册中...</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { validatePhone, validatePassword, validateUsername } from '@/utils/validate'
import request from '@/utils/request'

const formData = reactive({
  username: '',
  realName: '',
  phone: '',
  password: '',
  confirmPassword: '',
  agreed: false
})

const errors = reactive({
  username: '',
  realName: '',
  phone: '',
  password: '',
  confirmPassword: '',
  agreed: ''
})

const loading = ref(false)

const canSubmit = computed(() => {
  return formData.username &&
         formData.realName &&
         formData.phone &&
         formData.password &&
         formData.confirmPassword &&
         formData.agreed &&
         !Object.values(errors).some(error => error)
})

const validateField = (field) => {
  const value = formData[field]

  switch (field) {
    case 'username':
      if (!value.trim()) {
        errors.username = '请输入用户名'
      } else if (!validateUsername(value)) {
        errors.username = '用户名格式不正确（3-20位，可包含字母、数字、下划线和中文）'
      } else {
        errors.username = ''
      }
      break
    case 'realName':
      if (!value.trim()) {
        errors.realName = '请输入真实姓名'
      } else if (value.length < 2) {
        errors.realName = '姓名至少2个字符'
      } else {
        errors.realName = ''
      }
      break
    case 'phone':
      if (!value.trim()) {
        errors.phone = '请输入手机号'
      } else if (!validatePhone(value)) {
        errors.phone = '请输入正确的手机号'
      } else {
        errors.phone = ''
      }
      break
    case 'password':
      if (!value.trim()) {
        errors.password = '请输入密码'
      } else if (!validatePassword(value)) {
        errors.password = '密码长度6-30位，需包含大小写字母和数字'
      } else {
        errors.password = ''
      }
      break
    case 'confirmPassword':
      if (!value.trim()) {
        errors.confirmPassword = '请确认密码'
      } else if (value !== formData.password) {
        errors.confirmPassword = '两次输入的密码不一致'
      } else {
        errors.confirmPassword = ''
      }
      break
  }
}

const toggleAgreement = () => {
  formData.agreed = !formData.agreed
  if (formData.agreed) {
    errors.agreed = ''
  }
}

const viewAgreement = () => {
  uni.showModal({
    title: '用户协议',
    content: '用户协议内容...',
    showCancel: false,
    confirmText: '我知道了'
  })
}

const viewPrivacyPolicy = () => {
  uni.showModal({
    title: '隐私政策',
    content: '隐私政策内容...',
    showCancel: false,
    confirmText: '我知道了'
  })
}

const handleRegister = async () => {
  if (!canSubmit.value) {
    uni.showToast({ title: '请完善注册信息', icon: 'none' })
    return
  }
  if (!formData.agreed) {
    errors.agreed = '请阅读并同意用户协议和隐私政策'
    uni.showToast({ title: '请同意协议', icon: 'none' })
    return
  }

  loading.value = true

  const requestData = {
    username: formData.username.trim(),
    password: formData.password,
    realName: formData.realName.trim(),
    phone: formData.phone.trim()
  }

  try {
    const res = await request.post('/api/users/register', requestData)
    if (res.code === 200) {
      uni.showToast({ title: '注册成功，请等待审核', icon: 'success' })
      setTimeout(() => {
        uni.redirectTo({ url: '/pages/auth/login' })
      }, 1500)
    } else {
      uni.showToast({ title: res.msg || '注册失败', icon: 'none' })
    }
  } catch (error) {
    console.error('注册异常:', error)
    uni.showToast({ title: '网络错误，请检查连接', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const toLogin = () => {
  uni.redirectTo({ url: '/pages/auth/login' })
}
</script>

<style lang="scss">
.register-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  position: relative;
  
  .top-bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 300rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    
    .bg-shape {
      position: absolute;
      width: 200rpx;
      height: 200rpx;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.1);
      top: -100rpx;
      right: -50rpx;
      
      &.shape-2 {
        width: 150rpx;
        height: 150rpx;
        top: 50rpx;
        left: -30rpx;
      }
    }
  }
  
  .register-form {
    position: relative;
    z-index: 1;
    margin-top: 200rpx;
    padding: 0 40rpx;
    
    .form-header {
      text-align: center;
      margin-bottom: 60rpx;
      
      .title {
        display: block;
        font-size: 48rpx;
        font-weight: bold;
        color: #333;
        margin-bottom: 10rpx;
      }
      
      .subtitle {
        display: block;
        font-size: 28rpx;
        color: #666;
      }
    }
    
    .form-body {
      .form-item {
        margin-bottom: 30rpx;
        
        &:last-child {
          margin-bottom: 40rpx;
        }
        
        .input-wrapper {
          position: relative;
          display: flex;
          align-items: center;
          
          .input-icon {
            position: absolute;
            left: 20rpx;
            font-size: 32rpx;
            color: #666;
          }
          
          .input {
            width: 100%;
            height: 80rpx;
            padding: 0 20rpx 0 70rpx;
            border: 2rpx solid #e0e0e0;
            border-radius: 12rpx;
            font-size: 28rpx;
            
            &:focus {
              border-color: #667eea;
            }
          }
        }
        
        .error-msg {
          display: block;
          font-size: 24rpx;
          color: #f44336;
          margin-top: 8rpx;
          margin-left: 10rpx;
        }
        
        &.checkbox-item {
          .checkbox-wrapper {
            display: flex;
            align-items: center;
            padding: 20rpx 0;
            
            .checkbox {
              display: flex;
              align-items: center;
              justify-content: center;
              width: 36rpx;
              height: 36rpx;
              border: 2rpx solid #ccc;
              border-radius: 6rpx;
              margin-right: 15rpx;
              font-size: 24rpx;
              color: #fff;
              
              &.checked {
                background-color: #667eea;
                border-color: #667eea;
                &::after {
                  content: '✓';
                }
              }
            }
            
            .agreement-text {
              font-size: 24rpx;
              color: #666;
            }
            
            .agreement-link {
              font-size: 24rpx;
              color: #667eea;
              text-decoration: underline;
            }
          }
        }
      }
      
      .submit-btn {
        width: 100%;
        height: 80rpx;
        background: #667eea;
        color: #fff;
        border: none;
        border-radius: 40rpx;
        font-size: 32rpx;
        font-weight: 500;
        
        &.disabled {
          background-color: #ccc;
        }
      }
      
      .login-link {
        text-align: center;
        margin-top: 30rpx;
        
        .link-text {
          color: #667eea;
          font-weight: 500;
        }
      }
    }
  }
  
  .loading-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    
    .loading-content {
      background-color: #fff;
      padding: 40rpx;
      border-radius: 20rpx;
      display: flex;
      flex-direction: column;
      align-items: center;
      
      .loading-spinner {
        width: 40rpx;
        height: 40rpx;
        border: 4rpx solid #f0f0f0;
        border-top: 4rpx solid #667eea;
        border-radius: 50%;
        animation: spin 1s linear infinite;
      }
      
      .loading-text {
        margin-top: 20rpx;
        font-size: 28rpx;
        color: #666;
      }
    }
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>