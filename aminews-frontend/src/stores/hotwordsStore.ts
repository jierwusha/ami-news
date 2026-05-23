import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getHotWordList, refreshHotWords as refreshHotWordsAPI } from '../api'
import type { HotWordItem } from '../api'

export const useHotWordStore = defineStore('hotWord', () => {
  // 状态数据
  const hotWords = ref<HotWordItem[]>([])
  const loading = ref(false)
  const refreshing = ref(false)
  const error = ref<string | null>(null)
  const lastUpdated = ref<Date | null>(null)
  const selectedWord = ref<HotWordItem | null>(null)

  // 刷新操作的唯一标识符，用于防止重复请求
  const refreshTaskId = ref<string | null>(null)

  // 计算属性
  const hasData = computed(() => hotWords.value.length > 0)
  const isOperating = computed(() => loading.value || refreshing.value)

  // 加载热词数据
  async function fetchHotWords() {
    // 如果正在刷新中，不允许重新加载
    if (refreshing.value) {
      console.log('热词正在刷新中，跳过加载请求')
      return
    }

    // 防止重复加载
    if (loading.value) {
      console.log('热词正在加载中，跳过重复请求')
      return
    }

    loading.value = true
    error.value = null

    try {
      const response = await getHotWordList()
      if (response.code === 200 && response.data) {
        hotWords.value = response.data
        lastUpdated.value = new Date()
        console.log('热词数据加载成功:', {
          count: hotWords.value.length,
          lastUpdated: lastUpdated.value,
        })
      } else {
        throw new Error(response.message || '获取热词数据失败')
      }
    } catch (err) {
      console.error('加载热词失败:', err)
      error.value = err instanceof Error ? err.message : '加载热词失败'
    } finally {
      loading.value = false
    }
  }

  // 刷新热词数据
  async function refreshHotWords() {
    // 如果已经在刷新中，不允许重复请求
    if (refreshing.value) {
      console.log('热词刷新已在进行中，忽略重复请求')
      return false
    }

    // 如果正在加载基础数据，也不允许刷新
    if (loading.value) {
      console.log('热词正在加载中，不允许刷新')
      return false
    }

    // 生成唯一的任务ID
    const taskId = `refresh_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    refreshTaskId.value = taskId
    refreshing.value = true
    error.value = null

    try {
      console.log(`开始热词刷新任务: ${taskId}`)

      // 调用刷新API
      const refreshResponse = await refreshHotWordsAPI()

      // 检查任务是否仍然有效（防止组件卸载重新挂载导致的状态混乱）
      if (refreshTaskId.value !== taskId) {
        console.log(`热词刷新任务 ${taskId} 已被新任务取代，忽略结果`)
        return false
      }

      if (refreshResponse.code === 200) {
        console.log(`热词刷新任务 ${taskId} 成功，重新加载热词列表`)

        // 刷新成功后重新加载热词列表
        // 暂时清除refreshing状态以允许加载
        refreshing.value = false
        await fetchHotWords()

        // 重新设置refreshing状态（如果加载成功）
        if (!error.value) {
          // 清除之前选中的热词（因为热词数据已更新）
          selectedWord.value = null
          console.log(`热词刷新任务 ${taskId} 完成`)
          return true
        } else {
          refreshing.value = true // 恢复刷新状态
          throw new Error('重新加载热词数据失败')
        }
      } else {
        throw new Error(refreshResponse.message || '刷新热词失败')
      }
    } catch (err) {
      console.error(`热词刷新任务 ${taskId} 失败:`, err)
      let errorMessage = '刷新热词失败，请稍后重试'

      if (err instanceof Error) {
        if (err.message.includes('timeout') || err.message.includes('超时')) {
          errorMessage = '刷新超时，请检查网络连接后重试'
        } else if (err.message.includes('Network Error')) {
          errorMessage = '网络错误，请检查网络连接'
        } else {
          errorMessage = err.message
        }
      }

      error.value = errorMessage
      return false
    } finally {
      // 只有当前任务ID匹配时才清除状态
      if (refreshTaskId.value === taskId) {
        refreshing.value = false
        refreshTaskId.value = null
      }
    }
  }

  // 选择热词
  function selectWord(word: HotWordItem | null) {
    selectedWord.value = word
    console.log('热词选择更新:', word)
  }

  // 清除选择
  function clearSelection() {
    selectedWord.value = null
  }

  // 重置错误状态
  function clearError() {
    error.value = null
  }

  // 强制停止刷新状态（用于组件卸载时清理）
  function forceStopRefresh() {
    if (refreshing.value) {
      console.log('强制停止热词刷新状态')
      refreshing.value = false
      refreshTaskId.value = null
    }
  }

  return {
    // 状态
    hotWords,
    loading,
    refreshing,
    error,
    lastUpdated,
    selectedWord,
    refreshTaskId,

    // 计算属性
    hasData,
    isOperating,

    // 方法
    fetchHotWords,
    refreshHotWords,
    selectWord,
    clearSelection,
    clearError,
    forceStopRefresh,
  }
})
