<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getChatHistory,
  askQuestion,
  type ChatHistoryItem,
  type AskRequest,
  type RelevantNews,
} from '@/api'

// 路由
const route = useRoute()
const router = useRouter()

// 获取聊天ID
const chatId = route.params.id as string
console.log('当前聊天ID:', chatId)

// 消息数据类型
interface Message {
  id: string
  content: string
  isUser: boolean
  timestamp: string
  isStreaming?: boolean
  relevantNews?: RelevantNews[] // 添加相关新闻字段
}

// 聊天数据
const chatTitle = ref('AI 对话')
const messages = ref<Message[]>([])
const loadingHistory = ref(false)

// 输入相关
const messageInput = ref('')
const isLoading = ref(false)
const messagesContainer = ref<HTMLElement>()

// 获取对话历史
const fetchChatHistory = async () => {
  if (!chatId) return

  try {
    loadingHistory.value = true
    const response = await getChatHistory(chatId)

    if (response.code === 200) {
      // 转换API数据为UI需要的格式
      const historyMessages: Message[] = []

      // 反转数据顺序，从旧到新显示
      const reversedData = [...response.data].reverse()

      reversedData.forEach((item: ChatHistoryItem) => {
        // 添加用户问题
        historyMessages.push({
          id: `q_${item.id}`,
          content: item.question,
          isUser: true,
          timestamp: formatTime(item.createdTime),
        })

        // 添加AI回答
        historyMessages.push({
          id: `a_${item.id}`,
          content: item.answer,
          isUser: false,
          timestamp: formatTime(item.createdTime),
          relevantNews: item.newsItems || [], // 添加历史对话中的相关新闻
        })
      })

      messages.value = historyMessages

      // 设置聊天标题为第一个问题的截取
      if (response.data.length > 0) {
        const firstQuestion = response.data[0].question
        chatTitle.value =
          firstQuestion.length > 20 ? firstQuestion.substring(0, 20) + '...' : firstQuestion
      }
    } else {
      console.error('获取对话历史失败:', response.message)
    }
  } catch (error) {
    console.error('获取对话历史出错:', error)
  } finally {
    loadingHistory.value = false
  }
}

// 返回聊天列表
const goBack = () => {
  router.push('/chat')
}

// 发送消息
const sendMessage = async () => {
  if (!messageInput.value.trim() || isLoading.value) return

  const userMessage: Message = {
    id: Date.now().toString(),
    content: messageInput.value.trim(),
    isUser: true,
    timestamp: new Date().toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    }),
  }

  // 添加用户消息
  messages.value.push(userMessage)

  // 保存用户输入
  const userInput = messageInput.value.trim()

  // 清空输入框
  messageInput.value = ''

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 发送到API
  isLoading.value = true

  try {
    const askRequest: AskRequest = {
      question: userInput,
      sessionId: chatId,
    }

    const response = await askQuestion(askRequest)

    if (response.code === 200) {
      // 添加AI回复
      const aiMessage: Message = {
        id: (Date.now() + 1).toString(),
        content: response.data.answer,
        isUser: false,
        timestamp: new Date().toLocaleTimeString('zh-CN', {
          hour: '2-digit',
          minute: '2-digit',
        }),
        relevantNews: response.data.relevantNews || [], // 添加相关新闻
      }

      messages.value.push(aiMessage)
      await nextTick()
      scrollToBottom()
    } else {
      console.error('发送消息失败:', response.message)
      // 这里可以添加错误提示
    }
  } catch (error) {
    console.error('发送消息出错:', error)
    // 这里可以添加错误提示
  } finally {
    isLoading.value = false
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 处理回车键
const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

// 自动调整输入框高度
const adjustTextareaHeight = (element: HTMLTextAreaElement) => {
  element.style.height = 'auto'
  const scrollHeight = element.scrollHeight
  const maxHeight = 120 // 最大高度
  element.style.height = Math.min(scrollHeight, maxHeight) + 'px'
}

// 处理输入框输入事件
const handleInput = (event: Event) => {
  const target = event.target as HTMLTextAreaElement
  adjustTextareaHeight(target)
}

// 格式化消息内容（支持简单的markdown）
const formatMessage = (content: string) => {
  return content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>').replace(/\n/g, '<br>')
}

// 格式化时间显示
const formatTime = (timeString: string): string => {
  const date = new Date(timeString)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 跳转到新闻详情页
const openNewsDetail = (newsId: number) => {
  router.push(`/news/${newsId}`)
}

onMounted(() => {
  // 获取对话历史
  fetchChatHistory()

  // 初始化时滚动到底部
  nextTick(() => {
    scrollToBottom()
  })
})
</script>

<template>
  <div class="chat-detail-view detail-page">
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <button class="back-btn" @click="goBack" title="返回对话列表">
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
        <h1 class="page-title">{{ chatTitle }}</h1>
      </div>
      <div class="header-actions">
        <!-- <button class="action-btn" title="清空对话">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path
              d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"
            />
          </svg>
        </button>
        <button class="action-btn" title="分享对话">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8" />
            <polyline points="16,6 12,2 8,6" />
            <line x1="12" y1="2" x2="12" y2="15" />
          </svg>
        </button> -->
      </div>
    </header>

    <!-- Messages Container -->
    <main class="main-content">
      <div class="messages-container" ref="messagesContainer">
        <!-- 加载历史消息状态 -->
        <div v-if="loadingHistory" class="loading-state">
          <div class="loading-spinner"></div>
          <p>加载对话历史中...</p>
        </div>

        <div v-else class="messages-list">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-wrapper"
            :class="{ 'user-message': message.isUser, 'ai-message': !message.isUser }"
          >
            <div class="message-content">
              <div class="message-avatar">
                <div v-if="message.isUser" class="user-avatar">
                  <svg
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
                    <circle cx="12" cy="7" r="4" />
                  </svg>
                </div>
                <div v-else class="ai-avatar">
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
                </div>
              </div>
              <div class="message-bubble">
                <div class="message-text" v-html="formatMessage(message.content)"></div>

                <!-- 相关新闻列表 -->
                <div
                  v-if="!message.isUser && message.relevantNews && message.relevantNews.length > 0"
                  class="relevant-news"
                >
                  <h4 class="news-title">相关新闻</h4>
                  <ul class="news-list">
                    <li
                      v-for="(news, index) in message.relevantNews"
                      :key="news.id"
                      class="news-item"
                    >
                      <span class="news-index">{{ index + 1 }}.</span>
                      <a href="#" class="news-link" @click.prevent="openNewsDetail(news.id)">
                        {{ news.title }}
                      </a>
                    </li>
                  </ul>
                </div>

                <div class="message-meta">
                  <span class="message-time">{{ message.timestamp }}</span>
                  <span v-if="message.isStreaming" class="typing-indicator">
                    <span class="dot"></span>
                    <span class="dot"></span>
                    <span class="dot"></span>
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 加载状态 -->
          <div v-if="isLoading" class="message-wrapper ai-message">
            <div class="message-content">
              <div class="message-avatar">
                <div class="ai-avatar">
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
                </div>
              </div>
              <div class="message-bubble">
                <div class="loading-dots">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="input-area">
        <div class="input-container">
          <div class="input-wrapper">
            <textarea
              v-model="messageInput"
              placeholder="输入消息..."
              class="message-input"
              rows="1"
              :disabled="isLoading"
              @keydown="handleKeyDown"
              @input="handleInput"
            ></textarea>
            <button
              class="send-btn"
              :disabled="!messageInput.trim() || isLoading"
              @click="sendMessage"
            >
              <svg v-if="isLoading" width="16" height="16" viewBox="0 0 24 24">
                <circle
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  stroke-width="2"
                  fill="none"
                  opacity="0.3"
                />
                <path
                  d="M12 2a10 10 0 0 1 10 10"
                  stroke="currentColor"
                  stroke-width="2"
                  fill="none"
                >
                  <animateTransform
                    attributeName="transform"
                    type="rotate"
                    values="0 12 12;360 12 12"
                    dur="1s"
                    repeatCount="indefinite"
                  />
                </path>
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
                <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.chat-detail-view {
  height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.header {
  background: white;
  padding: 0 24px;
  height: 64px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  padding: 8px;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  padding: 8px;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn:hover {
  background: #f5f5f5;
  color: #333;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;
}

.messages-list {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 24px;
}

.message-wrapper {
  margin-bottom: 24px;
}

.message-content {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.user-message {
  flex-direction: row-reverse;
}

.user-message .message-content {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-avatar {
  width: 100%;
  height: 100%;
  background: #2196f3;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-avatar {
  width: 100%;
  height: 100%;
  background: #f0f0f0;
  color: #666;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-bubble {
  max-width: 70%;
  background: white;
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.user-message .message-bubble {
  background: #2196f3;
  color: white;
}

.message-text {
  font-size: 0.95rem;
  line-height: 1.5;
  word-wrap: break-word;
}

.message-text :deep(strong) {
  font-weight: 600;
}

.message-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  opacity: 0.7;
}

.user-message .message-meta {
  justify-content: flex-end;
}

/* 相关新闻样式 */
.relevant-news {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.news-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: #666;
  margin: 0 0 8px 0;
}

.news-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.news-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-bottom: 6px;
}

.news-item:last-child {
  margin-bottom: 0;
}

.news-index {
  font-size: 0.8rem;
  color: #999;
  font-weight: 500;
  flex-shrink: 0;
  margin-top: 1px;
}

.news-link {
  font-size: 0.85rem;
  color: #999;
  text-decoration: none;
  line-height: 1.4;
  flex: 1;
  word-wrap: break-word;
  cursor: pointer;
}

.news-link:hover {
  text-decoration: underline;
  color: #777;
}

.news-link:visited {
  color: #7b1fa2;
}

.user-message .relevant-news {
  border-top-color: rgba(255, 255, 255, 0.2);
}

.user-message .news-title {
  color: rgba(255, 255, 255, 0.9);
}

.user-message .news-index {
  color: rgba(255, 255, 255, 0.7);
}

.user-message .news-link {
  color: rgba(255, 255, 255, 0.95);
}

.user-message .news-link:hover {
  color: white;
}

.typing-indicator {
  display: flex;
  gap: 2px;
}

.loading-dots {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ccc;
  animation: bounce 1.4s infinite;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-10px);
  }
}

.loading-state {
  padding: 60px 24px;
  text-align: center;
  color: #666;
}

.loading-state p {
  margin-top: 16px;
  font-size: 0.9rem;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #2196f3;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.input-area {
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 16px 24px;
  position: sticky;
  bottom: 0;
}

.input-container {
  max-width: 800px;
  margin: 0 auto;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.message-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 0.95rem;
  line-height: 1.5;
  resize: none;
  min-height: 24px;
  max-height: 120px;
  font-family: inherit;
  background: transparent;
  overflow-y: auto;
  /* padding: 6px; */
}

.message-input:disabled {
  color: #999;
}

.message-input::placeholder {
  color: #999;
}

.send-btn {
  padding: 8px;
  background: #2196f3;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 36px;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: #1976d2;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 767px) {
  .header {
    padding: 0 16px;
  }

  .page-title {
    font-size: 1.25rem;
    max-width: 200px;
  }

  .messages-list {
    padding: 0 16px;
  }

  .message-bubble {
    max-width: 85%;
  }

  .input-area {
    padding: 12px 16px;
  }

  .input-wrapper {
    gap: 8px;
    align-items: center;
  }
}

/* 滚动条样式 */
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: #ccc;
}
</style>
