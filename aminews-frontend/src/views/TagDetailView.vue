<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import NewsCard from '../components/NewsCard.vue'
import DropdownMenu, { type DropdownItem } from '../components/DropdownMenu.vue'
import { getTagItems, getUserTags, removeItemFromTag, type TagItemDatum, type Tag } from '../api/tag'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 页面状态
const loading = ref(false)
const error = ref('')
const tagInfo = ref<Tag | null>(null)

// 搜索状态
const searchQuery = ref('')
const filteredNews = ref<any[]>([])

// 筛选器状态
const filterOptions = ref({
  sortBy: 'time', // time, title, source
  sortOrder: 'desc', // desc, asc
  timeRange: 'all', // all, today, week, month
})

// 选择模式
const isSelectionMode = ref(false)
const selectedItems = ref<Set<number>>(new Set())

// 新闻数据（转换后的格式，用于NewsCard组件）
const news = ref<any[]>([])

// 获取当前标签ID
const tagId = computed(() => route.params.id as string)

// 将TagItemDatum转换为NewsCard所需的格式
const transformTagItemToNewsItem = (item: TagItemDatum) => {
  return {
    id: item.id,
    title: item.title,
    source: item.channelTitle || '未知来源',
    time: item.pubDate,
    image: item.imageUrl,
    sourceIcon: item.channelIcon,
    excerpt: item.description || item.aiDescription || '',
    link: item.link,
  }
}


// 加载标签信息
const loadTagInfo = async () => {
  if (!userStore.isLoggedIn) {
    error.value = '请先登录'
    return
  }

  try {
    const response = await getUserTags()
    if (response.code === 200 && response.data) {
      const tag = response.data.find((t) => t.id.toString() === tagId.value)
      if (tag) {
        tagInfo.value = tag
      } else {
        error.value = '标签不存在'
      }
    } else {
      error.value = response.message || '获取标签信息失败'
    }
  } catch (err: any) {
    error.value = err.message || '获取标签信息失败，请稍后再试'
  }
}

// 加载标签下的新闻（获取所有数据，不分页）
const loadTagNews = async () => {
  if (!userStore.isLoggedIn) {
    error.value = '请先登录'
    return
  }

  try {
    loading.value = true
    error.value = ''

    // 获取所有数据，设置一个较大的pageSize
    const response = await getTagItems({
      tagId: tagId.value,
      pageNum: 1,
      pageSize: 1000, // 假设标签下不会有超过1000个新闻项目
    })

    if (response.code === 200 && response.data) {
      // 转换数据格式
      news.value = response.data.map(transformTagItemToNewsItem)
      // 应用筛选和搜索
      applyFiltersAndSearch()
    } else {
      error.value = response.message || '获取标签新闻失败'
      news.value = []
    }
  } catch (err: any) {
    error.value = err.message || '获取标签新闻失败，请稍后再试'
    news.value = []
  } finally {
    loading.value = false
  }
}

// 刷新数据
const handleRefresh = () => {
  exitSelectionMode()
  loadTagNews()
}

// 搜索和筛选功能
const applyFiltersAndSearch = () => {
  let result = [...news.value]

  // 先应用搜索
  const query = searchQuery.value.trim().toLowerCase()
  if (query) {
    result = result.filter((item) => {
      return (
        item.title.toLowerCase().includes(query) ||
        item.source.toLowerCase().includes(query) ||
        (item.excerpt && item.excerpt.toLowerCase().includes(query))
      )
    })
  }

  // 应用时间范围筛选
  if (filterOptions.value.timeRange !== 'all') {
    const now = new Date()
    result = result.filter((item) => {
      const itemDate = new Date(item.time || item.pubDate)
      const diffTime = now.getTime() - itemDate.getTime()
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

      switch (filterOptions.value.timeRange) {
        case 'today':
          return diffDays <= 1
        case 'week':
          return diffDays <= 7
        case 'month':
          return diffDays <= 30
        default:
          return true
      }
    })
  }

  // 应用排序
  result.sort((a, b) => {
    let aValue: any, bValue: any

    switch (filterOptions.value.sortBy) {
      case 'title':
        aValue = a.title
        bValue = b.title
        break
      case 'source':
        aValue = a.source
        bValue = b.source
        break
      case 'time':
      default:
        aValue = new Date(a.time || a.pubDate)
        bValue = new Date(b.time || b.pubDate)
        break
    }

    if (filterOptions.value.sortOrder === 'desc') {
      return aValue > bValue ? -1 : aValue < bValue ? 1 : 0
    } else {
      return aValue < bValue ? -1 : aValue > bValue ? 1 : 0
    }
  })

  filteredNews.value = result
}

// 清空搜索
const clearSearch = () => {
  searchQuery.value = ''
  applyFiltersAndSearch()
}

// 监听搜索输入
const handleSearchInput = () => {
  applyFiltersAndSearch()
}

// 处理新闻点击，跳转到详情页
const handleNewsClick = (newsItem: any) => {
  if (isSelectionMode.value) {
    toggleSelection(newsItem.id)
    return
  }

  try {
    router.push({
      name: 'news-detail',
      params: { id: newsItem.id.toString() },
    })
  } catch (error) {
    console.error('导航到新闻详情页失败:', error)
  }
}

// 单个删除 - 从标签中移除新闻
const handleRemoveItem = async (itemId: number) => {
  try {
    loading.value = true
    const response = await removeItemFromTag(tagId.value, itemId.toString())
    
    if (response.code === 200) {
      // 删除成功后重新获取最新的标签新闻列表
      selectedItems.value.delete(itemId)
      await loadTagNews()
    } else {
      error.value = response.message || '从标签中移除失败'
    }
  } catch (err: any) {
    error.value = err.message || '从标签中移除失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

// 进入/退出选择模式
const enterSelectionMode = () => {
  isSelectionMode.value = true
  selectedItems.value.clear()
}

const exitSelectionMode = () => {
  isSelectionMode.value = false
  selectedItems.value.clear()
}

// 切换选择状态
const toggleSelection = (itemId: number) => {
  if (selectedItems.value.has(itemId)) {
    selectedItems.value.delete(itemId)
  } else {
    selectedItems.value.add(itemId)
  }
}

// 全选/取消全选
const toggleSelectAll = () => {
  if (selectedItems.value.size === filteredNews.value.length) {
    selectedItems.value.clear()
  } else {
    selectedItems.value = new Set(filteredNews.value.map((item) => item.id))
  }
}

// 批量删除选中项 - 从标签中批量移除新闻
const batchDeleteSelected = async () => {
  if (selectedItems.value.size === 0) return
  
  if (!confirm(`确定要从标签中移除选中的 ${selectedItems.value.size} 篇新闻吗？`)) {
    return
  }
  
  try {
    loading.value = true
    const deletePromises = Array.from(selectedItems.value).map(id => 
      removeItemFromTag(tagId.value, id.toString())
    )
    
    await Promise.all(deletePromises)
    
    // 删除成功后重新获取最新的标签新闻列表
    exitSelectionMode()
    await loadTagNews()
  } catch (err: any) {
    error.value = err.message || '批量移除失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

// 清空所有项目 - 从标签中移除所有新闻
const clearAllItems = async () => {
  if (news.value.length === 0) return
  
  if (!confirm(`确定要从标签中移除所有 ${news.value.length} 篇新闻吗？此操作不可撤销。`)) {
    return
  }
  
  try {
    loading.value = true
    const deletePromises = news.value.map(item => 
      removeItemFromTag(tagId.value, item.id.toString())
    )
    
    await Promise.all(deletePromises)
    
    // 清空成功后重新获取最新的标签新闻列表
    exitSelectionMode()
    await loadTagNews()
  } catch (err: any) {
    error.value = err.message || '清空失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

// 返回到标签列表页面
const goBackToTags = () => {
  router.push({ name: 'tags' })
}

// 定义下拉菜单项
const mobileMenuItems = computed((): DropdownItem[] => [
  {
    id: 'select-mode',
    label: isSelectionMode.value ? '退出选择' : '批量选择',
    icon: isSelectionMode.value
      ? '<path d="M18 6L6 18M6 6l12 12" />'
      : '<rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><path d="M9 12l2 2 4-4"/>',
    action: isSelectionMode.value ? exitSelectionMode : enterSelectionMode,
  },
  {
    id: 'clear-all',
    label: '移除全部',
    icon: '<polyline points="3,6 5,6 21,6"></polyline><path d="M19,6v14a2,2 0,0 1,-2,2H7a2,2 0,0 1,-2,-2V6m3,0V4a2,2 0,0 1,2,-2h4a2,2 0,0 1,2,2v2"></path>',
    action: clearAllItems,
  },
  {
    id: 'refresh',
    label: '刷新',
    icon: '<path d="M23 4v6h-6" /><path d="M1 20v-6h6" /><path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15" />',
    action: handleRefresh,
  },
])

// 计算属性
const isEmpty = computed(() => !loading.value && filteredNews.value.length === 0)
const showContent = computed(() => !loading.value && !error.value && filteredNews.value.length > 0)
const selectedCount = computed(() => selectedItems.value.size)
const isAllSelected = computed(
  () => filteredNews.value.length > 0 && selectedItems.value.size === filteredNews.value.length,
)
const totalCount = computed(() => news.value.length)
const filteredCount = computed(() => filteredNews.value.length)
const hasSearchQuery = computed(() => searchQuery.value.trim().length > 0)

// 生命周期
onMounted(() => {
  loadTagInfo()
  loadTagNews()
})

// 监听路由参数变化，当标签ID变化时重新加载数据
watch(
  () => route.params.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      // 重置状态
      exitSelectionMode()
      error.value = ''
      tagInfo.value = null
      news.value = []
      filteredNews.value = []
      searchQuery.value = ''

      // 重新加载数据
      loadTagInfo()
      loadTagNews()
    }
  },
  { immediate: false },
)
</script>

<template>
  <div class="home-view">
    <!-- Main Content -->
    <main class="main-content">
      <!-- Header -->
      <header class="header">
        <div class="header-left">
          <button @click="goBackToTags" class="back-btn" title="返回标签列表">
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M19 12H5" />
              <path d="m12 19-7-7 7-7" />
            </svg>
          </button>
          <h1 class="page-title">{{ tagInfo?.name || '标签详情' }}</h1>
          <span v-if="!loading && totalCount > 0" class="item-count">
            <template v-if="hasSearchQuery">
              {{ filteredCount }} / {{ totalCount }} 篇文章
            </template>
            <template v-else> {{ totalCount }} 篇文章 </template>
          </span>
        </div>

        <!-- 选择模式工具栏（桌面端） -->
        <div v-if="isSelectionMode" class="selection-toolbar desktop-only">
          <button @click="exitSelectionMode" class="exit-selection-btn" title="退出选择模式">
            退出多选
          </button>
          <button @click="toggleSelectAll" class="select-all-btn">
            {{ isAllSelected ? '取消全选' : '全选' }}
          </button>
          <span class="selected-count">已选择 {{ selectedCount }} 项</span>
          <button
            @click="batchDeleteSelected"
            :disabled="selectedCount === 0"
            class="batch-delete-btn"
          >
            移除选中
          </button>
        </div>

        <!-- 普通模式操作按钮 -->
        <div v-else class="header-actions">
          <button
            @click="enterSelectionMode"
            class="action-btn"
            title="批量选择"
            :disabled="isEmpty"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
              <path d="M9 12l2 2 4-4" />
            </svg>
          </button>
          <button @click="clearAllItems" class="action-btn" title="清空全部" :disabled="isEmpty">
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <polyline points="3,6 5,6 21,6"></polyline>
              <path
                d="M19,6v14a2,2 0,0 1,-2,2H7a2,2 0,0 1,-2,-2V6m3,0V4a2,2 0,0 1,2,-2h4a2,2 0,0 1,2,2v2"
              ></path>
            </svg>
          </button>
          <button @click="handleRefresh" class="action-btn" title="刷新">
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
            button-title="更多选项"
            position="right"
            size="medium"
            :z-index="610"
          />
        </div>
      </header>
      
      <!-- 移动端选择模式工具栏（单独一栏） -->
      <div v-if="isSelectionMode" class="mobile-selection-toolbar">
        <button @click="exitSelectionMode" class="exit-selection-btn" title="退出选择模式">
          退出多选
        </button>
        <button @click="toggleSelectAll" class="select-all-btn">
          {{ isAllSelected ? '取消全选' : '全选' }}
        </button>
        <span class="selected-count">已选择 {{ selectedCount }} 项</span>
        <button
          @click="batchDeleteSelected"
          :disabled="selectedCount === 0"
          class="batch-delete-btn"
        >
          移除选中
        </button>
      </div>

      <!-- 搜索和筛选栏 -->
      <div v-if="!loading && !error && totalCount > 0" class="filters-bar">
        <div class="search-section">
          <div class="search-input-wrapper">
            <svg
              class="search-icon"
              width="16"
              height="16"
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
              @input="handleSearchInput"
              placeholder="搜索标题、来源或内容..."
              class="search-input"
            />
            <button
              v-if="hasSearchQuery"
              @click="clearSearch"
              class="clear-search-btn"
              title="清空搜索"
            >
              <svg
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
        </div>

        <div class="filter-section">
          <select
            v-model="filterOptions.sortBy"
            @change="applyFiltersAndSearch"
            class="filter-select"
          >
            <option value="time">按时间排序</option>
            <option value="title">按标题排序</option>
            <option value="source">按来源排序</option>
          </select>
          <select
            v-model="filterOptions.sortOrder"
            @change="applyFiltersAndSearch"
            class="filter-select"
          >
            <option value="desc">降序</option>
            <option value="asc">升序</option>
          </select>
          <select
            v-model="filterOptions.timeRange"
            @change="applyFiltersAndSearch"
            class="filter-select"
          >
            <option value="all">全部时间</option>
            <option value="today">今天</option>
            <option value="week">本周</option>
            <option value="month">本月</option>
          </select>
        </div>
      </div>

      <!-- Content Area -->
      <div class="content">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <!-- 错误状态 -->
        <div v-else-if="error" class="error-state">
          <div class="error-icon">
            <svg
              width="48"
              height="48"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          </div>
          <h3>{{ error }}</h3>
          <button @click="handleRefresh" class="retry-btn">重试</button>
        </div>

        <!-- 空状态 -->
        <div v-else-if="isEmpty" class="empty-state">
          <div class="empty-icon">
            <svg
              width="64"
              height="64"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
              <line x1="7" y1="7" x2="7.01" y2="7"/>
            </svg>
          </div>
          <h3>该标签下暂无新闻</h3>
          <p>为感兴趣的新闻添加此标签，文章就会出现在这里。</p>
        </div>

        <!-- 新闻列表 -->
        <div v-else-if="showContent" class="news-content">
          <!-- 搜索结果提示 -->
          <div v-if="hasSearchQuery" class="search-results-header">
            <span class="search-results-text"> 找到 {{ filteredCount }} 篇相关文章 </span>
          </div>

          <div class="news-list">
            <div
              v-for="newsItem in filteredNews"
              :key="newsItem.id"
              class="news-item-wrapper"
              :class="{ 'selection-mode': isSelectionMode }"
            >
              <!-- 选择复选框 -->
              <div v-if="isSelectionMode" class="selection-checkbox">
                <input
                  type="checkbox"
                  :checked="selectedItems.has(newsItem.id)"
                  @change="toggleSelection(newsItem.id)"
                />
              </div>

              <!-- 新闻卡片 -->
              <NewsCard
                :news="newsItem"
                :highlight-keyword="searchQuery"
                :show-remove-from-read-later="!isSelectionMode"
                @click="handleNewsClick(newsItem)"
                @remove-from-read-later="handleRemoveItem(newsItem.id)"
                :class="{
                  selected: selectedItems.has(newsItem.id),
                  clickable: !isSelectionMode,
                }"
              />
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
  gap: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: #f5f5f5;
  color: #333;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.item-count {
  font-size: 0.9rem;
  color: #666;
  background: #f5f5f5;
  padding: 4px 8px;
  border-radius: 12px;
}

/* ===== 选择模式工具栏样式 ===== */
.selection-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.desktop-only {
  display: flex;
}

/* 移动端专用的批量操作工具栏 */
.mobile-selection-toolbar {
  display: none; /* 默认隐藏，仅在移动端显示 */
  background: white;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e5e5;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
}

.exit-selection-btn {
  padding: 6px 12px;
  background: #f5f5f5;
  border: none;
  border-radius: 4px;
  color: #666;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.exit-selection-btn:hover {
  background: #e8e8e8;
  color: #333;
}

.select-all-btn {
  padding: 6px 12px;
  background: #f5f5f5;
  border: none;
  border-radius: 4px;
  color: #666;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.select-all-btn:hover {
  background: #e8e8e8;
}

.selected-count {
  font-size: 0.9rem;
  color: #666;
}

.batch-delete-btn {
  padding: 6px 12px;
  background: #f44336;
  border: none;
  border-radius: 4px;
  color: white;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.batch-delete-btn:hover:not(:disabled) {
  background: #d32f2f;
}

.batch-delete-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
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

.action-btn:hover:not(:disabled) {
  background-color: #f5f5f5;
  color: #333;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.mobile-menu {
  display: none;
}

.content {
  flex: 1;
  overflow-y: auto;
  background-color: #fafafa;
}

/* ===== 加载、错误、空状态样式 ===== */
.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  text-align: center;
  min-height: 400px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
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

.error-icon,
.empty-icon {
  color: #999;
  margin-bottom: 16px;
}

.error-state h3,
.empty-state h3 {
  color: #333;
  margin: 0 0 8px 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.error-state p,
.empty-state p {
  color: #666;
  margin: 0 0 24px 0;
  font-size: 1rem;
}

.retry-btn {
  padding: 12px 24px;
  background-color: #2196f3;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.retry-btn:hover {
  background-color: #1976d2;
}

/* ===== 新闻内容样式 ===== */
.news-content {
  padding: 24px;
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: #999;
  z-index: 1;
}

.search-input {
  width: 100%;
  padding: 8px 12px 8px 40px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 0.9rem;
  transition: all 0.2s ease;
}

/* ===== 筛选栏样式 ===== */
.filters-bar {
  background: white;
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 24px;
  display: flex;
  gap: 24px;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.search-section {
  /* flex: 0 1 400px; */
  max-width: 400px;
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: #999;
  z-index: 1;
}

.search-input {
  width: 100%;
  padding: 8px 12px 8px 40px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 0.9rem;
  transition: all 0.2s ease;
}

.search-input:focus {
  outline: none;
  border-color: #2196f3;
  box-shadow: 0 0 0 2px rgba(33, 150, 243, 0.1);
}

.clear-search-btn {
  position: absolute;
  right: 8px;
  padding: 4px;
  border: none;
  background: none;
  color: #999;
  cursor: pointer;
  border-radius: 3px;
  transition: all 0.2s ease;
}

.clear-search-btn:hover {
  background-color: #f5f5f5;
  color: #666;
}

.filter-section {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-select {
  padding: 6px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 0.85rem;
  background: white;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.filter-select:focus {
  outline: none;
  border-color: #2196f3;
}

.filter-select:focus {
  outline: none;
  border-color: #2196f3;
}

/* ===== 新闻列表样式 ===== */
.news-item-wrapper {
  margin-top: 20px;
  /* 移除 display: flex */
  transition: all 0.3s ease;
  position: relative; /* 为复选框定位做准备 */
}

.news-item-wrapper.selection-mode {
  padding-left: 40px; /* 为复选框留出空间 */
}

.selection-checkbox {
  position: absolute;
  left: 8px;
  top: 16px; /* 调整垂直位置 */
  z-index: 10;
}

.selection-checkbox input[type='checkbox'] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

/* 确保卡片在选择模式下有适当的背景对比 */
.news-item-wrapper .news-card.selected {
  background: #e3f2fd;
  border: 2px solid #2196f3;
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.2);
}

.news-item-wrapper .news-card.clickable {
  cursor: pointer;
}

.news-item-wrapper .news-card.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 确保下拉菜单不被遮挡 */
.news-item-wrapper :deep(.news-card.dropdown-open) {
  z-index: 9998;
  position: relative;
}

/* ===== 移动端响应式样式 ===== */
@media (max-width: 767px) {
  .header {
    padding: 0 16px;
    min-height: 56px; /* 减小高度，避免挤压 */
    flex-wrap: nowrap; /* 避免flex元素换行 */
  }

  .header-left {
    /* padding: 0 0 0 50px; 为了给移动菜单留出空间 */
    flex: 1; /* 占据剩余空间 */
  }

  .page-title {
    font-size: 1.25rem;
  }

  .item-count {
    font-size: 0.8rem;
  }

  .header-actions {
    display: none;
  }

  /* 隐藏桌面版批量操作工具栏 */
  .selection-toolbar.desktop-only {
    display: none;
  }

  /* 显示移动端专用的批量操作工具栏 */
  .mobile-selection-toolbar {
    display: flex;
  }

  .mobile-menu {
    display: block;
    position: absolute;
    top: 16px;
    right: 16px;
  }

  .news-content {
    padding: 16px;
  }

  /* 移动端搜索筛选栏适配 */
  .filters-bar {
    padding: 12px 16px;
    flex-direction: column;
    gap: 8px; /* 减少上下间隙 */
  }

  .search-section {
    max-width: none;
    width: 100%;
  }

  .filter-section {
    width: 100%;
    overflow-x: auto;
    padding-bottom: 2px; /* 减少底部间距避免滚动条 */
    /* 隐藏水平滚动条但保持滚动功能 */
    scrollbar-width: none; /* Firefox */
    -ms-overflow-style: none; /* IE/Edge */
  }

  .filter-section::-webkit-scrollbar {
    display: none; /* Chrome/Safari/Webkit */
  }

  .filter-select {
    min-width: 90px; /* 稍微减小最小宽度 */
    flex-shrink: 0;
    font-size: 0.8rem; /* 移动端稍小字体 */
  }
}
</style>
