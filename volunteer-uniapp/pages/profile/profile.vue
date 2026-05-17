<template>
  <view class="profile-page">
    <!-- 头部卡片 -->
    <view class="header-card">
      <view class="header-bg"></view>
      <view class="header-content">
        <view class="avatar-wrap">
          <image :src="profile.avatar || '/static/images/default-avatar.png'" class="avatar" mode="aspectFill" />
        </view>
        <view class="header-info">
          <text class="nickname">{{ displayName }}</text>
          <view class="tag-row">
            <text class="tag" :class="roleTagClass">{{ profile.isOrgUser ? '组织用户' : '志愿者' }}</text>
            <text class="tag" :class="verifyTagClass">{{ verifyLabel }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 数据卡片 -->
    <view class="stats-card">
      <view class="stat" v-for="s in stats" :key="s.label">
        <text class="stat-val">{{ s.value }}</text>
        <text class="stat-lbl">{{ s.label }}</text>
      </view>
    </view>

    <!-- 编辑个人信息 -->
    <view class="section-card">
      <view class="sec-title">
        <text class="sec-icon">&#9998;</text>
        <text>编辑个人信息</text>
      </view>
      <view class="edit-item" @tap="handleChangeAvatar">
        <text class="edit-label">更换头像</text>
        <view class="edit-right">
          <image :src="profile.avatar || '/static/images/default-avatar.png'" class="edit-avatar" mode="aspectFill" />
          <text class="arrow">></text>
        </view>
      </view>
      <view class="edit-item" @tap="editField('用户名', 'username', profile.username)">
        <text class="edit-label">用户名</text>
        <view class="edit-right">
          <text class="edit-val">{{ profile.username || '点击设置' }}</text>
          <text class="arrow">></text>
        </view>
      </view>
      <view class="edit-item">
        <text class="edit-label">真实姓名</text>
        <text class="edit-val">{{ profile.realName || '—' }}</text>
      </view>
      <view class="edit-item" @tap="editPhone">
        <text class="edit-label">手机号</text>
        <view class="edit-right">
          <text class="edit-val">{{ profile.phone || '点击设置' }}</text>
          <text class="arrow">></text>
        </view>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="section-card">
      <view class="sec-title">
        <text class="sec-icon">&#9776;</text>
        <text>功能服务</text>
      </view>
      <view class="menu-item" v-for="m in menus" :key="m.key" @tap="goTo(m.key)">
        <view class="menu-left">
          <view class="menu-icon" :style="{ background: m.color }">
            <text class="menu-icon-text">{{ m.icon }}</text>
          </view>
          <text class="menu-label">{{ m.label }}</text>
        </view>
        <text class="arrow">></text>
      </view>
    </view>

    <!-- 服务数据看板 -->
    <view class="section-card" v-if="hasToken">
      <view class="sec-title">
        <text class="sec-icon">&#9733;</text>
        <text>服务看板</text>
        <text class="sec-total">总计 {{ allTimeHours }}h</text>
      </view>
      <view v-if="statsLoading" class="chart-loading">
        <text>图表加载中...</text>
      </view>
      <view v-else-if="chartError" class="chart-error" @tap="loadStats">
        <text>图表加载失败，点击重试</text>
      </view>
      <view v-else>
        <!-- 年月选择器 -->
        <view class="month-picker">
          <view class="picker-row">
            <picker mode="selector" :range="yearOptions" :value="yearIndex" @change="onYearChange">
              <view class="picker-val">{{ selectedYear }}年 &#9660;</view>
            </picker>
            <picker mode="selector" :range="monthOptions" :value="monthIndex" @change="onMonthChange">
              <view class="picker-val">{{ selectedMonth ? selectedMonth + '月' : '全部月份' }} &#9660;</view>
            </picker>
          </view>
          <text class="picker-total" v-if="filteredMonthlyHours > 0">所选时段合计: <text class="highlight">{{ filteredMonthlyHours }}h</text></text>
        </view>
        <ServiceChart type="bar" title="月度服务时长 (h)" :data="filteredMonthlyStats" />
        <ServiceChart type="pie" title="活动类型分布 (h)" :data="typeDistribution" />
      </view>
    </view>

    <!-- 编辑弹窗 -->
    <view class="modal-mask" v-if="showEditModal" @tap="showEditModal = false">
      <view class="modal-card" @tap.stop>
        <text class="modal-title">修改{{ editingField }}</text>
        <input class="modal-input" v-model="editValue" :placeholder="'请输入' + editingField" />
        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showEditModal = false">取消</button>
          <button class="modal-btn confirm" @tap="saveEdit">保存</button>
        </view>
      </view>
    </view>

    <!-- 退出 -->
    <view class="logout-wrap" v-if="hasToken">
      <button class="logout-btn" @tap="handleLogout">退 出 登 录</button>
    </view>

    <!-- 未登录引导 -->
    <view class="login-guide" v-if="!hasToken">
      <view class="guide-card">
        <image src="/static/images/default-avatar.png" class="guide-avatar" mode="aspectFill" />
        <text class="guide-title">登录后查看个人中心</text>
        <text class="guide-desc">登录后可查看志愿时长、服务看板和更多功能</text>
        <button class="guide-btn" @tap="goToLogin">立即登录</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '@/utils/request'
import store from '@/store'
import ServiceChart from '@/components/charts/service-chart.vue'

const profile = ref({})
const hasToken = ref(false)
const isAdmin = computed(() => !!uni.getStorageSync('adminToken'))
const monthlyStats = ref([])
const typeDistribution = ref([])
const statsLoading = ref(false)
const chartError = ref(false)
const allTimeHours = ref(0)

// 从storage读取作为API失败时的兜底
const storageUser = () => {
  try {
    const cached = uni.getStorageSync('userInfo')
    return cached && typeof cached === 'object' ? cached : {}
  } catch { return {} }
}

const displayName = computed(() => {
  return profile.value.realName || profile.value.username || storageUser().username || storageUser().realName || '未登录'
})

const verifyLabel = computed(() => {
  const s = profile.value.realNameStatus
  if (s === 1) return '已实名'
  if (s === 2) return '认证中'
  return '未实名'
})
const verifyTagClass = computed(() => {
  const s = profile.value.realNameStatus
  if (s === 1) return 'tag-green'
  if (s === 2) return 'tag-orange'
  return 'tag-red'
})
const roleTagClass = computed(() => profile.value.isOrgUser ? 'tag-blue' : 'tag-purple')

const stats = computed(() => [
  { label: '可用时长(h)', value: profile.value.volunteerHours || 0 },
  { label: '累计赚取', value: profile.value.totalEarnedHours || 0 },
  { label: '累计支出', value: profile.value.totalSpentHours || 0 }
])

const menus = computed(() => {
  const list = [
    { key: 'myApplications', label: '我的报名', icon: '📋', color: '#667eea' },
    { key: 'myCertificates', label: '我的证书', icon: '🏆', color: '#4caf50' },
    { key: 'publishActivity', label: '发布活动', icon: '📝', color: '#ff9800' },
    { key: 'verifyIdentity', label: verifyLabel.value === '已实名' ? '实名认证 ✓' : '实名认证（待完成）', icon: '🛡', color: verifyLabel.value === '已实名' ? '#4caf50' : '#f44336' },
    { key: 'orgUpgrade', label: profile.value.isOrgUser ? '已是组织用户' : '申请成为组织用户', icon: '🏢', color: '#2196f3' }
  ]
  if (isAdmin.value) {
    list.push({ key: 'adminLogin', label: '管理后台', icon: '⚙', color: '#607d8b' })
  }
  return list
})

const loadProfile = async () => {
  try {
    const res = await request.get('/api/users/info')
    if (res.code === 200 && res.data) {
      profile.value = res.data
      // 同步更新storage（用于首页等快速读取）
      uni.setStorageSync('userInfo', res.data)
    }
  } catch (err) {
    console.error('加载个人信息失败:', err)
    // API失败时从storage兜底
    const cached = storageUser()
    if (cached.username) {
      profile.value = { ...profile.value, ...cached }
    }
  }
}

// 年月选择器
const selectedYear = ref(new Date().getFullYear())
const selectedMonth = ref(0) // 0 = 全部月份
const yearIndex = ref(0)
const monthIndex = ref(0)
const years = [2025, 2026, 2027, 2028]
const yearOptions = computed(() => years.map(y => y + '年'))
const monthOptions = computed(() => ['全部月份', '1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'])

const filteredMonthlyStats = computed(() => {
  if (!monthlyStats.value || monthlyStats.value.length === 0) return []
  if (selectedMonth.value === 0) return monthlyStats.value
  const target = `${selectedYear.value}-${String(selectedMonth.value).padStart(2, '0')}`
  return monthlyStats.value.filter(s => s.month && s.month.startsWith(target))
})

const filteredMonthlyHours = computed(() => {
  return filteredMonthlyStats.value.reduce((sum, s) => sum + (parseFloat(s.hours) || 0), 0)
})

const onYearChange = (e) => {
  yearIndex.value = e.detail.value
  selectedYear.value = years[e.detail.value]
}

const onMonthChange = (e) => {
  monthIndex.value = e.detail.value
  selectedMonth.value = e.detail.value // 0 = 全部
}

const loadStats = async () => {
  statsLoading.value = true
  chartError.value = false
  try {
    const res = await request.get('/api/users/stats')
    if (res.code === 200 && res.data) {
      monthlyStats.value = res.data.monthlyStats || []
      typeDistribution.value = res.data.typeDistribution || []
      allTimeHours.value = res.data.allTimeHours || res.data.totalEarnedHours || 0
      // 初始化年份选择器索引
      yearIndex.value = years.indexOf(selectedYear.value)
      if (yearIndex.value < 0) yearIndex.value = 1
    }
  } catch (err) {
    console.error('加载统计数据失败:', err)
    chartError.value = true
  } finally {
    statsLoading.value = false
  }
}

// 编辑弹窗（通用：用户名/手机号等）
const showEditModal = ref(false)
const editingField = ref('')
const editingKey = ref('')
const editValue = ref('')
const editField = (label, key, currentVal) => {
  editingField.value = label
  editingKey.value = key
  editValue.value = currentVal || ''
  showEditModal.value = true
}
const editPhone = () => editField('手机号', 'phone', profile.value.phone)
const saveEdit = async () => {
  if (!editValue.value.trim()) {
    uni.showToast({ title: '请输入内容', icon: 'none' })
    return
  }
  try {
    const payload = {}
    payload[editingKey.value] = editValue.value.trim()
    const res = await request.put('/api/users/update', payload)
    if (res.code === 200) {
      profile.value[editingKey.value] = editValue.value.trim()
      uni.setStorageSync('userInfo', profile.value)
      uni.showToast({ title: '修改成功', icon: 'success' })
      showEditModal.value = false
    } else {
      uni.showToast({ title: res.msg || '修改失败', icon: 'none' })
    }
  } catch (err) {
    uni.showToast({ title: '网络错误', icon: 'none' })
  }
}

const handleChangeAvatar = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const tempFilePath = res.tempFilePaths[0]
      uni.showLoading({ title: '上传中...' })
      try {
        const uploadRes = await new Promise((resolve, reject) => {
          uni.uploadFile({
            url: request.config.baseUrl + '/upload',
            filePath: tempFilePath,
            name: 'file',
            header: { Authorization: `Bearer ${uni.getStorageSync('token')}` },
            success: (r) => { try { resolve(JSON.parse(r.data)) } catch (e) { reject(e) } },
            fail: reject
          })
        })
        if (uploadRes.code === 200) {
          const url = uploadRes.data.url
          const avatarUrl = url.startsWith('http') ? url : (request.config.baseUrl || '') + url
          profile.value.avatar = avatarUrl
          uni.setStorageSync('userInfo', profile.value)
          // 持久化头像URL到后端数据库
          try {
            await request.put('/api/users/update', { avatar: avatarUrl })
          } catch (e) {
            console.error('保存头像到后端失败:', e)
          }
          uni.showToast({ title: '头像更新成功', icon: 'success' })
        } else {
          uni.showToast({ title: uploadRes.msg || '上传失败', icon: 'none' })
        }
      } catch (err) {
        uni.showToast({ title: '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

const goTo = (page) => {
  const routes = {
    myApplications: '/pages/my/applications',
    myCertificates: '/pages/my/certificates',
    publishActivity: '/pages/activities/publish',
    adminLogin: '/pages/auth/admin-login',
    verifyIdentity: '/pages/auth/verify-identity',
    orgUpgrade: '/pages/my/org-upgrade'
  }
  uni.navigateTo({ url: routes[page] })
}
const goToLogin = () => uni.navigateTo({ url: '/pages/auth/login' })

const handleLogout = () => {
  uni.showModal({
    title: '确认退出',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        uni.removeStorageSync('token')
        uni.removeStorageSync('userInfo')
        uni.removeStorageSync('adminToken')
        // Clear Vuex store
        store.commit('auth/SET_TOKEN', '')
        store.commit('auth/SET_USER_INFO', null)
        profile.value = {}
        hasToken.value = false
        uni.reLaunch({ url: '/pages/auth/login' })
      }
    }
  })
}

onShow(() => {
  const token = uni.getStorageSync('token')
  hasToken.value = !!token
  if (token) {
    loadProfile()
    loadStats()
  }
})
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: #f0f2f5;
  padding-bottom: 40rpx;
}

// 头部卡片
.header-card {
  margin: 0 0 20rpx;
  position: relative;
  .header-bg {
    height: 260rpx;
    background: linear-gradient(160deg, #5b6abf 0%, #7b5ea7 40%, #c4738f 100%);
    border-radius: 0 0 48rpx 48rpx;
  }
  .header-content {
    display: flex;
    align-items: center;
    padding: 0 30rpx;
    margin-top: -80rpx;
    position: relative;
    z-index: 2;
  }
  .avatar-wrap {
    width: 140rpx;
    height: 140rpx;
    border-radius: 50%;
    border: 6rpx solid rgba(255,255,255,0.9);
    box-shadow: 0 8rpx 30rpx rgba(0,0,0,0.15);
    flex-shrink: 0;
    .avatar {
      width: 100%;
      height: 100%;
      border-radius: 50%;
    }
  }
  .header-info {
    margin-left: 24rpx;
    .nickname {
      font-size: 38rpx;
      font-weight: 700;
      color: #fff;
      text-shadow: 0 2rpx 6rpx rgba(0,0,0,0.2);
      display: block;
      margin-bottom: 12rpx;
    }
    .tag-row {
      display: flex;
      gap: 12rpx;
      .tag {
        font-size: 22rpx;
        padding: 4rpx 18rpx;
        border-radius: 20rpx;
        color: #fff;
        &.tag-purple { background: rgba(255,255,255,0.25); }
        &.tag-blue { background: rgba(33,150,243,0.7); }
        &.tag-green { background: rgba(76,175,80,0.75); }
        &.tag-orange { background: rgba(255,152,0,0.75); }
        &.tag-red { background: rgba(244,67,54,0.7); }
      }
    }
  }
}

// 数据卡片
.stats-card {
  display: flex;
  margin: -20rpx 30rpx 20rpx;
  background: #fff;
  border-radius: 20rpx;
  box-shadow: 0 6rpx 24rpx rgba(0,0,0,0.06);
  position: relative;
  z-index: 3;
  .stat {
    flex: 1;
    text-align: center;
    padding: 28rpx 0;
    position: relative;
    &:not(:last-child)::after {
      content: '';
      position: absolute;
      right: 0;
      top: 30%;
      height: 40%;
      width: 1rpx;
      background: #eee;
    }
    .stat-val {
      display: block;
      font-size: 42rpx;
      font-weight: 800;
      background: linear-gradient(135deg, #5b6abf, #7b5ea7);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .stat-lbl {
      display: block;
      font-size: 22rpx;
      color: #999;
      margin-top: 6rpx;
    }
  }
}

// 通用卡片
.section-card {
  background: #fff;
  border-radius: 20rpx;
  margin: 0 30rpx 20rpx;
  padding: 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
  .sec-title {
    display: flex;
    align-items: center;
    margin-bottom: 24rpx;
    font-size: 30rpx;
    font-weight: 700;
    color: #333;
    .sec-icon { font-size: 32rpx; margin-right: 12rpx; }
    .sec-total { margin-left: auto; font-size: 26rpx; color: #667eea; font-weight: 500; }
  }
}

// 编辑项
.edit-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  &:last-child { border-bottom: none; }
  .edit-label { font-size: 28rpx; color: #666; }
  .edit-right { display: flex; align-items: center; }
  .edit-avatar {
    width: 60rpx;
    height: 60rpx;
    border-radius: 50%;
    margin-right: 10rpx;
    border: 2rpx solid #eee;
  }
  .edit-val { font-size: 26rpx; color: #999; }
}

// 菜单项
.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  &:last-child { border-bottom: none; }
  .menu-left { display: flex; align-items: center; }
  .menu-icon {
    width: 52rpx;
    height: 52rpx;
    border-radius: 14rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 18rpx;
    .menu-icon-text { font-size: 26rpx; }
  }
  .menu-label { font-size: 28rpx; color: #333; }
}

.arrow { font-size: 28rpx; color: #ccc; }

// 年月选择器
.month-picker {
  margin-bottom: 24rpx;
  .picker-row {
    display: flex;
    gap: 20rpx;
    margin-bottom: 12rpx;
  }
  .picker-val {
    font-size: 26rpx;
    color: #667eea;
    padding: 12rpx 24rpx;
    background: #f0f0ff;
    border-radius: 12rpx;
    font-weight: 500;
  }
  .picker-total {
    font-size: 26rpx;
    color: #666;
    .highlight { color: #667eea; font-weight: 700; font-size: 30rpx; }
  }
}

// 看板
.chart-loading, .chart-error {
  text-align: center;
  padding: 40rpx 0;
  color: #999;
  font-size: 26rpx;
}
.chart-error { color: #667eea; }

// 编辑弹窗
.modal-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}
.modal-card {
  width: 600rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
  .modal-title { font-size: 32rpx; font-weight: 700; color: #333; display: block; margin-bottom: 30rpx; }
  .modal-input {
    width: 100%;
    height: 80rpx;
    padding: 0 24rpx;
    background: #f8f8f8;
    border: 2rpx solid #eee;
    border-radius: 14rpx;
    font-size: 28rpx;
    margin-bottom: 30rpx;
  }
  .modal-btns { display: flex; gap: 20rpx; }
  .modal-btn {
    flex: 1;
    height: 76rpx;
    border-radius: 38rpx;
    font-size: 28rpx;
    border: none;
    &.cancel { background: #f0f0f0; color: #666; }
    &.confirm { background: linear-gradient(135deg, #667eea, #7b5ea7); color: #fff; }
  }
}

// 退出按钮
.logout-wrap {
  padding: 30rpx 50rpx;
  .logout-btn {
    width: 100%;
    height: 84rpx;
    background: #fff;
    color: #f44336;
    border: 2rpx solid #f44336;
    border-radius: 42rpx;
    font-size: 30rpx;
    font-weight: 500;
  }
}

// 未登录引导
.login-guide {
  display: flex;
  justify-content: center;
  padding-top: 120rpx;
  .guide-card {
    text-align: center;
    .guide-avatar {
      width: 160rpx;
      height: 160rpx;
      border-radius: 50%;
      margin-bottom: 30rpx;
      opacity: 0.6;
    }
    .guide-title {
      display: block;
      font-size: 34rpx;
      font-weight: 600;
      color: #333;
      margin-bottom: 16rpx;
    }
    .guide-desc {
      display: block;
      font-size: 26rpx;
      color: #999;
      margin-bottom: 40rpx;
    }
    .guide-btn {
      width: 360rpx;
      height: 80rpx;
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: #fff;
      border: none;
      border-radius: 40rpx;
      font-size: 30rpx;
    }
  }
}
</style>
