<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNewsDetail, streamAiSummary } from '../api/news'
import { useHomeStore } from '../stores/homeStore'
import type { NewsDetailData } from '../api/types'

// 路由
const route = useRoute()
const router = useRouter()
const homeStore = useHomeStore()

// 从URL获取新闻ID
const newsId = computed(() => route.params.id as string)

// 新闻详情数据
const newsDetail = ref<NewsDetailData | null>(null)
const loading = ref(true)
const error = ref('')

// AI摘要相关状态
const aiSummaryLoading = ref(false)
const aiSummaryContent = ref('')
const aiSummaryError = ref('')
const showAiSummary = ref(false)
let aiSummaryStream: { stop: () => void; isActive: () => boolean } | null = null

// 从 homeStore 中查找频道信息
const channelInfo = computed(() => {
  if (!newsDetail.value) return null

  // 如果 homeStore 数据还未加载，返回 null，等待数据加载
  if (!homeStore.hasData && homeStore.loading) {
    return null
  }

  // 在所有文件夹的频道中查找
  for (const folder of homeStore.folders) {
    const channel = folder.channels.find((ch) => ch.id === newsDetail.value!.channelId.toString())
    if (channel) {
      const result = {
        id: channel.id,
        name: channel.name,
        icon: channel.icon,
        description: channel.description,
        link: channel.link,
      }
      console.log('NewsDetailView 找到频道信息（文件夹中）:', {
        频道ID: channel.id,
        频道名称: channel.name,
        频道链接: channel.link,
        链接类型: typeof channel.link,
        结果对象: result,
      })
      return result
    }
  }

  // 在未分类频道中查找
  const channel = homeStore.unCategorizedChannels.find(
    (ch) => ch.id === newsDetail.value!.channelId.toString(),
  )
  if (channel) {
    const result = {
      id: channel.id,
      name: channel.name,
      icon: channel.icon,
      description: channel.description,
      link: channel.link,
    }
    console.log('NewsDetailView 找到频道信息（未分类频道）:', {
      频道ID: channel.id,
      频道名称: channel.name,
      频道链接: channel.link,
      链接类型: typeof channel.link,
      结果对象: result,
    })
    return result
  }

  return null
})

// 格式化发布时间
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  const now = new Date()
  const diffInHours = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60))

  if (diffInHours < 1) {
    return '刚刚'
  } else if (diffInHours < 24) {
    return `${diffInHours}小时前`
  } else if (diffInHours < 48) {
    return '昨天'
  } else {
    const diffInDays = Math.floor(diffInHours / 24)
    return `${diffInDays}天前`
  }
}

// 加载新闻详情
const loadNewsDetail = async () => {
  if (!newsId.value) {
    error.value = '新闻ID无效'
    loading.value = false
    return
  }

  try {
    loading.value = true
    error.value = ''

    const response = await getNewsDetail(parseInt(newsId.value))

    if (response.code === 200 && response.data) {
      newsDetail.value = response.data
    } else {
      error.value = response.message || '获取新闻详情失败'
    }
  } catch (err: unknown) {
    console.error('加载新闻详情失败:', err)
    const errorMessage = err instanceof Error ? err.message : String(err)
    if (errorMessage === 'Unauthorized') {
      error.value = '请先登录后再查看新闻详情'
    } else {
      error.value = errorMessage || '加载新闻详情失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 打开原文链接
const openOriginalLink = () => {
  if (newsDetail.value?.link) {
    console.log('打开原文链接:', newsDetail.value.link)
    window.open(newsDetail.value.link, '_blank', 'noopener,noreferrer')
  } else {
    console.warn('原文链接不可用')
    // 可以在这里添加用户提示
  }
}

// 刷新数据
const handleRefresh = () => {
  loadNewsDetail()
}

// 监听 homeStore 数据变化，确保频道信息能及时更新
watch(
  () => homeStore.hasData,
  (hasData) => {
    if (hasData && newsDetail.value) {
      console.log('HomeStore 数据已加载，频道信息可用:', channelInfo.value)
    }
  },
  { immediate: true },
)

// 监听路由参数变化，支持在新闻详情页之间跳转
watch(
  () => route.params.id,
  async (newId) => {
    if (newId) {
      await loadNewsDetail()
    }
  },
)

// 生命周期
onMounted(async () => {
  // 确保 homeStore 数据已加载（用于获取频道信息）
  if (!homeStore.hasData && !homeStore.loading) {
    console.log('HomeStore 数据未加载，开始加载...')
    await homeStore.fetchHomeData()
  }

  // 加载新闻详情
  await loadNewsDetail()
})

onUnmounted(() => {
  // 清理AI摘要流连接
  if (aiSummaryStream && aiSummaryStream.isActive()) {
    console.log('组件卸载，清理AI摘要流连接')
    aiSummaryStream.stop()
    aiSummaryStream = null
  }
})

// 新闻源图标默认图片
const defaultNewsSourceIcon = new URL(
  '../assets/placeholders/newsSouceIconDefault.png',
  import.meta.url,
).href

// 图片加载失败处理
const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = defaultNewsSourceIcon
}

// 跳转到频道外链
const openChannelLink = (link: string, channelName: string) => {
  console.log('NewsDetailView openChannelLink 调用参数:', {
    link,
    channelName,
    linkType: typeof link,
  })
  if (link && link.trim() !== '') {
    console.log('跳转到频道外链:', { channelName, link })
    window.open(link, '_blank', 'noopener,noreferrer')
  } else {
    console.warn('频道链接不可用:', { channelName, link })
  }
}

// 生成AI摘要
const generateAiSummary = async () => {
  if (!newsDetail.value || aiSummaryLoading.value) return

  try {
    aiSummaryLoading.value = true
    aiSummaryContent.value = ''
    aiSummaryError.value = ''
    showAiSummary.value = true

    console.log('开始生成AI摘要:', newsDetail.value.id)

    aiSummaryStream = streamAiSummary(
      newsDetail.value.id,
      // onToken: 接收到每个token时的回调
      (token: string) => {
        console.log(
          'Vue组件收到token:',
          JSON.stringify(token),
          '当前内容长度:',
          aiSummaryContent.value.length,
        )
        // 立即追加token到内容
        aiSummaryContent.value += token

        // 强制触发DOM更新，确保实时显示
        nextTick(() => {
          console.log('DOM更新后内容长度:', aiSummaryContent.value.length)
          // 可选：自动滚动到摘要底部
          const summaryElement = document.querySelector('.ai-summary-content')
          if (summaryElement) {
            summaryElement.scrollTop = summaryElement.scrollHeight
          }
        })
      },
      // onComplete: 完成时的回调
      (summary: string) => {
        console.log(
          'AI摘要生成完成，总长度:',
          summary.length,
          '当前显示长度:',
          aiSummaryContent.value.length,
        )
        aiSummaryLoading.value = false
        aiSummaryStream = null
        // 确保最终内容正确
        if (aiSummaryContent.value !== summary) {
          console.log('最终内容不一致，更新为完整内容')
          aiSummaryContent.value = summary
        }
      },
      // onError: 错误时的回调
      (error: string) => {
        console.error('AI摘要生成失败:', error)
        aiSummaryError.value = error
        aiSummaryLoading.value = false
        aiSummaryStream = null
      },
    )
  } catch (error) {
    console.error('启动AI摘要生成失败:', error)
    aiSummaryError.value = error instanceof Error ? error.message : '启动AI摘要失败'
    aiSummaryLoading.value = false
    aiSummaryStream = null
  }
}

// 停止AI摘要生成
const stopAiSummary = () => {
  if (aiSummaryStream && aiSummaryStream.isActive()) {
    console.log('用户手动停止AI摘要生成')
    aiSummaryStream.stop()
    aiSummaryStream = null
    aiSummaryLoading.value = false
  }
}

// 关闭AI摘要
const closeAiSummary = () => {
  if (aiSummaryStream && aiSummaryStream.isActive()) {
    stopAiSummary()
  }
  showAiSummary.value = false
  aiSummaryContent.value = ''
  aiSummaryError.value = ''
}

// 重试AI摘要生成
const retryAiSummary = () => {
  aiSummaryError.value = ''
  generateAiSummary()
}
</script>
<template>
  <div class="news-detail-view">
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <button class="back-btn" @click="goBack" title="返回">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M19 12H5" />
            <path d="M12 19l-7-7 7-7" />
          </svg>
        </button>
        <h1 class="page-title">新闻详情</h1>
      </div>
      <div class="header-actions">
        <button class="action-btn" @click="handleRefresh" title="刷新">
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
    </header>

    <!-- Main Content -->
    <main class="main-content">
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

      <!-- 新闻内容 -->
      <div v-else-if="newsDetail" class="news-content">
        <!-- 频道信息 -->
        <div v-if="channelInfo" class="channel-info">
          <div class="channel-header">
            <img
              :src="channelInfo.icon || defaultNewsSourceIcon"
              :alt="channelInfo.name"
              class="channel-icon"
              @error="handleImageError"
            />
            <div class="channel-details">
              <h3
                class="channel-name clickable"
                @click="openChannelLink(channelInfo.link, channelInfo.name)"
                :title="'点击访问频道: ' + channelInfo.link"
              >
                {{ channelInfo.name }}
              </h3>
              <p class="channel-description">{{ channelInfo.description }}</p>
            </div>
          </div>
        </div>

        <!-- 频道信息加载中 -->
        <div v-else-if="!homeStore.hasData && homeStore.loading" class="channel-loading">
          <div class="loading-spinner small"></div>
          <span>正在获取频道信息...</span>
        </div>

        <!-- 频道信息未找到 -->
        <div v-else class="channel-not-found">
          <div class="channel-header">
            <img :src="defaultNewsSourceIcon" alt="默认图标" class="channel-icon" />
            <div class="channel-details">
              <h3 class="channel-name">未知频道</h3>
              <p class="channel-description">频道信息暂时无法获取</p>
            </div>
          </div>
        </div>

        <!-- 新闻标题 -->
        <div class="news-header">
          <h1
            class="news-title"
            @click="openOriginalLink"
            :title="'点击查看原文: ' + newsDetail.link"
          >
            {{ newsDetail.title }}
            <svg
              class="external-link-icon"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
              <polyline points="15,3 21,3 21,9" />
              <line x1="10" y1="14" x2="21" y2="3" />
            </svg>
          </h1>
          <div class="news-meta">
            <span class="news-time">{{ formatDate(newsDetail.pubDate) }}</span>
            <span class="news-date">{{
              new Date(newsDetail.pubDate).toLocaleDateString('zh-CN')
            }}</span>
          </div>

          <!-- AI摘要按钮 -->
          <div class="ai-summary-actions">
            <button
              v-if="!showAiSummary"
              class="ai-summary-btn"
              @click="generateAiSummary"
              :disabled="aiSummaryLoading"
              title="生成AI摘要"
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"
                />
              </svg>
              {{ aiSummaryLoading ? '生成中...' : '生成AI摘要' }}
            </button>

            <button
              v-if="showAiSummary"
              class="ai-summary-btn secondary"
              @click="closeAiSummary"
              title="关闭AI摘要"
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
              关闭摘要
            </button>

            <button
              v-if="aiSummaryLoading"
              class="ai-summary-btn danger"
              @click="stopAiSummary"
              title="停止生成"
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <rect x="6" y="4" width="4" height="16" />
                <rect x="14" y="4" width="4" height="16" />
              </svg>
              停止
            </button>
          </div>
        </div>

        <!-- AI摘要显示区域 -->
        <div v-if="showAiSummary || aiSummaryLoading || aiSummaryError" class="ai-summary-section">
          <div class="ai-summary-header">
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"
              />
            </svg>
            <h3>AI 摘要</h3>
          </div>

          <!-- 加载状态 -->
          <div v-if="aiSummaryLoading" class="ai-summary-loading">
            <div class="loading-spinner small"></div>
            <span>正在生成AI摘要，请稍候...</span>
          </div>

          <!-- 错误状态 -->
          <div v-else-if="aiSummaryError" class="ai-summary-error">
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <p>{{ aiSummaryError }}</p>
            <button @click="retryAiSummary" class="retry-btn">重试</button>
          </div>

          <!-- 摘要内容 - 实时显示 -->
          <div v-if="aiSummaryContent" class="ai-summary-content">
            <p>{{ aiSummaryContent }}</p>
            <!-- 如果正在加载，显示光标提示 -->
            <span v-if="aiSummaryLoading" class="typing-cursor">|</span>
          </div>
        </div>

        <!-- 新闻内容 -->
        <div class="news-body">
          <div
            v-if="newsDetail.description"
            class="news-description"
            v-html="newsDetail.description"
          ></div>
          <div v-else class="no-content">
            <p>暂无内容。试着查看原文？</p>
          </div>
        </div>

        <!-- AI摘要 -->
        <!-- <div v-if="newsDetail.aiDescription" class="ai-summary">
          <div class="ai-summary-header">
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"
              />
            </svg>
            <h3>AI 摘要</h3>
          </div>
          <div class="ai-summary-content">
            <p>{{ cleanedAiDescription }}</p>
          </div>
        </div> -->

        <!-- AI摘要操作 -->
        <!-- <div v-if="newsDetail.aiDescription" class="ai-summary-actions">
          <button
            v-if="!showAiSummary"
            class="action-btn"
            @click="generateAiSummary"
            :disabled="aiSummaryLoading"
            title="生成AI摘要"
          >
            <span v-if="aiSummaryLoading" class="loading-spinner small"></span>
            <span v-else>生成AI摘要</span>
          </button> -->
        <!-- <button
            v-else
            class="action-btn"
            @click="showAiSummary = false"
            title="隐藏AI摘要"
          > -->
        <!-- 隐藏AI摘要
          </button> -->
        <!-- </div> -->

        <!-- AI摘要错误提示 -->
        <!-- <div v-if="aiSummaryError" class="ai-summary-error">
          <p>{{ aiSummaryError }}</p>
        </div> -->
      </div>
    </main>
  </div>
</template>

<style scoped>
.news-detail-view {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fafafa;
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
  z-index: 100;
  flex-shrink: 0;
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
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

.back-btn:hover {
  background-color: #f5f5f5;
  color: #333;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin: 0;
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

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

/* 加载和错误状态 */
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

.login-btn,
.retry-btn {
  padding: 10px 20px;
  background: #2196f3;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s ease;
}

.login-btn:hover,
.retry-btn:hover {
  background: #1976d2;
}

/* 新闻内容 */
.news-content {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.channel-info,
.channel-loading,
.channel-not-found {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.channel-loading {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #666;
  font-size: 0.9rem;
}

.channel-loading .loading-spinner.small {
  width: 16px;
  height: 16px;
  border-width: 2px;
  margin-bottom: 0;
}

.channel-not-found {
  background: #f9f9f9;
}

.channel-not-found .channel-name {
  color: #999;
}

.channel-not-found .channel-description {
  color: #aaa;
}

.channel-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.channel-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.channel-details {
  flex: 1;
  min-width: 0; /* 确保能够缩小 */
  overflow: hidden; /* 确保子元素不会溢出 */
}

.channel-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px 0;
}

.channel-name.clickable {
  cursor: pointer;
  transition: color 0.2s ease;
}

.channel-name.clickable:hover {
  color: #2196f3;
}

.channel-name.clickable:active {
  color: #1976d2;
}

.channel-description {
  font-size: 0.9rem;
  color: #666;
  margin: 0;
  line-height: 1.4;
  /* 限制为一行，溢出使用省略号 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.news-header {
  padding: 24px 24px 16px;
}

.news-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
  line-height: 1.4;
  cursor: pointer;
  transition: color 0.2s ease;
  position: relative;
  /* 使用 inline-block 保持 margin 生效，同时图标能紧跟文本 */
  display: inline-block;
  width: 100%;
}

.news-title:hover {
  color: #2196f3;
}

.news-title:active {
  color: #1976d2;
}

.external-link-icon {
  /* 改为 inline 图标，紧跟文本后面 */
  display: inline;
  vertical-align: text-top;
  margin-left: 4px;
  opacity: 0.6;
  transition: opacity 0.2s ease;
}

.news-title:hover .external-link-icon {
  opacity: 1;
}

.news-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.news-time {
  font-size: 0.9rem;
  color: #999;
}

.news-date {
  font-size: 0.9rem;
  color: #666;
}

.news-body {
  padding: 0 24px 24px;
}

.news-description {
  font-size: 1rem;
  line-height: 1.6;
  color: #333;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.news-description :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 16px 0;
  display: block;
}

.news-description :deep(p) {
  margin: 0 0 16px 0;
}

.news-description :deep(h1),
.news-description :deep(h2),
.news-description :deep(h3),
.news-description :deep(h4),
.news-description :deep(h5),
.news-description :deep(h6) {
  margin: 24px 0 12px 0;
  color: #333;
  line-height: 1.3;
}

.news-description :deep(ul),
.news-description :deep(ol) {
  margin: 16px 0;
  padding-left: 24px;
}

.news-description :deep(li) {
  margin: 4px 0;
  line-height: 1.5;
}

.news-description :deep(blockquote) {
  border-left: 4px solid #e0e0e0;
  padding: 12px 16px;
  margin: 16px 0;
  background: #f9f9f9;
  font-style: italic;
}

.news-description :deep(pre) {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 16px 0;
  font-family: 'Courier New', monospace;
}

.news-description :deep(code) {
  background: #f0f0f0;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

.news-description :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  border: 1px solid #e0e0e0;
}

.news-description :deep(th),
.news-description :deep(td) {
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  text-align: left;
}

.news-description :deep(th) {
  background: #f5f5f5;
  font-weight: 600;
}

.news-description :deep(a) {
  color: #2196f3;
  text-decoration: none;
}

.news-description :deep(a:hover) {
  text-decoration: underline;
}

.news-description :deep(strong),
.news-description :deep(b) {
  font-weight: 600;
}

.news-description :deep(em),
.news-description :deep(i) {
  font-style: italic;
}

.news-description :deep(hr) {
  border: none;
  border-top: 1px solid #e0e0e0;
  margin: 24px 0;
}

.news-description :deep(iframe) {
  max-width: 100%;
  margin: 16px 0;
}

.news-description :deep(video) {
  max-width: 100%;
  height: auto;
  margin: 16px 0;
}

.no-content {
  text-align: center;
  color: #999;
  padding: 40px 20px;
}

.ai-summary {
  margin: 24px 24px 32px 24px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%);
  border: 1px solid #e6edff;
  border-radius: 12px;
}

.ai-summary-actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.ai-summary-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  color: #666;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ai-summary-btn:hover:not(:disabled) {
  background: #f5f5f5;
  border-color: #2196f3;
  color: #2196f3;
}

.ai-summary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-summary-btn.secondary {
  background: #f5f5f5;
  border-color: #999;
  color: #666;
}

.ai-summary-btn.secondary:hover {
  background: #e0e0e0;
  border-color: #666;
  color: #333;
}

.ai-summary-btn.danger {
  background: #ffebee;
  border-color: #f44336;
  color: #f44336;
}

.ai-summary-btn.danger:hover {
  background: #ffcdd2;
  border-color: #d32f2f;
  color: #d32f2f;
}

.ai-summary-section {
  margin: 0 24px 24px 24px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%);
  border: 1px solid #e6edff;
  border-radius: 12px;
}

.ai-summary-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.ai-summary-header svg {
  color: #2196f3;
}

.ai-summary-header h3 {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.ai-summary-loading {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #666;
  font-size: 0.9rem;
}

.ai-summary-loading .loading-spinner.small {
  width: 16px;
  height: 16px;
  border-width: 2px;
  margin-bottom: 0;
}

.ai-summary-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px;
  text-align: center;
}

.ai-summary-error svg {
  color: #f44336;
}

.ai-summary-error p {
  margin: 0;
  color: #666;
}

.ai-summary-error .retry-btn {
  padding: 8px 16px;
  background: #2196f3;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background-color 0.2s ease;
}

.ai-summary-error .retry-btn:hover {
  background: #1976d2;
}

.ai-summary-content {
  font-size: 0.95rem;
  line-height: 1.6;
  color: #555;
}

.ai-summary-content p {
  margin: 0;
}

.typing-cursor {
  display: inline-block;
  animation: blink 1s infinite;
  font-weight: bold;
  color: #2196f3;
  margin-left: 2px;
}

@keyframes blink {
  0%,
  50% {
    opacity: 1;
  }
  51%,
  100% {
    opacity: 0;
  }
}

/* 响应式设计 */
@media (max-width: 767px) {
  .header {
    padding: 0 16px;
  }

  .main-content {
    padding: 16px;
  }

  .news-title {
    font-size: 1.3rem;
  }

  .external-link-icon {
    width: 16px;
    height: 16px;
    /* 确保移动端图标位置正确 */
    vertical-align: text-top;
    margin-left: 3px;
  }

  .channel-header,
  .news-header,
  .news-body {
    padding-left: 16px;
    padding-right: 16px;
  }

  .ai-summary-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .ai-summary-btn {
    justify-content: center;
  }

  .ai-summary-section {
    margin: 0 16px 16px 16px;
    padding: 16px;
  }
}
</style>
