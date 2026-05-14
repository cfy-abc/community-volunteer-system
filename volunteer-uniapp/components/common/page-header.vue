<template>
  <view class="page-header" :style="{ backgroundColor: backgroundColor }">
    <view class="header-left" @tap="onBack">
      <text class="iconfont icon-back back-icon"></text>
    </view>
    <view class="header-center">
      <text class="title">{{ title }}</text>
    </view>
    <view class="header-right">
      <slot name="right"></slot>
    </view>
  </view>
</template>

<script setup>
// Props
const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  backgroundColor: {
    type: String,
    default: '#fff'
  }
})

// Events
const emit = defineEmits(['back'])

// Methods
const onBack = () => {
  emit('back')
  uni.navigateBack()
}
</script>

<style lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  position: sticky;
  top: 0;
  z-index: 100;
  
  .header-left {
    .back-icon {
      font-size: 36rpx;
      color: #666;
    }
  }
  
  .header-center {
    flex: 1;
    text-align: center;
    
    .title {
      font-size: 36rpx;
      font-weight: bold;
      color: #333;
    }
  }
  
  .header-right {
    width: 60rpx;
  }
}
</style>
