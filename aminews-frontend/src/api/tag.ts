import { apiClient } from './interceptors'
import type { ApiResponse } from './types'

// 标签数据类型
export interface Tag {
  id: number
  userId: number
  name: string
  createTime: string
  updateTime: string
  color?: string
  count?: number // 保留兼容性
  itemCount?: number // 新增字段：标签下的新闻数量
}

// 标签下的新闻项目类型
export interface TagItemDatum {
  aiDescription: string | null
  description: string
  id: number
  imageUrl: string
  link: string
  pubDate: string
  title: string
  channelTitle?: string // 频道名称
  channelIcon?: string | null // 频道图标
  [property: string]: any
}

// 获取标签新闻列表的请求参数
export interface TagItemsRequest {
  tagId: string
  pageNum?: number
  pageSize?: number
  [property: string]: any
}

// 获取标签新闻列表的响应
export interface TagItemsResponse {
  code: number
  data: TagItemDatum[]
  message: string
  [property: string]: any
}

// 创建标签
export const createTag = async (name: string): Promise<ApiResponse<Tag>> => {
  try {
    const response = await apiClient.post<ApiResponse<Tag>>('/tag/create', null, {
      params: { name }
    })
    return response.data
  } catch (error: any) {
    // 处理和转换错误
    if (error.code && error.message) {
      return error as ApiResponse<Tag>
    }
    // 处理未知错误
    return {
      code: 500,
      message: error.message || '创建标签失败，请稍后再试',
      data: null
    }
  }
}

// 获取用户创建的标签列表
export const getUserTags = async (): Promise<ApiResponse<Tag[]>> => {
  try {
    const response = await apiClient.get<ApiResponse<Tag[]>>('/tag/list')
    return response.data
  } catch (error: any) {
    // 处理和转换错误
    if (error.code && error.message) {
      return error as ApiResponse<Tag[]>
    }
    // 处理未知错误
    return {
      code: 500,
      message: error.message || '获取标签列表失败，请稍后再试',
      data: null
    }
  }
}

// 将新闻添加到标签
export const addItemToTag = async (tagId: string, itemId: string): Promise<ApiResponse<null>> => {
  try {
    const response = await apiClient.post<ApiResponse<null>>('/tag/add', null, {
      params: { tagId, itemId }
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
      message: error.message || '添加到标签失败，请稍后再试',
      data: null
    }
  }
}

// 删除标签
export const deleteTag = async (tagId: string): Promise<ApiResponse<null>> => {
  try {
    const response = await apiClient.post<ApiResponse<null>>('/tag/delete', null, {
      params: { tagId }
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
      message: error.message || '删除标签失败，请稍后再试',
      data: null
    }
  }
}

// 获取标签下的所有新闻项目
export const getTagItems = async (params: TagItemsRequest): Promise<ApiResponse<TagItemDatum[]>> => {
  try {
    const response = await apiClient.get<ApiResponse<TagItemDatum[]>>('/tag/items', {
      params: {
        tagId: params.tagId,
        pageNum: params.pageNum || 1,
        pageSize: params.pageSize || 20
      }
    })
    return response.data
  } catch (error: any) {
    // 处理和转换错误
    if (error.code && error.message) {
      return error as ApiResponse<TagItemDatum[]>
    }
    // 处理未知错误
    return {
      code: 500,
      message: error.message || '获取标签下的新闻失败，请稍后再试',
      data: null
    }
  }
}

// 从标签中移除新闻
export const removeItemFromTag = async (tagId: string, itemId: string): Promise<ApiResponse<null>> => {
  try {
    const response = await apiClient.post<ApiResponse<null>>('/tag/remove', null, {
      params: { tagId, itemId }
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
      message: error.message || '从标签中移除失败，请稍后再试',
      data: null
    }
  }
}
