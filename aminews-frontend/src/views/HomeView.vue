<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import NewsCard from '../components/NewsCard.vue'
import WordCloud from '../components/WordCloud.vue'
import DropdownMenu, { type DropdownItem } from '../components/DropdownMenu.vue'
import { useHomeStore } from '../stores/homeStore'
import { getHotWordItems } from '../api/news'
import type { HotWordItem } from '../api'
import type { HotWordItemDatum } from '../api/types'

// 使用状态管理
const homeStore = useHomeStore()

// 路由
import { useRouter } from 'vue-router'
const router = useRouter()

// Tab 状态管理 - 从localStorage获取初始值
const getInitialTab = () => {
  const savedTab = localStorage.getItem('homeview-selected-tab')
  return savedTab === '热点' ? '热点' : '最新'
}

const selectedTab = ref(getInitialTab())

// 监听tab变化并保存到localStorage
watch(
  selectedTab,
  (newTab: string) => {
    localStorage.setItem('homeview-selected-tab', newTab)
  },
  { immediate: true },
)

// 监听热点标签页切换，恢复选中的热词
watch(selectedTab, async (newTab: string) => {
  if (newTab === '热点') {
    // 切换到热点标签页时，恢复选中的热词
    // 延迟一下确保页面渲染完成，并且重试机制
    setTimeout(async () => {
      // 检查是否已经有选中的热词，如果没有才恢复
      if (!selectedHotWord.value) {
        console.log('切换到热点tab，准备恢复热词...')
        await restoreHotWordFromStorage()
      } else {
        console.log('切换到热点tab，已有选中的热词:', selectedHotWord.value.word)
      }
    }, 300)
  }
})

// 热点页面的词云相关状态
const selectedHotWord = ref<HotWordItem | null>(null)

// 热词本地存储的键名
const HOT_WORD_STORAGE_KEY = 'homeview-selected-hotword'

// 从本地存储恢复选中的热词
const restoreSelectedHotWord = () => {
  try {
    const stored = localStorage.getItem(HOT_WORD_STORAGE_KEY)
    if (stored) {
      const hotWordData = JSON.parse(stored)

      // 检查数据是否过期（设置为24小时过期）
      const isExpired =
        hotWordData.timestamp && Date.now() - hotWordData.timestamp > 24 * 60 * 60 * 1000

      if (isExpired) {
        console.log('热词数据已过期，清除缓存')
        localStorage.removeItem(HOT_WORD_STORAGE_KEY)
        return
      }

      if (
        hotWordData.hotWord &&
        typeof hotWordData.hotWord.id === 'number' &&
        typeof hotWordData.hotWord.word === 'string' &&
        typeof hotWordData.hotWord.count === 'number'
      ) {
        selectedHotWord.value = hotWordData.hotWord
        console.log('恢复选中的热词:', selectedHotWord.value)
        return hotWordData.hotWord
      }
    }
  } catch (error) {
    console.error('恢复热词数据失败:', error)
    localStorage.removeItem(HOT_WORD_STORAGE_KEY)
  }
  return null
}

// 保存选中的热词到本地存储
const saveSelectedHotWord = (hotWord: HotWordItem | null) => {
  try {
    if (hotWord) {
      const hotWordData = {
        hotWord,
        timestamp: Date.now(),
      }
      localStorage.setItem(HOT_WORD_STORAGE_KEY, JSON.stringify(hotWordData))
      console.log('保存选中的热词:', hotWord)
    } else {
      localStorage.removeItem(HOT_WORD_STORAGE_KEY)
      console.log('清除热词缓存')
    }
  } catch (error) {
    console.error('保存热词数据失败:', error)
  }
}

// 热词新闻相关状态
const hotWordNews = ref<NewsItem[]>([])
const hotWordNewsLoading = ref(false)
const hotWordNewsError = ref('')

// 文件夹展开状态管理
const expandedFolders = ref<Set<string>>(new Set())

// 计算属性 - 从store获取数据
const folders = computed(() => homeStore.folders)
const loading = computed(() => homeStore.loading)
const error = computed(() => homeStore.error)

// 切换文件夹展开状态
const toggleFolder = (folderId: string) => {
  if (expandedFolders.value.has(folderId)) {
    expandedFolders.value.delete(folderId)
  } else {
    expandedFolders.value.add(folderId)
  }
}

// 检查文件夹是否展开
const isFolderExpanded = (folderId: string) => {
  return expandedFolders.value.has(folderId)
}

// 导航到新闻详情页
const navigateToNewsDetail = (newsId: string | number) => {
  try {
    if (!newsId || (typeof newsId === 'string' && newsId.trim() === '')) {
      console.error('无效的新闻ID:', newsId)
      return
    }

    const id = typeof newsId === 'number' ? newsId.toString() : newsId.toString()
    console.log('导航到新闻详情页:', id)
    router.push(`/news/${id}`)
  } catch (error) {
    console.error('导航到新闻详情页失败:', error)
  }
}

// 处理图片加载失败
const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement
  if (target) {
    target.src = new URL('../assets/placeholders/newsSouceIconDefault.png', import.meta.url).href
  }
}

// 跳转到频道外链
const openChannelLink = (link: string, channelName: string) => {
  console.log('openChannelLink 调用参数:', { link, channelName, linkType: typeof link })
  if (link && link.trim() !== '') {
    console.log('跳转到频道外链:', { channelName, link })
    window.open(link, '_blank', 'noopener,noreferrer')
  } else {
    console.warn('频道链接不可用:', { channelName, link })
  }
}

// 格式化相对时间（与FeedView保持一致）
const formatRelativeTime = (dateString: string): string => {
  try {
    const date = new Date(dateString)
    const now = new Date()
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60))

    if (diffInMinutes < 1) {
      return '刚刚'
    } else if (diffInMinutes < 60) {
      return `${diffInMinutes}分钟前`
    } else if (diffInMinutes < 1440) {
      // 24 hours
      const hours = Math.floor(diffInMinutes / 60)
      return `${hours}小时前`
    } else {
      const days = Math.floor(diffInMinutes / 1440)
      return `${days}天前`
    }
  } catch (error) {
    console.error('日期格式化错误:', error)
    return '时间未知'
  }
}

// 将热词后端数据转换为 NewsItem 格式
const transformHotWordItemToNewsItem = (item: HotWordItemDatum): NewsItem => {
  return {
    id: item.id,
    title: item.title,
    source: item.channelTitle,
    time: item.pubDate,
    image: item.imageUrl, // 直接传递，NewsCard 会处理回退逻辑
    sourceIcon: item.channelIcon, // 直接传递，NewsCard 会处理回退逻辑
    excerpt: item.description, // 直接传递，NewsCard 会处理 HTML 清理
  }
}

// 获取热词新闻数据
const loadHotWordNews = async (hotWordId: number) => {
  try {
    hotWordNewsLoading.value = true
    hotWordNewsError.value = ''
    console.log('加载热词新闻:', { hotWordId })

    const response = await getHotWordItems({
      hotWordId,
      pageNum: 1,
      pageSize: 20, // 热词新闻显示更多条目
    })

    if (response.code === 200 && response.data) {
      // 转换数据格式
      hotWordNews.value = response.data.map(transformHotWordItemToNewsItem)

      console.log('热词新闻加载成功:', {
        热词ID: hotWordId,
        新闻数量: hotWordNews.value.length,
      })
    } else {
      hotWordNewsError.value = response.message || '获取热词新闻失败'
      hotWordNews.value = []
      console.error('获取热词新闻失败:', response.message)
    }
  } catch (error) {
    console.error('加载热词新闻时发生错误:', error)
    hotWordNewsError.value = error instanceof Error ? error.message : '加载热词新闻失败，请稍后重试'
    hotWordNews.value = []
  } finally {
    hotWordNewsLoading.value = false
  }
}

// 处理热词选择
const handleWordSelected = async (word: HotWordItem | null) => {
  selectedHotWord.value = word
  console.log('选中的热词:', word)

  // 保存到本地存储
  saveSelectedHotWord(word)

  if (word) {
    // 加载选中热词的相关新闻
    await loadHotWordNews(word.id)
  } else {
    // 清空新闻列表
    hotWordNews.value = []
    hotWordNewsError.value = ''
  }
}

// 滚动处理 - 为粘性头部添加阴影效果
const handleScroll = () => {
  const headers = document.querySelectorAll('.folder-header')
  const mainHeaderHeight = 64 // 主header高度

  headers.forEach((header) => {
    const rect = header.getBoundingClientRect()
    // 当头部接近或紧贴主header时，添加阴影效果
    // 使用更宽松的判断条件以确保阴影在合适的时机出现
    if (rect.top <= mainHeaderHeight + 2 && rect.top >= mainHeaderHeight - 2) {
      header.classList.add('sticky-shadow')
    } else {
      header.classList.remove('sticky-shadow')
    }
  })
}

// 完整的热词恢复函数
const restoreHotWordFromStorage = async () => {
  try {
    console.log('开始恢复热词选择状态...')
    const restoredHotWord = restoreSelectedHotWord()

    if (restoredHotWord) {
      console.log('找到缓存的热词，准备恢复:', restoredHotWord)

      // 直接设置选中的热词（会自动通过 prop 传递给 WordCloud 组件）
      selectedHotWord.value = restoredHotWord

      // 等待一下确保状态更新完成
      await nextTick()

      // 加载对应的热词新闻
      await loadHotWordNews(restoredHotWord.id)

      console.log('热词恢复完成，选中热词:', restoredHotWord.word)
    } else {
      console.log('没有找到缓存的热词')
    }
  } catch (error) {
    console.error('恢复热词时发生错误:', error)
  }
}

// 生命周期钩子
onMounted(async () => {
  const contentElement = document.querySelector('.content')
  if (contentElement) {
    contentElement.addEventListener('scroll', handleScroll)
  }

  // 初始化数据
  await initializeData()

  // 延迟检查是否需要恢复热词选择（适用于页面刷新和从其他页面返回的情况）
  setTimeout(async () => {
    if (selectedTab.value === '热点' && !selectedHotWord.value) {
      console.log('页面加载完成，检测到热点tab且无选中热词，开始恢复...')
      await restoreHotWordFromStorage()
    }
  }, 500)
})

onUnmounted(() => {
  const contentElement = document.querySelector('.content')
  if (contentElement) {
    contentElement.removeEventListener('scroll', handleScroll)
  }
})

// 定义下拉菜单项
const mobileMenuItems: DropdownItem[] = [
  // {
  //   id: 'mark-read',
  //   label: 'Mark all as read',
  //   icon: '<path d="M20 6L9 17l-5-5" />',
  //   action: () => {
  //     console.log('Mark all as read')
  //     // TODO: 实现标记已读功能
  //   },
  // },
  // {
  //   id: 'filter',
  //   label: 'Filter',
  //   icon: '<polygon points="22,3 2,3 10,12.46 10,19 14,21 14,12.46" />',
  //   action: () => {
  //     console.log('Filter')
  //     // TODO: 实现筛选功能
  //   },
  // },
  {
    id: 'refresh',
    label: 'Refresh',
    icon: '<path d="M23 4v6h-6" /><path d="M1 20v-6h6" /><path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15" />',
    action: async () => {
      console.log('Refresh')
      await handleRefresh()
    },
  },
]

// 处理刷新功能
const handleRefresh = async () => {
  const success = await homeStore.refreshData()
  if (success) {
    console.log('数据刷新成功')
    // 刷新成功后，同时刷新热词数据（如果当前在热点tab）
    if (selectedTab.value === '热点') {
      await refreshHotWordsData()
    }
  } else {
    console.error('数据刷新失败')
    // 可以添加错误提示
  }
}

// 刷新热词数据的方法
const refreshHotWordsData = async () => {
  try {
    console.log('开始刷新热词数据')
    // 这里可以通过ref获取WordCloud组件实例并调用刷新方法
    // 或者直接调用API
    const { refreshHotWords } = await import('../api')
    await refreshHotWords()
    console.log('热词数据刷新成功')
  } catch (error) {
    console.error('热词数据刷新失败:', error)
  }
}

// 初始化数据获取
const initializeData = async () => {
  await homeStore.fetchHomeData()

  // 如果有数据，默认展开前两个文件夹
  if (homeStore.folders.length > 0) {
    homeStore.folders.slice(0, 2).forEach((folder) => {
      expandedFolders.value.add(folder.id)
    })
  }
}

// 定义统一的新闻项接口（与FeedView保持一致）
interface NewsItem {
  id: number | string
  title: string
  source: string
  time: string
  image?: string | null
  sourceIcon?: string | null
  excerpt?: string
  description?: string
}
</script>

<template>
  <div class="home-view">
    <!-- Main Content -->
    <main class="main-content">
      <!-- Header -->
      <header class="header">
        <div class="header-left">
          <h1 class="page-title">主页</h1>
        </div>
        <div class="header-tabs">
          <button
            class="tab"
            :class="{ active: selectedTab === '最新' }"
            @click="selectedTab = '最新'"
          >
            最新
          </button>
          <button
            class="tab"
            :class="{ active: selectedTab === '热点' }"
            @click="selectedTab = '热点'"
          >
            热点
          </button>
        </div>
        <div class="header-actions">
          <!-- <button
            class="action-btn"
            title="Mark all as read"
            @click="console.log('Mark all as read')"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M20 6L9 17l-5-5" />
            </svg>
          </button> -->
          <!-- <button class="action-btn" title="Filter" @click="console.log('Filter')">
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <polygon points="22,3 2,3 10,12.46 10,19 14,21 14,12.46" />
            </svg>
          </button> -->
          <button class="action-btn" title="Refresh" @click="handleRefresh">
            <svg
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
          </button>
        </div>
        <!-- 移动端省略号菜单 -->
        <div class="mobile-menu">
          <DropdownMenu
            :items="mobileMenuItems"
            button-icon="more"
            button-title="More options"
            position="right"
            size="medium"
            :z-index="610"
          />
        </div>
      </header>

      <!-- Content Area -->
      <div class="content">
        <!-- 最新页面 -->
        <div v-if="selectedTab === '最新'" class="subscriptions-view">
          <!-- 加载状态 -->
          <div v-if="loading" class="loading-state">
            <div class="loading-spinner"></div>
            <p>加载中...</p>
          </div>

          <!-- 错误状态 -->
          <div v-else-if="error" class="error-state">
            <div v-if="error.includes('登录')" class="login-required">
              <svg
                width="48"
                height="48"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
                <polyline points="10,17 15,12 10,7" />
                <line x1="15" y1="12" x2="3" y2="12" />
              </svg>
              <h3>需要登录</h3>
              <p>{{ error }}</p>
              <button @click="$router.push('/login')" class="login-btn">去登录</button>
            </div>
            <div v-else>
              <p>{{ error }}</p>
              <button @click="handleRefresh" class="retry-btn">重试</button>
            </div>
          </div>

          <!-- 数据展示 -->
          <div v-else class="folders-container">
            <div
              v-for="folder in folders"
              :key="folder.id"
              class="folder-drawer"
              :class="{ expanded: isFolderExpanded(folder.id) }"
            >
              <!-- 文件夹头部 -->
              <div
                class="folder-header"
                :class="{ expanded: isFolderExpanded(folder.id) }"
                @click="toggleFolder(folder.id)"
              >
                <div class="folder-info">
                  <!-- <div class="folder-icon" :style="{ backgroundColor: folder.color }">
                    {{ folder.icon }}
                  </div> -->
                  <div class="folder-details">
                    <h3 class="folder-name">{{ folder.name }}</h3>
                    <span class="folder-stats">
                      {{ folder.channels.length }} 个订阅源
                      <!-- · {{ folder.unreadCount }} 条未读 -->
                    </span>
                  </div>
                </div>
                <div class="folder-actions">
                  <!-- <span v-if="folder.unreadCount > 0" class="unread-badge">
                    {{ folder.unreadCount }}
                  </span> -->
                  <button class="expand-btn">
                    <svg
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      :class="{ rotated: isFolderExpanded(folder.id) }"
                    >
                      <path d="M9 18l6-6-6-6" />
                    </svg>
                  </button>
                </div>
              </div>

              <!-- 文件夹内容 -->
              <div v-show="isFolderExpanded(folder.id)" class="folder-content">
                <div class="sources-grid">
                  <div v-for="channel in folder.channels" :key="channel.id" class="source-card">
                    <!-- 订阅源头部 -->
                    <div class="source-header">
                      <div class="source-info">
                        <img
                          :src="channel.icon"
                          :alt="channel.name"
                          class="source-icon"
                          @error="handleImageError"
                        />
                        <div class="source-details">
                          <h4
                            class="source-name clickable"
                            @click="openChannelLink(channel.link, channel.name)"
                            :title="'点击访问频道: ' + channel.link"
                          >
                            {{ channel.name }}
                          </h4>
                          <p class="source-description">{{ channel.description }}</p>
                        </div>
                      </div>
                      <div class="source-meta">
                        <!-- <span v-if="channel.unreadCount > 0" class="source-unread-badge">
                          {{ channel.unreadCount }}
                        </span> -->
                        <span class="source-last-update">{{ channel.lastUpdate }}</span>
                      </div>
                    </div>

                    <!-- 新闻列表 -->
                    <div class="source-news">
                      <div
                        v-for="(news, index) in channel.recentNews.slice(0, 10)"
                        :key="news.id"
                        class="news-item"
                        :class="{ 'news-divider': index > 0 }"
                      >
                        <h5 class="news-title" @click="navigateToNewsDetail(news.id)">
                          {{ news.title }}
                        </h5>
                        <div class="news-meta">
                          <span class="news-time">{{ news.time }}</span>
                        </div>
                      </div>
                    </div>

                    <!-- 查看更多按钮 -->
                    <!-- <div class="source-footer">
                      <button class="view-more-btn">查看全部 ({{ source.unreadCount }})</button>
                    </div> -->
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 未分类频道 -->
          <div v-if="homeStore.unCategorizedChannels.length > 0" class="uncategorized-section">
            <div class="section-header">
              <h3 class="section-title">未分类频道</h3>
              <span class="section-stats">{{ homeStore.unCategorizedChannels.length }} 个频道</span>
            </div>

            <div class="sources-grid">
              <div
                v-for="channel in homeStore.unCategorizedChannels"
                :key="channel.id"
                class="source-card"
              >
                <!-- 订阅源头部 -->
                <div class="source-header">
                  <div class="source-info">
                    <img
                      :src="channel.icon"
                      :alt="channel.name"
                      class="source-icon"
                      @error="handleImageError"
                    />
                    <div class="source-details">
                      <h4
                        class="source-name clickable"
                        @click="openChannelLink(channel.link, channel.name)"
                        :title="'点击访问频道: ' + channel.link"
                      >
                        {{ channel.name }}
                      </h4>
                      <p class="source-description">{{ channel.description }}</p>
                    </div>
                  </div>
                  <div class="source-meta">
                    <span class="source-last-update">{{ channel.lastUpdate }}</span>
                  </div>
                </div>

                <!-- 新闻列表 -->
                <div class="source-news">
                  <div
                    v-for="(news, index) in channel.recentNews.slice(0, 10)"
                    :key="news.id"
                    class="news-item"
                    :class="{ 'news-divider': index > 0 }"
                    @click="navigateToNewsDetail(news.id)"
                  >
                    <h5 class="news-title">{{ news.title }}</h5>
                    <div class="news-meta">
                      <span class="news-time">{{ news.time }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <!-- 热点页面 -->
        <div v-else class="trending-view">
          <!-- 词云图区域 -->
          <WordCloud
            :height="400"
            :selectedWord="selectedHotWord"
            @word-selected="handleWordSelected"
          />

          <!-- 新闻卡片区域 - 只有选中热词时才显示 -->
          <div v-if="selectedHotWord" class="news-card-section">
            <div class="section-header">
              <h3 class="section-title">
                热词「{{ selectedHotWord.word }}」相关新闻 ({{ selectedHotWord.count }}条)
              </h3>
              <div class="header-actions">
                <span class="highlight-tip">
                  <svg
                    width="12"
                    height="12"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path
                      d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
                    />
                  </svg>
                  关键词已高亮
                </span>
                <!-- <button
                  class="clear-selection-btn"
                  @click="handleWordSelected(null)"
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
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                </button> -->
              </div>
            </div>

            <!-- 加载状态 -->
            <div v-if="hotWordNewsLoading" class="loading-state">
              <div class="loading-spinner"></div>
              <p>加载相关新闻中...</p>
            </div>

            <!-- 错误状态 -->
            <div v-else-if="hotWordNewsError" class="error-state">
              <p>{{ hotWordNewsError }}</p>
              <button @click="loadHotWordNews(selectedHotWord.id)" class="retry-btn">重试</button>
            </div>

            <!-- 新闻列表 -->
            <div v-else-if="hotWordNews.length > 0" class="news-list">
              <div
                v-for="news in hotWordNews"
                :key="news.id"
                class="news-card-wrapper"
                @click="navigateToNewsDetail(news.id)"
              >
                <NewsCard :news="news" :highlightKeyword="selectedHotWord?.word" />
              </div>
            </div>

            <!-- 空状态 -->
            <div v-else class="empty-state">
              <div class="empty-icon">📰</div>
              <h3>暂无相关新闻</h3>
              <p>该热词暂时没有相关新闻内容</p>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
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

.header {
  background: white;
  border-bottom: 1px solid #e5e5e5;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: var(--z-page-header);
  flex-shrink: 0;
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.header-tabs {
  display: flex;
  gap: 8px;
}

.tab {
  padding: 8px 16px;
  border: none;
  background: none;
  color: #666;
  font-size: 1rem;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.tab:hover {
  background-color: #f5f5f5;
}

.tab.active {
  background-color: #2196f3;
  color: white;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 8px;
  border: none;
  background: none;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn:hover {
  background-color: #f5f5f5;
  color: #333;
}

.mobile-menu {
  display: none;
}

.content {
  flex: 1;
  overflow-y: auto;
  background-color: #fafafa;
}

/* ===== 最新页面样式 ===== */
.subscriptions-view {
  padding: 24px;
}

.folders-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.folder-drawer {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: visible; /* 允许粘性头部溢出 */
  transition: all 0.3s ease;
  position: relative;
}

.folder-drawer:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.folder-header {
  padding: 20px 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.2s ease;
  position: sticky;
  top: 0px; /* 紧贴主header下方 */
  background: white;
  z-index: calc(var(--z-page-header) - 1); /* 略低于主header */
  border-radius: 12px; /* 保持顶部圆角 */
}

.folder-header:hover {
  background-color: #fafafa;
}

/* 当文件夹头部处于粘性状态时，只添加下边缘阴影 */
.folder-header.sticky-shadow {
  box-shadow: 0 4px 8px -2px rgba(0, 0, 0, 0.1); /* 只有下边缘阴影 */
  border-bottom-color: #e0e0e0;
  border-radius: 12px 12px 0 0; /* 保持顶部圆角 */
}

.folder-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.folder-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
}

.folder-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.folder-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.folder-stats {
  font-size: 0.9rem;
  color: #666;
}

.folder-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.unread-badge {
  background: #2196f3;
  color: white;
  font-size: 0.8rem;
  padding: 4px 8px;
  border-radius: 12px;
  font-weight: 500;
  min-width: 20px;
  text-align: center;
}

.expand-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: #666;
  transition: all 0.2s ease;
}

.expand-btn svg {
  transition: transform 0.2s ease;
}

.expand-btn svg.rotated {
  transform: rotate(90deg);
}

.folder-content {
  border-top: 1px solid #f0f0f0;
  animation: expandContent 0.3s ease-out;
  position: relative;
  z-index: calc(var(--z-page-header) - 2); /* 确保内容在粘性头部下方 */
  border-radius: 0 0 12px 12px; /* 添加底部圆角 */
  background: white; /* 确保背景色一致 */
}

@keyframes expandContent {
  from {
    opacity: 0;
    max-height: 0;
  }
  to {
    opacity: 1;
    max-height: 1000px;
  }
}

.sources-grid {
  padding: 24px;
  /* 优先使用 CSS Masonry (瀑布流) 布局 */
  columns: 3;
  column-gap: 20px;
  /* 为不支持 columns 的浏览器提供 grid 降级 */
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 20px;
}

/* 如果浏览器支持 columns，覆盖 grid 布局 */
@supports (columns: 3) {
  .sources-grid {
    display: block;
    columns: 3;
    column-gap: 20px;
    gap: initial;
  }
}

.source-card {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s ease;
  /* 防止卡片在 columns 布局中被分割 */
  break-inside: avoid;
  /* 为 masonry 布局设置固定宽度和底部间距 */
  width: 100%;
  margin-bottom: 20px;
  /* 确保卡片宽度一致 */
  box-sizing: border-box;
}

.source-card:hover {
  border-color: #e0e0e0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.uncategorized-section .source-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  background: white;
}

.source-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.source-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0; /* 允许缩小 */
}

.source-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.source-icon-emoji {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background: #f5f5f5;
  flex-shrink: 0;
}

.source-details {
  flex: 1;
  min-width: 0; /* 确保能够缩小 */
  overflow: hidden; /* 确保子元素不会溢出 */
}

.source-name {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px 0;
}

.source-name.clickable {
  cursor: pointer;
  transition: color 0.2s ease;
}

.source-name.clickable:hover {
  color: #2196f3;
}

.source-name.clickable:active {
  color: #1976d2;
}

.source-description {
  font-size: 0.85rem;
  color: #666;
  margin: 0;
  line-height: 1.3;
  /* 限制为一行，溢出使用省略号 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.source-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0; /* 防止被压缩 */
}

.source-unread-badge {
  background: #ff9800;
  color: white;
  font-size: 0.75rem;
  padding: 2px 6px;
  border-radius: 8px;
  font-weight: 500;
}

.source-last-update {
  font-size: 0.75rem;
  color: #999;
}

.news-item {
  padding: 8px 0;
}

.news-item.news-divider {
  border-top: 1px solid #f0f0f0;
}

.news-title {
  font-size: 0.85rem;
  font-weight: 500;
  color: #333;
  margin: 0 0 4px 0;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  cursor: pointer;
}

.news-title:hover {
  color: #2196f3;
}

.news-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.news-time {
  font-size: 0.75rem;
  color: #999;
}

.source-footer {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.view-more-btn {
  width: 100%;
  padding: 8px 16px;
  background: none;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  color: #666;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.view-more-btn:hover {
  background: #f5f5f5;
  border-color: #d0d0d0;
  color: #333;
}

/* ===== 未分类频道样式 ===== */
.uncategorized-section {
  margin-top: 32px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  /* margin-bottom: 20px; */
  padding: 0 4px;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.section-stats {
  font-size: 0.9rem;
  color: #666;
}

/* ===== 热点页面样式 ===== */
.trending-view {
  padding: 24px;
}

.news-card-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  color: #666;
  /* margin: 0 0 16px 0; */
  font-weight: 500;
}

.news-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.news-card-wrapper {
  cursor: pointer;
  transition: transform 0.2s ease;
}

.news-card-wrapper:hover {
  transform: translateY(-2px);
}

/* ===== 移动端响应式样式 ===== */
@media (max-width: 767px) {
  .header {
    padding: 0 16px;
    height: 64px;
  }

  .header-left {
    padding: 0 0 0 50px; /* 为了给移动菜单留出空间 */
  }

  .header-actions {
    display: none;
  }

  .mobile-menu {
    display: block;
  }

  .header-tabs {
    margin: 0 auto;
  }

  .tab {
    padding: 6px 12px;
    font-size: 0.9rem;
  }

  .subscriptions-view,
  .trending-view {
    padding: 16px;
  }

  .sources-grid {
    padding: 16px;
    /* Masonry 布局：移动端单列 */
    columns: 1;
    column-gap: 16px;
    /* Grid 降级：移动端单列 */
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .source-card {
    padding: 16px;
    margin-bottom: 16px;
  }

  .folder-header {
    padding: 16px 20px;
    top: 0px; /* 移动端也保持相同的粘性定位 */
  }

  .folder-icon {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }

  .folder-name {
    font-size: 1rem;
  }

  .folder-stats {
    font-size: 0.8rem;
  }
}

/* ===== 中等屏幕样式 ===== */
@media (min-width: 768px) and (max-width: 1199px) {
  .subscriptions-view,
  .trending-view {
    padding: 20px;
  }

  .sources-grid {
    /* Masonry 布局：中屏双列 */
    columns: 2;
    column-gap: 18px;
    /* Grid 降级：中屏双列 */
    grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  }

  .source-card {
    margin-bottom: 18px;
  }
}

/* ===== 大屏幕样式 ===== */
@media (min-width: 1200px) {
  .subscriptions-view,
  .trending-view {
    padding: 32px;
  }

  .sources-grid {
    /* Masonry 布局：大屏三列 */
    columns: 3;
    column-gap: 20px;
    /* Grid 降级：大屏三列 */
    grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  }

  .source-card {
    margin-bottom: 20px;
  }
}

/* ===== 加载和错误状态样式 ===== */
.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #666;
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

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.error-state p {
  margin-bottom: 16px;
  font-size: 1rem;
}

.retry-btn {
  padding: 8px 16px;
  background: #2196f3;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background-color 0.2s ease;
}

.retry-btn:hover {
  background: #1976d2;
}

/* ===== 登录提示样式 ===== */
.login-required {
  text-align: center;
  color: #666;
}

.login-required svg {
  color: #2196f3;
  margin-bottom: 16px;
}

.login-required h3 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 1.2rem;
}

.login-required p {
  margin: 0 0 20px 0;
  color: #666;
}

.login-btn {
  padding: 10px 20px;
  background: #2196f3;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s ease;
}

.login-btn:hover {
  background: #1976d2;
}

/* ===== 热词新闻相关样式 ===== */
.news-card-section .section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.news-card-section .section-title {
  margin: 0;
  flex: 1;
}

.news-card-section .header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.news-card-section .highlight-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
  /* background: #f8f9fa; */
  padding: 4px 8px;
  border-radius: 4px;
  /* border: 1px solid #e9ecef; */
}

.news-card-section .highlight-tip svg {
  color: #ffc107;
}

.news-card-section .clear-selection-btn {
  padding: 6px;
  border: none;
  background: #f5f5f5;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.news-card-section .clear-selection-btn:hover {
  background: #e0e0e0;
  color: #333;
}

.news-card-section .loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #666;
}

.news-card-section .loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #2196f3;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

.news-card-section .error-state p {
  margin-bottom: 12px;
  font-size: 0.9rem;
}

.news-card-section .retry-btn {
  padding: 6px 12px;
  background: #2196f3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: background-color 0.2s ease;
}

.news-card-section .retry-btn:hover {
  background: #1976d2;
}

.news-card-section .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #666;
  text-align: center;
}

.news-card-section .empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.news-card-section .empty-state h3 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 18px;
  font-weight: 600;
}

.news-card-section .empty-state p {
  margin: 0;
  color: #666;
  font-size: 14px;
}
</style>
