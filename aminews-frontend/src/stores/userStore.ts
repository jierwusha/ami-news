import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout, isAuthenticated } from '../api/user'
import type { LoginRequest } from '../api/types'

// JWT解析辅助函数
function parseJWTPayload(token: string) {
  try {
    const base64Payload = token.split('.')[1]
    const decodedPayload = atob(base64Payload)
    return JSON.parse(decodedPayload)
  } catch (error) {
    console.error('JWT解析失败:', error)
    throw error
  }
}

export const useUserStore = defineStore('user', () => {
  // 用户登录状态
  const isLoggedIn = ref(false)

  // 用户信息
  const userInfo = ref<{
    username: string
    id?: string
    email?: string
  } | null>(null)

  // 初始化用户状态
  const initializeUserState = () => {
    const hasToken = isAuthenticated()
    isLoggedIn.value = hasToken

    console.log('初始化用户状态:', { hasToken, isLoggedIn: isLoggedIn.value })

    if (hasToken) {
      // 从本地存储读取用户信息
      try {
        const storedUserInfo = localStorage.getItem('user_info')
        if (storedUserInfo) {
          userInfo.value = JSON.parse(storedUserInfo)
          console.log('从localStorage恢复用户信息:', userInfo.value)
        } else {
          // 如果有token但没有用户信息，尝试从token解析
          const token = localStorage.getItem('auth_token')
          if (token) {
            try {
              const payload = parseJWTPayload(token)
              userInfo.value = {
                id: payload.id?.toString() || '',
                username: payload.sub || 'User',
                email: payload.email || '',
              }
              localStorage.setItem('user_info', JSON.stringify(userInfo.value))
              console.log('从token解析用户信息:', userInfo.value)
            } catch (error) {
              console.warn('无法从token解析用户信息:', error)
            }
          }
        }
      } catch (error) {
        console.error('Failed to parse user info from localStorage:', error)
      }
    }
  }

  // 初始化
  initializeUserState()

  // 登录
  async function login(credentials: LoginRequest) {
    const response = await apiLogin(credentials)

    if (response.code === 200 && response.data) {
      // 检查返回的数据结构
      console.log('登录响应数据:', response.data)

      // 根据实际返回的数据结构处理
      if (typeof response.data === 'string') {
        // 如果返回的是直接的token字符串
        const token = response.data
        localStorage.setItem('auth_token', token)

        // 从token中解析用户信息（如果是JWT）
        try {
          const payload = parseJWTPayload(token)
          userInfo.value = {
            id: payload.id?.toString() || '',
            username: payload.sub || credentials.usernameOrEmail, // 使用payload中的sub或登录用户名/邮箱
            email: payload.email || '', // 从token中获取email
          }
        } catch (error) {
          console.warn('无法解析JWT token，使用默认用户信息:', error)
          userInfo.value = {
            id: '',
            username: credentials.usernameOrEmail,
            email: '',
          }
        }
      } else if (typeof response.data === 'object' && response.data !== null) {
        // 如果返回的是对象，检查是否有token属性
        const dataObj = response.data as Record<string, unknown>
        if ('token' in dataObj && typeof dataObj.data === 'string') {
          localStorage.setItem('auth_token', dataObj.data)
          if ('user' in dataObj && typeof dataObj.user === 'object' && dataObj.user !== null) {
            const userObj = dataObj.user as Record<string, unknown>
            userInfo.value = {
              id: userObj.id?.toString() || '',
              username: userObj.username?.toString() || credentials.usernameOrEmail,
              email: userObj.email?.toString() || '',
            }
          }
        }
      }

      // 更新登录状态
      isLoggedIn.value = true

      // 将用户信息保存到本地存储
      if (userInfo.value) {
        localStorage.setItem('user_info', JSON.stringify(userInfo.value))
      }

      console.log('用户状态更新:', { isLoggedIn: isLoggedIn.value, userInfo: userInfo.value })
    }

    return response
  }

  // 仅清理本地状态的退出登录（用于token已失效的情况）
  function logoutLocal() {
    console.log('清理本地登录状态（跳过API调用）...')

    // 清理用户状态
    isLoggedIn.value = false
    userInfo.value = null

    // 清理本地存储
    localStorage.removeItem('user_info')
    localStorage.removeItem('auth_token')

    // 清理其他可能的缓存数据
    localStorage.removeItem('homeview-selected-tab')
    localStorage.removeItem('hot-word-selection')

    console.log('本地登录状态已清理')
  }

  // 退出登录
  async function logout() {
    try {
      console.log('开始退出登录...')

      // 调用API的退出登录函数（异步调用后端接口）
      const result = await apiLogout()

      // 清理用户状态
      isLoggedIn.value = false
      userInfo.value = null

      // 清理本地存储
      localStorage.removeItem('user_info')
      localStorage.removeItem('auth_token')

      // 清理其他可能的缓存数据
      localStorage.removeItem('homeview-selected-tab')
      localStorage.removeItem('hot-word-selection')

      console.log('退出登录成功，状态已清理')

      // 返回结果给调用者
      return result
    } catch (error) {
      console.error('退出登录过程中发生错误:', error)

      // 即使出错，也要清理本地状态
      isLoggedIn.value = false
      userInfo.value = null
      localStorage.removeItem('user_info')
      localStorage.removeItem('auth_token')
      localStorage.removeItem('homeview-selected-tab')
      localStorage.removeItem('hot-word-selection')

      // 抛出错误给调用者处理
      throw error
    }
  }

  // 用户显示名称
  const displayName = computed(() => {
    return userInfo.value?.username || '游客'
  })

  // 检查并清理过期的认证状态
  const checkAuthValidity = async () => {
    if (!isAuthenticated()) {
      // 如果token无效或过期，清理状态
      if (isLoggedIn.value) {
        console.log('Token已失效，清理用户状态')
        await logout()
      }
      return false
    }

    // 如果有token，可以通过一个轻量级的API调用来验证token是否仍然有效
    // 这里可以调用一个简单的用户信息接口来验证
    try {
      // 简单的token验证 - 可以根据实际情况调整
      const token = localStorage.getItem('auth_token')
      if (token) {
        // 解析token检查是否过期（如果是JWT）
        try {
          const payload = parseJWTPayload(token)
          const currentTime = Math.floor(Date.now() / 1000)

          if (payload.exp && payload.exp < currentTime) {
            console.log('Token已过期，自动登出')
            await logout()
            return false
          }
        } catch {
          console.warn('Token解析失败，可能不是有效的JWT')
        }
      }

      return true
    } catch (error) {
      console.error('Token验证失败:', error)
      if (isLoggedIn.value) {
        await logout()
      }
      return false
    }
  }

  return {
    isLoggedIn,
    userInfo,
    displayName,
    initializeUserState,
    login,
    logout,
    logoutLocal,
    checkAuthValidity,
  }
})
