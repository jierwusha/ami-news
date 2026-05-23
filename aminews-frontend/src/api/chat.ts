import { apiClient } from './interceptors'

// 会话数据类型
export interface ChatSession {
  sessionId: string
  title: string
  questionCount: number
  lastQuestionTime: string
}

// 会话列表响应类型
export interface SessionsResponse {
  code: number
  message: string
  data: ChatSession[]
}

// 对话历史项类型
export interface ChatHistoryItem {
  id: number
  userId: number
  sessionId: string
  question: string
  answer: string
  contextItems: string
  retrievalScore: number
  createdTime: string
  updatedTime: string
  newsItems?: RelevantNews[] // 添加历史对话中的新闻项
}

// 对话历史响应类型
export interface ChatHistoryResponse {
  code: number
  message: string
  data: ChatHistoryItem[]
}

// 相关新闻类型
export interface RelevantNews {
  id: number
  channelId?: number
  title: string
  description?: string
  link: string
  aiDescription?: string | null
  pubDate: string
  createTime?: string
  updateTime?: string
  relevanceScore?: number
}

// AI问答响应数据类型
export interface AskResponseData {
  sessionId: string
  question: string
  answer: string
  relevantNews: RelevantNews[]
  avgRelevanceScore: number
  totalNewsCount: number
  status: string
  timestamp: number
}

// AI问答响应类型
export interface AskResponse {
  code: number
  message: string
  data: AskResponseData
}

// AI问答请求参数类型
export interface AskRequest {
  question: string
  sessionId?: string
}

/**
 * 获取用户会话列表
 */
export const getUserSessions = async (): Promise<SessionsResponse> => {
  try {
    const response = await apiClient.get<SessionsResponse>('/qa/user/sessions')
    return response.data
  } catch (error) {
    console.error('获取会话列表失败:', error)
    throw error
  }
}

/**
 * 获取指定会话的对话历史
 */
export const getChatHistory = async (sessionId: string): Promise<ChatHistoryResponse> => {
  try {
    const response = await apiClient.get<ChatHistoryResponse>(`/qa/history/${sessionId}`)
    return response.data
  } catch (error) {
    console.error('获取对话历史失败:', error)
    throw error
  }
}

/**
 * 发起AI问答
 */
export const askQuestion = async (params: AskRequest): Promise<AskResponse> => {
  try {
    console.log('发送AI问答请求:', params)
    console.log('请求数据类型:', typeof params)
    console.log('请求数据内容:', JSON.stringify(params))

    // 尝试使用 URLSearchParams 发送表单数据
    const formData = new URLSearchParams()
    formData.append('question', params.question)
    if (params.sessionId) {
      formData.append('sessionId', params.sessionId)
    }

    console.log('发送表单数据:', formData.toString())

    const response = await apiClient.post<AskResponse>('/qa/ask', formData, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      timeout: 60000, // 设置30秒超时，给AI充足的生成时间
    })
    console.log('AI问答响应:', response.data)
    return response.data
  } catch (error) {
    console.error('发起AI问答失败:', error)
    throw error
  }
}

/**
 * 删除会话
 */
export const deleteSession = async (
  sessionId: string,
): Promise<{ code: number; message: string }> => {
  try {
    console.log('删除会话请求:', sessionId)
    const response = await apiClient.post<{ code: number; message: string }>(
      `/qa/session/delete/${sessionId}`,
    )
    console.log('删除会话响应:', response.data)
    return response.data
  } catch (error) {
    console.error('删除会话失败:', error)
    throw error
  }
}
