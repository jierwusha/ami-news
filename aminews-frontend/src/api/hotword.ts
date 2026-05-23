import { apiClient } from './interceptors'
import type { ApiResponse, HotWordItem } from './types'

/**
 * 获取热词列表
 */
export const getHotWordList = async (): Promise<ApiResponse<HotWordItem[]>> => {
  try {
    const response = await apiClient.get<ApiResponse<HotWordItem[]>>('/hotword/list')
    console.log('热词列表API响应:', response.data)
    return response.data
  } catch (error) {
    console.error('获取热词列表失败:', error)
    throw error
  }
}

/**
 * 刷新当前用户的热词数据
 * 注意：这是一个耗时操作，建议设置较长的超时时间
 */
export const refreshHotWords = async (): Promise<ApiResponse<string>> => {
  try {
    const response = await apiClient.post<ApiResponse<string>>(
      '/hotword/refresh',
      {},
      {
        timeout: 60000, // 设置60秒超时
      },
    )
    console.log('热词刷新API响应:', response.data)
    return response.data
  } catch (error) {
    console.error('刷新热词失败:', error)
    throw error
  }
}
