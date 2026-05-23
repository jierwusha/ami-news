<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getUserTags, addItemToTag, type Tag } from '@/api/tag'
import { useTagStore } from '@/stores/tagStore'
import { addToReadLater } from '@/api/readlater'
import { computed } from 'vue'

// 更灵活的新闻数据接口
interface NewsCardItem {
  id: number | string
  title: string
  source: string
  time: string
  image?: string | null // 支持 null 值
  excerpt?: string // 内容摘要，可选
  sourceIcon?: string | null // 来源图标，支持 null 值

  // 兼容其他可能的字段名
  imageUrl?: string | null // image 同义词
  // excerpt 同义词
  content?: string
  summary?: string
  description?: string
}

interface Props {
  news: NewsCardItem
  highlightKeyword?: string // 需要高亮的关键词
  showRemoveFromReadLater?: boolean // 是否显示从稍后再读移除按钮
}

const props = defineProps<Props>()

// 定义事件
interface Emits {
  (e: 'click', newsItem: NewsCardItem): void
}

const emit = defineEmits<Emits>()

// 使用 tagStore
const tagStore = useTagStore()

// 处理卡片点击
const handleCardClick = (event: MouseEvent) => {
  // 如果点击的是按钮或其子元素，不触发卡片点击
  const target = event.target as HTMLElement
  if (target.closest('.news-actions') || target.closest('button')) {
    return
  }

  emit('click', props.news)
}

// 图片加载失败状态
const imageLoadFailed = ref(false)

// 标签相关状态
const tags = ref<Tag[]>([])
const showTagDropdown = ref(false)
const isLoadingTags = ref(false)
const isAddingToTag = ref(false)

// 稍后再读状态
const isAddingToReadLater = ref(false)

// 提示消息状态
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'error'>('success')

// 显示提示消息
const showToastMessage = (message: string, type: 'success' | 'error' = 'success') => {
  toastMessage.value = message
  toastType.value = type
  showToast.value = true

  // 3秒后自动隐藏提示
  setTimeout(() => {
    showToast.value = false
  }, 3000)
}

// 默认频道图标
const defaultChannelIcon = new URL(
  '../assets/placeholders/newsSouceIconDefault.png',
  import.meta.url,
).href

// 默认标签颜色
const defaultColors = [
  '#2196f3',
  '#4caf50',
  '#ff9800',
  '#9c27b0',
  '#f44336',
  '#607d8b',
  '#795548',
  '#ff5722',
]

// 获取标签颜色
const getTagColor = (index: number, customColor?: string) => {
  return customColor || defaultColors[index % defaultColors.length]
}
// 智能图片处理 - 内置回退逻辑
const processedImage = computed(() => {
  // 优先使用 image，然后 imageUrl
  const imageUrl = props.news.image || props.news.imageUrl

  // 如果没有图片或图片为空字符串，返回 undefined
  if (!imageUrl || imageUrl.trim() === '') {
    return undefined
  }

  return imageUrl.trim()
})

// 智能来源图标处理 - 内置回退逻辑
const processedSourceIcon = computed(() => {
  try {
    const sourceIcon = props.news.sourceIcon

    // 调试信息（生产环境可以移除）
    // console.log('NewsCard: 处理来源图标', {
    //   rawSourceIcon: sourceIcon,
    //   type: typeof sourceIcon,
    //   newsId: props.news.id,
    //   newsSource: props.news.source,
    //   defaultIcon: defaultChannelIcon,
    // })

    // 强化的回退逻辑：
    // 1. 检查 sourceIcon 是否存在且不为空
    // 2. 检查是否为有效的字符串
    // 3. 任何无效情况都使用默认图标
    if (
      !sourceIcon ||
      typeof sourceIcon !== 'string' ||
      sourceIcon.trim() === '' ||
      sourceIcon === 'undefined' ||
      sourceIcon === 'null'
    ) {
      // console.log('NewsCard: 使用默认图标，原因:', { sourceIcon, type: typeof sourceIcon })
      return defaultChannelIcon
    }

    const trimmedIcon = sourceIcon.trim()

    // 进一步验证 URL 是否看起来合理
    if (
      trimmedIcon.length < 4 ||
      (!trimmedIcon.startsWith('http') &&
        !trimmedIcon.startsWith('/') &&
        !trimmedIcon.startsWith('blob:'))
    ) {
      // console.warn('NewsCard: 图标URL格式可疑，使用默认图标:', trimmedIcon)
      return defaultChannelIcon
    }

    // console.log('NewsCard: 使用传入图标:', trimmedIcon)
    return trimmedIcon
  } catch {
    // console.error('NewsCard: 处理来源图标时发生错误，使用默认图标', error)
    return defaultChannelIcon
  }
})

// 格式化相对时间
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
// 智能摘要处理 - 内置HTML清理和回退逻辑
const processedExcerpt = computed(() => {
  // 优先级：excerpt > description > summary > content > title
  const rawExcerpt =
    props.news.excerpt || props.news.description || props.news.summary || props.news.content || ''

  if (!rawExcerpt) {
    return props.news.title || '暂无摘要'
  }

  // 清理HTML标签和格式化
  return stripHtmlTags(rawExcerpt)
})

// 去除HTML标签的工具函数
const stripHtmlTags = (html: string): string => {
  if (!html || typeof html !== 'string') {
    return ''
  }

  try {
    // 预处理：移除视频嵌入标记
    const cleanedHtml = html
      // 移除视频嵌入标记 [!--begin:htmlVideoCode--]...[!--end:htmlVideoCode--]
      .replace(/\[!--begin:htmlVideoCode--\].*?\[!--end:htmlVideoCode--\]/g, '')
      // 移除其他类似的嵌入标记
      .replace(/\[!--begin:.*?--\].*?\[!--end:.*?--\]/g, '')
      // 移除HTML注释
      .replace(/<!--.*?-->/g, '')

    // 创建临时DOM元素解析HTML
    const tempDiv = document.createElement('div')
    tempDiv.innerHTML = cleanedHtml

    // 获取纯文本内容
    const textContent = tempDiv.textContent || tempDiv.innerText || ''

    // 清理空白字符并限制长度
    return textContent.trim().replace(/\s+/g, ' ').replace(/\n+/g, ' ').slice(0, 200)
  } catch {
    // console.warn('HTML标签清理失败，使用回退方案:', error)
    // 回退方案：使用正则表达式
    return html
      .replace(/\[!--begin:htmlVideoCode--\].*?\[!--end:htmlVideoCode--\]/g, '') // 移除视频标记
      .replace(/\[!--begin:.*?--\].*?\[!--end:.*?--\]/g, '') // 移除其他嵌入标记
      .replace(/<!--.*?-->/g, '') // 移除HTML注释
      .replace(/<[^>]*>/g, '') // 去除HTML标签
      .replace(/&[a-zA-Z0-9#]+;/g, ' ') // 去除HTML实体
      .trim()
      .replace(/\s+/g, ' ')
      .slice(0, 200)
  }
}

// 高亮关键词的工具函数
const highlightText = (text: string, keyword?: string): string => {
  if (!keyword || !text || keyword.trim() === '') {
    return text
  }

  try {
    // 将关键词按空格和常见分隔符拆分
    const keywords = keyword
      .split(/[\s,，、。；;]+/)
      .filter((word) => word.trim().length > 0)
      .map((word) => word.trim())

    if (keywords.length === 0) {
      return text
    }

    // 为每个关键词创建正则表达式并高亮
    let highlightedText = text
    keywords.forEach((word) => {
      // 转义特殊字符，创建不区分大小写的正则表达式
      const escapedKeyword = word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      const regex = new RegExp(`(${escapedKeyword})`, 'gi')
      highlightedText = highlightedText.replace(regex, '<mark class="highlight-keyword">$1</mark>')
    })

    return highlightedText
  } catch {
    // console.warn('高亮关键词失败:', error)
    return text
  }
}

// 处理新闻图片加载失败
const handleImageError = () => {
  imageLoadFailed.value = true
}

// 处理频道图标加载失败
const handleSourceIconError = (event: Event) => {
  const target = event.target as HTMLImageElement

  // console.warn('NewsCard: 频道图标加载失败，切换到默认图标', {
  //   originalSrc: target.src,
  //   defaultIcon: defaultChannelIcon,
  //   newsData: props.news,
  // })

  // 防止循环错误：只有当前不是默认图标时才切换
  if (target.src !== defaultChannelIcon) {
    target.src = defaultChannelIcon
  } else {
    // console.error('NewsCard: 默认图标也加载失败，隐藏图标显示')
    // 如果默认图标也失败，隐藏图片元素
    target.style.display = 'none'

    // 可选：在图标位置显示文字占位符
    const placeholder = document.createElement('div')
    placeholder.className = 'news-source-icon news-source-icon-placeholder'
    placeholder.innerHTML = '📰'
    placeholder.title = `${props.news.source} 图标`
    target.parentNode?.insertBefore(placeholder, target)
  }
}

// 获取用户标签列表
const fetchTags = async () => {
  if (isLoadingTags.value) return

  try {
    isLoadingTags.value = true
    const response = await getUserTags()
    if (response.code === 200 && response.data) {
      tags.value = response.data
    } else {
      console.error('获取标签列表失败:', response.message)
    }
  } catch (error) {
    console.error('获取标签列表失败:', error)
  } finally {
    isLoadingTags.value = false
  }
}

// 切换标签下拉菜单
const toggleTagDropdown = async () => {
  if (!showTagDropdown.value && tags.value.length === 0) {
    await fetchTags()
  }
  showTagDropdown.value = !showTagDropdown.value
}

// 将新闻添加到标签
const addToTag = async (tag: Tag) => {
  if (isAddingToTag.value) return

  try {
    isAddingToTag.value = true
    const response = await addItemToTag(tag.id.toString(), props.news.id.toString())

    if (response.code === 200) {
      console.log(`成功将新闻添加到标签 "${tag.name}"`)
      
      // 更新tagStore中对应标签的数量
      tagStore.updateTagItemCount(tag.id, 1)
      // 更新本地标签列表中的数量
      const localTag = tags.value.find(t => t.id === tag.id)
      if (localTag) {
        if (localTag.itemCount !== undefined) {
          localTag.itemCount = (localTag.itemCount || 0) + 1
        } else {
          localTag.count = (localTag.count || 0) + 1
        }
      }
      
      showToastMessage(`已添加到标签 "${tag.name}"`, 'success')
    } else {
      console.error('添加到标签失败:', response.message)
      showToastMessage(response.message || '添加到标签失败', 'error')
    }
  } catch (error) {
    console.error('添加到标签失败:', error)
    showToastMessage('添加到标签失败，请稍后再试', 'error')
  } finally {
    isAddingToTag.value = false
    showTagDropdown.value = false
  }
}

// 添加到稍后再读
const handleAddToReadLater = async () => {
  if (isAddingToReadLater.value) return

  try {
    isAddingToReadLater.value = true
    console.log('添加到稍后再读:', props.news.id)

    const response = await addToReadLater(props.news.id.toString())

    if (response.code === 200) {
      console.log('成功添加到稍后再读')
      showToastMessage('已添加到稍后再读', 'success')
    } else {
      console.error('添加到稍后再读失败:', response.message)
      // 根据不同的错误码显示不同的错误信息
      let errorMessage = '添加失败'
      if (response.code === 409) {
        errorMessage = '该文章已在稍后再读列表中'
      } else if (response.code === 401) {
        errorMessage = '请先登录'
      } else if (response.code === 404) {
        errorMessage = '文章不存在'
      } else if (response.message) {
        errorMessage = response.message
      } else {
        errorMessage = '添加失败，请稍后再试'
      }
      showToastMessage(errorMessage, 'error')
    }
  } catch (error) {
    console.error('添加到稍后再读失败:', error)
    showToastMessage('网络错误，请检查网络连接', 'error')
  } finally {
    isAddingToReadLater.value = false
  }
}

// 点击外部关闭下拉菜单
const handleClickOutside = (event: Event) => {
  const target = event.target as Element
  const dropdown = document.querySelector('.tag-dropdown')
  const button = document.querySelector('.bookmark-btn')

  if (dropdown && button && !dropdown.contains(target) && !button.contains(target)) {
    showTagDropdown.value = false
  }
}

// 组件挂载时添加点击外部事件监听
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

// 组件卸载时移除事件监听
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <article class="news-card" :class="{ 'dropdown-open': showTagDropdown }" @click="handleCardClick">
    <!-- 提示消息 -->
    <div v-if="showToast" class="toast-message" :class="toastType">
      <div class="toast-content">
        <svg
          v-if="toastType === 'success'"
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path d="M20 6L9 17l-5-5" />
        </svg>
        <svg
          v-else
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        <span>{{ toastMessage }}</span>
      </div>
    </div>

    <div class="news-content" :class="{ 'text-only': !processedImage || imageLoadFailed }">
      <!-- 只有当图片存在且未加载失败时才显示图片区域 -->
      <div v-if="processedImage && !imageLoadFailed" class="news-image">
        <img :src="processedImage" :alt="news.title" @error="handleImageError" />
      </div>
      <div class="news-info" :class="{ 'full-width': !processedImage || imageLoadFailed }">
        <h3 class="news-title" v-html="highlightText(news.title, highlightKeyword)"></h3>
        <div class="news-meta">
          <div class="news-source-wrapper">
            <!-- 始终显示图标，使用多层回退保障 -->
            <img
              :src="processedSourceIcon"
              :alt="`${news.source} icon`"
              class="news-source-icon"
              @error="handleSourceIconError"
            />
            <span class="news-source">{{ news.source }}</span>
          </div>
          <span class="news-time">{{ formatRelativeTime(news.time) }}</span>
        </div>
        <p class="news-excerpt" v-html="highlightText(processedExcerpt, highlightKeyword)"></p>
        <div class="news-actions">
          <div class="bookmark-container">
            <button
              class="action-btn bookmark-btn"
              :class="{ active: showTagDropdown }"
              @click.stop="toggleTagDropdown"
              title="收藏到标签"
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
                  d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"
                ></path>
                <line x1="7" y1="7" x2="7.01" y2="7"></line>
              </svg>
            </button>

            <!-- 标签下拉菜单 -->
            <div v-if="showTagDropdown" class="tag-dropdown" @click.stop>
              <div class="dropdown-header">
                <span>收藏到标签</span>
              </div>
              <div class="dropdown-content">
                <div v-if="isLoadingTags" class="dropdown-loading">
                  <div class="loading-spinner"></div>
                  <span>加载中...</span>
                </div>
                <div v-else-if="tags.length === 0" class="dropdown-empty">
                  <span>暂无标签</span>
                  <span class="empty-tip">请先创建标签</span>
                </div>
                <div v-else class="tag-list">
                  <button
                    v-for="(tag, index) in tags"
                    :key="tag.id"
                    class="tag-item"
                    :disabled="isAddingToTag"
                    @click.stop="addToTag(tag)"
                  >
                    <div
                      class="tag-color-dot"
                      :style="{ backgroundColor: getTagColor(index, tag.color) }"
                    ></div>
                    <span class="tag-name">{{ tag.name }}</span>
                    <span class="tag-count">{{ tag.itemCount || tag.count || 0 }}</span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 稍后再读按钮 -->
          <button
            class="action-btn read-later-btn"
            @click.stop="handleAddToReadLater"
            :disabled="isAddingToReadLater"
            :title="isAddingToReadLater ? '添加中...' : '稍后再读'"
          >
            <svg
              v-if="!isAddingToReadLater"
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle cx="12" cy="12" r="10" />
              <polyline points="12,6 12,12 16,14" />
            </svg>
            <!-- 加载中的旋转图标 -->
            <svg
              v-else
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              class="loading-icon"
            >
              <path
                d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83"
              />
            </svg>
          </button>

          <!-- <button class="action-btn" title="Share" @click.stop>
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle cx="18" cy="5" r="3" />
              <circle cx="6" cy="12" r="3" />
              <circle cx="18" cy="19" r="3" />
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
              <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
            </svg>
          </button> -->
          <button class="action-btn" title="More options" @click.stop>
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle cx="12" cy="12" r="1" />
              <circle cx="19" cy="12" r="1" />
              <circle cx="5" cy="12" r="1" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
/* ===== 提示消息样式 ===== */
.toast-message {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1001;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  animation: slideInRight 0.3s ease-out;
  max-width: 200px;
}

.toast-message.success {
  background: #4caf50;
  color: white;
}

.toast-message.error {
  background: #f44336;
  color: white;
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 6px;
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* ===== 新闻卡片样式 ===== */
.news-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
  cursor: pointer;
  border: 1px solid #e5e5e5;
  position: relative; /* 为提示消息定位 */
}

.news-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

/* 当下拉菜单打开时，提高新闻卡片的层级 */
.news-card.dropdown-open {
  z-index: 9998;
  position: relative;
}

.news-content {
  display: flex;
  gap: 16px;
  /* margin-bottom: 12px; */
}

/* 当没有图片时的特殊布局类 */
.news-content.text-only {
  gap: 0;
}

.news-image {
  flex-shrink: 0;
  width: 120px;
  /* height: 0; */
  max-height: 180px;
  border-radius: 6px;
  overflow: hidden;
  background-color: #f5f5f5;
}

.news-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: linear-gradient(45deg, #e3f2fd, #bbdefb);
}

.news-info {
  flex: 1;
  min-width: 0;
}

.news-info.full-width {
  width: 100%;
}

.news-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.news-source-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
}

.news-source-icon {
  width: 16px;
  height: 16px;
  border-radius: 3px;
  object-fit: cover;
  flex-shrink: 0;
}

/* 文字占位符样式 */
.news-source-icon-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #999;
  font-size: 10px;
  border: 1px solid #e0e0e0;
  box-sizing: border-box;
}

.news-source {
  font-size: 12px;
  color: #4caf50;
  font-weight: 500;
}

.news-time {
  font-size: 12px;
  color: #666;
}

.news-excerpt {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
}

.news-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.news-card:hover .news-actions {
  opacity: 1;
}

.action-btn {
  padding: 6px;
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

.action-btn.active {
  background-color: #e3f2fd;
  color: #2196f3;
}

/* 稍后再读按钮样式 */
.read-later-btn:hover {
  background-color: #f5f5f5;
  color: #333;
}

.read-later-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.read-later-btn .loading-icon {
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

/* 收藏按钮容器 */
.bookmark-container {
  position: relative;
}

/* 标签下拉菜单 */
.tag-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 200px;
  max-width: 280px;
  z-index: 9999; /* 提高z-index确保不被遮挡 */
}

.dropdown-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e5e5e5;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.dropdown-content {
  max-height: 240px;
  overflow-y: auto;
}

.dropdown-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  color: #666;
  font-size: 14px;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #2196f3;
  border-radius: 50%;
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

.dropdown-empty {
  padding: 20px 16px;
  text-align: center;
  color: #666;
}

.dropdown-empty .empty-tip {
  display: block;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.tag-list {
  padding: 8px 0;
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 16px;
  border: none;
  background: none;
  cursor: pointer;
  transition: background-color 0.2s ease;
  text-align: left;
}

.tag-item:hover:not(:disabled) {
  background-color: #f5f5f5;
}

.tag-item:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.tag-color-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.tag-name {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.tag-count {
  font-size: 12px;
  color: #666;
}

@media (max-width: 425px) {
  .news-content {
    flex-direction: column;
  }
  .news-image {
    width: 100%;
    /* max-height: 200px; */
  }
}

@media (max-width: 767px) {
  /* 移动端始终显示操作按钮 */
  .news-actions {
    opacity: 1;
  }
}
/* 高亮关键词样式 - 荧光笔标记风格 */
:deep(.highlight-keyword) {
  background-color: rgba(33, 150, 243, 0.2);
  color: #333;
  /* padding: 2px 4px; */
  /* border-radius: 2px; */
  font-weight: 500;
  display: inline;
  margin: 0;
  transition: background-color 0.2s ease;
}

:deep(.highlight-keyword:hover) {
  background-color: rgba(33, 150, 243, 0.3);
}
</style>
