import { apiClient } from './interceptors'
import type {
  ApiResponse,
  HomeViewResponse,
  FolderData,
  HomeViewFolder,
  HomeViewChannel,
  HomeViewNewsItem,
  NewsDetailData,
  NewsDetailResponse,
  ChannelItemsResponse,
  ChannelItemDatum,
  FolderItemsResponse,
  FolderItemDatum,
  HotWordItemDatum,
  HotWordItemsResponse,
  PaginatedNewsResponse,
} from './types'

/**
 * 获取HomeView页面数据（文件夹和频道信息）
 * @returns 文件夹和频道数据
 */
export const getHomeViewData = async (): Promise<ApiResponse<HomeViewFolder[]>> => {
  try {
    // 检查是否有认证token
    const token = localStorage.getItem('auth_token')
    console.log('检查认证token:', {
      exists: !!token,
      tokenLength: token ? token.length : 0,
      tokenStart: token ? token.substring(0, 20) + '...' : 'null',
    })

    if (!token) {
      // 开发环境下可以返回mock数据用于测试
      if (import.meta.env.DEV) {
        console.warn('开发模式：未找到认证token，返回mock数据')
        return getMockHomeViewData()
      }

      return {
        code: 401,
        message: '请先登录以查看内容',
        data: null,
      }
    }

    console.log('使用token调用API...')
    const response = await apiClient.get<HomeViewResponse>('/folder/new')

    if (response.data.code === 200 && response.data.data) {
      // 转换后端数据格式为前端组件所需的格式
      const transformedData = transformHomeViewData(response.data.data)
      console.log('API数据获取成功:', transformedData)

      return {
        code: 200,
        message: response.data.message,
        data: transformedData,
      }
    } else {
      return {
        code: response.data.code,
        message: response.data.message || '获取数据失败',
        data: null,
      }
    }
  } catch (error: unknown) {
    console.error('API调用失败:', error)

    // 处理认证错误
    if (error && typeof error === 'object' && 'response' in error) {
      const axiosError = error as { response?: { status?: number } }
      if (axiosError.response?.status === 401) {
        return {
          code: 401,
          message: '认证失败，请重新登录',
          data: null,
        }
      } else if (axiosError.response?.status === 500) {
        return {
          code: 500,
          message: '服务器内部错误，请稍后再试',
          data: null,
        }
      }
    }

    // 处理和转换其他错误
    if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
      return error as ApiResponse<HomeViewFolder[]>
    }

    // 处理未知错误
    const errorMessage = error instanceof Error ? error.message : '获取首页数据失败，请稍后再试'
    return {
      code: 500,
      message: errorMessage,
      data: null,
    }
  }
}

/**
 * 获取mock数据（仅用于开发环境测试）
 */
const getMockHomeViewData = (): ApiResponse<HomeViewFolder[]> => {
  const mockData: HomeViewFolder[] = [
    {
      id: '1',
      name: '测试文件夹',
      icon: '💻',
      color: '#2196f3',
      unreadCount: 5,
      channels: [
        {
          id: '1',
          name: '测试频道',
          icon: getDefaultChannelIcon(),
          description: '这是一个测试频道',
          unreadCount: 5,
          lastUpdate: '1小时前',
          link: 'https://www.whu.edu.cn',
          recentNews: [
            {
              id: 1,
              title: '测试新闻标题 1',
              source: '测试源',
              time: '1小时前',
              image: '',
              sourceIcon: '',
              category: 'test',
            },
            {
              id: 2,
              title: '测试新闻标题 2',
              source: '测试源',
              time: '2小时前',
              image: '',
              sourceIcon: '',
              category: 'test',
            },
          ],
        },
      ],
    },
  ]

  return {
    code: 200,
    message: 'Mock数据加载成功',
    data: mockData,
  }
}

/**
 * 转换后端数据格式为前端组件格式
 * @param backendData 后端返回的数据
 * @returns 转换后的前端数据格式
 */
const transformHomeViewData = (backendData: FolderData[]): HomeViewFolder[] => {
  // 预定义的文件夹颜色和图标
  const folderStyles = [
    { icon: '💻', color: '#2196f3' },
    { icon: '📈', color: '#4caf50' },
    { icon: '🏃‍♀️', color: '#ff9800' },
    { icon: '📰', color: '#9c27b0' },
    { icon: '🎮', color: '#e91e63' },
    { icon: '🎨', color: '#00bcd4' },
    { icon: '🔬', color: '#795548' },
    { icon: '⚽', color: '#607d8b' },
  ]

  return backendData.map((folder, folderIndex) => {
    // 为每个文件夹分配样式
    const folderStyle = folderStyles[folderIndex % folderStyles.length]

    // 转换频道数据
    const channels: HomeViewChannel[] = folder.channels.map((channel) => {
      // 计算最近更新时间
      const lastUpdate =
        channel.top5news.length > 0 ? formatRelativeTime(channel.top5news[0].pubDate) : '暂无更新'

      // 转换新闻数据
      const recentNews: HomeViewNewsItem[] = channel.top5news.map((news) => ({
        id: parseInt(news.id),
        title: news.title,
        source: channel.name,
        time: formatRelativeTime(news.pubDate),
        image: '', // 后端暂无图片数据
        sourceIcon: channel.icon || getDefaultChannelIcon(),
        category: 'news',
      }))

      const channelResult = {
        id: channel.id.toString(),
        name: channel.name,
        icon: channel.icon || getDefaultChannelIcon(),
        description: channel.description,
        link: channel.link,
        unreadCount: channel.top5news.length, // 使用新闻数量作为未读数
        lastUpdate,
        recentNews,
      }

      console.log('转换频道数据:', {
        原始频道ID: channel.id,
        原始频道名称: channel.name,
        原始频道链接: channel.link,
        转换后频道链接: channelResult.link,
        链接类型: typeof channel.link,
      })

      return channelResult
    })

    // 计算文件夹未读数
    const unreadCount = channels.reduce((sum, channel) => sum + channel.unreadCount, 0)

    return {
      id: folder.id.toString(),
      name: folder.name,
      icon: folderStyle.icon,
      color: folderStyle.color,
      channels,
      unreadCount,
    }
  })
}

/**
 * 格式化相对时间
 * @param dateString 时间字符串
 * @returns 格式化后的相对时间
 */
const formatRelativeTime = (dateString: string): string => {
  try {
    const date = new Date(dateString)
    const now = new Date()
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60))

    if (diffInMinutes < 1) {
      return '刚刚'
    } else if (diffInMinutes < 60) {
      return `${diffInMinutes}分钟前`
    } else if (diffInMinutes < 1440) {
      // 24 hours
      const hours = Math.floor(diffInMinutes / 60)
      return `${hours}小时前`
    } else {
      const days = Math.floor(diffInMinutes / 1440)
      return `${days}天前`
    }
  } catch (error) {
    console.error('日期格式化错误:', error)
    return '时间未知'
  }
}

/**
 * 获取默认频道图标
 * @returns 默认图标URL
 */
const getDefaultChannelIcon = (): string => {
  // 返回默认图标路径，您可以根据实际情况调整
  return new URL('../assets/placeholders/newsSouceIconDefault.png', import.meta.url).href
}

/**
 * 刷新HomeView数据
 * @returns 刷新后的数据
 */
export const refreshHomeViewData = async (): Promise<ApiResponse<HomeViewFolder[]>> => {
  // 可以在这里添加特殊的刷新逻辑，比如清除缓存等
  return getHomeViewData()
}

/**
 * 获取新闻详情
 * @param itemId 新闻ID
 * @returns 新闻详情数据
 */
export const getNewsDetail = async (
  itemId: string | number,
): Promise<ApiResponse<NewsDetailData>> => {
  try {
    // 检查是否有认证token
    const token = localStorage.getItem('auth_token')
    console.log('获取新闻详情，token:', token ? '存在' : '不存在')

    if (!token) {
      return {
        code: 401,
        message: '请先登录以查看新闻详情',
        data: null,
      }
    }

    // 确保 itemId 是有效的
    const id = typeof itemId === 'string' ? parseInt(itemId, 10) : itemId
    if (isNaN(id) || id <= 0) {
      return {
        code: 400,
        message: '无效的新闻ID',
        data: null,
      }
    }

    console.log('调用新闻详情API，ID:', id)
    const response = await apiClient.get<NewsDetailResponse>(`/item/${id}`)

    if (response.data.code === 200 && response.data.data) {
      console.log('新闻详情获取成功:', response.data.data)

      return {
        code: 200,
        message: response.data.message,
        data: response.data.data,
      }
    } else {
      return {
        code: response.data.code,
        message: response.data.message || '获取新闻详情失败',
        data: null,
      }
    }
  } catch (error: unknown) {
    console.error('获取新闻详情失败:', error)

    // 处理认证错误
    if (error && typeof error === 'object' && 'response' in error) {
      const axiosError = error as { response?: { status?: number } }
      if (axiosError.response?.status === 401) {
        return {
          code: 401,
          message: '认证失败，请重新登录',
          data: null,
        }
      } else if (axiosError.response?.status === 404) {
        return {
          code: 404,
          message: '新闻不存在或已被删除',
          data: null,
        }
      }
    }

    const errorMessage = error instanceof Error ? error.message : '获取新闻详情时发生未知错误'
    return {
      code: 500,
      message: errorMessage,
      data: null,
    }
  }
}

/**
 * 获取频道新闻列表
 * @param params 请求参数
 * @returns 频道新闻列表数据
 */
export const getChannelItems = async (params: {
  channelId?: string
  pageNum?: number
  pageSize?: number
}): Promise<ApiResponse<PaginatedNewsResponse<ChannelItemDatum>>> => {
  try {
    // 检查是否有认证token
    const token = localStorage.getItem('auth_token')
    console.log('获取频道新闻列表，token:', token ? '存在' : '不存在')

    if (!token) {
      return {
        code: 401,
        message: '请先登录以查看频道新闻',
        data: null,
      }
    }

    // 设置默认参数
    const requestParams = {
      channelId: params.channelId,
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10,
    }

    console.log('调用频道新闻列表API，参数:', requestParams)
    const response = await apiClient.get<ChannelItemsResponse>('/channel/items', {
      params: requestParams,
    })

    if (response.data.code === 200 && response.data.data) {
      console.log('频道新闻列表获取成功:', {
        总数: response.data.data.items.length,
        页码: requestParams.pageNum,
        页大小: requestParams.pageSize,
        是否有下一页: response.data.data.hasNext,
      })

      return {
        code: 200,
        message: response.data.message,
        data: response.data.data,
      }
    } else {
      return {
        code: response.data.code,
        message: response.data.message || '获取频道新闻列表失败',
        data: null,
      }
    }
  } catch (error: unknown) {
    console.error('获取频道新闻列表失败:', error)

    // 处理认证错误
    if (error && typeof error === 'object' && 'response' in error) {
      const axiosError = error as { response?: { status?: number } }
      if (axiosError.response?.status === 401) {
        return {
          code: 401,
          message: '认证失败，请重新登录',
          data: null,
        }
      } else if (axiosError.response?.status === 404) {
        return {
          code: 404,
          message: '频道不存在或已被删除',
          data: null,
        }
      }
    }

    const errorMessage = error instanceof Error ? error.message : '获取频道新闻列表时发生未知错误'
    return {
      code: 500,
      message: errorMessage,
      data: null,
    }
  }
}

/**
 * 获取文件夹新闻列表
 * @param params 请求参数
 * @returns 文件夹新闻列表数据
 */
export const getFolderItems = async (params: {
  folderId?: string
  pageNum?: number
  pageSize?: number
}): Promise<ApiResponse<PaginatedNewsResponse<FolderItemDatum>>> => {
  try {
    // 检查是否有认证token
    const token = localStorage.getItem('auth_token')
    console.log('获取文件夹新闻列表，token:', token ? '存在' : '不存在')

    if (!token) {
      return {
        code: 401,
        message: '请先登录以查看文件夹新闻',
        data: null,
      }
    }

    // 设置默认参数
    const requestParams = {
      folderId: params.folderId,
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10,
    }

    console.log('调用文件夹新闻列表API，参数:', requestParams)
    const response = await apiClient.get<FolderItemsResponse>('/folder/items', {
      params: requestParams,
    })

    if (response.data.code === 200 && response.data.data) {
      console.log('文件夹新闻列表获取成功:', {
        总数: response.data.data.items.length,
        页码: requestParams.pageNum,
        页大小: requestParams.pageSize,
        是否有下一页: response.data.data.hasNext,
      })

      return {
        code: 200,
        message: response.data.message,
        data: response.data.data,
      }
    } else {
      return {
        code: response.data.code,
        message: response.data.message || '获取文件夹新闻列表失败',
        data: null,
      }
    }
  } catch (error: unknown) {
    console.error('获取文件夹新闻列表失败:', error)

    // 处理认证错误
    if (error && typeof error === 'object' && 'response' in error) {
      const axiosError = error as { response?: { status?: number } }
      if (axiosError.response?.status === 401) {
        return {
          code: 401,
          message: '认证失败，请重新登录',
          data: null,
        }
      } else if (axiosError.response?.status === 404) {
        return {
          code: 404,
          message: '文件夹不存在或已被删除',
          data: null,
        }
      }
    }

    const errorMessage = error instanceof Error ? error.message : '获取文件夹新闻列表时发生未知错误'
    return {
      code: 500,
      message: errorMessage,
      data: null,
    }
  }
}

/**
 * 获取热词新闻列表
 * @param params 请求参数
 * @returns 热词新闻列表数据
 */
export const getHotWordItems = async (params: {
  hotWordId: number
  pageNum?: number
  pageSize?: number
}): Promise<ApiResponse<HotWordItemDatum[]>> => {
  try {
    // 检查是否有认证token
    const token = localStorage.getItem('auth_token')
    console.log('获取热词新闻列表，token:', token ? '存在' : '不存在')

    if (!token) {
      return {
        code: 401,
        message: '请先登录以查看热词新闻',
        data: null,
      }
    }

    // 设置默认参数
    const requestParams = {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10,
    }

    console.log('调用热词新闻列表API，参数:', {
      hotWordId: params.hotWordId,
      ...requestParams,
    })

    const response = await apiClient.get<HotWordItemsResponse>(
      `/hotword/items/${params.hotWordId}`,
      {
        params: requestParams,
      },
    )

    if (response.data.code === 200 && response.data.data) {
      console.log('热词新闻列表获取成功:', {
        热词ID: params.hotWordId,
        总数: response.data.data.length,
        页码: requestParams.pageNum,
        页大小: requestParams.pageSize,
      })

      return {
        code: 200,
        message: response.data.message,
        data: response.data.data,
      }
    } else {
      return {
        code: response.data.code,
        message: response.data.message || '获取热词新闻列表失败',
        data: null,
      }
    }
  } catch (error: unknown) {
    console.error('获取热词新闻列表失败:', error)

    // 处理认证错误
    if (error && typeof error === 'object' && 'response' in error) {
      const axiosError = error as { response?: { status?: number } }
      if (axiosError.response?.status === 401) {
        return {
          code: 401,
          message: '认证失败，请重新登录',
          data: null,
        }
      } else if (axiosError.response?.status === 404) {
        return {
          code: 404,
          message: '热词不存在或已过期',
          data: null,
        }
      }
    }

    const errorMessage = error instanceof Error ? error.message : '获取热词新闻列表时发生未知错误'
    return {
      code: 500,
      message: errorMessage,
      data: null,
    }
  }
}

/**
 * 流式获取新闻AI摘要
 * @param itemId 新闻项ID
 * @param onToken 接收到token时的回调
 * @param onComplete 完成时的回调
 * @param onError 错误时的回调
 * @returns 控制对象，包含停止方法
 */
export const streamAiSummary = (
  itemId: number,
  onToken: (token: string) => void,
  onComplete: (summary: string) => void,
  onError: (error: string) => void,
) => {
  let abortController: AbortController | null = null
  let accumulatedSummary = ''

  const start = async () => {
    try {
      // 获取认证token
      const token = localStorage.getItem('auth_token')
      if (!token) {
        onError('请先登录')
        return
      }

      // 创建AbortController用于取消请求
      abortController = new AbortController()

      // 构建URL（不再需要查询参数传token）
      const url = `${import.meta.env.VITE_API_BASE_URL || ''}/ai/summary/${itemId}`

      console.log('开始流式获取AI摘要:', { itemId, url })

      // 使用fetch代替EventSource，这样可以设置Authorization头
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: 'text/event-stream',
          'Cache-Control': 'no-cache',
        },
        signal: abortController.signal,
      })

      if (!response.ok) {
        const errorText = await response.text()
        console.error('AI摘要请求失败:', response.status, errorText)
        onError(`请求失败: ${response.status} ${errorText}`)
        return
      }

      if (!response.body) {
        onError('响应体为空')
        return
      }

      // 创建流读取器
      const reader = response.body.getReader()
      const decoder = new TextDecoder()

      // 用于处理跨chunk的数据
      let buffer = ''
      let currentEvent = ''

      try {
        while (true) {
          const { done, value } = await reader.read()

          if (done) {
            console.log('流读取完成')
            break
          }

          // 解码数据并添加到缓冲区
          const chunk = decoder.decode(value, { stream: true })
          buffer += chunk

          // 处理缓冲区中的完整行
          const lines = buffer.split('\n')

          // 保留最后一行（可能不完整）
          buffer = lines.pop() || ''

          for (const line of lines) {
            const trimmedLine = line.trim()

            if (trimmedLine === '') {
              // 空行表示事件结束，重置当前事件类型
              currentEvent = ''
              continue
            }

            // 解析SSE格式: "event: eventType\ndata: eventData\n\n"
            if (trimmedLine.startsWith('event:')) {
              currentEvent = trimmedLine.substring(6).trim()
              console.log('收到事件类型:', currentEvent)
              continue
            }

            if (trimmedLine.startsWith('data:')) {
              const data = trimmedLine.substring(5).trim()

              // 处理不同类型的事件
              if (currentEvent === 'token' || currentEvent === '') {
                // token事件或默认数据
                console.log('收到token:', JSON.stringify(data), '长度:', data.length)
                if (data !== '') {
                  // 明确检查不为空字符串
                  accumulatedSummary += data
                  onToken(data)
                } else {
                  console.warn('收到空token，跳过')
                }
              } else if (currentEvent === 'complete') {
                console.log('AI摘要生成完成:', data)
                onComplete(accumulatedSummary)
                stop()
                return
              } else if (currentEvent === 'error') {
                console.error('AI摘要生成错误:', data)
                onError(data || '生成AI摘要时发生错误')
                stop()
                return
              }
            }
          }
        }

        // 处理剩余缓冲区中的数据
        if (buffer.trim()) {
          const trimmedBuffer = buffer.trim()
          if (trimmedBuffer.startsWith('data:')) {
            const data = trimmedBuffer.substring(5).trim()
            if (data !== '' && (currentEvent === 'token' || currentEvent === '')) {
              console.log('收到最后token:', JSON.stringify(data), '长度:', data.length)
              accumulatedSummary += data
              onToken(data)
            }
          }
        }
      } finally {
        reader.releaseLock()
      }
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        console.log('AI摘要请求被取消')
        return
      }

      console.error('启动AI摘要流失败:', error)
      onError(error instanceof Error ? error.message : '启动AI摘要失败')
    }
  }

  const stop = () => {
    if (abortController) {
      console.log('取消AI摘要流请求')
      abortController.abort()
      abortController = null
    }
  }

  // 立即开始
  start()

  return {
    stop,
    isActive: () => abortController !== null,
  }
}
