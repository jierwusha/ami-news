<script setup lang="ts">
import { ref, onMounted } from 'vue'
import SourceCard from '@/components/SourceCard.vue'
import {
  addRssSource,
  getRandomChannels,
  searchChannels,
  subscribeRssByUrl,
  unsubscribeRssByUrl,
} from '@/api/sources'
import { useHomeStore } from '@/stores/homeStore'

// 定义频道数据接口（从API返回的数据格式）
interface Channel {
  id: number | string
  title: string
  description?: string
  icon?: string | null
  link?: string
  url: string
  subscribed?: boolean
  updateTime?: string
}

// 定义新闻项接口
interface NewsItem {
  id: number
  title: string
  source: string
  time: string
  image: string
  category: string
  sourceIcon?: string
}

// 定义源数据接口（匹配SourceCard组件的Source类型）
interface Source {
  id: string
  name: string
  icon: string
  description: string
  unreadCount: number
  lastUpdate: string
  recentNews: NewsItem[]
  link?: string
  url?: string
  isSubscribed?: boolean
}

// 使用 homeStore
const homeStore = useHomeStore()

// 搜索相关
const searchQuery = ref('')
const isSearching = ref(false)
const hasSearched = ref(false)
const searchError = ref('')
const searchResults = ref<Source[]>([])

// 随机订阅源相关
const randomSources = ref<Source[]>([])
const isLoadingRandomSources = ref(false)
const randomSourcesError = ref('')

// 订阅状态
const subscribingSourceIds = ref<Set<string>>(new Set())

// 处理订阅/取消订阅
const handleSubscribe = async (source: Source) => {
  if (!source.url) {
    console.error('订阅源缺少URL信息')
    return
  }

  subscribingSourceIds.value.add(source.id)

  try {
    let response
    if (source.isSubscribed) {
      // 如果已订阅，则取消订阅
      response = await unsubscribeRssByUrl(source.url)
    } else {
      // 如果未订阅，则订阅
      response = await subscribeRssByUrl(source.url)
    }

    if (response.code === 200) {
      // 操作成功，更新UI状态
      const newSubscribedState = !source.isSubscribed

      const sourceIndex = randomSources.value.findIndex((s) => s.id === source.id)
      if (sourceIndex !== -1) {
        randomSources.value[sourceIndex] = {
          ...randomSources.value[sourceIndex],
          isSubscribed: newSubscribedState,
        }
      }

      const searchIndex = searchResults.value.findIndex((s) => s.id === source.id)
      if (searchIndex !== -1) {
        searchResults.value[searchIndex] = {
          ...searchResults.value[searchIndex],
          isSubscribed: newSubscribedState,
        }
      }

      console.log(`${newSubscribedState ? '订阅' : '取消订阅'}成功:`, response.message)

      // 刷新 homeStore 数据以同步到 Sidebar
      try {
        await homeStore.fetchHomeData()
        console.log('已刷新侧边栏数据')
      } catch (error) {
        console.warn('刷新侧边栏数据失败:', error)
        // 即使刷新失败也不影响主要功能
      }
    } else {
      console.error(`${source.isSubscribed ? '取消订阅' : '订阅'}失败:`, response.message)
    }
  } catch (error: unknown) {
    console.error(`${source.isSubscribed ? '取消订阅' : '订阅'}失败:`, error)
  } finally {
    subscribingSourceIds.value.delete(source.id)
  }
}

// 获取随机订阅源
const fetchRandomSources = async () => {
  isLoadingRandomSources.value = true
  randomSourcesError.value = ''

  try {
    const response = await getRandomChannels(10)

    if (response.code === 200 && response.data) {
      randomSources.value = response.data.map((channel: Channel) => ({
        id: channel.id.toString(),
        name: channel.title,
        icon:
          channel.icon ||
          new URL('../assets/placeholders/newsSouceIconDefault.png', import.meta.url).href,
        description: truncateDescription(channel.description || '暂无描述'),
        unreadCount: 0,
        lastUpdate: formatDate(channel.updateTime || new Date().toISOString()),
        recentNews: [],
        link: channel.link,
        url: channel.url,
        isSubscribed: channel.subscribed || false,
      }))
    } else {
      randomSourcesError.value = response.message || '获取推荐订阅源失败'
    }
  } catch (error: unknown) {
    console.error('获取随机订阅源失败:', error)
    randomSourcesError.value = '获取推荐订阅源失败，请稍后再试'
  } finally {
    isLoadingRandomSources.value = false
  }
}

// 转换搜索结果数据格式
const convertSearchResults = (channels: Channel[]) => {
  return channels.map((channel: Channel) => ({
    id: channel.id.toString(),
    name: channel.title,
    icon:
      channel.icon ||
      new URL('../assets/placeholders/newsSouceIconDefault.png', import.meta.url).href,
    description: truncateDescription(channel.description || '暂无描述'),
    unreadCount: 0,
    lastUpdate: '搜索结果',
    recentNews: [],
    link: '',
    url: channel.url,
    isSubscribed: channel.subscribed,
  }))
}

// 截断描述文本
const truncateDescription = (description: string, maxLength: number = 120) => {
  if (description.length <= maxLength) return description
  return description.substring(0, maxLength) + '...'
}

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return '未知时间'

  const date = new Date(dateString)
  if (isNaN(date.getTime())) return '未知时间'

  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffHours / 24)

  if (diffHours < 1) return '刚刚更新'
  if (diffHours < 24) return `${diffHours}小时前`
  if (diffDays < 7) return `${diffDays}天前`
  return date.toLocaleDateString('zh-CN')
}

// 刷新随机订阅源
const refreshRandomSources = () => {
  fetchRandomSources()
}

// 搜索处理函数
const handleSearch = async () => {
  if (!searchQuery.value.trim()) return

  isSearching.value = true
  searchError.value = ''
  searchResults.value = []

  // 判断输入是否是URL
  const isUrl = /^(http|https):\/\/[^ "]+$/.test(searchQuery.value.trim())

  if (isUrl) {
    // 如果是URL，尝试添加为RSS源
    try {
      const response = await addRssSource(searchQuery.value.trim())

      if (response.code === 200 && response.data) {
        const newSource = {
          id: response.data.id.toString(),
          name: response.data.title,
          icon:
            response.data.icon ||
            new URL('../assets/placeholders/newsSouceIconDefault.png', import.meta.url).href,
          description: response.data.description,
          unreadCount: 0,
          lastUpdate: '刚刚添加',
          recentNews: [],
          link: response.data.link,
          url: searchQuery.value.trim(),
          isSubscribed: false,
        }

        searchResults.value = [newSource]
        searchError.value = `成功添加 RSS 源: ${response.data.title}`

        // 刷新 homeStore 数据以同步新添加的RSS源到 Sidebar
        try {
          await homeStore.fetchHomeData()
          console.log('RSS源添加成功，已刷新侧边栏数据')
        } catch (error) {
          console.warn('刷新侧边栏数据失败:', error)
          // 即使刷新失败也不影响主要功能
        }
      } else {
        searchError.value = response.message || 'RSS源添加失败，请检查URL是否正确'
      }
    } catch (error: unknown) {
      console.error('添加RSS源失败:', error)
      const errorMessage = error instanceof Error ? error.message : 'RSS源添加失败，请稍后再试'
      searchError.value = errorMessage
    }
  } else {
    // 如果不是URL，调用搜索接口
    try {
      const response = await searchChannels(searchQuery.value.trim())

      if (response.code === 200 && response.data) {
        if (response.data.length > 0) {
          searchResults.value = convertSearchResults(response.data)
          searchError.value = `找到 ${response.data.length} 个相关订阅源`
        } else {
          searchResults.value = []
          searchError.value = '未找到匹配的订阅源，请尝试其他关键词或输入完整的RSS URL'
        }
      } else {
        searchError.value = response.message || '搜索失败，请稍后再试'
      }
    } catch (error: unknown) {
      console.error('搜索订阅源失败:', error)
      searchError.value = '搜索失败，请稍后再试'
    }
  }

  isSearching.value = false
  hasSearched.value = true
}

// 清除搜索内容
const clearSearch = () => {
  searchQuery.value = ''
  isSearching.value = false
  hasSearched.value = false
  searchError.value = ''
  searchResults.value = []
}

// 生命周期钩子
onMounted(() => {
  fetchRandomSources()
})
</script>

<template>
  <div class="home-view">
    <main class="main-content">
      <!-- Header -->
      <header class="header">
        <div class="header-left">
          <h1 class="page-title">发现</h1>
        </div>
      </header>

      <!-- Content Area -->
      <div class="content">
        <!-- 主内容容器 -->
        <div class="content-container">
          <!-- 搜索框 -->
          <div class="search-container">
            <div class="search-box">
              <svg
                class="search-icon"
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <circle cx="11" cy="11" r="8" />
                <path d="M21 21l-4.35-4.35" />
              </svg>
              <input
                type="text"
                v-model="searchQuery"
                @keyup.enter="handleSearch"
                placeholder="搜索订阅源或输入RSS URL"
                class="search-input"
              />
              <button v-if="searchQuery" class="clear-btn" @click="clearSearch">
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
              <button class="search-btn" @click="handleSearch" :disabled="isSearching">
                {{ isSearching ? '搜索中...' : '搜索' }}
              </button>
            </div>
          </div>

          <!-- 搜索结果 -->
          <div v-if="hasSearched" class="search-results-container">
            <div class="results-header">
              <h3 class="results-title">搜索结果</h3>
              <div
                v-if="searchError"
                :class="[
                  searchResults.length > 0 &&
                  !searchError.includes('未找到') &&
                  !searchError.includes('失败')
                    ? 'success-message'
                    : 'error-message',
                ]"
              >
                {{ searchError }}
              </div>
            </div>

            <div v-if="searchResults.length > 0" class="sources-list-centered">
              <SourceCard
                v-for="source in searchResults"
                :key="source.id"
                :source="source"
                :show-news="false"
                :layout="'list'"
                :show-subscribe="true"
                :is-subscribing="subscribingSourceIds.has(source.id)"
                @subscribe="handleSubscribe"
              />
            </div>

            <div v-else-if="!searchError.includes('成功')" class="empty-results">
              <svg
                width="48"
                height="48"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#9e9e9e"
                stroke-width="1.5"
              >
                <circle cx="11" cy="11" r="8" />
                <path d="M21 21l-4.35-4.35" />
                <path d="M8 11h6" />
              </svg>
              <p class="empty-hint">请尝试其他关键词或输入完整的RSS URL</p>
            </div>
          </div>

          <!-- 推荐订阅源 -->
          <div v-if="!hasSearched" class="recommended-sources">
            <div class="section-header">
              <h3 class="section-title">
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  style="margin-right: 8px; vertical-align: middle"
                >
                  <path
                    d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
                  />
                </svg>
                推荐订阅源
              </h3>
              <button
                class="refresh-btn"
                @click="refreshRandomSources"
                :disabled="isLoadingRandomSources"
              >
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  :class="{ spinning: isLoadingRandomSources }"
                >
                  <path d="M23 4v6h-6" />
                  <path d="M1 20v-6h6" />
                  <path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15" />
                </svg>
                换一批
              </button>
            </div>

            <!-- 加载状态 -->
            <div v-if="isLoadingRandomSources" class="loading-state">
              <div class="loading-spinner"></div>
              <p>正在加载推荐订阅源...</p>
            </div>

            <!-- 错误状态 -->
            <div v-else-if="randomSourcesError" class="error-state">
              <svg
                width="48"
                height="48"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#f44336"
                stroke-width="1.5"
              >
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <p class="error-message">{{ randomSourcesError }}</p>
              <button class="retry-btn" @click="fetchRandomSources">重试</button>
            </div>

            <!-- 推荐源列表 -->
            <div v-else-if="randomSources.length > 0" class="sources-list-centered">
              <SourceCard
                v-for="source in randomSources"
                :key="source.id"
                :source="source"
                :show-news="false"
                :layout="'list'"
                :show-subscribe="true"
                :is-subscribing="subscribingSourceIds.has(source.id)"
                @subscribe="handleSubscribe"
              />
            </div>

            <!-- 空状态 -->
            <div v-else class="empty-state">
              <svg
                width="48"
                height="48"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#9e9e9e"
                stroke-width="1.5"
              >
                <path
                  d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"
                />
                <line x1="7" y1="7" x2="7.01" y2="7" />
              </svg>
              <p>暂无推荐订阅源</p>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* 全局页面结构 */
.home-view {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-content {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Header */
.header {
  background: white;
  border-bottom: 1px solid #e5e5e5;
  padding: 0 24px;
  display: flex;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0;
  height: 64px;
  min-height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin: 0;
  white-space: nowrap;
}

/* 主内容区域 */
.content {
  flex: 1;
  width: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: #c1c1c1 transparent;
}

.content::-webkit-scrollbar {
  width: 8px;
}

.content::-webkit-scrollbar-track {
  background: transparent;
}

.content::-webkit-scrollbar-thumb {
  background-color: #c1c1c1;
  border-radius: 4px;
  border: 2px solid transparent;
  background-clip: content-box;
}

.content::-webkit-scrollbar-thumb:hover {
  background-color: #a8a8a8;
}

.content-container {
  width: 100%;
  min-height: 100%;
  padding: 24px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

/* 搜索框 */
.search-container {
  width: 100%;
  max-width: 600px;
  margin: 0 auto 32px auto;
  flex-shrink: 0;
}

.search-box {
  display: flex;
  align-items: center;
  background-color: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 0 8px 0 16px;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  width: 100%;
  box-sizing: border-box;
}

.search-box:focus-within {
  border-color: #2196f3;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.1);
}

.search-icon {
  color: #757575;
  margin-right: 8px;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  height: 48px;
  padding: 0;
  border: none;
  font-size: 16px;
  background: transparent;
  color: #333;
  outline: none;
  min-width: 0;
}

.search-input::placeholder {
  color: #9e9e9e;
}

.clear-btn {
  background: none;
  border: none;
  color: #9e9e9e;
  padding: 8px;
  margin-right: 4px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.clear-btn:hover {
  background-color: #f5f5f5;
  color: #757575;
}

.search-btn {
  font-size: 14px;
  background-color: #2196f3;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  height: 36px;
  margin: 6px 4px 6px 8px;
  white-space: nowrap;
  flex-shrink: 0;
}

.search-btn:hover {
  background-color: #1976d2;
}

.search-btn:disabled {
  background-color: #90caf9;
  cursor: not-allowed;
}

/* 搜索结果 */
.search-results-container {
  width: 100%;
  margin-bottom: 32px;
  flex-shrink: 0;
}

.results-header {
  margin-bottom: 20px;
  text-align: center;
  width: 100%;
}

.results-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
}

.success-message,
.error-message {
  padding: 12px 16px;
  border-radius: 4px;
  font-size: 14px;
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
  box-sizing: border-box;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.success-message {
  background-color: #e8f5e9;
  border-left: 4px solid #4caf50;
  color: #2e7d32;
}

.error-message {
  background-color: #ffebee;
  border-left: 4px solid #f44336;
  color: #c62828;
}

/* 推荐源 */
.section-header {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
  width: 100%;
  flex-wrap: wrap;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  white-space: nowrap;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: none;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.refresh-btn:hover {
  background-color: #f5f5f5;
  border-color: #2196f3;
  color: #2196f3;
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 源列表 */
.sources-list-centered {
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 状态样式 */
.loading-state,
.error-state,
.empty-state,
.empty-results {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  text-align: center;
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
  box-sizing: border-box;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #2196f3;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

.retry-btn {
  padding: 8px 16px;
  background: #2196f3;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  margin-top: 12px;
  transition: background-color 0.2s ease;
  white-space: nowrap;
}

.retry-btn:hover {
  background: #1976d2;
}

.empty-hint {
  color: #9e9e9e;
  font-size: 14px;
  margin: 8px 0 0 0;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

/* 移动端响应式 */
@media (max-width: 767px) {
  .header {
    padding: 0 16px;
  }

  .page-title {
    font-size: 1.25rem;
    padding-left: 50px;
  }

  .content-container {
    padding: 16px;
  }

  .search-container {
    max-width: 100%;
  }

  .sources-list-centered {
    max-width: 100%;
  }

  .section-header {
    flex-direction: column;
    gap: 12px;
  }

  .refresh-btn {
    align-self: center;
  }

  .success-message,
  .error-message {
    max-width: 100%;
  }

  .loading-state,
  .error-state,
  .empty-state,
  .empty-results {
    max-width: 100%;
    padding: 32px 16px;
  }
}

/* 桌面端响应式 */
@media (min-width: 768px) {
  .header {
    padding: 0 24px;
  }

  .content-container {
    padding: 24px;
  }
}

@media (min-width: 1200px) {
  .content-container {
    padding: 32px;
    max-width: 1200px;
    margin: 0 auto;
  }
}

/* 确保文本不会影响布局宽度 */
* {
  word-wrap: break-word;
  overflow-wrap: break-word;
}
</style>
