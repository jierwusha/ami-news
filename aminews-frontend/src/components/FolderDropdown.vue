<template>
  <div class="folder-dropdown" ref="dropdownRef">
    <!-- 触发按钮 -->
    <button
      class="folder-btn"
      :class="{ active: showDropdown }"
      @click="toggleDropdown"
      :title="buttonTitle"
    >
      <svg
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
      >
        <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
      </svg>
    </button>

    <!-- 下拉菜单 -->
    <div v-if="showDropdown" class="dropdown-menu" :style="{ zIndex: zIndex }">
      <div class="dropdown-header">
        <h4>管理文件夹</h4>
        <button @click="closeDropdown" class="close-btn">
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

      <div class="dropdown-content">
        <div v-if="loading" class="loading-state">
          <div class="loading-spinner"></div>
          <span>加载中...</span>
        </div>

        <div v-else-if="folders.length === 0" class="empty-state">
          <span>暂无文件夹</span>
        </div>

        <div v-else class="folder-list">
          <label
            v-for="folder in folders"
            :key="folder.id"
            class="folder-item"
            :class="{ disabled: processingFolders.has(folder.id) }"
          >
            <input
              type="checkbox"
              :checked="selectedFolders.has(folder.id)"
              @change="handleFolderToggle(folder.id, $event)"
              :disabled="processingFolders.has(folder.id)"
            />
            <span class="folder-icon">📁</span>
            <span class="folder-name">{{ folder.name }}</span>
            <span v-if="processingFolders.has(folder.id)" class="processing-indicator">
              <div class="mini-spinner"></div>
            </span>
          </label>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useHomeStore } from '../stores/homeStore'
import { addChannelToFolder, removeChannelFromFolder } from '../api'

interface Props {
  channelId: string
  currentFolderIds?: string[]
  buttonTitle?: string
  zIndex?: number
}

const props = withDefaults(defineProps<Props>(), {
  currentFolderIds: () => [],
  buttonTitle: '管理文件夹',
  zIndex: 600,
})

interface Emits {
  (e: 'folders-updated', folderIds: string[]): void
}

const emit = defineEmits<Emits>()

const homeStore = useHomeStore()
const showDropdown = ref(false)
const loading = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)
const processingFolders = ref<Set<string>>(new Set())

// 当前选中的文件夹ID集合
const selectedFolders = ref<Set<string>>(new Set(props.currentFolderIds))

// 可用的文件夹列表（排除根文件夹）
const folders = computed(() => {
  return homeStore.folders.filter(
    (folder) =>
      folder.name !== 'ROOTFOLDER' && folder.name !== '未分类' && folder.name !== 'Uncategorized',
  )
})

// 监听props变化
watch(
  () => props.currentFolderIds,
  (newIds) => {
    selectedFolders.value = new Set(newIds)
  },
  { immediate: true },
)

// 切换下拉菜单
const toggleDropdown = async () => {
  if (!showDropdown.value) {
    // 打开时刷新数据
    await loadFolders()
  }
  showDropdown.value = !showDropdown.value
}

// 关闭下拉菜单
const closeDropdown = () => {
  showDropdown.value = false
}

// 加载文件夹数据
const loadFolders = async () => {
  if (homeStore.folders.length === 0) {
    loading.value = true
    try {
      await homeStore.fetchHomeData()
    } catch (error) {
      console.error('加载文件夹数据失败:', error)
    } finally {
      loading.value = false
    }
  }
}

// 处理文件夹选择切换
const handleFolderToggle = async (folderId: string, event: Event) => {
  const checkbox = event.target as HTMLInputElement
  const isChecked = checkbox.checked

  // 添加处理状态
  processingFolders.value.add(folderId)

  try {
    if (isChecked) {
      // 添加到文件夹
      await addChannelToFolder(folderId, props.channelId)
      selectedFolders.value.add(folderId)
      console.log(`频道 ${props.channelId} 已添加到文件夹 ${folderId}`)
    } else {
      // 从文件夹移除
      await removeChannelFromFolder(folderId, props.channelId)
      selectedFolders.value.delete(folderId)
      console.log(`频道 ${props.channelId} 已从文件夹 ${folderId} 移除`)
    }

    // 发出更新事件
    emit('folders-updated', Array.from(selectedFolders.value))

    // 刷新homeStore数据
    await homeStore.refreshData()
  } catch (error) {
    console.error('文件夹操作失败:', error)
    // 操作失败时恢复checkbox状态
    checkbox.checked = !isChecked

    // 显示错误提示（可以集成toast组件）
    alert(isChecked ? '添加到文件夹失败' : '从文件夹移除失败')
  } finally {
    // 移除处理状态
    processingFolders.value.delete(folderId)
  }
}

// 点击外部区域关闭菜单
const handleClickOutside = (event: MouseEvent) => {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target as Node)) {
    closeDropdown()
  }
}

// ESC键关闭菜单
const handleEscKey = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    closeDropdown()
  }
}

// 生命周期
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleEscKey)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleEscKey)
})
</script>

<style scoped>
.folder-dropdown {
  position: relative;
  display: inline-block;
}

.folder-btn {
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

.folder-btn:hover,
.folder-btn.active {
  background-color: #f5f5f5;
  color: #333;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 280px;
  max-width: 320px;
  max-height: 400px;
  overflow: hidden;
  animation: dropdown-enter 0.15s ease-out;
}

@keyframes dropdown-enter {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.dropdown-header h4 {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: #333;
}

.close-btn {
  padding: 4px;
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

.close-btn:hover {
  background: #e0e0e0;
  color: #333;
}

.dropdown-content {
  max-height: 300px;
  overflow-y: auto;
}

.loading-state,
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #666;
  font-size: 0.9rem;
}

.loading-state {
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

.folder-list {
  padding: 8px 0;
}

.folder-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  user-select: none;
}

.folder-item:hover:not(.disabled) {
  background-color: #f5f5f5;
}

.folder-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.folder-item input[type='checkbox'] {
  margin: 0;
  width: 16px;
  height: 16px;
  accent-color: #2196f3;
}

.folder-item input[type='checkbox']:disabled {
  cursor: not-allowed;
}

.folder-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.folder-name {
  flex: 1;
  font-size: 0.9rem;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.processing-indicator {
  flex-shrink: 0;
}

.mini-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #2196f3;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 移动端适配 */
@media (max-width: 767px) {
  .dropdown-menu {
    right: -20px;
    left: -20px;
    min-width: auto;
    max-width: none;
    margin-left: -50px;
    margin-right: -50px;
  }
}
</style>
