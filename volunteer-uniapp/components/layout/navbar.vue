<template>
  <view class="navbar-container" :style="{ height: navbarHeight + 'px', paddingTop: statusBarHeight + 'px' }">
    <view class="navbar-content">
      <view class="left-area" @tap="handleLeftClick">
        <slot name="left">
          <view v-if="showBack" class="back-btn">
            <text class="iconfont icon-back"></text>
          </view>
        </slot>
      </view>
      
      <view class="center-area">
        <slot name="center">
          <text class="title">{{ title }}</text>
        </slot>
      </view>
      
      <view class="right-area">
        <slot name="right">
          <view v-if="showMenu" class="menu-btn" @tap="handleMenuClick">
            <text class="iconfont icon-menu"></text>
          </view>
        </slot>
      </view>
    </view>
    
    <!-- 阴影分隔线 -->
    <view class="navbar-shadow" v-if="showShadow"></view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// Props
const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  showBack: {
    type: Boolean,
    default: true
  },
  showMenu: {
    type: Boolean,
    default: false
  },
  showShadow: {
    type: Boolean,
    default: true
  },
  backgroundColor: {
    type: String,
    default: '#fff'
  },
  textColor: {
    type: String,
    default: '#333'
  }
})

// Events
const emit = defineEmits(['back', 'menu'])

// 数据
const statusBarHeight = ref(0)
const navbarHeight = ref(88)

// 方法
const handleLeftClick = () => {
  if (props.showBack) {
    uni.navigateBack()
    emit('back')
  }
}

const handleMenuClick = () => {
  emit('menu')
}

// 获取系统信息
const getSystemInfo = () => {
  const systemInfo = uni.getSystemInfoSync()
  statusBarHeight.value = systemInfo.statusBarHeight
  // 导航栏高度 = 状态栏高度 + 标题栏高度(44px)
  navbarHeight.value = systemInfo.statusBarHeight + 44
}

// 页面生命周期
onMounted(() => {
  getSystemInfo()
})
</script>

<style lang="scss">
.navbar-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background-color: v-bind(backgroundColor);
  
  .navbar-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 100%;
    padding: 0 30rpx;
    
    .left-area,
    .right-area {
      width: 80rpx;
      height: 100%;
      display: flex;
      align-items: center;
      
      .back-btn,
      .menu-btn {
        width: 60rpx;
        height: 60rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        background-color: #f0f0f0;
        
        .iconfont {
          font-size: 32rpx;
          color: #666;
        }
      }
    }
    
    .center-area {
      flex: 1;
      text-align: center;
      
      .title {
        font-size: 36rpx;
        font-weight: bold;
        color: v-bind(textColor);
      }
    }
  }
  
  .navbar-shadow {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 1rpx;
    background: linear-gradient(to bottom, rgba(0,0,0,0.1), transparent);
  }
}
</style>