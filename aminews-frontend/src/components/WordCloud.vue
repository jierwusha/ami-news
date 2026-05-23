<template>
  <div class="word-cloud-container">
    <!-- 词云图内容 -->
    <div class="word-cloud-content">
      <div class="word-cloud-header">
        <h3 class="section-title">热词趋势</h3>
        <button
          class="refresh-btn"
          @click="refreshHotWords"
          :disabled="hotWordStore.isOperating"
          title="刷新热词数据"
        >
          <svg
            :class="{ rotating: hotWordStore.refreshing }"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M23 4v6h-6" />
            <path d="M1 20v-6h6" />
            <path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15" />
          </svg>
          {{ hotWordStore.refreshing ? '刷新中...' : '刷新' }}
        </button>
      </div>

      <!-- ECharts 词云图容器 - 始终渲染，但在不同状态显示不同内容 -->
      <div ref="chartContainer" class="chart-container">
        <!-- 加载状态 -->
        <div v-if="hotWordStore.loading || hotWordStore.refreshing" class="loading-state">
          <div class="loading-spinner"></div>
          <p>{{ hotWordStore.refreshing ? '正在刷新热词数据，请稍候...' : '加载热词中...' }}</p>
        </div>

        <!-- 错误状态 -->
        <div v-else-if="hotWordStore.error" class="error-state">
          <svg
            width="32"
            height="32"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <p>{{ hotWordStore.error }}</p>
          <button @click="loadHotWords" class="retry-btn">重试</button>
        </div>
      </div>

      <!-- 选中的热词显示 -->
      <div v-if="currentSelectedWord" class="selected-word-info">
        <span class="selected-word">已选择：</span>
        <span class="word-tag">{{ currentSelectedWord.word }}</span>
        <!-- <span class="word-count">({{ currentSelectedWord.count }}次)</span> -->

        <button
          v-if="currentSelectedWord"
          @click="clearSelection"
          class="clear-btn"
          title="清除选择"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
          清除选择
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import * as echarts from 'echarts'
// 导入词云插件
import 'echarts-wordcloud'
import { useHotWordStore } from '@/stores/hotwordsStore'
import type { HotWordItem } from '../api'

// Props
interface Props {
  height?: number
  selectedWord?: HotWordItem | null
}

const props = withDefaults(defineProps<Props>(), {
  height: 400,
  selectedWord: null,
})

// Emits
interface Emits {
  (e: 'word-selected', word: HotWordItem | null): void
}

const emit = defineEmits<Emits>()

// 使用热词状态管理store
const hotWordStore = useHotWordStore()

// 本地响应式数据（仅用于UI状态）
const internalSelectedWord = ref<HotWordItem | null>(null)

// 计算属性：优先使用外部传入的 selectedWord，否则使用store中的状态
const currentSelectedWord = computed(() => {
  return props.selectedWord || hotWordStore.selectedWord || internalSelectedWord.value
})

// DOM引用
const chartContainer = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

// 加载热词数据
const loadHotWords = async () => {
  await hotWordStore.fetchHotWords()

  // 如果加载成功且有数据，初始化图表
  if (hotWordStore.hasData && !hotWordStore.error) {
    // 等待DOM更新完成后再初始化图表
    await nextTick()
    // 再次确保DOM已完全渲染
    setTimeout(() => {
      initChart()
    }, 50)
  }
}

// 刷新热词数据
const refreshHotWords = async () => {
  const success = await hotWordStore.refreshHotWords()

  if (success) {
    // 刷新成功，清除本地选择状态并重新初始化图表
    internalSelectedWord.value = null
    emit('word-selected', null)

    // 重新初始化图表
    await nextTick()
    setTimeout(() => {
      initChart()
    }, 100)

    // 显示成功提示
    alert('热词数据更新成功！')
  }
}

// 初始化图表
const initChart = () => {
  console.log('initChart 被调用')
  console.log('chartContainer.value:', chartContainer.value)
  console.log('hotWordStore.hotWords length:', hotWordStore.hotWords.length)

  if (!hotWordStore.hotWords.length) {
    console.log('初始化图表失败: 没有热词数据')
    return
  }

  if (!chartContainer.value) {
    console.log('初始化图表失败: 容器不存在，重试中...')
    // 容器还未准备好，再次尝试
    setTimeout(initChart, 100)
    return
  }

  // 确保容器有正确的尺寸
  const containerRect = chartContainer.value.getBoundingClientRect()
  console.log('容器尺寸:', containerRect)

  if (containerRect.width === 0 || containerRect.height === 0) {
    console.log('容器尺寸为0，延迟初始化')
    setTimeout(initChart, 100)
    return
  }

  // 销毁现有图表实例
  if (chartInstance) {
    chartInstance.dispose()
  }

  // 创建新的图表实例
  chartInstance = echarts.init(chartContainer.value)
  console.log('图表实例创建成功:', chartInstance)

  // 准备词云数据
  const wordCloudData = hotWordStore.hotWords.map((item: HotWordItem) => ({
    name: item.word,
    value: item.count,
    textStyle: {
      color: getRandomColor(),
      // 移除 fontSize 设置，让 sizeRange 生效
      // fontSize: Math.max(20, Math.min(40, item.count * 0.8)),
    },
    itemData: item, // 保存原始数据
  }))

  console.log('词云数据:', wordCloudData)

  // 配置选项
  const option = {
    backgroundColor: '#fff',
    tooltip: {
      show: true,
      formatter: (params: unknown) => {
        const param = params as { name: string; value: number }
        return `${param.name}: ${param.value}次`
      },
    },
    series: [
      {
        type: 'wordCloud',
        shape: 'circle',
        keepAspect: false,
        left: 'center',
        top: 'center',
        width: '100%',
        height: '100%',
        sizeRange: [20, 80], // 增大字体范围：最小20px，最大80px
        rotationRange: [-0, 0],
        rotationStep: 15,
        gridSize: 8, // 增大间隙以适应更大的字体
        drawOutOfBound: false,
        textStyle: {
          fontFamily: 'sans-serif',
          fontWeight: 'bold',
        },
        emphasis: {
          focus: 'self',
          textStyle: {
            textShadowBlur: 10,
            textShadowColor: '#ddd',
          },
        },
        data: wordCloudData,
      },
    ],
  }

  // 设置配置
  chartInstance.setOption(option)
  console.log('图表配置设置完成')

  // 强制刷新图表
  setTimeout(() => {
    chartInstance?.resize()
    console.log('图表强制刷新完成')
  }, 100)

  // 绑定点击事件
  chartInstance.on('click', (params: unknown) => {
    const clickParams = params as { data: { itemData: HotWordItem } }
    const wordData = clickParams.data.itemData
    handleWordClick(wordData)
  })

  // 监听容器大小变化
  const resizeObserver = new ResizeObserver(() => {
    chartInstance?.resize()
  })
  resizeObserver.observe(chartContainer.value)
}

// 获取随机颜色
const getRandomColor = () => {
  const colors = [
    '#2196F3',
    '#4CAF50',
    '#FF9800',
    '#E91E63',
    '#9C27B0',
    '#00BCD4',
    '#8BC34A',
    '#FFEB3B',
    '#FF5722',
    '#607D8B',
    '#3F51B5',
    '#009688',
    '#CDDC39',
    '#FFC107',
    '#795548',
  ]
  return colors[Math.floor(Math.random() * colors.length)]
}

// 处理热词点击
const handleWordClick = (word: HotWordItem) => {
  // 同时更新本地状态和store状态
  internalSelectedWord.value = word
  hotWordStore.selectWord(word)
  emit('word-selected', word)
  console.log('选中热词:', word)
}

// 清除选择
const clearSelection = () => {
  internalSelectedWord.value = null
  hotWordStore.clearSelection()
  emit('word-selected', null)
}

// 响应式调整图表大小
const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

// 监听高度变化
watch(
  () => props.height,
  () => {
    if (chartInstance) {
      nextTick(() => {
        chartInstance?.resize()
      })
    }
  },
)

// 监听外部传入的 selectedWord 变化
watch(
  () => props.selectedWord,
  (newSelectedWord) => {
    // 当外部传入新的 selectedWord 时，同步到内部状态
    internalSelectedWord.value = newSelectedWord
    console.log('WordCloud: 外部 selectedWord 变化:', newSelectedWord)
  },
  { immediate: true },
)

// 生命周期
onMounted(async () => {
  console.log('WordCloud 组件已挂载')

  // 如果store中没有数据或数据过期，则加载
  if (!hotWordStore.hasData) {
    await loadHotWords()
  } else {
    // 如果store中有数据，直接初始化图表
    await nextTick()
    setTimeout(() => {
      initChart()
    }, 50)
  }

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  window.removeEventListener('resize', handleResize)

  // 注意：不要在组件卸载时强制停止刷新状态
  // 因为其他组件可能还在依赖这个状态
  // hotWordStore.forceStopRefresh() // 不调用这个方法
})

// 暴露方法
defineExpose({
  loadHotWords,
  refreshHotWords,
  clearSelection,
})
</script>

<style scoped>
.word-cloud-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  margin-bottom: 24px;
}

.word-cloud-content {
  padding: 20px;
}

.word-cloud-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #f8f9fa;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  color: #666;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: #e3f2fd;
  border-color: #2196f3;
  color: #2196f3;
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.refresh-btn svg.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.word-cloud-actions {
  display: flex;
  gap: 8px;
  /* right: 0; */
}

.clear-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  color: #666;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-left: auto;
}

.clear-btn:hover {
  background: #e0e0e0;
  color: #333;
}

.selected-word-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #e3f2fd;
  border: 1px solid #2196f3;
  border-radius: 6px;
  /* margin-bottom: 16px; */
  font-size: 0.9rem;
}

.selected-word {
  color: #666;
}

.word-tag {
  background: #2196f3;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.word-count {
  color: #666;
  font-size: 0.85rem;
}

.chart-container {
  width: 100%;
  height: 400px;
  min-height: 300px;
  background: #fafafa;
  /* border: 1px solid #e0e0e0; */
  border-radius: 8px;
  position: relative;
  margin-bottom: 16px;
}

.chart-container .loading-state,
.chart-container .error-state {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  color: #666;
}

.chart-container .loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #2196f3;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.chart-container .error-state svg {
  color: #f44336;
  margin-bottom: 12px;
}

.chart-container .error-state p {
  margin: 0 0 16px 0;
}

.chart-container .retry-btn {
  padding: 8px 16px;
  background: #2196f3;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background-color 0.2s ease;
}

.chart-container .retry-btn:hover {
  background: #1976d2;
}

/* 移动端响应式 */
@media (max-width: 767px) {
  .word-cloud-content {
    padding: 16px;
  }

  .word-cloud-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .chart-container {
    height: 280px;
    min-height: 280px;
  }

  .selected-word-info {
    flex-wrap: wrap;
    gap: 6px;
  }
}
</style>
