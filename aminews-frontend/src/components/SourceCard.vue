<script setup lang="ts">
import { ref, computed } from 'vue';

interface NewsItem {
  id: number
  title: string
  source: string
  time: string
  image: string
  category: string
  sourceIcon?: string
}

interface Source {
  id: string
  name: string
  icon: string
  description: string
  unreadCount: number
  lastUpdate: string
  recentNews: NewsItem[]
  link?: string
  url?: string
  isSubscribed?: boolean
}

interface Props {
  source: Source
  showNews?: boolean
  layout?: 'grid' | 'list'
  showSubscribe?: boolean
  isSubscribing?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showNews: true,
  layout: 'grid',
  showSubscribe: false,
  isSubscribing: false
});

// 定义事件
const emit = defineEmits<{
  subscribe: [source: Source]
}>()

// 计算订阅按钮状态
const subscribeButtonText = computed(() => {
  if (props.isSubscribing) {
    return props.source.isSubscribed ? '取消订阅中...' : '订阅中...'
  }
  return props.source.isSubscribed ? '取消订阅' : '订阅'
})

const subscribeButtonClass = computed(() => ({
  'subscribe-btn': props.layout === 'grid',
  'subscribe-btn-small': props.layout === 'list',
  'subscribed': props.source.isSubscribed,
  'subscribing': props.isSubscribing
}))

// 处理订阅点击
const handleSubscribeClick = () => {
  if (props.isSubscribing) return
  emit('subscribe', props.source)
}

// 打开订阅源链接
const openSource = () => {
  if (props.source.link) {
    window.open(props.source.link, '_blank');
  }
};
</script>

<template>
  <article>
    <div class="source-card" :class="{ 'list-layout': layout === 'list' }">
      <!-- 订阅源头部 -->
      <div class="source-header">
        <div class="source-info">
          <img :src="source.icon" :alt="source.name" class="source-icon" />
          <div class="source-details">
            <h4 class="source-name">{{ source.name }}</h4>
            <p class="source-description">{{ source.description }}</p>
            <span v-if="layout === 'list'" class="source-last-update-inline">{{ source.lastUpdate }}</span>
          </div>
        </div>
        <div class="source-meta">
          <span v-if="source.unreadCount > 0" class="source-unread-badge">
            {{ source.unreadCount }}
          </span>
          <span v-if="layout === 'grid'" class="source-last-update">{{ source.lastUpdate }}</span>
          <!-- 列表布局时的订阅按钮 -->
          <button 
            v-if="layout === 'list' && showSubscribe"
            :class="subscribeButtonClass"
            @click="handleSubscribeClick"
            :disabled="isSubscribing"
          >
            {{ subscribeButtonText }}
          </button>
        </div>
      </div>

      <!-- 最新新闻列表（只有在有新闻时才显示且 showNews 为 true） -->
      <div v-if="showNews && source.recentNews && source.recentNews.length > 0 && layout === 'grid'" class="source-news">
        <div
          v-for="(news, index) in source.recentNews.slice(0, 3)"
          :key="news.id"
          class="news-item"
          :class="{ 'news-divider': index > 0 }"
        >
          <h5 class="news-title">{{ news.title }}</h5>
        </div>
      </div>

      <!-- 网格布局的底部按钮 -->
      <div v-if="layout === 'grid' && showSubscribe" class="source-footer">
        <button 
          :class="subscribeButtonClass"
          @click="handleSubscribeClick"
          :disabled="isSubscribing"
        >
          {{ subscribeButtonText }}
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.source-card {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s ease;
  break-inside: avoid;
  margin-bottom: 20px;
  box-sizing: border-box;
}

.source-card:hover {
  border-color: #e0e0e0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* 列表布局样式 */
.source-card.list-layout {
  background: white;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}

.source-card.list-layout:hover {
  border-color: #d0d0d0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.source-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.list-layout .source-header {
  margin-bottom: 0;
  align-items: center;
}

.source-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.list-layout .source-info {
  align-items: flex-start;
}

.source-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.list-layout .source-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
}

.source-details {
  flex: 1;
  min-width: 0;
}

.source-name {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px 0;
}

.list-layout .source-name {
  font-size: 0.95rem;
  margin: 0 0 2px 0;
}

.source-description {
  font-size: 0.85rem;
  color: #666;
  margin: 0;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.list-layout .source-description {
  font-size: 0.8rem;
  margin: 0 0 4px 0;
  color: #777;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.source-last-update-inline {
  font-size: 0.7rem;
  color: #999;
}

.source-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.list-layout .source-meta {
  flex-direction: row;
  align-items: center;
  gap: 12px;
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

.source-news {
  margin-bottom: 16px;
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

.source-footer {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.subscribe-btn {
  width: 100%;
  padding: 8px 16px;
  background: none;
  border: 1px solid #2196f3;
  border-radius: 6px;
  color: #2196f3;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.subscribe-btn:hover:not(:disabled) {
  background-color: rgba(33, 150, 243, 0.05);
}

.subscribe-btn:active:not(:disabled) {
  transform: scale(0.98);
}

/* 小订阅按钮（用于列表布局） */
.subscribe-btn-small {
  padding: 6px 12px;
  background: none;
  border: 1px solid #2196f3;
  border-radius: 4px;
  color: #2196f3;
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.subscribe-btn-small:hover:not(:disabled) {
  background-color: rgba(33, 150, 243, 0.05);
}

.subscribe-btn-small:active:not(:disabled) {
  transform: scale(0.95);
}

/* 已订阅状态 - 改为红色，表示可以取消订阅 */
.subscribe-btn.subscribed,
.subscribe-btn-small.subscribed {
  background-color: #f44336;
  border-color: #f44336;
  color: white;
  cursor: pointer;
}

.subscribe-btn.subscribed:hover,
.subscribe-btn-small.subscribed:hover {
  background-color: #d32f2f;
  border-color: #d32f2f;
}

/* 订阅中状态 */
.subscribe-btn.subscribing,
.subscribe-btn-small.subscribing {
  background-color: #f5f5f5;
  border-color: #e0e0e0;
  color: #999;
  cursor: not-allowed;
}

/* 禁用状态 */
.subscribe-btn:disabled,
.subscribe-btn-small:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

/* 移动端响应式调整 */
@media (max-width: 767px) {
  .source-card {
    padding: 16px;
    margin-bottom: 16px;
  }
  
  .source-card.list-layout {
    padding: 12px;
    margin-bottom: 8px;
  }
  
  .subscribe-btn {
    padding: 10px 16px;
    font-size: 1rem;
  }

  .subscribe-btn-small {
    padding: 4px 8px;
    font-size: 0.7rem;
  }

  .list-layout .source-meta {
    gap: 8px;
  }
}
</style>