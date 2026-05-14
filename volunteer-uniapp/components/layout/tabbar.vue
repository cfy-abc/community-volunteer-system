<template>
  <view class="tabbar-container" :style="{ paddingBottom: safeAreaBottom + 'px' }">
    <view class="tabbar-wrapper">
      <view 
        class="tabbar-item" 
        v-for="(item, index) in tabBarList" 
        :key="index"
        :class="{ active: currentIndex === index }"
        @tap="switchTab(index, item.pagePath)"
      >
        <view class="tabbar-icon">
          <image 
            :src="currentIndex === index ? item.selectedIconPath : item.iconPath" 
            class="icon-img"
            mode="aspectFit"
          ></image>
        </view>
        <text class="tabbar-text" :class="{ active: currentIndex === index }">{{ item.text }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// 当前选中的索引
const currentIndex = ref(0)

// TabBar 配置
const tabBarList = [
  {
    pagePath: '/pages/home/home',
    text: '首页',
    iconPath: '/static/icons/home.png',
    selectedIconPath: '/static/icons/home-active.png'
  },
  {
    pagePath: '/pages/activities/list',
    text: '活动',
    iconPath: '/static/icons/activity.png',
    selectedIconPath: '/static/icons/activity-active.png'
  },
  {
    pagePath: '/pages/my/applications',
    text: '报名',
    iconPath: '/static/icons/application.png',
    selectedIconPath: '/static/icons/application-active.png'
  },
  {
    pagePath: '/pages/profile/profile',
    text: '我的',
    iconPath: '/static/icons/profile.png',
    selectedIconPath: '/static/icons/profile-active.png'
  }
]

// 数据
const safeAreaBottom = ref(0)

// 方法
const switchTab = (index, path) => {
  currentIndex.value = index
  uni.switchTab({
    url: path
  })
}

// 根据当前路径更新选中项
const updateCurrentIndex = () => {
  const pages = getCurrentPages()
  if (pages.length === 0) return
  const currentPage = pages[pages.length - 1]
  // currentPage.route 不带前导斜杠，例如 "pages/home/home"
  const currentPath = '/' + currentPage.route
  const index = tabBarList.findIndex(item => item.pagePath === currentPath)
  if (index !== -1) {
    currentIndex.value = index
  }
}

// 获取安全区域底部距离
const getSafeAreaInfo = () => {
  const systemInfo = uni.getSystemInfoSync()
  safeAreaBottom.value = systemInfo.safeAreaInsets?.bottom || 0
}

// 页面生命周期
onMounted(() => {
  updateCurrentIndex()
  getSafeAreaInfo()
})
</script>

<style lang="scss">
.tabbar-container {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 998;
  background-color: #fff;
  border-top: 1rpx solid #f0f0f0;
  
  .tabbar-wrapper {
    display: flex;
    height: 100rpx;
  }
  
  .tabbar-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    
    &.active {
      .tabbar-text {
        color: #667eea;
        font-weight: 500;
      }
    }
    
    .tabbar-icon {
      width: 40rpx;
      height: 40rpx;
      margin-bottom: 6rpx;
      
      .icon-img {
        width: 100%;
        height: 100%;
      }
    }
    
    .tabbar-text {
      font-size: 24rpx;
      color: #666;
      
      &.active {
        color: #667eea;
      }
    }
  }
}
</style>