<template>
  <view class="loading-container" v-if="visible">
    <view class="loading-overlay"></view>
    <view class="loading-content">
      <view class="loading-spinner">
        <view class="spinner-dot"></view>
        <view class="spinner-dot"></view>
        <view class="spinner-dot"></view>
      </view>
      <text class="loading-text">{{ text }}</text>
    </view>
  </view>
</template>

<script setup>
// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  text: {
    type: String,
    default: '加载中...'
  }
})
</script>

<style lang="scss">
.loading-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  
  .loading-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
  }
  
  .loading-content {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background-color: #fff;
    padding: 40rpx;
    border-radius: 20rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .loading-spinner {
      display: flex;
      gap: 10rpx;
      margin-bottom: 20rpx;
      
      .spinner-dot {
        width: 20rpx;
        height: 20rpx;
        background-color: #667eea;
        border-radius: 50%;
        animation: bounce 1.4s infinite ease-in-out both;
        
        &:nth-child(1) { animation-delay: -0.32s; }
        &:nth-child(2) { animation-delay: -0.16s; }
      }
    }
    
    .loading-text {
      font-size: 28rpx;
      color: #666;
    }
  }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1.0); }
}
</style>