<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import DropdownMenu, { type DropdownItem } from '../components/DropdownMenu.vue'
import { useUserStore } from '../stores/userStore'
import { useTagStore } from '../stores/tagStore'
import { useRouter } from 'vue-router'
import { deleteTag, createTag, type Tag } from '../api/tag'

const router = useRouter()
const userStore = useUserStore()
const tagStore = useTagStore()

// 页面状态 - 使用tagStore的状态
const loading = computed(() => tagStore.loading)
const tagStoreError = computed(() => tagStore.error)

// 本地错误状态（用于登录等非标签相关错误）
const localError = ref('')

// 综合错误状态
const error = computed(() => localError.value || tagStoreError.value)
// 删除相关状态
const deleting = ref(false)

// 创建标签状态
const showCreateInput = ref(false)
const newTagName = ref('')
const createTagLoading = ref(false)
const createTagError = ref('')

// 标签数据 - 使用tagStore的数据
const tags = computed(() => tagStore.tags)

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

// 获取标签列表 - 使用tagStore
const fetchTags = async () => {
  try {
    localError.value = ''
    await tagStore.fetchTags()
  } catch (err: any) {
    console.error('获取标签列表失败:', err)
    localError.value = err.message || '获取标签列表失败，请稍后再试'
  }
}

// 导航到标签详情页
const navigateToTagDetail = (tagId: number) => {
  router.push(`/tags/${tagId}`)
}

// 显示创建标签输入框
const showCreateTagInput = () => {
  console.log('showCreateTagInput 被调用')
  showCreateInput.value = true
  createTagError.value = ''
  newTagName.value = ''
  // 延迟聚焦，确保输入框已渲染
  setTimeout(() => {
    const input = document.querySelector('.create-tag-input') as HTMLInputElement
    if (input) {
      input.focus()
    }
  }, 100)
}

// 隐藏创建标签输入框
const hideCreateTagInput = () => {
  showCreateInput.value = false
  newTagName.value = ''
  createTagError.value = ''
}

// 创建标签
const handleCreateTag = async () => {
  console.log('handleCreateTag 被调用，标签名称:', newTagName.value)
  const name = newTagName.value.trim()
  if (!name) {
    createTagError.value = '标签名称不能为空'
    return
  }

  if (name.length > 10) {
    createTagError.value = '标签名称不能超过10个字符'
    return
  }

  // 检查是否重名
  if (tags.value.some((t) => t.name === name)) {
    createTagError.value = '标签名称已存在'
    return
  }

  try {
    createTagLoading.value = true
    createTagError.value = ''

    console.log('开始调用 createTag API')
    const response = await createTag(name)
    console.log('createTag API 响应:', response)

    if (response.code === 200 && response.data) {
      console.log('标签创建成功')

      // 更新标签store，这样所有组件都会刷新
      tagStore.addTag(response.data)

      // 重置状态
      hideCreateTagInput()
    } else {
      createTagError.value = response.message || '创建标签失败'
    }
  } catch (error: any) {
    console.error('创建标签失败:', error)
    createTagError.value = error.message || '创建失败，请重试'
  } finally {
    createTagLoading.value = false
  }
}

// 处理输入框回车事件
const handleEnterKey = () => {
  if (!createTagLoading.value) {
    handleCreateTag()
  }
}

// 处理输入框ESC事件
const handleEscapeKey = () => {
  hideCreateTagInput()
}

// 刷新数据
const handleRefresh = async () => {
  await fetchTags()
}

// 删除标签
const deleteTagHandler = async (tag: Tag) => {
  // 使用浏览器原生confirm弹窗确认
  const confirmResult = window.confirm(`确定要删除标签"${tag.name}"吗？此操作无法撤销。`)
  
  if (!confirmResult) {
    return
  }

  try {
    deleting.value = true
    const response = await deleteTag(tag.id.toString())

    if (response.code === 200) {
      // 删除成功，更新标签store
      tagStore.removeTag(tag.id)
    } else {
      localError.value = response.message || '删除标签失败'
    }
  } catch (err: any) {
    console.error('删除标签失败:', err)
    localError.value = err.message || '删除标签失败，请稍后再试'
  } finally {
    deleting.value = false
  }
}

// 定义下拉菜单项
const mobileMenuItems: DropdownItem[] = [
  {
    id: 'create-tag',
    label: '创建标签',
    icon: '<line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line>',
    action: showCreateTagInput,
  },
  {
    id: 'refresh',
    label: '刷新',
    icon: '<path d="M23 4v6h-6" /><path d="M1 20v-6h6" /><path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15" />',
    action: handleRefresh,
  },
]

// 格式化时间
const formatTime = (timeString: string) => {
  try {
    const date = new Date(timeString)
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    })
  } catch {
    return timeString
  }
}

// 计算总标签数量
const totalTagsCount = computed(() => {
  return tags.value.length
})

// 生命周期
onMounted(() => {
  if (!userStore.isLoggedIn) {
    localError.value = '请先登录以查看标签内容'
    return
  }
  fetchTags()
})
</script>

<template>
  <div class="tags-view">
    <!-- Main Content -->
    <main class="main-content">
      <!-- Header -->
      <header class="header">
        <div class="header-left">
          <h1 class="page-title">标签总览</h1>
          <span v-if="!loading && totalTagsCount > 0" class="item-count">
            {{ totalTagsCount }} 个标签
          </span>
        </div>
        <div class="header-actions">
          <!-- 创建标签按钮和下拉框 -->
          <div class="create-tag-container">
            <button
              class="action-btn"
              title="创建标签"
              @click="showCreateTagInput"
              v-if="!showCreateInput"
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
            </button>

            <!-- 创建标签下拉框 -->
            <div v-if="showCreateInput" class="create-tag-dropdown">
              <div class="create-tag-form">
                <input
                  v-model="newTagName"
                  class="create-tag-input"
                  type="text"
                  placeholder="输入标签名称"
                  maxlength="10"
                  @keyup.enter="handleEnterKey"
                  @keyup.escape="handleEscapeKey"
                />
                <div v-if="createTagError" class="create-tag-error">
                  {{ createTagError }}
                </div>
                <div class="create-tag-actions">
                  <button @mousedown="hideCreateTagInput" class="cancel-btn" type="button">
                    取消
                  </button>
                  <button
                    @mousedown="handleCreateTag"
                    class="confirm-btn"
                    :disabled="createTagLoading || !newTagName.trim()"
                    type="button"
                  >
                    {{ createTagLoading ? '创建中...' : '创建' }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <button class="action-btn" title="刷新" @click="handleRefresh">
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

      <!-- Content Area -->
      <div class="content">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>加载标签数据中...</p>
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
          <div v-else class="empty-state">
            <svg
              width="64"
              height="64"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <path
                d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"
              ></path>
              <line x1="7" y1="7" x2="7.01" y2="7"></line>
            </svg>
            <h3>还没有标签</h3>
            <p>创建你的第一个标签来组织收藏的文章</p>
            <button @click="showCreateTagInput" class="create-first-tag-btn">创建标签</button>
          </div>
        </div>

        <!-- 标签数据展示 -->
        <div v-else class="tags-container">
          <!-- 空状态 -->
          <div v-if="tags.length === 0" class="empty-state">
            <svg
              width="64"
              height="64"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <path
                d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"
              ></path>
              <line x1="7" y1="7" x2="7.01" y2="7"></line>
            </svg>
            <h3>还没有标签</h3>
            <p>创建你的第一个标签来组织收藏的文章</p>
            <button @click="showCreateTagInput" class="create-first-tag-btn">创建标签</button>
          </div>

          <!-- 标签列表 -->
          <div v-else class="tags-list">
            <div v-for="tag in tags" :key="tag.id" class="tag-drawer">
              <!-- 标签头部 -->
              <div class="tag-header">
                <div class="tag-info" @click="navigateToTagDetail(tag.id)">
                  <div
                    class="tag-color"
                    :style="{ backgroundColor: getTagColor(tags.indexOf(tag), tag.color) }"
                  ></div>
                  <div class="tag-details">
                    <h3 class="tag-name">{{ tag.name }}</h3>
                    <div class="tag-stats">
                      <span class="news-count">{{ tag.itemCount || tag.count || 0 }} 篇文章</span>
                      <span class="created-date">创建于 {{ formatTime(tag.createTime) }}</span>
                    </div>
                  </div>
                </div>
                <div class="tag-actions">
                  <button
                    class="delete-btn"
                    @click.stop="deleteTagHandler(tag)"
                    v-if="!deleting"
                    title="删除标签"
                  >
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
                      <line x1="10" y1="11" x2="10" y2="17"></line>
                      <line x1="14" y1="11" x2="14" y2="17"></line>
                    </svg>
                  </button>
                  <button class="delete-btn loading" v-else title="删除中..." disabled>
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
                      <line x1="10" y1="11" x2="10" y2="17"></line>
                      <line x1="14" y1="11" x2="14" y2="17"></line>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.tags-view {
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

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
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

/* ===== 创建标签下拉框样式 ===== */
.create-tag-container {
  position: relative;
}

.create-tag-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  padding: 16px;
  min-width: 280px;
  z-index: 1000;
  margin-top: 8px;
}

.create-tag-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.create-tag-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
  outline: none;
  transition: border-color 0.2s ease;
}

.create-tag-input:focus {
  border-color: #2196f3;
  box-shadow: 0 0 0 2px rgba(33, 150, 243, 0.1);
}

.create-tag-error {
  color: #f44336;
  font-size: 0.8rem;
  margin-top: -4px;
}

.create-tag-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 4px;
}

.cancel-btn {
  padding: 6px 12px;
  background: #f5f5f5;
  border: none;
  border-radius: 4px;
  color: #666;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.cancel-btn:hover {
  background: #e8e8e8;
}

.confirm-btn {
  padding: 6px 12px;
  background: #2196f3;
  border: none;
  border-radius: 4px;
  color: white;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.confirm-btn:hover:not(:disabled) {
  background: #1976d2;
}

.confirm-btn:disabled {
  background: #ccc;
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

/* ===== 标签容器样式 ===== */
.tags-container {
  padding: 24px;
}

.tags-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tag-drawer {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: all 0.3s ease;
}

.tag-drawer:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.tag-header {
  padding: 20px 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.2s ease;
}

.tag-header:hover {
  background-color: #fafafa;
}

.tag-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.tag-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  flex-shrink: 0;
}

.tag-details {
  flex: 1;
}

.tag-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px 0;
}

.tag-stats {
  display: flex;
  gap: 16px;
  font-size: 0.8rem;
  color: #999;
}

.tag-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.delete-btn {
  padding: 6px;
  background: none;
  border: none;
  border-radius: 4px;
  color: #999;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-btn:hover {
  background: #ffebee;
  color: #f44336;
}

.delete-btn.loading {
  opacity: 0.5;
  cursor: not-allowed;
}

.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ===== 空状态样式 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-state svg {
  color: #ccc;
  margin-bottom: 20px;
}

.empty-state h3 {
  font-size: 1.2rem;
  color: #666;
  margin: 0 0 8px 0;
}

.empty-state p {
  color: #999;
  margin: 0 0 24px 0;
  font-size: 0.9rem;
}

.create-first-tag-btn {
  padding: 10px 20px;
  background: #2196f3;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s ease;
}

.create-first-tag-btn:hover {
  background: #1976d2;
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

.error-content p {
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

/* ===== 移动端响应式样式 ===== */
@media (max-width: 767px) {
  .header {
    padding: 0 16px;
    min-height: 64px;
  }

  .header-left {
    padding: 0 0 0 50px; /* 为了给移动菜单留出空间 */
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

  .mobile-menu {
    display: block;
    position: absolute;
    top: 16px;
    right: 16px;
  }

  .tags-container {
    padding: 16px;
  }

  .tag-header {
    padding: 16px 20px;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .tag-info {
    width: 100%;
  }

  .tag-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .create-tag-dropdown {
    right: -50%;
    min-width: 240px;
  }
}

/* ===== 中等屏幕样式 ===== */
@media (min-width: 768px) and (max-width: 1199px) {
  .tags-container {
    padding: 20px;
  }

  .header {
    padding: 0 20px;
  }
}

/* ===== 大屏幕样式 ===== */
@media (min-width: 1200px) {
  .tags-container {
    padding: 32px;
    max-width: 1200px;
    margin: 0 auto;
  }

  .header {
    padding: 0 32px;
  }
}
</style>
