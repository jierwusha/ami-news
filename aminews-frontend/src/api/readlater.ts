import { apiClient } from './interceptors'
import type { ApiResponse } from './types'

// 稍后再读项目类型
export interface ReadLaterItem {
  id: number
  title: string
  description?: string
  link: string
  pubDate: string
  imageUrl?: string
  channelTitle: string
  channelIcon?: string
  //addedAt: string // 添加到稍后再读的时间
}

// 获取稍后再读列表的请求参数
export interface ReadLaterListRequest {
  pageNum?: number
  pageSize?: number
}

// 添加到稍后再读
export const addToReadLater = async (itemId: string): Promise<ApiResponse<null>> => {
  try {
    const response = await apiClient.post<ApiResponse<null>>('/readlater/add', null, {
      params: { itemId }
    })
    return response.data
  } catch (error: any) {
    // 处理和转换错误
    if (error.code && error.message) {
      return error as ApiResponse<null>
    }
    // 处理未知错误
    return {
      code: 500,
      message: error.message || '添加到稍后再读失败，请稍后再试',
      data: null
    }
  }
}

// 从稍后再读中移除
export const removeFromReadLater = async (itemId: string): Promise<ApiResponse<null>> => {
  try {
    const response = await apiClient.post<ApiResponse<null>>('/readlater/remove', null,{
      params: { itemId }
    })
    return response.data
  } catch (error: any) {
    // 处理和转换错误
    if (error.code && error.message) {
      return error as ApiResponse<null>
    }
    // 处理未知错误
    return {
      code: 500,
      message: error.message || '从稍后再读中移除失败，请稍后再试',
      data: null
    }
  }
}

// 获取稍后再读列表
export const getReadLaterList = async (params: ReadLaterListRequest): Promise<ApiResponse<ReadLaterItem[]>> => {
  try {
    const response = await apiClient.get<ApiResponse<ReadLaterItem[]>>('/readlater/list', {
      params: {
        pageNum: params.pageNum || 1,
        pageSize: params.pageSize || 20
      }
    })
    return response.data
  } catch (error: any) {
    // 处理和转换错误
    if (error.code && error.message) {
      return error as ApiResponse<ReadLaterItem[]>
    }
    // 处理未知错误
    return {
      code: 500,
      message: error.message || '获取稍后再读列表失败，请稍后再试',
      data: null
    }
  }
}
