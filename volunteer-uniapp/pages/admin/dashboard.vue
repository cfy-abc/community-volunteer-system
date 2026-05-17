<template>
  <view class="page">
    <view class="top-bar">
      <text class="title">管理后台</text>
      <text class="logout" @tap="handleLogout">退出</text>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="loadErr" class="err" @tap="loadAll">{{ loadErr }}（点击重试）</view>

    <template v-else>
      <!-- 统计卡片 -->
      <view class="stats">
        <view class="stat s1"><text class="n">{{ d.totalUsers }}</text><text class="l">用户</text></view>
        <view class="stat s2"><text class="n">{{ d.totalActivities }}</text><text class="l">活动</text></view>
        <view class="stat s3"><text class="n">{{ d.totalOrganizations }}</text><text class="l">组织</text></view>
        <view class="stat s4"><text class="n">{{ d.totalVolunteerHours }}</text><text class="l">总时长(h)</text></view>
      </view>

      <!-- 快捷入口 -->
      <view class="actions">
        <view class="act" @tap="go('users')"><text>&#128101;</text><text>用户管理</text></view>
        <view class="act" @tap="go('activities')"><text>&#128203;</text><text>活动管理</text></view>
        <view class="act" @tap="go('organizations')"><text>&#127970;</text><text>组织管理</text></view>
        <view class="act" @tap="go('sign-approvals')"><text>✅</text><text>签到审批</text></view>
      </view>

      <!-- 月度服务时长趋势（折线图） -->
      <view class="chart-card">
        <view class="picker-row">
          <picker mode="selector" :range="yearOptions" :value="adminYearIdx" @change="onAdminYearChange">
            <view class="picker-val">{{ adminYear }}年 &#9660;</view>
          </picker>
          <picker mode="selector" :range="monthOptions" :value="adminMonthIdx" @change="onAdminMonthChange">
            <view class="picker-val">{{ adminMonth ? adminMonth + '月' : '全部月份' }} &#9660;</view>
          </picker>
          <text class="picker-sum" v-if="adminFilteredHours > 0">合计 {{ adminFilteredHours }}h</text>
        </view>
        <service-chart type="line" title="月度服务时长趋势" :data="adminFilteredMonthly" />
      </view>

      <!-- 活动类型分布（柱状图） -->
      <view class="chart-card">
        <service-chart type="bar" title="活动类型分布 (h)" :data="typeData" />
      </view>

      <!-- 志愿者排行 -->
      <view class="chart-card">
        <text class="sec-title">志愿时长排行 TOP10</text>
        <view class="rank" v-for="(u, i) in topList" :key="i">
          <text class="rk" :class="i<3?'top':''">{{ i+1 }}</text>
          <text class="rn">{{ u.realName }}</text>
          <text class="rh">{{ u.volunteerHours || 0 }}h</text>
        </view>
        <text v-if="!topList.length" class="no-data">暂无数据</text>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import ServiceChart from '@/components/charts/service-chart.vue'

const d = ref({ totalUsers:0, totalActivities:0, totalOrganizations:0, totalVolunteerHours:0, topVolunteers:[], topOrganizations:[], activityTypeDistribution:[], monthlyVolunteerHours:[] })
const loading = ref(true)
const loadErr = ref('')

const topList = computed(() => d.value.topVolunteers || [])
const typeData = computed(() => (d.value.activityTypeDistribution || []).map(t => ({ name: t.name, value: t.value })))

// 年月选择器
const years = [2025, 2026, 2027, 2028]
const adminYear = ref(new Date().getFullYear())
const adminMonth = ref(0)
const adminYearIdx = ref(1)
const adminMonthIdx = ref(0)
const yearOptions = computed(() => years.map(y => y + '年'))
const monthOptions = computed(() => ['全部月份', '1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'])

const rawMonthly = computed(() => d.value.monthlyVolunteerHours || [])

const adminFilteredMonthly = computed(() => {
  if (!rawMonthly.value.length) return []
  if (adminMonth.value === 0) return rawMonthly.value
  const target = `${adminYear.value}-${String(adminMonth.value).padStart(2, '0')}`
  return rawMonthly.value.filter(s => s.month && s.month.startsWith(target))
})

const adminFilteredHours = computed(() => {
  return adminFilteredMonthly.value.reduce((sum, s) => sum + (parseFloat(s.hours) || 0), 0)
})

const onAdminYearChange = (e) => {
  adminYearIdx.value = e.detail.value
  adminYear.value = years[e.detail.value]
}
const onAdminMonthChange = (e) => {
  adminMonthIdx.value = e.detail.value
  adminMonth.value = e.detail.value
}

const loadAll = async () => {
  loading.value = true
  loadErr.value = ''
  try {
    const res = await request.get('/admin/dashboard')
    if (res.code === 200 && res.data) {
      d.value = { ...d.value, ...res.data }
    } else {
      loadErr.value = res.msg || '加载失败'
    }
  } catch (e) {
    loadErr.value = e.message || '网络错误'
  } finally {
    loading.value = false
  }
}

const go = (p) => {
  const urls = { users:'/pages/admin/users', activities:'/pages/admin/activities', organizations:'/pages/admin/organizations', 'sign-approvals':'/pages/admin/sign-approvals' }
  uni.navigateTo({ url: urls[p] })
}

const handleLogout = () => {
  uni.removeStorageSync('adminToken')
  uni.redirectTo({ url: '/pages/auth/admin-login' })
}

onMounted(() => loadAll())
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #0f1923; padding: 0 0 40rpx; }
.top-bar { display: flex; justify-content: space-between; align-items: center; padding: 30rpx 30rpx 20rpx; background: #1a2736;
  .title { font-size: 36rpx; font-weight: 700; color: #fff; }
  .logout { font-size: 26rpx; color: #f44336; }
}
.loading, .err { text-align: center; padding: 100rpx 0; color: #8899aa; font-size: 28rpx; }
.stats { display: flex; padding: 20rpx 20rpx; gap: 12rpx; }
.stat { flex: 1; background: #1a2736; border-radius: 14rpx; padding: 28rpx 16rpx; text-align: center;
  .n { display: block; font-size: 44rpx; font-weight: 800; }
  .l { display: block; font-size: 22rpx; margin-top: 4rpx; color: #8899aa; }
  &.s1 .n { color: #667eea; } &.s2 .n { color: #4caf50; } &.s3 .n { color: #ff9800; } &.s4 .n { color: #00bcd4; }
}
.actions { display: flex; padding: 0 20rpx; gap: 16rpx; margin-bottom: 20rpx; }
.act { flex: 1; background: #1a2736; border-radius: 14rpx; padding: 24rpx 0; text-align: center; color: #8899aa; font-size: 24rpx;
  text:first-child { display: block; font-size: 40rpx; margin-bottom: 6rpx; }
}
.chart-card { background: #1a2736; border-radius: 14rpx; margin: 0 20rpx 16rpx; padding: 24rpx; }
.picker-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 16rpx;
  .picker-val {
    font-size: 24rpx;
    color: #667eea;
    padding: 8rpx 20rpx;
    background: #253544;
    border-radius: 10rpx;
  }
  .picker-sum {
    margin-left: auto;
    font-size: 24rpx;
    color: #00bcd4;
    font-weight: 600;
  }
}
.sec-title { font-size: 28rpx; font-weight: 600; color: #ccd6e0; display: block; margin-bottom: 20rpx; }
.rank { display: flex; align-items: center; padding: 12rpx 0; border-bottom: 1rpx solid #253544;
  .rk { width: 44rpx; height: 44rpx; border-radius: 50%; background: #253544; color: #8899aa; font-size: 22rpx; display: flex; align-items: center; justify-content: center; margin-right: 16rpx;
    &.top { background: #667eea; color: #fff; }
  }
  .rn { flex: 1; font-size: 26rpx; color: #ccd6e0; }
  .rh { font-size: 26rpx; color: #667eea; font-weight: 600; }
}
.no-data { display: block; text-align: center; color: #556; font-size: 24rpx; padding: 20rpx 0; }
</style>
