import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getHomeViewData, refreshHomeViewData } from '../api/news'
import { isAuthenticated } from '../api/user'
import type { HomeViewFolder, HomeViewChannel } from '../api/types'

// 处理token失效的辅助函数
const handleTokenExpired = async () => {
  // 动态导入userStore以避免循环依赖
  const { useUserStore } = await import('./userStore')
  const userStore = useUserStore()

  if (userStore.isLoggedIn) {
    console.log('检测到token失效，执行本地登出操作')
    userStore.logoutLocal()

    // 添加页面跳转逻辑
    const routerModule = await import('../router')
    const router = routerModule.default

    // 延迟跳转，确保用户看到提示信息
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  }
}

export const useHomeStore = defineStore('home', () => {
  // 状态数据
  const folders = ref<HomeViewFolder[]>([])
  const unCategorizedChannels = ref<HomeViewChannel[]>([])
  const loading = ref(false)
  const error = ref('')
  const lastUpdated = ref<Date | null>(null)

  // 计算属性
  const totalUnreadCount = computed(() => {
    const foldersUnread = folders.value.reduce((sum, folder) => sum + folder.unreadCount, 0)
    const unCategorizedUnread = unCategorizedChannels.value.reduce(
      (sum, channel) => sum + channel.unreadCount,
      0,
    )
    return foldersUnread + unCategorizedUnread
  })

  const hasData = computed(() => {
    return folders.value.length > 0 || unCategorizedChannels.value.length > 0
  })

  // 方法
  async function fetchHomeData() {
    // 检查用户是否已登录
    if (!isAuthenticated()) {
      console.log('用户未登录，跳过获取首页数据')
      // 清空数据状态
      folders.value = []
      unCategorizedChannels.value = []
      error.value = ''
      loading.value = false
      return
    }

    // 防止重复加载
    if (loading.value) {
      console.log('数据正在加载中，跳过重复请求')
      return
    }

    loading.value = true
    error.value = ''

    try {
      const response = await getHomeViewData()

      if (response.code === 200 && response.data) {
        folders.value = response.data

        // 提取 ROOTFOLDER 中的频道作为未分类频道
        const rootFolder = folders.value.find(
          (folder) =>
            folder.name === 'ROOTFOLDER' ||
            folder.name === '未分类' ||
            folder.name === 'Uncategorized' ||
            folder.id === 'root' ||
            folder.id === 'ROOTFOLDER',
        )

        if (rootFolder) {
          unCategorizedChannels.value = rootFolder.channels
          // 从 folders 中移除 ROOTFOLDER，避免重复显示
          folders.value = folders.value.filter((folder) => folder.id !== rootFolder.id)
          console.log('找到 ROOTFOLDER，提取未分类频道:', {
            rootFolderId: rootFolder.id,
            rootFolderName: rootFolder.name,
            unCategorizedChannelsCount: rootFolder.channels.length,
          })
        } else {
          unCategorizedChannels.value = []
          console.log('未找到 ROOTFOLDER')
        }

        lastUpdated.value = new Date()
        console.log('首页数据获取成功:', {
          folders: folders.value.length,
          unCategorizedChannels: unCategorizedChannels.value.length,
          totalChannels:
            folders.value.reduce((sum, folder) => sum + folder.channels.length, 0) +
            unCategorizedChannels.value.length,
        })
      } else if (response.code === 401) {
        error.value = response.message || '请先登录'
        console.warn('需要登录:', response.message)
        // 清空数据
        folders.value = []
        unCategorizedChannels.value = []
      } else if (
        response.code === 500 &&
        response.message &&
        response.message.includes('获取用户信息失败')
      ) {
        error.value = '登录已过期，请重新登录'
        console.warn('Token已失效:', response.message)
        // 清空数据
        folders.value = []
        unCategorizedChannels.value = []
        // 调用登出操作清除本地token
        await handleTokenExpired()
      } else {
        error.value = response.message || '获取首页数据失败'
        console.error('获取首页数据失败:', response.message)
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '获取首页数据时发生未知错误'
      error.value = errorMessage
      console.error('获取首页数据异常:', err)
    } finally {
      loading.value = false
    }
  }

  async function refreshData() {
    // 检查用户是否已登录
    if (!isAuthenticated()) {
      console.log('用户未登录，跳过刷新首页数据')
      // 清空数据状态
      folders.value = []
      unCategorizedChannels.value = []
      error.value = ''
      loading.value = false
      return false
    }

    // 防止重复加载
    if (loading.value) {
      console.log('数据正在加载中，跳过重复刷新请求')
      return false
    }

    loading.value = true
    error.value = ''

    try {
      const response = await refreshHomeViewData()

      if (response.code === 200 && response.data) {
        folders.value = response.data

        // 提取 ROOTFOLDER 中的频道作为未分类频道
        const rootFolder = folders.value.find(
          (folder) =>
            folder.name === 'ROOTFOLDER' ||
            folder.name === '未分类' ||
            folder.name === 'Uncategorized' ||
            folder.id === 'root' ||
            folder.id === 'ROOTFOLDER',
        )

        if (rootFolder) {
          unCategorizedChannels.value = rootFolder.channels
          // 从 folders 中移除 ROOTFOLDER，避免重复显示
          folders.value = folders.value.filter((folder) => folder.id !== rootFolder.id)
          console.log('刷新时找到 ROOTFOLDER，提取未分类频道:', {
            rootFolderId: rootFolder.id,
            rootFolderName: rootFolder.name,
            unCategorizedChannelsCount: rootFolder.channels.length,
          })
        } else {
          unCategorizedChannels.value = []
          console.log('刷新时未找到 ROOTFOLDER')
        }

        lastUpdated.value = new Date()
        console.log('首页数据刷新成功:', {
          folders: folders.value.length,
          unCategorizedChannels: unCategorizedChannels.value.length,
          totalChannels:
            folders.value.reduce((sum, folder) => sum + folder.channels.length, 0) +
            unCategorizedChannels.value.length,
        })
        return true
      } else if (response.code === 401) {
        error.value = response.message || '请先登录'
        console.warn('需要登录:', response.message)
        // 清空数据
        folders.value = []
        unCategorizedChannels.value = []
        return false
      } else if (
        response.code === 500 &&
        response.message &&
        response.message.includes('获取用户信息失败')
      ) {
        error.value = '登录已过期，请重新登录'
        console.warn('Token已失效:', response.message)
        // 清空数据
        folders.value = []
        unCategorizedChannels.value = []
        // 调用登出操作清除本地token
        await handleTokenExpired()
        return false
      } else {
        error.value = response.message || '刷新首页数据失败'
        console.error('刷新首页数据失败:', response.message)
        return false
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '刷新首页数据时发生未知错误'
      error.value = errorMessage
      console.error('刷新首页数据异常:', err)
      return false
    } finally {
      loading.value = false
    }
  }

  // 根据ID查找文件夹
  function getFolderById(folderId: string) {
    return folders.value.find((folder) => folder.id === folderId)
  }

  // 根据ID查找频道
  function getChannelById(channelId: string) {
    // 在文件夹中查找
    for (const folder of folders.value) {
      const channel = folder.channels.find((ch) => ch.id === channelId)
      if (channel) return channel
    }
    // 在未分类频道中查找
    return unCategorizedChannels.value.find((ch) => ch.id === channelId)
  }

  // 清除错误状态
  function clearError() {
    error.value = ''
  }

  // 重置状态
  function resetState() {
    folders.value = []
    unCategorizedChannels.value = []
    loading.value = false
    error.value = ''
    lastUpdated.value = null
  }

  return {
    // 状态
    folders,
    unCategorizedChannels,
    loading,
    error,
    lastUpdated,

    // 计算属性
    totalUnreadCount,
    hasData,

    // 方法
    fetchHomeData,
    refreshData,
    getFolderById,
    getChannelById,
    clearError,
    resetState,
  }
})
