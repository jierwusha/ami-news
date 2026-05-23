import axios from 'axios'
import type { AxiosInstance } from 'axios'

// 创建axios实例
export const createApiClient = (): AxiosInstance => {
  const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json',
    },
  })

  // 请求拦截器
  apiClient.interceptors.request.use(
    (config) => {
      // 从本地存储获取token
      const token = localStorage.getItem('auth_token')
      console.log('拦截器: 当前token是否存在:', !!token)

      // 如果有token则添加到请求头
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`
        console.log('拦截器: 已添加token到请求头')
      } else {
        console.warn('拦截器: 没有找到token')
      }

      return config
    },
    (error) => {
      return Promise.reject(error)
    },
  )

  // 响应拦截器
  apiClient.interceptors.response.use(
    (response) => {
      return response
    },
    (error) => {
      // 统一处理错误
      if (error.response) {
        // 服务器返回错误状态码
        const { status, data } = error.response

        // 401未授权，可能是token过期
        if (status === 401) {
          console.log('收到401错误，token过期或无效，清除本地认证信息')
          handleAuthError().catch(console.error)
        }

        // 500错误，但包含"获取用户信息失败"消息，说明token已失效
        if (status === 500 && data && data.message && data.message.includes('获取用户信息失败')) {
          console.log('收到服务器错误：获取用户信息失败，token已被后端删除，清除本地认证信息')
          handleAuthError().catch(console.error)
        }

        // 返回错误信息
        return Promise.reject(data || error)
      }

      // 网络错误等
      return Promise.reject(error)
    },
  )

  // 处理认证错误的公共方法
  async function handleAuthError() {
    console.log('开始处理认证错误，清除所有本地认证信息')

    // 尝试调用userStore的logoutLocal方法以确保状态同步
    try {
      // 动态导入userStore以避免循环依赖
      const { useUserStore } = await import('../stores/userStore')
      const userStore = useUserStore()

      // 使用logoutLocal方法，避免调用后端API
      userStore.logoutLocal()
      console.log('已通过userStore清理本地状态')
    } catch (error) {
      console.warn('无法通过userStore清理状态，手动清理:', error)
      // 手动清理作为后备
      localStorage.removeItem('auth_token')
      localStorage.removeItem('user_info')
      localStorage.removeItem('homeview-selected-tab')
      localStorage.removeItem('hot-word-selection')
    }

    // 显示登录过期提示
    showLoginExpiredMessage()

    // 延迟跳转到登录页，给用户时间看到提示
    setTimeout(() => {
      window.location.href = '/login'
    }, 2000)
  }

  // 显示登录过期提示
  function showLoginExpiredMessage() {
    // 创建提示元素
    const message = document.createElement('div')
    message.textContent = '登录已过期，请重新登录'
    message.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      background: #fff3cd;
      border: 1px solid #ffeaa7;
      color: #856404;
      padding: 12px 20px;
      border-radius: 6px;
      font-size: 14px;
      z-index: 10000;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    `
    document.body.appendChild(message)

    // 2秒后自动移除提示
    setTimeout(() => {
      if (document.body.contains(message)) {
        document.body.removeChild(message)
      }
    }, 2000)
  }

  return apiClient
}

// 导出一个默认的API客户端实例
export const apiClient = createApiClient()
