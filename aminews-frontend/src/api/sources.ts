import { apiClient } from './interceptors'
import type { RssSource, ApiResponse, RandomChannelData } from './types'

export const addRssSource = async (url: string): Promise<ApiResponse<RssSource>> => {
  try {
    const response = await apiClient.post<ApiResponse<RssSource>>('/rss/add', { url })
    return response.data
  } catch (error: unknown) {
    // 处理和转换错误
    if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
      return error as ApiResponse<RssSource>
    }
    // 处理未知错误
    return {
      code: 500,
      message: error instanceof Error ? error.message : 'RSS源添加失败，请稍后再试',
      data: null,
    }
  }
}

// 获取随机订阅源
export const getRandomChannels = async (
  num: number = 10,
): Promise<ApiResponse<RandomChannelData[]>> => {
  try {
    const response = await apiClient.get<ApiResponse<RandomChannelData[]>>('/channel/random', {
      params: { num },
    })
    return response.data
  } catch (error: unknown) {
    // 处理和转换错误
    if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
      return error as ApiResponse<RandomChannelData[]>
    }
    // 处理未知错误
    return {
      code: 500,
      message: error instanceof Error ? error.message : '获取推荐订阅源失败，请稍后再试',
      data: null,
    }
  }
}

// 根据关键字搜索订阅源
export const searchChannels = async (
  keyword: string,
): Promise<ApiResponse<RandomChannelData[]>> => {
  try {
    const response = await apiClient.get<ApiResponse<RandomChannelData[]>>('/channel/search', {
      params: { keyword },
    })
    return response.data
  } catch (error: unknown) {
    // 处理和转换错误
    if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
      return error as ApiResponse<RandomChannelData[]>
    }
    // 处理未知错误
    return {
      code: 500,
      message: error instanceof Error ? error.message : '搜索订阅源失败，请稍后再试',
      data: null,
    }
  }
}

// 新增：通过URL订阅RSS源
export const subscribeRssByUrl = async (url: string): Promise<ApiResponse<null>> => {
  try {
    const response = await apiClient.post<ApiResponse<null>>('/rss/subscribe/url', { url })
    return response.data
  } catch (error: unknown) {
    // 处理和转换错误
    if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
      return error as ApiResponse<null>
    }
    // 处理未知错误
    return {
      code: 500,
      message: error instanceof Error ? error.message : '订阅失败，请稍后再试',
      data: null,
    }
  }
}

// 新增：通过URL取消订阅RSS源
export const unsubscribeRssByUrl = async (url: string): Promise<ApiResponse<null>> => {
  try {
    await apiClient.post<ApiResponse<null>>('/rss/unsubscribe', { url })
    return {
      code: 200,
      message: '取消订阅成功',
      data: null,
    }
  } catch (error: unknown) {
    // 处理和转换错误
    if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
      return error as ApiResponse<null>
    }
    // 处理未知错误
    return {
      code: 500,
      message: error instanceof Error ? error.message : '取消订阅失败，请稍后再试',
      data: null,
    }
  }
}

// 新增：通过频道ID取消订阅频道
export const unsubscribeChannelById = async (channelId: string): Promise<ApiResponse<null>> => {
  try {
    const response = await apiClient.post<ApiResponse<null>>(
      `/rss/unsubscribe/channel/${channelId}`,
    )
    return response.data
  } catch (error: unknown) {
    // 处理和转换错误
    if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
      return error as ApiResponse<null>
    }
    // 处理未知错误
    return {
      code: 500,
      message: error instanceof Error ? error.message : '取消订阅失败，请稍后再试',
      data: null,
    }
  }
}
