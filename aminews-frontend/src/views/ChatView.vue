<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getUserSessions,
  askQuestion,
  deleteSession,
  type ChatSession,
  type AskRequest,
} from '@/api/chat'

// 路由
const router = useRouter()

// 会话数据
const chatSessions = ref<ChatSession[]>([])
const loading = ref(false)
const loadingSessions = ref(false)

// 新对话输入框
const newChatInput = ref('')

// 用于取消请求的控制器
const currentAbortController = ref<AbortController | null>(null)

// 获取会话列表
const fetchSessions = async () => {
  try {
    loadingSessions.value = true
    const response = await getUserSessions()
    if (response.code === 200) {
      chatSessions.value = response.data
    } else {
      console.error('获取会话列表失败:', response.message)
    }
  } catch (error) {
    console.error('获取会话列表出错:', error)
  } finally {
    loadingSessions.value = false
  }
}

// 进入对话详情
const enterChat = (sessionId: string) => {
  router.push(`/chat/${sessionId}`)
}

// 发送新消息（创建新对话）
const sendNewMessage = async () => {
  if (!newChatInput.value.trim() || loading.value) return

  loading.value = true

  // 创建一个 AbortController 用于取消请求
  const abortController = new AbortController()
  currentAbortController.value = abortController

  try {
    const askRequest: AskRequest = {
      question: newChatInput.value.trim(),
    }

    const response = await askQuestion(askRequest)

    if (response.code === 200) {
      // 清空输入框
      newChatInput.value = ''

      // 跳转到新创建的对话
      router.push(`/chat/${response.data.sessionId}`)
    } else {
      console.error('发送消息失败:', response.message)
      // 这里可以添加错误提示
    }
  } catch (error: unknown) {
    if (error instanceof Error && error.name === 'AbortError') {
      console.log('用户取消了请求')
    } else {
      console.error('发送消息出错:', error)
    }
    // 这里可以添加错误提示
  } finally {
    loading.value = false
    currentAbortController.value = null
  }
}

// 处理回车键
const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendNewMessage()
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

// 格式化时间显示
const formatTime = (timeString: string): string => {
  const date = new Date(timeString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  // 计算时间差
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (minutes < 60) {
    if (minutes === 0) {
      return '刚刚'
    }
    return `${minutes}分钟前`
  } else if (hours < 24) {
    return `${hours}小时前`
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

// 删除对话
const deleteChat = async (sessionId: string) => {
  if (!confirm('确定要删除这个对话吗？删除后无法恢复。')) {
    return
  }

  try {
    const response = await deleteSession(sessionId)

    if (response.code === 200) {
      // 删除成功，从本地列表中移除
      chatSessions.value = chatSessions.value.filter((session) => session.sessionId !== sessionId)
      console.log('删除对话成功')
    } else {
      console.error('删除对话失败:', response.message)
      alert('删除对话失败: ' + response.message)
    }
  } catch (error) {
    console.error('删除对话出错:', error)
    alert('删除对话失败，请稍后重试')
  }
}

onMounted(() => {
  // 页面加载时获取会话列表
  fetchSessions()
})
</script>

<template>
  <div class="chat-view">
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <h1 class="page-title">AI 对话</h1>
      </div>
      <div class="header-actions">
        <!-- <button class="action-btn" title="新建对话">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M12 5v14M5 12h14" />
          </svg>
        </button> -->
      </div>
    </header>

    <!-- Main Content -->
    <main class="main-content">
      <!-- 对话列表 -->
      <div class="chat-sessions">
        <div class="section-header">
          <h2>对话列表</h2>
          <span class="session-count">{{ chatSessions.length }} 个对话</span>
        </div>

        <!-- 加载状态 -->
        <div v-if="loadingSessions" class="loading-state">
          <div class="loading-spinner"></div>
          <p>加载对话列表中...</p>
        </div>

        <div v-else class="sessions-list">
          <div
            v-for="session in chatSessions"
            :key="session.sessionId"
            class="session-card"
            @click="enterChat(session.sessionId)"
          >
            <div class="session-content">
              <div class="session-header">
                <h3 class="session-title">{{ session.title }}</h3>
                <div class="session-actions">
                  <button
                    class="action-btn small"
                    @click.stop="deleteChat(session.sessionId)"
                    title="删除对话"
                  >
                    <svg
                      width="12"
                      height="12"
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
                </div>
              </div>
              <div class="session-meta">
                <span class="message-count">{{ session.questionCount }} 条消息</span>
                <span class="timestamp">{{ formatTime(session.lastQuestionTime) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="chatSessions.length === 0" class="empty-state">
          <svg
            width="64"
            height="64"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
          >
            <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
          </svg>
          <h3>还没有对话</h3>
          <p>在下方输入框中输入消息开始你的第一次AI对话吧</p>
        </div>
      </div>

      <!-- Input Area -->
      <div class="input-area">
        <div class="input-container">
          <div class="input-wrapper">
            <textarea
              v-model="newChatInput"
              placeholder="输入消息开始新对话..."
              class="message-input"
              rows="1"
              :disabled="loading"
              @keydown="handleKeyDown"
              @input="handleInput"
            ></textarea>
            <button
              class="send-btn"
              :disabled="!newChatInput.trim() || loading"
              @click="sendNewMessage"
            >
              <svg v-if="loading" width="16" height="16" viewBox="0 0 24 24">
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
.chat-view {
  min-height: 100vh;
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

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin: 0;
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

.action-btn.small {
  padding: 4px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-sessions {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
  max-width: 800px;
  align-self: center;
  width: 100%;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-header h2 {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.session-count {
  font-size: 0.9rem;
  color: #666;
}

.sessions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.session-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  /* max-width: 800px;
  align-self: center;
  width: 100%; */
}

.session-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border-color: #e0e0e0;
}

.session-card.active {
  border-color: #2196f3;
  background: #f8fcff;
}

.session-content {
  width: 100%;
}

.session-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.session-title {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.session-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.session-card:hover .session-actions {
  opacity: 1;
}

.session-last-message {
  color: #666;
  font-size: 0.9rem;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 0.8rem;
  color: #999;
}

.empty-state {
  padding: 60px 24px;
  text-align: center;
  color: #666;
}

.empty-state svg {
  margin-bottom: 16px;
  color: #ccc;
}

.empty-state h3 {
  margin: 0 0 8px 0;
  font-size: 1.1rem;
  color: #333;
}

.empty-state p {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.5;
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

  .chat-sessions {
    padding: 16px;
  }

  .session-card {
    padding: 12px;
  }

  .session-actions {
    opacity: 1; /* 移动端总是显示操作按钮 */
  }

  .input-area {
    padding: 12px 16px;
  }

  .input-wrapper {
    gap: 8px;
    align-items: center;
  }

  .empty-state {
    padding: 40px 16px;
  }
  .page-title {
    padding-left: 50px;
    font-size: 1.25rem;
  }
}

/* 滚动条样式 */
.chat-sessions::-webkit-scrollbar {
  width: 6px;
}

.chat-sessions::-webkit-scrollbar-track {
  background: transparent;
}

.chat-sessions::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.chat-sessions::-webkit-scrollbar-thumb:hover {
  background: #ccc;
}

.message-input::-webkit-scrollbar {
  width: 4px;
}

.message-input::-webkit-scrollbar-track {
  background: transparent;
}

.message-input::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 2px;
}

.message-input::-webkit-scrollbar-thumb:hover {
  background: #ccc;
}
</style>
