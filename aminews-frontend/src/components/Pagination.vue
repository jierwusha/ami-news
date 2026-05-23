<script setup lang="ts">
// Props
interface Props {
  currentPage: number
  pageSize: number
  hasNextPage?: boolean // 是否有下一页
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  hasNextPage: true,
  loading: false,
})

// Events
interface Emits {
  (e: 'page-change', page: number): void
  (e: 'size-change', size: number): void
}

const emit = defineEmits<Emits>()

// 页面大小选项
const pageSizeOptions = [10, 20, 50, 100]

// 事件处理
const handlePageChange = (page: number) => {
  if (page < 1 || page === props.currentPage || props.loading) {
    return
  }
  emit('page-change', page)
}

const handleSizeChange = (size: number) => {
  if (size === props.pageSize || props.loading) {
    return
  }
  emit('size-change', size)
}

// 上一页
const handlePrev = () => {
  if (props.currentPage > 1) {
    handlePageChange(props.currentPage - 1)
  }
}

// 下一页
const handleNext = () => {
  if (props.hasNextPage) {
    handlePageChange(props.currentPage + 1)
  }
}
</script>

<template>
  <div class="pagination" :class="{ loading }">
    <!-- 分页控制 -->
    <div class="pagination-controls">
      <!-- 页面大小选择器 -->
      <div class="page-size-selector">
        <span class="page-size-label">每页</span>
        <select
          :value="pageSize"
          @change="handleSizeChange(parseInt(($event.target as HTMLSelectElement).value))"
          :disabled="loading"
          class="page-size-select"
        >
          <option v-for="size in pageSizeOptions" :key="size" :value="size">
            {{ size }}
          </option>
        </select>
        <span class="page-size-label">条</span>
      </div>

      <!-- 页码控制 -->
      <div class="page-controls">
        <!-- 上一页 -->
        <button
          class="page-btn prev-btn"
          :disabled="currentPage <= 1 || loading"
          @click="handlePrev"
          title="上一页"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="15,18 9,12 15,6" />
          </svg>
        </button>

        <!-- 当前页码显示 -->
        <div class="page-info">第 {{ currentPage }} 页</div>

        <!-- 下一页 -->
        <button
          class="page-btn next-btn"
          :disabled="!hasNextPage || loading"
          @click="handleNext"
          title="下一页"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="9,18 15,12 9,6" />
          </svg>
        </button>
      </div>

      <!-- 页面跳转 -->
      <!-- <div class="page-jumper">
        <span class="jumper-label">跳至</span>
        <input
          type="number"
          class="jumper-input"
          :min="1"
          :value="currentPage"
          :disabled="loading"
          @keyup.enter="handlePageChange(parseInt(($event.target as HTMLInputElement).value))"
        />
        <span class="jumper-label">页</span>
      </div> -->
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="pagination-loading">
      <div class="loading-spinner"></div>
    </div>
  </div>
</template>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  /* padding: 0 0 16px 0; */
  /* border-top: 1px solid #e5e5e5; */
  /* background: white; */
  position: relative;
  min-height: 56px;
}

.pagination.loading {
  opacity: 0.6;
  pointer-events: none;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 24px;
}

/* 页面大小选择器 */
.page-size-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-size-label {
  color: #666;
  font-size: 14px;
}

.page-size-select {
  padding: 4px 8px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  background: white;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.page-size-select:hover:not(:disabled) {
  border-color: #2196f3;
}

.page-size-select:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

/* 页码控制 */
.page-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid #d0d0d0;
  background: white;
  color: #333;
  font-size: 14px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-btn:hover:not(:disabled) {
  border-color: #2196f3;
  color: #2196f3;
}

.page-btn:disabled {
  background: #f5f5f5;
  color: #999;
  cursor: not-allowed;
  border-color: #e0e0e0;
}

.prev-btn,
.next-btn {
  width: 32px;
}

.page-info {
  padding: 0 12px;
  color: #333;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}

/* 页面跳转 */
.page-jumper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.jumper-label {
  color: #666;
  font-size: 14px;
}

.jumper-input {
  width: 60px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  font-size: 14px;
  text-align: center;
  transition: border-color 0.2s ease;
}

.jumper-input:hover:not(:disabled) {
  border-color: #2196f3;
}

.jumper-input:focus {
  outline: none;
  border-color: #2196f3;
  box-shadow: 0 0 0 2px rgba(33, 150, 243, 0.1);
}

.jumper-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

/* 加载状态 */
.pagination-loading {
  position: absolute;
  top: 50%;
  right: 16px;
  transform: translateY(-50%);
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

/* 响应式设计 */
@media (max-width: 768px) {
  .pagination-controls {
    flex-wrap: wrap;
    gap: 16px;
    justify-content: center;
  }

  .page-controls {
    order: 1;
    width: 100%;
    justify-content: center;
  }

  .page-size-selector {
    order: 2;
  }

  .page-jumper {
    order: 3;
  }
}

@media (max-width: 480px) {
  .pagination {
    padding: 12px 16px;
  }

  .pagination-controls {
    gap: 12px;
  }

  .page-btn {
    min-width: 28px;
    height: 28px;
    font-size: 13px;
  }

  .prev-btn,
  .next-btn {
    width: 28px;
  }

  .jumper-input {
    width: 50px;
    height: 28px;
  }

  .page-info {
    font-size: 13px;
  }
}
</style>
