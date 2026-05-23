import { apiClient } from './interceptors'
import type { ApiResponse, LoginRequest, RegisterRequest } from './types'

/**
 * 用户注册
 * @param userData 注册信息（用户名和密码）
 * @returns 注册结果
 */
export const register = async (userData: RegisterRequest): Promise<ApiResponse> => {
  try {
    const response = await apiClient.post<ApiResponse>('/user/register', userData)
    return response.data
  } catch (error: unknown) {
    // 处理和转换错误
    if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
      return error as ApiResponse
    }
    // 处理未知错误
    const errorMessage = error instanceof Error ? error.message : '注册失败，请稍后再试'
    return {
      code: 500,
      message: errorMessage,
      data: null,
    }
  }
}

/**
 * 用户登录
 * @param credentials 登录凭据（用户名和密码）
 * @returns 登录结果，包含token和用户信息
 */
export const login = async (credentials: LoginRequest): Promise<ApiResponse<string>> => {
  try {
    const response = await apiClient.post<ApiResponse<string>>('/user/login', credentials)

    console.log('API登录响应:', response.data)

    // 如果登录成功，保存token到本地存储
    if (response.data.code === 200 && response.data.data) {
      // 后端直接返回token字符串
      const token = response.data.data
      localStorage.setItem('auth_token', token)
      console.log('Token已保存到localStorage:', token)
    }

    return response.data
  } catch (error: unknown) {
    // 处理和转换错误
    if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
      return error as ApiResponse<string>
    }
    // 处理未知错误
    const errorMessage = error instanceof Error ? error.message : '登录失败，请稍后再试'
    return {
      code: 500,
      message: errorMessage,
      data: null,
    }
  }
}

/**
 * 退出登录
 * @returns 退出登录结果
 */
export const logout = async (): Promise<ApiResponse> => {
  try {
    // 调用后端退出登录接口（GET 请求）
    const response = await apiClient.get<ApiResponse>('/user/logout')

    // 不管后端返回什么，都清理本地token
    localStorage.removeItem('auth_token')

    return response.data
  } catch (error: unknown) {
    // 即使接口调用失败，也要清理本地token
    localStorage.removeItem('auth_token')

    // 处理和转换错误
    if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
      return error as ApiResponse
    }
    // 处理未知错误
    const errorMessage = error instanceof Error ? error.message : '退出登录失败，但本地状态已清理'
    return {
      code: 500,
      message: errorMessage,
      data: null,
    }
  }
}

/**
 * 发送邮箱验证码
 * @param email 邮箱地址
 * @returns 发送结果
 */
export const sendEmailCode = async (email: string): Promise<ApiResponse> => {
  try {
    const response = await apiClient.get<ApiResponse>('/user/code', {
      params: { email },
    })
    return response.data
  } catch (error: unknown) {
    // 处理和转换错误
    if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
      return error as ApiResponse
    }
    // 处理未知错误
    const errorMessage = error instanceof Error ? error.message : '发送验证码失败，请稍后再试'
    return {
      code: 500,
      message: errorMessage,
      data: null,
    }
  }
}

/**
 * 重设密码
 * @param resetData 重设密码信息（邮箱、新密码、验证码）
 * @returns 重设结果
 */
export const resetPassword = async (resetData: {
  email: string
  password: string
  code: string
}): Promise<ApiResponse> => {
  try {
    const response = await apiClient.post<ApiResponse>('/user/reset-password', resetData)
    return response.data
  } catch (error: unknown) {
    // 处理和转换错误
    if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
      return error as ApiResponse
    }
    // 处理未知错误
    const errorMessage = error instanceof Error ? error.message : '重设密码失败，请稍后再试'
    return {
      code: 500,
      message: errorMessage,
      data: null,
    }
  }
}

/**
 * 检查用户是否已登录
 * @returns 是否已登录
 */
export const isAuthenticated = (): boolean => {
  return !!localStorage.getItem('auth_token')
}
