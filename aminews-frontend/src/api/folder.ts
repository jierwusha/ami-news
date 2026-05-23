import { apiClient } from './interceptors'
import type { ApiResponse } from './types'

/**
 * 将频道添加到文件夹
 */
export const addChannelToFolder = async (
  folderId: string,
  channelId: string,
): Promise<ApiResponse<null>> => {
  try {
    const response = await apiClient.post<ApiResponse<null>>('/folder/add', null, {
      params: {
        folderId,
        channelId,
      },
    })
    console.log('添加频道到文件夹API响应:', response.data)
    return response.data
  } catch (error) {
    console.error('添加频道到文件夹失败:', error)
    throw error
  }
}

/**
 * 从文件夹中移除频道
 */
export const removeChannelFromFolder = async (
  folderId: string,
  channelId: string,
): Promise<ApiResponse<null>> => {
  try {
    const response = await apiClient.post<ApiResponse<null>>('/folder/remove', null, {
      params: {
        folderId,
        channelId,
      },
    })
    console.log('从文件夹移除频道API响应:', response.data)
    return response.data
  } catch (error) {
    console.error('从文件夹移除频道失败:', error)
    throw error
  }
}

/**
 * 删除文件夹
 */
export const deleteFolder = async (folderId: string): Promise<ApiResponse<null>> => {
  try {
    const response = await apiClient.post<ApiResponse<null>>('/folder/delete', null, {
      params: {
        folderId,
      },
    })
    console.log('删除文件夹API响应:', response.data)
    return response.data
  } catch (error) {
    console.error('删除文件夹失败:', error)
    throw error
  }
}

/**
 * 创建文件夹
 */
export const createFolder = async (name: string): Promise<ApiResponse<null>> => {
  try {
    const response = await apiClient.post<ApiResponse<null>>('/folder/create', null, {
      params: {
        name,
      },
    })
    console.log('创建文件夹API响应:', response.data)
    return response.data
  } catch (error) {
    console.error('创建文件夹失败:', error)
    throw error
  }
}
