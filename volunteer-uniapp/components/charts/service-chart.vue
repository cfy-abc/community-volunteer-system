<template>
  <view class="chart-card">
    <view class="chart-header">
      <text class="chart-title">{{ title }}</text>
    </view>
    <view class="chart-body">
      <!-- ECharts 渲染区域 -->
      <view v-if="useEcharts && hasData" :id="chartId" class="echarts-box"></view>
      <!-- CSS 回退：柱状图 -->
      <view v-else-if="!useEcharts && hasData && (type === 'bar' || type === 'line')" class="bar-chart">
        <view class="bar-item" v-for="(item, idx) in chartData" :key="idx">
          <text class="bar-val">{{ item.hours }}h</text>
          <view class="bar-track">
            <view
              class="bar-fill"
              :style="{ height: barPct(item.hours), background: barColor(idx) }"
            ></view>
          </view>
          <text class="bar-label">{{ item.month || item.name || '' }}</text>
        </view>
      </view>
      <!-- CSS 回退：饼图 -->
      <view v-else-if="!useEcharts && hasData && type === 'pie'" class="pie-chart-fallback">
        <view class="pie-total">
          <text class="pie-total-num">{{ totalValue }}h</text>
          <text class="pie-total-lbl">总时长</text>
        </view>
        <view class="pie-legends">
          <view class="pie-legend" v-for="(item, idx) in chartData" :key="idx">
            <view class="legend-dot" :style="{ background: colors[idx % colors.length] }"></view>
            <text class="legend-name">{{ item.name || item.label || '' }}</text>
            <text class="legend-val">{{ item.hours || item.value || 0 }}h</text>
          </view>
        </view>
      </view>
      <!-- 空数据 -->
      <view v-if="!hasData" class="empty-chart">
        <text>暂无数据</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const props = defineProps({
  type: { type: String, default: 'bar' },
  title: { type: String, default: '' },
  data: { type: Array, default: () => [] }
})

const chartId = 'chart_' + Math.random().toString(36).slice(2, 8)
const colors = ['#667eea', '#7b5ea7', '#ff976a', '#4caf50', '#ff9800', '#2196f3', '#f44336', '#00bcd4']
const useEcharts = ref(false)

const hasData = computed(() => props.data && props.data.length > 0)
const chartData = computed(() => props.data || [])
const totalValue = computed(() => chartData.value.reduce((s, d) => s + (d.hours || d.value || 0), 0))
const maxValue = computed(() => Math.max(...chartData.value.map(d => d.hours || d.value || 0), 1))

const barPct = (v) => Math.max((v / maxValue.value) * 100, 4) + '%'
const barColor = (i) => `linear-gradient(180deg, ${colors[i % colors.length]}, ${colors[i % colors.length]}88)`

// 尝试加载ECharts
const tryEcharts = async () => {
  try {
    const container = document.getElementById(chartId)
    if (!container) return
    const echarts = await import('@/utils/echarts.js')
    if (!echarts || !echarts.init) return
    const instance = echarts.init(container)
    const items = chartData.value

    let option = {}
    if (props.type === 'pie') {
      option = {
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie', radius: ['45%', '70%'], center: ['50%', '50%'],
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 12 } },
          data: items.map((d, i) => ({
            name: d.name || d.month || '', value: d.value || d.hours || 0,
            itemStyle: { color: colors[i % colors.length] }
          }))
        }]
      }
    } else {
      option = {
        tooltip: {
          trigger: 'axis',
          formatter: function(p) { return p[0].name + '<br/>服务时长: ' + p[0].value + 'h' }
        },
        grid: { left: 40, right: 30, bottom: 60, top: 20 },
        xAxis: {
          type: 'category',
          data: items.map(d => d.month || d.name || ''),
          axisLabel: { fontSize: 10, color: '#999', rotate: 45 }
        },
        yAxis: { type: 'value', name: 'h', splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } } },
        dataZoom: [
          { type: 'slider', start: 0, end: 100, height: 24, bottom: 8 },
          { type: 'inside' }
        ],
        series: [{
          type: 'line',
          data: items.map(d => d.hours || d.value || 0),
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          itemStyle: { color: '#667eea' },
          areaStyle: {
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [{ offset: 0, color: '#667eea44' }, { offset: 1, color: '#667eea08' }]
            }
          }
        }]
      }
    }
    instance.setOption(option)
    useEcharts.value = true
  } catch (e) {
    // ECharts 加载失败，保留CSS回退
    useEcharts.value = false
  }
}

onMounted(() => {
  if (hasData.value) {
    // 延迟尝试ECharts加载，先显示CSS回退
    setTimeout(() => tryEcharts(), 100)
  }
})

watch(() => props.data, () => {
  if (hasData.value) setTimeout(() => tryEcharts(), 100)
}, { deep: true })
</script>

<style lang="scss" scoped>
.chart-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}
.chart-header {
  margin-bottom: 16rpx;
  .chart-title {
    font-size: 26rpx;
    font-weight: 600;
    color: #333;
  }
}
.chart-body {
  .echarts-box { width: 100%; height: 360rpx; }
}

// CSS 柱状图
.bar-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 300rpx;
  .bar-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;
    .bar-val { font-size: 18rpx; color: #667eea; font-weight: 500; margin-bottom: 4rpx; }
    .bar-track {
      width: 40rpx;
      height: 200rpx;
      background: #f0f0f0;
      border-radius: 8rpx;
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      overflow: hidden;
    }
    .bar-fill {
      width: 100%;
      border-radius: 8rpx 8rpx 0 0;
      min-height: 6rpx;
      transition: height 0.5s ease;
    }
    .bar-label { font-size: 18rpx; color: #999; margin-top: 8rpx; white-space: nowrap; }
  }
}

// CSS 饼图（图例式）
.pie-chart-fallback {
  display: flex;
  align-items: center;
  gap: 20rpx;
  .pie-total {
    width: 140rpx;
    height: 140rpx;
    border-radius: 50%;
    border: 8rpx solid #667eea;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    .pie-total-num { font-size: 32rpx; font-weight: 700; color: #333; }
    .pie-total-lbl { font-size: 20rpx; color: #999; }
  }
  .pie-legends { flex: 1; }
  .pie-legend {
    display: flex;
    align-items: center;
    padding: 10rpx 0;
    border-bottom: 1rpx solid #f8f8f8;
    &:last-child { border-bottom: none; }
    .legend-dot { width: 12rpx; height: 12rpx; border-radius: 50%; margin-right: 12rpx; flex-shrink: 0; }
    .legend-name { flex: 1; font-size: 24rpx; color: #333; }
    .legend-val { font-size: 24rpx; color: #667eea; font-weight: 500; }
  }
}

.empty-chart {
  height: 160rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 24rpx;
}
</style>
