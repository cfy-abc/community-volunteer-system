<template>
  <view class="activity-list">
    <!-- 顶部筛选栏 -->
    <view class="filter-bar">
      <view class="filter-item" :class="{ active: currentFilter === 'all' }" @click="switchFilter('all')">全部</view>
      <view class="filter-item" :class="{ active: currentFilter === 'open' }" @click="switchFilter('open')">进行中</view>
      <view class="filter-item" :class="{ active: currentFilter === 'closed' }" @click="switchFilter('closed')">已结束</view>
    </view>

    <!-- 活动列表 -->
    <view v-if="activityList.length > 0" class="list-content">
      <view v-for="item in activityList" :key="item.activityId" class="card">
        <!-- 活动封面 -->
        <image v-if="item.poster" :src="item.poster" mode="aspectFill" class="card-cover" />
        <view class="card-info">
          <view class="card-title">{{ item.title }}</view>
          <view class="card-desc">{{ item.description }}</view>
          <view class="card-meta">
            <text class="time">⏰ {{ formatTime(item.startTime) }}</text>
            <text class="location">📍 {{ item.location }}</text>
          </view>
          <view class="card-stats">
            <text class="reward">🏆 奖励 {{ item.rewardHours }} 小时</text>
            <text class="participants">👥 {{ item.appliedCount || 0 }}/{{ item.maxParticipants }}</text>
          </view>
          <button 
            class="register-btn" 
            :disabled="!canRegister(item)" 
            :class="{ disabled: !canRegister(item) }"
            @click="register(item.activityId)"
          >
            {{ getButtonText(item) }}
          </button>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else class="empty-state">
      <image src="/static/images/empty.png" mode="aspectFit" class="empty-img" />
      <text class="empty-text">暂无活动，快来发布第一个吧～</text>
    </view>

    <!-- 加载更多 -->
    <view v-if="hasMore" class="load-more">
      <button v-if="loadingMore" class="loading-btn" loading>加载中...</button>
      <button v-else class="load-btn" @click="loadMore">加载更多</button>
    </view>
    <view v-else-if="activityList.length > 0" class="no-more">没有更多了</view>

    <!-- 底部发布按钮（悬浮） -->
    <view class="publish-fab" @click="toPublish">
      <text class="fab-icon">+</text>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request'

export default {
  data() {
    return {
      activityList: [],
      currentFilter: 'all',   // all, open, closed
      page: 1,
      size: 10,
      total: 0,
      loading: false,
      loadingMore: false,
      hasMore: true
    }
  },
  onLoad() {
    this.fetchActivities(true)
  },
  // 上拉加载更多
  onReachBottom() {
    if (this.hasMore && !this.loadingMore) {
      this.loadMore()
    }
  },
  methods: {
    // 获取活动列表
    async fetchActivities(reset = false) {
      if (reset) {
        this.page = 1
        this.hasMore = true
        this.activityList = []
      }
      if (this.loading || this.loadingMore) return
      if (!reset && !this.hasMore) return

      this.loading = !reset ? false : true
      this.loadingMore = !reset

      try {
        const statusMap = {
          all: null,
          open: 1,      // 假设1表示进行中
          closed: 2     // 2表示已结束
        }
        const params = {
          status: statusMap[this.currentFilter],
          page: this.page,
          size: this.size
        }
        const res = await request({
          url: '/activities',
          method: 'GET',
          data: params
        })
        if (res.code === 200) {
          const { list, total } = res.data
          if (reset) {
            this.activityList = list || []
          } else {
            this.activityList = this.activityList.concat(list || [])
          }
          this.total = total
          this.hasMore = this.activityList.length < total
        } else {
          uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
        }
      } catch (err) {
        console.error(err)
        uni.showToast({ title: '网络错误', icon: 'none' })
      } finally {
        this.loading = false
        this.loadingMore = false
      }
    },
    // 切换筛选条件
    switchFilter(filter) {
      if (this.currentFilter === filter) return
      this.currentFilter = filter
      this.fetchActivities(true)
    },
    // 加载下一页
    loadMore() {
      if (!this.hasMore) return
      this.page++
      this.fetchActivities()
    },
    // 报名
    async register(activityId) {
      // 再次实时检查活动状态（避免快速重复点击）
      const activity = this.activityList.find(item => item.activityId === activityId)
      if (!this.canRegister(activity)) {
        uni.showToast({ title: this.getButtonText(activity), icon: 'none' })
        return
      }

      uni.showModal({
        title: '确认报名',
        content: `确定要报名参加「${activity.title}」吗？`,
        success: async (modalRes) => {
          if (!modalRes.confirm) return

          try {
            const res = await request({
              url: `/activities/${activityId}/register`,
              method: 'POST'
            })
            if (res.code === 200) {
              uni.showToast({ title: '报名成功', icon: 'success' })
              // 更新本地数据中的已报名人数
              activity.appliedCount = (activity.appliedCount || 0) + 1
              // 可选：强制刷新列表（取消注释以下行会重新拉取）
              // this.fetchActivities(true)
            } else {
              uni.showToast({ title: res.msg || '报名失败', icon: 'none' })
            }
          } catch (err) {
            console.error(err)
            uni.showToast({ title: '网络错误', icon: 'none' })
          }
        }
      })
    },
    // 判断是否可以报名
    canRegister(activity) {
      if (!activity) return false
      // 状态为进行中（status === 1 或 open）且未满员
      const isOpen = activity.status === 1 || activity.status === 'open'
      const hasSpace = (activity.appliedCount || 0) < activity.maxParticipants
      return isOpen && hasSpace
    },
    // 获取按钮文本
    getButtonText(activity) {
      if (!activity) return '报名'
      if (activity.status === 2 || activity.status === 'closed') return '已结束'
      if ((activity.appliedCount || 0) >= activity.maxParticipants) return '已满员'
      return '报名'
    },
    // 跳转发布页
    toPublish() {
      uni.navigateTo({ url: '/pages/activity/publish' })
    },
    formatTime(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getMonth()+1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2,'0')}`
    }
  }
}
</script>

<style scoped>
.activity-list {
  background-color: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 120rpx;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  background-color: #fff;
  padding: 20rpx 30rpx;
  position: sticky;
  top: 0;
  z-index: 10;
  border-bottom: 1rpx solid #eee;
}
.filter-item {
  flex: 1;
  text-align: center;
  font-size: 28rpx;
  color: #666;
  padding: 10rpx 0;
  border-radius: 40rpx;
  transition: all 0.2s;
}
.filter-item.active {
  background-color: #1989fa;
  color: #fff;
}

/* 卡片样式 */
.card {
  background-color: #fff;
  margin: 20rpx 30rpx;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
}
.card-cover {
  width: 100%;
  height: 300rpx;
  background-color: #eee;
}
.card-info {
  padding: 25rpx;
}
.card-title {
  font-size: 34rpx;
  font-weight: bold;
  margin-bottom: 10rpx;
  color: #333;
}
.card-desc {
  font-size: 26rpx;
  color: #666;
  line-height: 1.4;
  margin-bottom: 20rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
  color: #999;
  margin-bottom: 15rpx;
}
.card-stats {
  display: flex;
  justify-content: space-between;
  font-size: 26rpx;
  color: #ff976a;
  margin-bottom: 20rpx;
}
.reward {
  font-weight: 500;
}
.register-btn {
  background-color: #1989fa;
  color: #fff;
  border-radius: 44rpx;
  height: 68rpx;
  line-height: 68rpx;
  font-size: 28rpx;
  margin-top: 10rpx;
}
.register-btn.disabled {
  background-color: #ccc;
  color: #f0f0f0;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 0;
}
.empty-img {
  width: 300rpx;
  height: 300rpx;
}
.empty-text {
  font-size: 28rpx;
  color: #999;
  margin-top: 30rpx;
}

/* 加载更多 */
.load-more, .no-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
}
.load-btn, .loading-btn {
  background: transparent;
  font-size: 26rpx;
  color: #1989fa;
}

/* 悬浮发布按钮 */
.publish-fab {
  position: fixed;
  bottom: 50rpx;
  right: 50rpx;
  width: 100rpx;
  height: 100rpx;
  background: linear-gradient(135deg, #1989fa, #0a7ae5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.2);
  z-index: 99;
}
.fab-icon {
  font-size: 60rpx;
  color: #fff;
  font-weight: bold;
}
</style>