<template>
  <view class="apply-container">
    <!-- 自定义导航栏 -->
    <view class="custom-navbar">
      <view class="nav-left" @tap="goBack">
        <text class="iconfont icon-back"></text>
      </view>
      <view class="nav-center">
        <text class="nav-title">活动报名</text>
      </view>
      <view class="nav-right"></view>
    </view>

    <!-- 活动信息 -->
    <view class="activity-info">
      <image :src="activity.coverImage || '/static/images/default-activity.jpg'" class="activity-cover" mode="aspectFill"></image>
      <view class="activity-details">
        <text class="activity-title">{{ activity.title }}</text>
        <view class="activity-meta">
          <view class="meta-item">
            <text class="iconfont icon-time"></text>
            <text class="meta-text">{{ formatDate(activity.startTime) }}</text>
          </view>
          <view class="meta-item">
            <text class="iconfont icon-location"></text>
            <text class="meta-text">{{ activity.location }}</text>
          </view>
          <view class="meta-item">
            <text class="iconfont icon-users"></text>
            <text class="meta-text">{{ activity.appliedCount }}/{{ activity.maxParticipants }}人</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 报名表单 -->
    <view class="apply-form">
      <view class="form-section">
        <text class="section-title">个人信息</text>
        <view class="form-item">
          <text class="label">姓名</text>
          <input 
            class="input" 
            type="text" 
            placeholder="请输入您的姓名" 
            v-model="formData.name"
            @blur="validateField('name')"
          />
          <text v-if="errors.name" class="error-msg">{{ errors.name }}</text>
        </view>
        <view class="form-item">
          <text class="label">手机号</text>
          <input 
            class="input" 
            type="number" 
            placeholder="请输入您的手机号" 
            v-model="formData.phone"
            @blur="validateField('phone')"
          />
          <text v-if="errors.phone" class="error-msg">{{ errors.phone }}</text>
        </view>
        <view class="form-item">
          <text class="label">邮箱</text>
          <input 
            class="input" 
            type="text" 
            placeholder="请输入您的邮箱" 
            v-model="formData.email"
            @blur="validateField('email')"
          />
          <text v-if="errors.email" class="error-msg">{{ errors.email }}</text>
        </view>
      </view>

      <view class="form-section">
        <text class="section-title">报名信息</text>
        <view class="form-item">
          <text class="label">紧急联系人</text>
          <input 
            class="input" 
            type="text" 
            placeholder="请输入紧急联系人姓名" 
            v-model="formData.emergencyContact"
            @blur="validateField('emergencyContact')"
          />
          <text v-if="errors.emergencyContact" class="error-msg">{{ errors.emergencyContact }}</text>
        </view>
        <view class="form-item">
          <text class="label">紧急联系电话</text>
          <input 
            class="input" 
            type="number" 
            placeholder="请输入紧急联系电话" 
            v-model="formData.emergencyPhone"
            @blur="validateField('emergencyPhone')"
          />
          <text v-if="errors.emergencyPhone" class="error-msg">{{ errors.emergencyPhone }}</text>
        </view>
        <view class="form-item textarea-item">
          <text class="label">备注</text>
          <textarea 
            class="textarea" 
            placeholder="请输入备注信息（可选）" 
            v-model="formData.remarks"
            maxlength="200"
          />
        </view>
      </view>

      <!-- 同意条款 -->
      <view class="agreement-section">
        <view class="agreement-checkbox" @tap="toggleAgreement">
          <text class="checkbox" :class="{ checked: formData.agreed }"></text>
          <text class="agreement-text">我已阅读并同意</text>
          <text class="agreement-link" @tap.stop="viewAgreement">《活动报名条款》</text>
        </view>
        <text v-if="errors.agreed" class="error-msg">{{ errors.agreed }}</text>
      </view>
    </view>

    <!-- 底部按钮 -->
    <view class="apply-footer">
      <button 
        class="submit-btn" 
        :class="{ disabled: !canSubmit }"
        @tap="submitApply"
      >
        确认报名
      </button>
    </view>

    <!-- 加载提示 -->
    <view class="loading-overlay" v-if="loading">
      <view class="loading-content">
        <view class="loading-spinner"></view>
        <text class="loading-text">提交中...</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
// import { activityApi } from '@/api/index' // 暂时未使用，可保留或注释

// 页面参数
const activityId = ref('')

onLoad((options) => {
  activityId.value = options.id || ''
  loadData()
})

// 数据
const activity = ref({})
const loading = ref(false)

// 表单数据
const formData = reactive({
  name: '',
  phone: '',
  email: '',
  emergencyContact: '',
  emergencyPhone: '',
  remarks: '',
  agreed: false
})

// 验证错误
const errors = reactive({
  name: '',
  phone: '',
  email: '',
  emergencyContact: '',
  emergencyPhone: '',
  agreed: ''
})

// 模拟活动详情
const mockActivity = {
  id: 1,
  title: '社区环保清洁活动',
  startTime: '2024-01-15 09:00',
  location: '市中心公园',
  appliedCount: 45,
  maxParticipants: 50,
  coverImage: '/static/images/activity1.jpg'
}

// 计算属性
const canSubmit = computed(() => {
  return formData.name && 
         formData.phone && 
         formData.email && 
         formData.emergencyContact && 
         formData.emergencyPhone && 
         formData.agreed &&
         !Object.values(errors).some(error => error)
})

// 方法
const loadData = async () => {
  try {
    // 模拟加载活动详情
    activity.value = { ...mockActivity }
    
    // 如果用户已登录，尝试获取用户信息填充表单
    const userInfo = uni.getStorageSync('userInfo')
    if (userInfo) {
      formData.name = userInfo.realName || ''
      formData.phone = userInfo.phone || ''
      formData.email = userInfo.email || ''
    }
  } catch (error) {
    console.error('加载活动详情失败:', error)
    uni.showToast({
      title: '加载活动信息失败',
      icon: 'none'
    })
  }
}

const goBack = () => {
  uni.navigateBack()
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`
}

const validateField = (field) => {
  const value = formData[field]
  
  switch (field) {
    case 'name':
      if (!value.trim()) {
        errors.name = '请输入姓名'
      } else if (value.length < 2) {
        errors.name = '姓名至少2个字符'
      } else {
        errors.name = ''
      }
      break
    case 'phone':
      if (!value.trim()) {
        errors.phone = '请输入手机号'
      } else if (!/^1[3-9]\d{9}$/.test(value)) {
        errors.phone = '请输入正确的手机号'
      } else {
        errors.phone = ''
      }
      break
    case 'email':
      if (value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
        errors.email = '请输入正确的邮箱格式'
      } else {
        errors.email = ''
      }
      break
    case 'emergencyContact':
      if (!value.trim()) {
        errors.emergencyContact = '请输入紧急联系人'
      } else {
        errors.emergencyContact = ''
      }
      break
    case 'emergencyPhone':
      if (!value.trim()) {
        errors.emergencyPhone = '请输入紧急联系电话'
      } else if (!/^1[3-9]\d{9}$/.test(value)) {
        errors.emergencyPhone = '请输入正确的手机号'
      } else {
        errors.emergencyPhone = ''
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
    title: '活动报名条款',
    content: '1. 报名成功后请按时参加活动\n2. 如有特殊情况无法参加，请提前请假\n3. 活动期间请注意安全\n4. 服从现场工作人员安排',
    showCancel: false,
    confirmText: '我知道了'
  })
}

const submitApply = async () => {
  if (!canSubmit.value) {
    uni.showToast({
      title: '请完善报名信息',
      icon: 'none'
    })
    return
  }

  if (!formData.agreed) {
    errors.agreed = '请阅读并同意活动报名条款'
    uni.showToast({
      title: '请同意报名条款',
      icon: 'none'
    })
    return
  }

  loading.value = true

  try {
    // 模拟报名请求
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    // 模拟成功响应
    uni.showToast({
      title: '报名成功',
      icon: 'success'
    })

    // 延迟跳转到我的报名页面
    setTimeout(() => {
      uni.redirectTo({
        url: '/pages/my/applications'
      })
    }, 1500)
  } catch (error) {
    console.error('报名失败:', error)
    uni.showToast({
      title: '报名失败，请重试',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 注意：onLoad 已经调用 loadData，因此不再需要单独的 onMounted 中的 loadData
// onMounted(() => {
//   loadData()
// })
</script>

<style lang="scss">
.apply-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 120rpx;
  padding-top: 88rpx; // 导航栏高度
  
  .custom-navbar {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    height: 88rpx;
    background-color: #fff;
    display: flex;
    align-items: center;
    padding: 0 30rpx;
    z-index: 100;
    
    .nav-left {
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
    
    .nav-center {
      flex: 1;
      text-align: center;
      
      .nav-title {
        font-size: 36rpx;
        font-weight: bold;
        color: #333;
      }
    }
    
    .nav-right {
      width: 60rpx;
    }
  }
  
  .activity-info {
    background-color: #fff;
    margin: 20rpx;
    border-radius: 20rpx;
    overflow: hidden;
    
    .activity-cover {
      width: 100%;
      height: 200rpx;
    }
    
    .activity-details {
      padding: 20rpx;
      
      .activity-title {
        font-size: 32rpx;
        font-weight: bold;
        color: #333;
        margin-bottom: 15rpx;
      }
      
      .activity-meta {
        display: flex;
        flex-direction: column;
        gap: 10rpx;
        
        .meta-item {
          display: flex;
          align-items: center;
          font-size: 26rpx;
          color: #666;
          
          .iconfont {
            margin-right: 8rpx;
            font-size: 24rpx;
          }
        }
      }
    }
  }
  
  .apply-form {
    padding: 0 20rpx;
    
    .form-section {
      background-color: #fff;
      border-radius: 20rpx;
      padding: 30rpx;
      margin-bottom: 20rpx;
      
      .section-title {
        display: block;
        font-size: 32rpx;
        font-weight: bold;
        color: #333;
        margin-bottom: 20rpx;
      }
      
      .form-item {
        margin-bottom: 30rpx;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .label {
          display: block;
          font-size: 28rpx;
          color: #333;
          margin-bottom: 10rpx;
          font-weight: 500;
        }
        
        .input {
          width: 100%;
          height: 80rpx;
          border: 2rpx solid #e0e0e0;
          border-radius: 12rpx;
          padding: 0 20rpx;
          font-size: 28rpx;
          
          &:focus {
            border-color: #667eea;
          }
        }
        
        .textarea-item {
          .textarea {
            width: 100%;
            min-height: 120rpx;
            border: 2rpx solid #e0e0e0;
            border-radius: 12rpx;
            padding: 20rpx;
            font-size: 28rpx;
            background-color: #fff;
            
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
        }
      }
    }
    
    .agreement-section {
      background-color: #fff;
      border-radius: 20rpx;
      padding: 30rpx;
      margin-bottom: 20rpx;
      
      .agreement-checkbox {
        display: flex;
        align-items: center;
        
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
          font-size: 26rpx;
          color: #666;
        }
        
        .agreement-link {
          font-size: 26rpx;
          color: #667eea;
          text-decoration: underline;
        }
      }
      
      .error-msg {
        display: block;
        font-size: 24rpx;
        color: #f44336;
        margin-top: 8rpx;
      }
    }
  }
  
  .apply-footer {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 20rpx 30rpx;
    background-color: #fff;
    border-top: 1rpx solid #f0f0f0;
    
    .submit-btn {
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
