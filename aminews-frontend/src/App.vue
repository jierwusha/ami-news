<script setup lang="ts">
import { ref, computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import Sidebar from './components/Sidebar.vue'

const sidebarCollapsed = ref(false)
const route = useRoute()

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

// 检查是否为新闻详情页或聊天详情页
const isNewsDetailPage = computed(() => {
  return route.name === 'news-detail'
})

const isChatDetailPage = computed(() => {
  return route.name === 'chat-detail'
})

const isTagDetailPage = computed(() => {
  return route.name === 'tag-detail'
})

// 检查是否需要隐藏侧边栏按钮
const shouldHideSidebarBtn = computed(() => {
  return isNewsDetailPage.value || isChatDetailPage.value || isTagDetailPage.value
})

// Close sidebar on route change on mobile
</script>

<template>
  <div class="app-layout" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <Sidebar :collapsed="sidebarCollapsed" @toggle="toggleSidebar" />
    <div class="main-content-wrapper">
      <RouterView />
    </div>
    <div v-if="!sidebarCollapsed" class="overlay" @click="toggleSidebar"></div>
    <button
      class="sidebar-toggle-btn"
      :class="{ 'hidden-on-detail': shouldHideSidebarBtn }"
      @click="toggleSidebar"
    >
      <svg
        width="20"
        height="20"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
      >
        <line x1="3" y1="6" x2="21" y2="6" />
        <line x1="3" y1="12" x2="21" y2="12" />
        <line x1="3" y1="18" x2="21" y2="18" />
      </svg>
    </button>
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell',
    'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  background-color: #f0f2f5;
}

.app-layout {
  display: flex;
  /* transition: margin-left 0.3s ease; */
}

.main-content-wrapper {
  flex: 1;
  overflow-y: auto;
  transition: margin-left 0.3s ease;
  /* 响应式 padding */
  padding: 0;
  height: 100vh; /* 确保高度为视口高度 */
}

/* Styles for larger screens */
@media (min-width: 768px) {
  .app-layout:not(.sidebar-collapsed) .main-content-wrapper {
    margin-left: 280px;
  }

  .app-layout.sidebar-collapsed .main-content-wrapper {
    margin-left: 70px;
  }
}

.sidebar-toggle-btn {
  display: none;
  position: fixed;
  top: 14px;
  left: 15px;
  z-index: var(--z-fixed-header); /* 使用规范的z-index值 */
  background: white;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #666;
  /* box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); */
}

.sidebar-toggle-btn:hover {
  background-color: #f8f9fa;
  border-color: #dee2e6;
  color: #333;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.sidebar-toggle-btn:active {
  transform: translateY(1px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.sidebar-toggle-btn svg {
  display: block;
  transition: color 0.2s ease;
}

/* Styles for smaller screens */
@media (max-width: 767px) {
  .main-content-wrapper {
    padding: 0;
    margin-left: 0 !important;
  }

  .app-layout.sidebar-collapsed .sidebar {
    transform: translateX(-100%);
  }

  .sidebar-toggle-btn {
    display: block;
  }

  /* 在移动端详情页（新闻详情、聊天详情）隐藏汉堡按钮 */
  .sidebar-toggle-btn.hidden-on-detail {
    opacity: 0;
    pointer-events: none;
  }

  .app-layout:not(.sidebar-collapsed) .sidebar-toggle-btn {
    display: none;
  }

  .overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    z-index: var(--z-sidebar-overlay); /* 使用专门的sidebar遮罩层级 */
    backdrop-filter: blur(2px);
  }
}
</style>
