<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import DropdownMenu, { type DropdownItem } from './DropdownMenu.vue'
import { useUserStore } from '../stores/userStore'
import { useHomeStore } from '../stores/homeStore'
import { useTagStore } from '../stores/tagStore'
import { createFolder } from '../api/folder'
import { createTag } from '../api/tag'

const userStore = useUserStore()
const homeStore = useHomeStore()
const tagStore = useTagStore()

interface Props {
  collapsed: boolean
}

defineProps<Props>()
defineEmits<{
  toggle: []
}>()

const router = useRouter()
const route = useRoute()

// 默认图标路径
const defaultChannelIcon = new URL(
  '../assets/placeholders/newsSouceIconDefault.png',
  import.meta.url,
).href

// 将用户下拉菜单项改为计算属性
const userMenuItems = computed<DropdownItem[]>(() => [
  {
    id: 'settings',
    label: '设置',
    icon: '<path d="M12.013 2.25c.734.008 1.465.093 2.182.253a.75.75 0 0 1 .582.649l.17 1.527a1.384 1.384 0 0 0 1.928 1.116l1.4-.615a.75.75 0 0 1 .85.174 9.792 9.792 0 0 1 2.204 3.792.75.75 0 0 1-.271.825l-1.242.916a1.381 1.381 0 0 0 0 2.226l1.243.915a.75.75 0 0 1 .272.826 9.797 9.797 0 0 1-2.204 3.792.75.75 0 0 1-.848.175l-1.407-.617a1.38 1.38 0 0 0-1.926 1.114l-.169 1.526a.75.75 0 0 1-.572.647 9.518 9.518 0 0 1-4.406 0 .75.75 0 0 1-.572-.647l-.168-1.524a1.382 1.382 0 0 0-1.926-1.11l-1.406.616a.75.75 0 0 1-.849-.175 9.798 9.798 0 0 1-2.204-3.796.75.75 0 0 1 .272-.826l1.243-.916a1.38 1.38 0 0 0 0-2.226l-1.243-.914a.75.75 0 0 1-.271-.826 9.793 9.793 0 0 1 2.204-3.792.75.75 0 0 1 .85-.174l1.4.615a1.387 1.387 0 0 0 1.93-1.118l.17-1.526a.75.75 0 0 1 .583-.65c.717-.159 1.448-.244 2.201-.252ZM12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z"/>',
    action: () => {
      router.push('/settings')
    },
  },
  {
    id: 'auth',
    label: userStore.isLoggedIn === false ? '登录' : '退出登录',
    icon: userStore.isLoggedIn
      ? '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16,17 21,12 16,7"/><line x1="21" y1="12" x2="9" y2="12"/>'
      : '<path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10,17 15,12 10,7"/><line x1="15" y1="12" x2="3" y2="12"/>',
    action: async () => {
      console.log('userStore.isLoggedIn:', userStore.isLoggedIn)
      if (userStore.isLoggedIn) {
        // 退出登录逻辑
        try {
          await userStore.logout()
          console.log('用户已退出登录')

          // 显示退出成功提示
          const successMessage = document.createElement('div')
          successMessage.textContent = '已成功退出登录'
          successMessage.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: #f0f9ff;
            border: 1px solid #0ea5e9;
            color: #0369a1;
            padding: 12px 20px;
            border-radius: 6px;
            font-size: 14px;
            z-index: 9999;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
          `
          document.body.appendChild(successMessage)

          // 1.5秒后刷新页面，确保界面完全更新
          setTimeout(() => {
            if (document.body.contains(successMessage)) {
              document.body.removeChild(successMessage)
            }
            // 强制刷新页面，确保所有状态被重置
            window.location.reload()
          }, 1500)
        } catch (error) {
          console.error('退出登录失败:', error)

          // 显示错误提示
          const errorMessage = document.createElement('div')
          errorMessage.textContent = '退出登录失败，请重试'
          errorMessage.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: #fef2f2;
            border: 1px solid #ef4444;
            color: #dc2626;
            padding: 12px 20px;
            border-radius: 6px;
            font-size: 14px;
            z-index: 9999;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
          `
          document.body.appendChild(errorMessage)

          // 3秒后自动移除提示
          setTimeout(() => {
            if (document.body.contains(errorMessage)) {
              document.body.removeChild(errorMessage)
            }
          }, 3000)
        }
      } else {
        // 跳转到登录页面
        router.push('/login')
      }
    },
  },
])
// 在 script 部分添加以下代码

// 标签展开状态
const isTagsExpanded = ref(false)
const menuItems = ref([
  { icon: 'today', label: '主页', path: '/', active: route.path === '/' },
  // { icon: 'follow', label: 'Follow Sources', path: '/follow', active: route.path === '/follow' },
  // { icon: 'create', label: 'Create AI Feed', path: '/create', active: route.path === '/create' },
  { icon: 'search', label: '发现', path: '/search', active: route.path === '/search' },
  { icon: 'chat', label: 'AI 对话', path: '/chat', active: route.path.startsWith('/chat') },
  { icon: 'later', label: '稍后再读', path: '/later', active: route.path === '/later' },
  {
    icon: 'tags',
    label: '标签',
    path: '/tags',
    active: route.path === '/tags',
    expandable: true,
    expanded: isTagsExpanded,
  },
])

// 标签列表相关状态 - 使用tagStore
const tags = computed(() => {
  // 生成随机颜色的函数
  const colors = [
    '#2196f3',
    '#4caf50',
    '#ff9800',
    '#9c27b0',
    '#f44336',
    '#00bcd4',
    '#795548',
    '#607d8b',
  ]

  return tagStore.tags.map((tag, index) => ({
    id: tag.id.toString(),
    name: tag.name,
    color: colors[index % colors.length], // 循环使用颜色
    count: tag.itemCount || tag.count || 0, // 优先使用itemCount，保持向后兼容
  }))
})
// 获取用户标签列表 - 使用tagStore的方法
const fetchUserTags = async () => {
  // 只有在用户登录时才获取标签
  if (userStore.isLoggedIn) {
    await tagStore.fetchTags()
  } else {
    console.log('用户未登录，跳过获取标签列表')
  }
}

// 切换标签展开状态
const toggleTags = () => {
  isTagsExpanded.value = !isTagsExpanded.value
}
// 从 homeStore 获取数据
const folders = computed(() => homeStore.folders)
const unCategorizedChannels = computed(() => homeStore.unCategorizedChannels)

// 文件夹展开状态 - 默认展开所有文件夹
const expandedFolders = ref<Set<string>>(new Set())

// 初始化展开状态
const initializeExpandedFolders = () => {
  if (folders.value.length > 0) {
    folders.value.forEach((folder) => {
      expandedFolders.value.add(folder.id)
    })
  }
}

// 确保数据已加载
onMounted(async () => {
  // 只有在用户登录时才加载数据
  if (userStore.isLoggedIn) {
    // 如果 homeStore 还没有数据，尝试获取
    if (!homeStore.hasData && !homeStore.loading) {
      await homeStore.fetchHomeData()
    }
    // 延迟一点初始化展开状态，确保数据已加载
    setTimeout(initializeExpandedFolders, 100)

    // 获取用户标签列表
    await fetchUserTags()
  } else {
    console.log('用户未登录，跳过加载 folders、channels 和 tags 数据')
  }
})

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

// 创建文件夹相关状态
const showCreateFolderInput = ref(false)
const newFolderName = ref('')
const createFolderLoading = ref(false)
const createFolderError = ref('')

// 显示创建文件夹输入框
const showCreateFolder = () => {
  // 检查用户是否已登录
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }

  showCreateFolderInput.value = true
  newFolderName.value = ''
  createFolderError.value = ''
  // 延迟聚焦，确保输入框已渲染
  setTimeout(() => {
    const input = document.querySelector('.create-folder-input') as HTMLInputElement
    if (input) {
      input.focus()
    }
  }, 100)
}

// 取消创建文件夹
const cancelCreateFolder = () => {
  showCreateFolderInput.value = false
  newFolderName.value = ''
  createFolderError.value = ''
}

// 创建文件夹
const handleCreateFolder = async () => {
  const name = newFolderName.value.trim()
  if (!name) {
    createFolderError.value = '文件夹名称不能为空'
    return
  }

  if (name.length > 20) {
    createFolderError.value = '文件夹名称不能超过20个字符'
    return
  }

  // 检查是否重名
  if (folders.value.some((f) => f.name === name)) {
    createFolderError.value = '文件夹名称已存在'
    return
  }

  try {
    createFolderLoading.value = true
    createFolderError.value = ''

    await createFolder(name)
    console.log('文件夹创建成功')

    // 刷新数据
    await homeStore.fetchHomeData()

    // 重置状态
    showCreateFolderInput.value = false
    newFolderName.value = ''
  } catch (error) {
    console.error('创建文件夹失败:', error)
    createFolderError.value = '创建失败，请重试'
  } finally {
    createFolderLoading.value = false
  }
}

// 处理输入框键盘事件
const handleCreateFolderKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter') {
    handleCreateFolder()
  } else if (event.key === 'Escape') {
    cancelCreateFolder()
  }
}

// 创建标签相关状态
const showCreateTagInput = ref(false)
const newTagName = ref('')
const createTagLoading = ref(false)
const createTagError = ref('')

// 显示创建标签输入框
const showCreateTag = () => {
  // 检查用户是否已登录
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }

  showCreateTagInput.value = true
  newTagName.value = ''
  createTagError.value = ''
  // 延迟聚焦，确保输入框已渲染
  setTimeout(() => {
    const input = document.querySelector('.create-tag-input') as HTMLInputElement
    if (input) {
      input.focus()
    }
  }, 100)
}

// 取消创建标签
const cancelCreateTag = () => {
  showCreateTagInput.value = false
  newTagName.value = ''
  createTagError.value = ''
}

// 创建标签
const handleCreateTag = async () => {
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

    const response = await createTag(name)

    if (response.code === 200 && response.data) {
      console.log('标签创建成功')

      // 添加到tagStore
      tagStore.addTag(response.data)

      // 重置状态
      showCreateTagInput.value = false
      newTagName.value = ''
    } else {
      createTagError.value = response.message || '创建失败，请重试'
    }
  } catch (error) {
    console.error('创建标签失败:', error)
    createTagError.value = '创建失败，请重试'
  } finally {
    createTagLoading.value = false
  }
}

// 处理标签输入框键盘事件
const handleCreateTagKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter') {
    handleCreateTag()
  } else if (event.key === 'Escape') {
    cancelCreateTag()
  }
}
</script>

<template>
  <aside class="sidebar" :class="{ collapsed }">
    <!-- Header - 固定在顶部 -->
    <div class="sidebar-header">
      <div class="brand" v-if="!collapsed">
        <div class="brand-icon">📰</div>
        <span class="brand-name">AmiNews</span>
      </div>
      <div class="header-actions">
        <!-- 用户下拉菜单 -->
        <DropdownMenu
          :items="userMenuItems"
          button-icon="user"
          button-title="用户菜单"
          position="right"
          size="small"
          class="user-menu"
          v-if="!collapsed"
        />
        <button class="toggle-btn" @click="$emit('toggle')" title="切换侧边栏">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <line x1="3" y1="8" x2="21" y2="8" />
            <line x1="3" y1="14" x2="21" y2="14" />
            <line x1="3" y1="20" x2="21" y2="20" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 添加 sidebar-content 容器 -->
    <div class="sidebar-content">
      <!-- Navigation Menu -->
      <nav class="nav-menu">
        <ul class="menu-list">
          <li v-for="item in menuItems" :key="item.label" class="menu-item">
            <!-- 修改标签菜单项部分 -->
            <div v-if="item.expandable" class="expandable-menu-item">
              <!-- 将 div 改为 router-link，并添加点击事件处理 -->
              <router-link
                :to="item.path"
                class="menu-link"
                :class="{ active: item.active }"
                @click="toggleTags"
                role="button"
                tabindex="0"
              >
                <span class="menu-icon">
                  <svg
                    v-if="item.icon === 'tags'"
                    width="20"
                    height="20"
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
                </span>
                <span class="menu-text" v-show="!collapsed">{{ item.label }}</span>

                <span class="expand-icon-container" v-show="!collapsed">
                  <svg
                    v-if="item.expanded"
                    class="expand-icon"
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <polyline points="18 15 12 9 6 15"></polyline>
                  </svg>
                  <svg
                    v-else
                    class="expand-icon"
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <polyline points="6 9 12 15 18 9"></polyline>
                  </svg>
                </span>
              </router-link>

              <!-- 标签列表 -->
              <div v-if="item.expanded && !collapsed" class="tags-list">
                <router-link
                  v-for="tag in tags"
                  :key="tag.id"
                  :to="`/tags/${tag.id}`"
                  class="tag-link"
                  :class="{ active: route.params.id === tag.id && route.path.includes('/tags/') }"
                >
                  <span class="tag-color" :style="`background-color: ${tag.color}`"></span>
                  <span class="tag-name">{{ tag.name }}</span>
                  <span class="tag-count">{{ tag.count }}</span>
                </router-link>

                <div class="tag-actions">
                  <!-- 创建标签输入框 -->
                  <div v-if="showCreateTagInput" class="create-tag-form">
                    <div class="input-group">
                      <input
                        v-model="newTagName"
                        type="text"
                        class="create-tag-input"
                        placeholder="输入标签名称"
                        maxlength="10"
                        @keydown="handleCreateTagKeydown"
                        :disabled="createTagLoading"
                      />
                      <div class="input-actions">
                        <button
                          class="confirm-btn"
                          @click="handleCreateTag"
                          :disabled="createTagLoading || !newTagName.trim()"
                          title="确认创建"
                        >
                          <svg
                            width="14"
                            height="14"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                          >
                            <polyline points="20,6 9,17 4,12"></polyline>
                          </svg>
                        </button>
                        <button
                          class="cancel-btn"
                          @click="cancelCreateTag"
                          :disabled="createTagLoading"
                          title="取消"
                        >
                          <svg
                            width="14"
                            height="14"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                          >
                            <line x1="18" y1="6" x2="6" y2="18"></line>
                            <line x1="6" y1="6" x2="18" y2="18"></line>
                          </svg>
                        </button>
                      </div>
                    </div>
                    <div v-if="createTagError" class="error-message">{{ createTagError }}</div>
                  </div>

                  <!-- 创建标签按钮 -->
                  <button v-else class="create-tag-btn" @click="showCreateTag" title="创建新标签">
                    <svg
                      width="14"
                      height="14"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <line x1="12" y1="5" x2="12" y2="19"></line>
                      <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                    <span>创建新标签</span>
                  </button>
                </div>
              </div>
            </div>

            <router-link v-else :to="item.path" class="menu-link" :class="{ active: item.active }">
              <span class="menu-icon">
                <!-- 保持原有图标渲染逻辑不变 -->
                <svg
                  v-if="item.icon === 'today'"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                  <line x1="16" y1="2" x2="16" y2="6" />
                  <line x1="8" y1="2" x2="8" y2="6" />
                  <line x1="3" y1="10" x2="21" y2="10" />
                </svg>
                <svg
                  v-else-if="item.icon === 'follow'"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
                  <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
                <svg
                  v-else-if="item.icon === 'create'"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <line x1="12" y1="5" x2="12" y2="19" />
                  <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
                <svg
                  v-else-if="item.icon === 'search'"
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
                <svg
                  v-else-if="item.icon === 'chat'"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
                </svg>
                <svg
                  v-else-if="item.icon === 'later'"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12,6 12,12 16,14" />
                </svg>
              </span>
              <span class="menu-text" v-show="!collapsed">{{ item.label }}</span>
            </router-link>
          </li>
        </ul>
      </nav>
      <!-- Feeds Section -->
      <div class="feeds-section" v-if="!collapsed">
        <div class="section-header">
          <h3 class="feeds-title">我的订阅</h3>
          <!-- <span class="total-unread">{{ homeStore.totalUnreadCount }}</span> -->
        </div>

        <!-- 加载状态 -->
        <div v-if="homeStore.loading" class="loading-state">
          <div class="loading-spinner"></div>
          <span class="loading-text">加载中...</span>
        </div>

        <!-- 错误状态 -->
        <div v-else-if="homeStore.error" class="error-state">
          <span class="error-text">{{ homeStore.error }}</span>
          <button @click="homeStore.fetchHomeData()" class="retry-btn">重试</button>
        </div>

        <!-- 数据展示 -->
        <div v-else>
          <!-- 订阅源文件夹 -->
          <div v-if="folders.length > 0" class="folders-section">
            <div
              v-for="folder in folders"
              :key="folder.id"
              class="folder-item"
              :class="{ expanded: isFolderExpanded(folder.id) }"
            >
              <!-- 文件夹头部 -->
              <div class="folder-header">
                <!-- 左侧可点击区域 - 导航到文件夹页面 -->
                <router-link :to="`/feeds/folder/${folder.id}`" class="folder-link">
                  <div class="folder-info">
                    <span class="folder-icon">📁</span>
                    <span class="folder-name">{{ folder.name }}</span>
                  </div>
                  <!-- <span class="folder-count">{{ folder.unreadCount }}</span> -->
                </router-link>

                <!-- 右侧展开/收起按钮 -->
                <button
                  class="folder-toggle-btn"
                  @click="toggleFolder(folder.id)"
                  :title="isFolderExpanded(folder.id) ? '收起文件夹' : '展开文件夹'"
                >
                  <svg
                    width="14"
                    height="14"
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

              <!-- 文件夹内的频道列表 -->
              <div v-show="isFolderExpanded(folder.id)" class="folder-channels">
                <div v-for="channel in folder.channels" :key="channel.id" class="channel-item">
                  <router-link :to="`/feeds/channel/${channel.id}`" class="channel-link">
                    <img
                      :src="channel.icon"
                      :alt="channel.name"
                      class="channel-icon"
                      @error="(e) => ((e.target as HTMLImageElement).src = defaultChannelIcon)"
                    />
                    <span class="channel-name">{{ channel.name }}</span>
                    <!-- <span class="channel-count">{{ channel.unreadCount }}</span> -->
                  </router-link>
                </div>
              </div>
            </div>
          </div>

          <!-- 未分类的频道 -->
          <div v-if="unCategorizedChannels.length > 0" class="uncategorized-channels">
            <div v-for="channel in unCategorizedChannels" :key="channel.id" class="channel-item">
              <router-link :to="`/feeds/channel/${channel.id}`" class="channel-link">
                <img
                  :src="channel.icon"
                  :alt="channel.name"
                  class="channel-icon"
                  @error="(e) => ((e.target as HTMLImageElement).src = defaultChannelIcon)"
                />
                <span class="channel-name">{{ channel.name }}</span>
                <!-- <span class="channel-count">{{ channel.unreadCount }}</span> -->
              </router-link>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="!homeStore.hasData && !homeStore.loading" class="empty-state">
            <span class="empty-text">暂无订阅源</span>
          </div>

          <!-- 创建文件夹 -->
          <div class="create-folder-section">
            <!-- 创建文件夹输入框 -->
            <div v-if="showCreateFolderInput" class="create-folder-form">
              <div class="input-group">
                <input
                  v-model="newFolderName"
                  type="text"
                  class="create-folder-input"
                  placeholder="输入文件夹名称"
                  maxlength="20"
                  @keydown="handleCreateFolderKeydown"
                  :disabled="createFolderLoading"
                />
                <div class="input-actions">
                  <button
                    class="confirm-btn"
                    @click="handleCreateFolder"
                    :disabled="createFolderLoading || !newFolderName.trim()"
                    title="确认创建"
                  >
                    <svg
                      width="14"
                      height="14"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <path d="M20 6L9 17l-5-5" />
                    </svg>
                  </button>
                  <button
                    class="cancel-btn"
                    @click="cancelCreateFolder"
                    :disabled="createFolderLoading"
                    title="取消"
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
              <div v-if="createFolderError" class="error-message">{{ createFolderError }}</div>
            </div>

            <!-- 创建文件夹按钮 -->
            <button v-else class="create-folder-btn" @click="showCreateFolder" title="创建新文件夹">
              <svg
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              <span>创建文件夹</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Bottom Links -->
      <div class="sidebar-footer" v-if="!collapsed">
        <a href="#" class="footer-link">Integrations & API</a>
        <a href="#" class="footer-link">Blog</a>
        <a href="#" class="footer-link">Learn & Get Support</a>
      </div>
    </div>
  </aside>
</template>

<style scoped>
/* 标签相关样式 */
.expandable-menu-item {
  display: flex;
  flex-direction: column;
}

.tags-list {
  display: flex;
  flex-direction: column;
  margin-left: 32px;
  margin-top: 4px;
  margin-bottom: 8px;
}

.tag-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  color: #666;
  text-decoration: none;
  font-size: 13px;
  border-radius: 4px;
  transition: all 0.2s ease;
  margin-bottom: 2px;
}

/* 修改悬停效果，与其他菜单项保持一致 */
.tag-link:hover {
  background-color: #f0f0f0;
  color: #333;
}

/* 修改激活状态，与其他菜单项保持一致 */
.tag-link.active {
  background-color: #e3f2fd;
  color: #2196f3;
}

.tag-color {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  flex-shrink: 0;
}

.tag-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-count {
  font-size: 11px;
  color: #999;
  background: rgba(0, 0, 0, 0.05);
  padding: 1px 6px;
  border-radius: 10px;
}

.tag-actions {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
  margin-right: 16px;
}

.create-tag-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 6px 12px;
  background: none;
  border: none;
  color: #2196f3;
  cursor: pointer;
  font-size: 12px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.create-tag-btn:hover {
  background-color: #e3f2fd;
}

/* 创建标签表单样式 */
.create-tag-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
  margin-left: -16px;
}

.create-tag-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 6px 8px;
  font-size: 13px;
  background: transparent;
}

.create-tag-input:focus {
  outline: none;
}

/* 折叠时的样式 */
.sidebar.collapsed .expandable-menu-item {
  position: relative;
}

.sidebar.collapsed .tags-list {
  display: none;
}

/* 侧边栏基础样式 */
.sidebar {
  width: 280px;
  background: white;
  border-right: 1px solid #e5e5e5;
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  height: 100vh;
  overflow: hidden;
  transition:
    width 0.3s ease,
    transform 0.3s ease;
  z-index: var(--z-sidebar);
}

.sidebar.collapsed {
  width: 70px;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e5e5e5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  flex-shrink: 0;
  position: relative;
  z-index: var(--z-sidebar-header);
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.sidebar-content::-webkit-scrollbar {
  width: 6px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.sidebar-content::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-content::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.sidebar-content:hover::-webkit-scrollbar {
  opacity: 1;
}

.sidebar-content:hover {
  scrollbar-width: thin;
}

/* 品牌和标题 */
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-icon {
  font-size: 24px;
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

/* 头部操作区 */
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-menu {
  opacity: 1;
  transition: opacity 0.2s ease;
}

.user-menu :deep(.dropdown-menu) {
  z-index: var(--z-mega-menu) !important;
}

.user-menu :deep(.dropdown-overlay) {
  z-index: var(--z-dropdown-overlay) !important;
}

.toggle-btn {
  padding: 8px;
  border: none;
  background: none;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.toggle-btn:hover {
  background-color: #f0f0f0;
}

/* 导航菜单 */
.nav-menu {
  padding: 16px 0;
  border-bottom: 1px solid #e5e5e5;
}

.menu-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.menu-item {
  margin-bottom: 4px;
}

.menu-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  color: #666;
  text-decoration: none;
  transition: all 0.2s ease;
  border-radius: 0;
  position: relative;
}

/* 为所有菜单链接添加悬停效果 */
.menu-link:hover {
  background-color: #f0f0f0;
  color: #333;
}

.expand-icon-container {
  position: absolute;
  right: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.expand-icon {
  transition: transform 0.2s ease;
}

.router-link-active,
.router-link-exact-active {
  background-color: #e3f2fd;
  color: #2196f3;
  border-right: 3px solid #2196f3;
}

.menu-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.menu-text {
  font-size: 14px;
  font-weight: 500;
}

/* 订阅源区域 */
.feeds-section {
  padding: 16px;
  flex: 1;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.feeds-title {
  font-size: 14px;
  color: #666;
  margin: 0;
  font-weight: 600;
}

/* 加载和错误状态 */
.loading-state,
.error-state,
.empty-state {
  padding: 16px 12px;
  text-align: center;
  color: #666;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
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
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-text {
  font-size: 12px;
  color: #666;
}

.error-state {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.error-text {
  font-size: 12px;
  color: #f44336;
}

.retry-btn {
  padding: 4px 8px;
  background: #2196f3;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.retry-btn:hover {
  background: #1976d2;
}

.empty-text {
  font-size: 12px;
  color: #999;
}

/* 文件夹和频道样式 */
.folder-header {
  display: flex;
  align-items: center;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.folder-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  padding: 6px 8px 6px 12px;
  color: #666;
  text-decoration: none;
  border-radius: 4px 0 0 4px;
  transition: all 0.2s ease;
}

.folder-link:hover {
  background-color: #f0f0f0;
  color: #333;
}

.folder-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.folder-icon {
  flex-shrink: 0;
  width: 16px;
  text-align: center;
  font-size: 12px;
}

.folder-toggle-btn {
  flex-shrink: 0;
  padding: 6px 10px;
  border: none;
  background: none;
  color: #666;
  cursor: pointer;
  border-radius: 0 4px 4px 0;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
}

.folder-toggle-btn:hover {
  background-color: #f0f0f0;
  color: #333;
}

.folder-toggle-btn svg {
  width: 14px;
  height: 14px;
  transition: transform 0.2s ease;
}

.folder-toggle-btn svg.rotated {
  transform: rotate(90deg);
}

.folder-name {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.folder-channels {
  margin-left: 18px;
  margin-top: 4px;
  margin-bottom: 8px;
}

.channel-item {
  margin-bottom: 2px;
}

.channel-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  color: #666;
  text-decoration: none;
  font-size: 13px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.channel-link:hover {
  background-color: #f0f0f0;
  color: #333;
}

.channel-icon {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  border-radius: 2px;
  object-fit: cover;
}

.channel-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 底部链接 */
.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #e5e5e5;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.footer-link {
  color: #666;
  text-decoration: none;
  font-size: 12px;
  padding: 4px 0;
  transition: color 0.2s ease;
}

.footer-link:hover {
  color: #333;
}

/* 创建文件夹样式 */
.create-folder-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.create-folder-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 8px 12px;
  background: none;
  border: none;
  color: #2196f3;
  cursor: pointer;
  font-size: 13px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.create-folder-btn:hover {
  background-color: #e3f2fd;
}

.create-folder-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-group {
  display: flex;
  align-items: center;
  gap: 4px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 2px;
}

.create-folder-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 6px 8px;
  font-size: 13px;
  background: transparent;
}

.create-folder-input:focus {
  outline: none;
}

.input-group:focus-within {
  border-color: #2196f3;
  box-shadow: 0 0 0 2px rgba(33, 150, 243, 0.1);
}

.input-actions {
  display: flex;
  gap: 2px;
}

.confirm-btn,
.cancel-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.confirm-btn {
  background: #4caf50;
  color: white;
}

.confirm-btn:hover:not(:disabled) {
  background: #45a049;
}

.confirm-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.cancel-btn {
  background: #f44336;
  color: white;
}

.cancel-btn:hover:not(:disabled) {
  background: #da190b;
}

.cancel-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.error-message {
  font-size: 11px;
  color: #f44336;
  padding: 0 4px;
}

/* 响应式样式 */
@media (max-width: 767px) {
  .sidebar-header {
    padding: 0 16px;
    height: 64px;
  }
}

/* 保留这个样式用于文本大小 */
.expandable-menu-item .menu-text {
  font-size: 14px;
  font-weight: 500;
}
</style>
