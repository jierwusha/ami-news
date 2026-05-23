// 通用Dropdown菜单组件，用于处理简单点击逻辑，不建议用于复杂交互或需要状态管理的场景。

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

export interface DropdownItem {
  id: string
  label: string
  icon?: string
  action: () => void
  disabled?: boolean
}

interface Props {
  items: DropdownItem[]
  buttonIcon?: 'more' | 'menu' | 'user' | 'settings'
  buttonTitle?: string
  position?: 'left' | 'right' | 'center'
  size?: 'small' | 'medium' | 'large'
  zIndex?: number
}

const props = withDefaults(defineProps<Props>(), {
  buttonIcon: 'more',
  buttonTitle: 'More options',
  position: 'right',
  size: 'medium',
  zIndex: 600,
})

const showDropdown = ref(false)
const isToggling = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)

// 切换下拉菜单（带防抖）
const toggleDropdown = () => {
  if (isToggling.value) return

  isToggling.value = true
  showDropdown.value = !showDropdown.value

  // 防抖延迟
  setTimeout(() => {
    isToggling.value = false
  }, 200)
}

// 关闭下拉菜单
const closeDropdown = () => {
  showDropdown.value = false
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

// 执行菜单项操作
const handleItemClick = (item: DropdownItem) => {
  if (!item.disabled) {
    item.action()
    closeDropdown()
  }
}

// 生命周期钩子
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleEscKey)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleEscKey)
})

// 获取按钮图标
const getButtonIcon = () => {
  const icons = {
    more: `<circle cx="12" cy="12" r="1" />
           <circle cx="19" cy="12" r="1" />
           <circle cx="5" cy="12" r="1" />`,
    menu: `<line x1="3" y1="6" x2="21" y2="6"/>
           <line x1="3" y1="12" x2="21" y2="12"/>
           <line x1="3" y1="18" x2="21" y2="18"/>`,
    user: `<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
           <circle cx="12" cy="7" r="4"/>`,
    settings: `<circle cx="12" cy="12" r="3"/>
               <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1 1.51V6a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>`,
  }
  return icons[props.buttonIcon as keyof typeof icons] || icons.more
}

// 获取尺寸相关的样式类
const getSizeClasses = () => {
  const sizeClasses = {
    small: {
      button: 'dropdown-button--small',
      dropdown: 'dropdown-menu--small',
    },
    medium: {
      button: 'dropdown-button--medium',
      dropdown: 'dropdown-menu--medium',
    },
    large: {
      button: 'dropdown-button--large',
      dropdown: 'dropdown-menu--large',
    },
  }
  return sizeClasses[props.size]
}

// 获取位置相关的类
const getPositionClass = () => {
  return `dropdown-menu--${props.position}`
}
</script>

<template>
  <div class="dropdown" ref="dropdownRef">
    <!-- 触发按钮 -->
    <button
      class="dropdown-button"
      :class="[getSizeClasses().button, { active: showDropdown }]"
      @click="toggleDropdown"
      :title="props.buttonTitle"
      :aria-expanded="showDropdown"
      aria-haspopup="true"
    >
      <svg
        :width="props.size === 'small' ? 16 : props.size === 'large' ? 24 : 20"
        :height="props.size === 'small' ? 16 : props.size === 'large' ? 24 : 20"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        v-html="getButtonIcon()"
      />
    </button>

    <!-- 透明遮罩层（仅用于点击关闭，无视觉效果） -->
    <div v-if="showDropdown" class="dropdown-overlay" @click="closeDropdown"></div>

    <!-- 下拉菜单 -->
    <Transition name="dropdown">
      <div
        v-show="showDropdown"
        class="dropdown-menu"
        :class="[getSizeClasses().dropdown, getPositionClass()]"
        :style="{ zIndex: props.zIndex }"
        role="menu"
      >
        <button
          v-for="item in props.items"
          :key="item.id"
          class="dropdown-item"
          :class="{ disabled: item.disabled }"
          @click="handleItemClick(item)"
          :title="item.label"
          role="menuitem"
          :disabled="item.disabled"
        >
          <svg
            v-if="item.icon"
            :width="props.size === 'small' ? 14 : 16"
            :height="props.size === 'small' ? 14 : 16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            v-html="item.icon"
          />
          <span>{{ item.label }}</span>
        </button>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.dropdown {
  position: relative;
  display: inline-block;
}

/* 触发按钮样式 */
.dropdown-button {
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

.dropdown-button:hover {
  background-color: #f5f5f5;
  color: #333;
}

.dropdown-button:active {
  background-color: #e0e0e0;
  transform: scale(0.95);
}

.dropdown-button.active {
  background-color: #2196f3;
  color: white;
}

.dropdown-button.active:hover {
  background-color: #1976d2;
}

/* 按钮尺寸变体 */
.dropdown-button--small {
  padding: 6px;
  border-radius: 3px;
}

.dropdown-button--medium {
  padding: 8px;
  border-radius: 4px;
}

.dropdown-button--large {
  padding: 10px;
  border-radius: 6px;
}

/* 透明遮罩层 */
.dropdown-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: var(--z-dropdown-overlay);
  /* 移除所有视觉效果，仅保留点击关闭功能 */
  background: transparent;
  backdrop-filter: none;
}

/* 下拉菜单容器 */
.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  background: white;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  padding: 8px 0;
  transform-origin: top center;
  min-width: 180px;
}

/* 下拉菜单位置变体 */
.dropdown-menu--left {
  left: 0;
  transform-origin: top left;
}

.dropdown-menu--right {
  right: 0;
  transform-origin: top right;
}

.dropdown-menu--center {
  left: 50%;
  transform: translateX(-50%);
  transform-origin: top center;
}

/* 下拉菜单尺寸变体 */
.dropdown-menu--small {
  min-width: 150px;
  border-radius: 8px;
}

.dropdown-menu--medium {
  min-width: 180px;
  border-radius: 12px;
}

.dropdown-menu--large {
  min-width: 220px;
  border-radius: 16px;
}

/* Vue Transition 动画 */
.dropdown-enter-active {
  transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.dropdown-leave-active {
  transition: all 0.15s cubic-bezier(0.4, 0, 0.6, 1);
}

.dropdown-enter-from {
  opacity: 0;
  transform: scale(0.9) translateY(-8px);
}

.dropdown-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(-4px);
}

/* 下拉菜单项 */
.dropdown-item {
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: none;
  color: #666;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
  font-size: 14px;
  font-weight: 500;
}

.dropdown-item:hover:not(.disabled) {
  background-color: #f8f9fa;
  color: #333;
}

.dropdown-item:active:not(.disabled) {
  background-color: #e9ecef;
  transform: scale(0.98);
}

.dropdown-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.dropdown-item:first-child {
  border-radius: 12px 12px 0 0;
}

.dropdown-item:last-child {
  border-radius: 0 0 12px 12px;
}

.dropdown-item svg {
  flex-shrink: 0;
}

/* 小尺寸下拉菜单项 */
.dropdown-menu--small .dropdown-item {
  padding: 10px 14px;
  font-size: 13px;
  gap: 10px;
}

.dropdown-menu--small .dropdown-item:first-child {
  border-radius: 8px 8px 0 0;
}

.dropdown-menu--small .dropdown-item:last-child {
  border-radius: 0 0 8px 8px;
}

/* 大尺寸下拉菜单项 */
.dropdown-menu--large .dropdown-item {
  padding: 14px 20px;
  font-size: 15px;
  gap: 14px;
}

.dropdown-menu--large .dropdown-item:first-child {
  border-radius: 16px 16px 0 0;
}

.dropdown-menu--large .dropdown-item:last-child {
  border-radius: 0 0 16px 16px;
}
</style>
