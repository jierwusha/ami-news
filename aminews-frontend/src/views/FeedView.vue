<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NewsCard from '../components/NewsCard.vue'
import DropdownMenu, { type DropdownItem } from '../components/DropdownMenu.vue'
import FolderDropdown from '../components/FolderDropdown.vue'
import PaginationComponent from '../components/Pagination.vue'
import { useHomeStore } from '../stores/homeStore'
import { deleteFolder, addChannelToFolder, removeChannelFromFolder } from '../api/folder'
import { getChannelItems, getFolderItems } from '../api/news'
import { unsubscribeChannelById } from '../api/sources'
import type { ChannelItemDatum, FolderItemDatum } from '../api/types'

// 定义接口
interface NewsItem {
  id: number | string
  title: string
  source: string
  time: string
  image?: string | null // 允许为 null，NewsCard 会处理
  sourceIcon?: string | null // 允许为 null，NewsCard 会处理
  excerpt?: string // 可选，NewsCard 会处理 HTML 清理和回退
  description?: string // 备用字段
}

interface SourceInfo {
  id: string
  name: string
  description?: string
  icon?: string
  type: 'all' | 'bookmarks' | 'folder' | 'channel'
}

// 路由参数
const route = useRoute()
const router = useRouter()
const homeStore = useHomeStore()

// 当前显示的源信息
const currentSource = ref<SourceInfo | null>(null)
const currentChannelFolders = ref<string[]>([])

// 新闻数据和分页
const news = ref<NewsItem[]>([])
const loading = ref(true)
const currentPage = ref(1)
const pageSize = ref(10)
const totalItems = ref(0) // 保留用于内部计算
const hasNextPage = ref(false) // 是否有下一页

// 是否为频道页面
const isChannelPage = computed(() => currentSource.value?.type === 'channel')

// 是否为文件夹页面
const isFolderPage = computed(() => currentSource.value?.type === 'folder')

// 获取频道ID
const channelId = computed(() => {
  if (route.path.includes('/feeds/channel/')) {
    return route.params.id as string
  }
  return null
})

// 获取文件夹ID
const folderId = computed(() => {
  if (route.path.includes('/feeds/folder/')) {
    return route.params.id as string
  }
  return null
})

// 分页信息本地存储key
const getPaginationStorageKey = (sourceId: string) => `feedview_pagination_${sourceId}`

// 从本地存储恢复分页信息（带验证）
const restorePaginationFromStorage = () => {
  const sourceId = currentSource.value?.id
  if (!sourceId) return

  try {
    const storageKey = getPaginationStorageKey(sourceId)
    const stored = localStorage.getItem(storageKey)
    if (stored) {
      const paginationData = JSON.parse(stored)

      // 检查数据是否过期（7天）
      const isExpired =
        paginationData.timestamp && Date.now() - paginationData.timestamp > 7 * 24 * 60 * 60 * 1000

      if (isExpired) {
        console.log('分页数据已过期，使用默认值')
        localStorage.removeItem(storageKey)
        return
      }

      if (paginationData.currentPage && paginationData.pageSize) {
        // 验证页码的合理性
        const restoredPage = Math.max(1, paginationData.currentPage)

        // 对于历史页码进行额外保护：如果恢复的页码大于10，可能存在问题，重置为1
        if (restoredPage > 10) {
          console.warn('恢复的页码过大，可能存在问题，重置为第1页:', restoredPage)
          currentPage.value = 1
          pageSize.value = paginationData.pageSize
          // 清除这个可能有问题的存储数据
          localStorage.removeItem(storageKey)
        } else {
          currentPage.value = restoredPage
          pageSize.value = paginationData.pageSize
        }

        console.log('恢复分页状态:', {
          源ID: sourceId,
          当前页: currentPage.value,
          每页条数: pageSize.value,
        })
      }
    }
  } catch (error) {
    console.error('恢复分页状态失败:', error)
    // 如果恢复失败，重置为默认值
    currentPage.value = 1
    pageSize.value = 10
  }
}

// 保存分页信息到本地存储
const savePaginationToStorage = () => {
  const sourceId = currentSource.value?.id
  if (!sourceId) return

  try {
    const storageKey = getPaginationStorageKey(sourceId)
    const paginationData = {
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      timestamp: Date.now(),
    }
    localStorage.setItem(storageKey, JSON.stringify(paginationData))
    console.log('保存分页状态:', {
      源ID: sourceId,
      当前页: currentPage.value,
      每页条数: pageSize.value,
    })
  } catch (error) {
    console.error('保存分页状态失败:', error)
  }
}

// 清理过期的分页存储数据
const cleanupExpiredPaginationData = () => {
  try {
    const keysToRemove: string[] = []
    const expireTime = 7 * 24 * 60 * 60 * 1000 // 7天

    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && key.startsWith('feedview_pagination_')) {
        try {
          const data = JSON.parse(localStorage.getItem(key) || '{}')
          if (data.timestamp && Date.now() - data.timestamp > expireTime) {
            keysToRemove.push(key)
          }
        } catch {
          // 数据格式错误，也删除
          keysToRemove.push(key)
        }
      }
    }

    keysToRemove.forEach((key) => {
      localStorage.removeItem(key)
      console.log('清理过期分页数据:', key)
    })

    if (keysToRemove.length > 0) {
      console.log(`清理了 ${keysToRemove.length} 个过期的分页数据`)
    }
  } catch (error) {
    console.error('清理过期分页数据失败:', error)
  }
}

// 将后端数据转换为 NewsItem 格式
const transformChannelItemToNewsItem = (
  item: ChannelItemDatum,
  channelName: string,
  channelIcon: string,
): NewsItem => {
  return {
    id: item.id,
    title: item.title,
    source: channelName,
    time: item.pubDate,
    image: item.imageUrl, // 直接传递，NewsCard 会处理回退逻辑
    sourceIcon: channelIcon, // 直接传递，NewsCard 会处理回退逻辑
    excerpt: item.description, // 直接传递，NewsCard 会处理 HTML 清理
  }
}


// 获取频道新闻数据
const loadChannelNews = async () => {
  if (!channelId.value) {
    console.warn('channelId 为空，无法加载频道新闻')
    loading.value = false
    return
  }

  // 在加载新闻前，验证频道信息是否可用
  const channelInfo = getChannelInfoFromStore(channelId.value)
  if (!channelInfo) {
    console.error('无法获取频道信息，channelId:', channelId.value)
    console.error('homeStore状态:', {
      foldersCount: homeStore.folders.length,
      unCategorizedChannelsCount: homeStore.unCategorizedChannels.length,
      hasData: homeStore.hasData,
    })
    loading.value = false
    return
  }

  try {
    loading.value = true
    console.log('加载频道新闻:', {
      channelId: channelId.value,
      channelName: channelInfo.name,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })

    const response = await getChannelItems({
      channelId: channelId.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })

    if (response.code === 200 && response.data) {
      // 使用已验证的频道信息
      const channelName = channelInfo.name
      const channelIcon =
        channelInfo.icon ||
        new URL('../assets/placeholders/newsSouceIconDefault.png', import.meta.url).href

      // 转换数据格式 - 适配新的接口数据结构
      news.value = response.data.items.map((item) =>
        transformChannelItemToNewsItem(item, channelName, channelIcon),
      )

      // 使用接口返回的 hasNext 字段判断是否有下一页
      hasNextPage.value = response.data.hasNext

      // 验证分页信息一致性
      if (response.data.pageNum !== currentPage.value) {
        console.warn('返回的页码与请求页码不一致:', {
          请求页码: currentPage.value,
          返回页码: response.data.pageNum,
        })
      }

      // 更新总数估算（仅用于内部计算）
      if (hasNextPage.value) {
        totalItems.value = currentPage.value * pageSize.value + 1 // 估算还有下一页
      } else {
        totalItems.value = (currentPage.value - 1) * pageSize.value + response.data.items.length
      }

      console.log('频道新闻加载成功:', {
        频道名称: channelName,
        新闻数量: news.value.length,
        当前页: currentPage.value,
        是否有下一页: hasNextPage.value,
      })
    } else {
      console.error('获取频道新闻失败:', response.message)
      news.value = []
      totalItems.value = 0
      hasNextPage.value = false

      // 如果当前页码大于1且返回失败，可能是页面不存在，尝试回到第一页
      if (
        currentPage.value > 1 &&
        (response.code === 404 ||
          response.message?.includes('找不到') ||
          response.message?.includes('不存在'))
      ) {
        console.warn('当前页面可能不存在，自动回到第一页')
        currentPage.value = 1
        savePaginationToStorage() // 保存修正后的页码

        // 给用户一个提示
        setTimeout(() => {
          loadChannelNews() // 重新加载第一页数据
        }, 100)
        return
      }

      // 如果是认证错误，可以考虑跳转到登录页面
      if (response.code === 401) {
        console.warn('用户未认证，需要登录')
        // 这里可以添加跳转到登录页面的逻辑
      }
    }
  } catch (error) {
    console.error('加载频道新闻时发生错误:', error)
    news.value = []
    totalItems.value = 0
    hasNextPage.value = false

    // 如果当前页码大于1且发生错误，可能是页面不存在，尝试回到第一页
    if (currentPage.value > 1) {
      console.warn('加载失败，可能页面不存在，自动回到第一页')
      currentPage.value = 1
      savePaginationToStorage() // 保存修正后的页码

      // 给用户一个提示
      setTimeout(() => {
        loadChannelNews() // 重新加载第一页数据
      }, 100)
      return
    }
  } finally {
    loading.value = false
  }
}

// 获取文件夹新闻数据
const loadFolderNews = async () => {
  if (!folderId.value) {
    console.warn('folderId 为空，无法加载文件夹新闻')
    loading.value = false
    return
  }

  // 在加载新闻前，验证文件夹信息是否可用
  const folderInfo = getFolderInfoFromStore(folderId.value)
  if (!folderInfo) {
    console.error('无法获取文件夹信息，folderId:', folderId.value)
    console.error('homeStore状态:', {
      foldersCount: homeStore.folders.length,
      hasData: homeStore.hasData,
    })
    loading.value = false
    return
  }

  try {
    loading.value = true
    console.log('加载文件夹新闻:', {
      folderId: folderId.value,
      folderName: folderInfo.name,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })

    const response = await getFolderItems({
      folderId: folderId.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })

    if (response.code === 200 && response.data) {
      // 使用已验证的文件夹信息
      const folderName = folderInfo.name

      // 转换数据格式 - 适配新的接口数据结构
      news.value = response.data.items.map((item) =>
        transformFolderItemToNewsItem(item, folderName),
      )

      // 使用接口返回的 hasNext 字段判断是否有下一页
      hasNextPage.value = response.data.hasNext

      // 验证分页信息一致性
      if (response.data.pageNum !== currentPage.value) {
        console.warn('返回的页码与请求页码不一致:', {
          请求页码: currentPage.value,
          返回页码: response.data.pageNum,
        })
      }

      // 更新总数估算
      if (hasNextPage.value) {
        totalItems.value = currentPage.value * pageSize.value + 1
      } else {
        totalItems.value = (currentPage.value - 1) * pageSize.value + response.data.items.length
      }

      console.log('文件夹新闻加载成功:', {
        文件夹名称: folderName,
        新闻数量: news.value.length,
        当前页: currentPage.value,
        是否有下一页: hasNextPage.value,
      })
    } else {
      console.error('获取文件夹新闻失败:', response.message)
      news.value = []
      totalItems.value = 0
      hasNextPage.value = false

      // 如果当前页码大于1且返回失败，可能是页面不存在，尝试回到第一页
      if (
        currentPage.value > 1 &&
        (response.code === 404 ||
          response.message?.includes('找不到') ||
          response.message?.includes('不存在'))
      ) {
        console.warn('当前页面可能不存在，自动回到第一页')
        currentPage.value = 1
        savePaginationToStorage() // 保存修正后的页码

        // 给用户一个提示
        setTimeout(() => {
          loadFolderNews() // 重新加载第一页数据
        }, 100)
        return
      }

      // 如果是认证错误，可以考虑跳转到登录页面
      if (response.code === 401) {
        console.warn('用户未认证，需要登录')
      }
    }
  } catch (error) {
    console.error('加载文件夹新闻时发生错误:', error)
    news.value = []
    totalItems.value = 0
    hasNextPage.value = false

    // 如果当前页码大于1且发生错误，可能是页面不存在，尝试回到第一页
    if (currentPage.value > 1) {
      console.warn('加载失败，可能页面不存在，自动回到第一页')
      currentPage.value = 1
      savePaginationToStorage() // 保存修正后的页码

      // 给用户一个提示
      setTimeout(() => {
        loadFolderNews() // 重新加载第一页数据
      }, 100)
      return
    }
  } finally {
    loading.value = false
  }
}

// 分页事件处理
const handlePageChange = (page: number) => {
  if (page === currentPage.value || loading.value) return

  currentPage.value = page
  console.log('切换到第', page, '页')

  // 保存分页状态
  savePaginationToStorage()

  if (isChannelPage.value) {
    loadChannelNews()
  } else if (isFolderPage.value) {
    loadFolderNews()
  } else {
    console.warn('当前页面类型不支持分页')
  }
}

const handlePageSizeChange = (size: number) => {
  if (size === pageSize.value || loading.value) return

  pageSize.value = size
  currentPage.value = 1 // 重置到第一页
  console.log('每页显示数量改为', size)

  // 保存分页状态
  savePaginationToStorage()

  if (isChannelPage.value) {
    loadChannelNews()
  } else if (isFolderPage.value) {
    loadFolderNews()
  } else {
    console.warn('当前页面类型不支持分页')
  }
}

// 导航到新闻详情页
const navigateToNewsDetail = (newsId: string | number) => {
  try {
    if (!newsId || (typeof newsId === 'string' && newsId.trim() === '')) {
      console.error('无效的新闻ID:', newsId)
      return
    }

    const id = typeof newsId === 'number' ? newsId.toString() : newsId.toString()
    console.log('导航到新闻详情页:', id)
    router.push(`/news/${id}`)
  } catch (error) {
    console.error('导航到新闻详情页失败:', error)
  }
}

// 根据路由参数获取源信息
const getSourceInfo = () => {
  const path = route.path
  const params = route.params

  if (path === '/feeds/all') {
    return {
      id: 'all',
      name: '全部',
      type: 'all' as const,
    }
  } else if (path === '/feeds/bookmarks') {
    return {
      id: 'bookmarks',
      name: '稍后阅读',
      type: 'bookmarks' as const,
    }
  } else if (path.includes('/feeds/folder/')) {
    // 文件夹页面 - 从homeStore获取真实数据
    const folderId = params.id as string
    return getFolderInfoFromStore(folderId)
  } else if (path.includes('/feeds/channel/')) {
    // 频道页面 - 从homeStore获取真实数据
    const channelId = params.id as string
    return getChannelInfoFromStore(channelId)
  }

  return null
}

// 从store获取文件夹信息
const getFolderInfoFromStore = (folderId: string): SourceInfo | null => {
  const folder = homeStore.folders.find((f) => f.id === folderId)
  if (folder) {
    return {
      id: folder.id,
      name: folder.name,
      type: 'folder' as const,
    }
  }

  // 如果store中没有数据，返回默认值
  return getFolderInfo(folderId)
}

// 从store获取频道信息
const getChannelInfoFromStore = (channelId: string): SourceInfo | null => {
  console.log('查找频道信息:', {
    channelId,
    foldersCount: homeStore.folders.length,
    unCategorizedChannelsCount: homeStore.unCategorizedChannels.length,
  })

  // 在所有文件夹和未分类频道中查找
  for (const folder of homeStore.folders) {
    const channel = folder.channels.find((c) => c.id === channelId)
    if (channel) {
      console.log('在文件夹中找到频道:', {
        folderId: folder.id,
        folderName: folder.name,
        channelId: channel.id,
        channelName: channel.name,
      })

      // 获取频道所属的文件夹ID列表
      const folderIds: string[] = []
      homeStore.folders.forEach((f) => {
        if (f.channels.some((c) => c.id === channelId)) {
          folderIds.push(f.id)
        }
      })
      currentChannelFolders.value = folderIds

      return {
        id: channel.id,
        name: channel.name,
        description: channel.description,
        icon: channel.icon,
        type: 'channel' as const,
      }
    }
  }

  // 在未分类频道中查找
  const unCategorizedChannel = homeStore.unCategorizedChannels.find((c) => c.id === channelId)
  if (unCategorizedChannel) {
    console.log('在未分类频道中找到频道:', {
      channelId: unCategorizedChannel.id,
      channelName: unCategorizedChannel.name,
    })

    currentChannelFolders.value = []
    return {
      id: unCategorizedChannel.id,
      name: unCategorizedChannel.name,
      description: unCategorizedChannel.description,
      icon: unCategorizedChannel.icon,
      type: 'channel' as const,
    }
  }

  console.warn('在 homeStore 中未找到频道:', {
    channelId,
    searchedFolders: homeStore.folders.map((f) => ({
      id: f.id,
      name: f.name,
      channelsCount: f.channels.length,
      channelIds: f.channels.map((c) => c.id),
    })),
    unCategorizedChannelIds: homeStore.unCategorizedChannels.map((c) => c.id),
  })

  // 如果store中没有数据，返回默认值
  return getChannelInfo(channelId)
}

// 获取文件夹信息（回退）
const getFolderInfo = (folderId: string): SourceInfo | null => {
  console.warn(`无法从 homeStore 找到文件夹 ${folderId}，使用默认值`)
  return {
    id: folderId,
    name: '未知文件夹',
    type: 'folder' as const,
  }
}

// 获取频道信息（回退）
const getChannelInfo = (channelId: string): SourceInfo | null => {
  console.warn(`无法从 homeStore 找到频道 ${channelId}，使用默认值`)
  return {
    id: channelId,
    name: '未知频道',
    type: 'channel' as const,
  }
}

// 计算显示标题
const pageTitle = computed(() => {
  if (!currentSource.value) {
    // 数据还在加载时，根据路由显示临时标题
    const path = route.path
    const params = route.params

    if (path === '/feeds/all') {
      return '全部新闻'
    } else if (path === '/feeds/bookmarks') {
      return '稍后阅读'
    } else if (path.includes('/feeds/folder/')) {
      // 尝试从 homeStore 获取文件夹名称，如果没有则显示默认
      const folderId = params.id as string
      const folder = homeStore.folders.find((f) => f.id === folderId)
      return folder ? `${folder.name} - 文件夹` : '文件夹'
    } else if (path.includes('/feeds/channel/')) {
      // 尝试从 homeStore 获取频道名称，如果没有则显示默认
      const channelId = params.id as string

      // 在文件夹中查找频道
      for (const folder of homeStore.folders) {
        const channel = folder.channels.find((c) => c.id === channelId)
        if (channel) {
          return channel.name
        }
      }

      // 在未分类频道中查找
      const unCategorizedChannel = homeStore.unCategorizedChannels.find((c) => c.id === channelId)
      if (unCategorizedChannel) {
        return unCategorizedChannel.name
      }

      return '频道'
    }

    return '新闻'
  }

  switch (currentSource.value.type) {
    case 'all':
      return '全部新闻'
    case 'bookmarks':
      return '稍后阅读'
    case 'folder':
      return `${currentSource.value.name} - 文件夹`
    case 'channel':
      return currentSource.value.name
    default:
      return '新闻'
  }
})

// 是否显示频道描述（仅在大屏且为频道时显示）
const showChannelDescription = computed(() => {
  // 如果 currentSource 存在，使用它的数据
  if (currentSource.value?.type === 'channel' && currentSource.value.description) {
    return true
  }

  // 如果 currentSource 还在加载中，尝试从 homeStore 获取频道信息
  if (!currentSource.value && route.path.includes('/feeds/channel/')) {
    const channelId = route.params.id as string

    // 在文件夹中查找频道
    for (const folder of homeStore.folders) {
      const channel = folder.channels.find((c) => c.id === channelId)
      if (channel && channel.description) {
        return true
      }
    }

    // 在未分类频道中查找
    const unCategorizedChannel = homeStore.unCategorizedChannels.find((c) => c.id === channelId)
    if (unCategorizedChannel && unCategorizedChannel.description) {
      return true
    }
  }

  return false
})

// 获取当前频道描述（用于显示）
const currentChannelDescription = computed(() => {
  // 如果 currentSource 存在，使用它的描述
  if (currentSource.value?.type === 'channel') {
    return currentSource.value.description
  }

  // 如果 currentSource 还在加载中，尝试从 homeStore 获取频道描述
  if (!currentSource.value && route.path.includes('/feeds/channel/')) {
    const channelId = route.params.id as string

    // 在文件夹中查找频道
    for (const folder of homeStore.folders) {
      const channel = folder.channels.find((c) => c.id === channelId)
      if (channel) {
        return channel.description
      }
    }

    // 在未分类频道中查找
    const unCategorizedChannel = homeStore.unCategorizedChannels.find((c) => c.id === channelId)
    if (unCategorizedChannel) {
      return unCategorizedChannel.description
    }
  }

  return undefined
})

// 是否显示文件夹管理按钮（仅频道页面显示）
const showFolderManagement = computed(() => {
  return currentSource.value?.type === 'channel'
})

// 处理文件夹更新
const handleFoldersUpdated = (folderIds: string[]) => {
  currentChannelFolders.value = folderIds
  console.log('频道文件夹已更新:', folderIds)
}

// 移动端文件夹管理
const showMobileFolderManager = ref(false)
const mobileFolderError = ref('')
const selectedMobileFolders = ref<Set<string>>(new Set())
const processingMobileFolders = ref<Set<string>>(new Set())

// 初始化移动端文件夹选择状态
const initMobileFolderSelection = () => {
  selectedMobileFolders.value = new Set(currentChannelFolders.value)
  mobileFolderError.value = ''
}

// 打开移动端文件夹管理器
const openMobileFolderManager = () => {
  showMobileFolderManager.value = true
  initMobileFolderSelection()
}

// 关闭移动端文件夹管理器
const closeMobileFolderManager = async () => {
  showMobileFolderManager.value = false
  mobileFolderError.value = ''
  processingMobileFolders.value.clear()

  // 关闭时也刷新一下数据，确保界面状态同步
  try {
    await homeStore.fetchHomeData()
    // 重新获取当前频道的文件夹信息
    if (currentSource.value?.type === 'channel') {
      const channelId = currentSource.value.id
      const folderIds: string[] = []
      homeStore.folders.forEach((f) => {
        if (f.channels.some((c) => c.id === channelId)) {
          folderIds.push(f.id)
        }
      })
      currentChannelFolders.value = folderIds
    }
  } catch (error) {
    console.error('刷新数据失败:', error)
  }
}

// 切换移动端文件夹选择
const toggleMobileFolder = async (folderId: string, checked: boolean) => {
  if (!currentSource.value || processingMobileFolders.value.has(folderId)) return

  try {
    processingMobileFolders.value.add(folderId)
    mobileFolderError.value = ''

    if (checked) {
      // 添加到文件夹
      await addChannelToFolder(folderId, currentSource.value.id)
      selectedMobileFolders.value.add(folderId)
    } else {
      // 从文件夹移除
      await removeChannelFromFolder(folderId, currentSource.value.id)
      selectedMobileFolders.value.delete(folderId)
    }

    // 更新父组件的文件夹状态
    const newFolderIds = Array.from(selectedMobileFolders.value)
    handleFoldersUpdated(newFolderIds)

    // 刷新 homeStore 数据以更新界面
    await homeStore.fetchHomeData()
  } catch (error) {
    console.error('文件夹操作失败:', error)
    mobileFolderError.value = '操作失败，请重试'

    // 恢复之前的状态
    if (checked) {
      selectedMobileFolders.value.delete(folderId)
    } else {
      selectedMobileFolders.value.add(folderId)
    }
  } finally {
    processingMobileFolders.value.delete(folderId)
  }
}

// 删除文件夹
const handleDeleteFolder = async () => {
  if (!currentSource.value || currentSource.value.type !== 'folder') return

  const confirmed = confirm(`确定要删除文件夹"${currentSource.value.name}"吗？`)
  if (!confirmed) return

  try {
    await deleteFolder(currentSource.value.id)
    console.log('文件夹删除成功')

    // 刷新homeStore数据
    await homeStore.fetchHomeData()

    // 跳转到首页
    router.push('/')
  } catch (error) {
    console.error('删除文件夹失败:', error)
    alert('删除文件夹失败，请重试')
  }
}

// 取消订阅频道
const handleUnsubscribeChannel = async () => {
  if (!currentSource.value || currentSource.value.type !== 'channel') return

  const confirmed = confirm(`确定要取消订阅频道"${currentSource.value.name}"吗？`)
  if (!confirmed) return

  try {
    const response = await unsubscribeChannelById(currentSource.value.id)

    if (response.code === 200) {
      console.log('频道取消订阅成功')

      // 刷新homeStore数据以更新侧边栏
      await homeStore.fetchHomeData()

      // 跳转，因为当前频道已经不存在
      router.push('/feeds/')
    } else {
      console.error('取消订阅失败:', response.message)
      alert(response.message || '取消订阅失败，请重试')
    }
  } catch (error) {
    console.error('取消订阅频道失败:', error)
    alert('取消订阅失败，请重试')
  }
}

// 头部操作菜单
const headerMenuItems = computed<DropdownItem[]>(() => {
  const baseItems: DropdownItem[] = [
    {
      id: 'refresh',
      label: '刷新',
      icon: '<path d="M23 4v6h-6" /><path d="M1 20v-6h6" /><path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15" />',
      action: () => {
        console.log('刷新数据')
        handleRefresh()
      },
    },
  ]

  // 如果是频道页面，添加文件夹管理和取消订阅选项
  if (currentSource.value?.type === 'channel') {
    baseItems.unshift({
      id: 'manage-folders',
      label: '管理文件夹',
      icon: '<path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />',
      action: openMobileFolderManager,
    })

    baseItems.push({
      id: 'unsubscribe-channel',
      label: '取消订阅',
      icon: '<path d="M22 12h-6l-2-3h-4l-2 3H2" /><path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z" /><line x1="4" y1="4" x2="20" y2="20" stroke-width="2.5" />',
      action: handleUnsubscribeChannel,
    })
  }

  // 如果是文件夹页面，添加删除选项
  if (currentSource.value?.type === 'folder') {
    baseItems.push({
      id: 'delete-folder',
      label: '删除文件夹',
      icon: '<polyline points="3,6 5,6 21,6"></polyline><path d="M19,6v14a2,2 0 0,1 -2,2H7a2,2 0 0,1 -2,-2V6m3,0V4a2,2 0 0,1 2,-2h4a2,2 0 0,1 2,2v2"></path>',
      action: handleDeleteFolder,
    })
  }

  return baseItems
})

// 刷新功能
const handleRefresh = async () => {
  if (loading.value) return

  console.log('刷新当前页面数据')
  await initializeData()
}

// 初始化数据
const initializeData = async () => {
  try {
    loading.value = true

    // 清理数据状态
    news.value = []
    totalItems.value = 0
    hasNextPage.value = false

    // 第一步：确保homeStore有数据，并等待加载完成
    console.log('检查 homeStore 状态:', {
      hasData: homeStore.hasData,
      loading: homeStore.loading,
      foldersCount: homeStore.folders.length,
      unCategorizedChannelsCount: homeStore.unCategorizedChannels.length,
    })

    if (!homeStore.hasData) {
      console.log('homeStore 数据为空，开始加载...')
      await homeStore.fetchHomeData()
      console.log('homeStore 数据加载完成:', {
        foldersCount: homeStore.folders.length,
        unCategorizedChannelsCount: homeStore.unCategorizedChannels.length,
      })
    }

    // 第二步：对于频道页面，确保频道数据完全加载
    if (route.path.includes('/feeds/channel/')) {
      const channelId = route.params.id as string
      console.log('频道页面，验证频道数据:', channelId)

      const isChannelDataReady = await ensureChannelDataLoaded(channelId)
      if (!isChannelDataReady) {
        console.error('频道数据加载失败，无法继续')
        loading.value = false
        return
      }
    }

    // 第二步B：对于文件夹页面，确保文件夹数据完全加载
    if (route.path.includes('/feeds/folder/')) {
      const folderId = route.params.id as string
      console.log('文件夹页面，验证文件夹数据:', folderId)

      const isFolderDataReady = await ensureFolderDataLoaded(folderId)
      if (!isFolderDataReady) {
        console.error('文件夹数据加载失败，无法继续')
        loading.value = false
        return
      }
    }

    // 第三步：获取当前源信息（此时应该可以获取到完整信息）
    currentSource.value = getSourceInfo()
    console.log('当前源信息:', currentSource.value)

    // 第四步：恢复分页状态（必须在获取 currentSource 之后）
    if (currentSource.value) {
      restorePaginationFromStorage()
    }

    // 第五步：加载新闻数据
    if (isChannelPage.value) {
      await loadChannelNews()
    } else if (isFolderPage.value) {
      await loadFolderNews()
    } else {
      console.log('非频道/文件夹页面，暂不加载数据')
      loading.value = false
    }
  } catch (error) {
    console.error('初始化数据失败:', error)
    // 确保在出错时设置正确的状态
    news.value = []
    totalItems.value = 0
    hasNextPage.value = false
    loading.value = false
  }
}

// 等待并验证频道数据完全加载
const ensureChannelDataLoaded = async (channelId: string, maxRetries = 3): Promise<boolean> => {
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    console.log(`验证频道数据，尝试 ${attempt + 1}/${maxRetries}`)

    // 检查 homeStore 是否有数据
    if (!homeStore.hasData) {
      console.log('homeStore 无数据，重新加载...')
      await homeStore.fetchHomeData()
    }

    // 检查指定频道是否存在
    const channelInfo = getChannelInfoFromStore(channelId)
    if (channelInfo && channelInfo.name !== '未知频道') {
      console.log('频道数据验证成功:', channelInfo)
      return true
    }

    console.warn(`频道数据验证失败，尝试 ${attempt + 1}`, {
      channelId,
      channelInfo,
      homeStoreHasData: homeStore.hasData,
      foldersCount: homeStore.folders.length,
      unCategorizedChannelsCount: homeStore.unCategorizedChannels.length,
    })

    // 如果不是最后一次尝试，等待一小段时间后重试
    if (attempt < maxRetries - 1) {
      await new Promise((resolve) => setTimeout(resolve, 500))
      // 强制重新加载数据
      await homeStore.fetchHomeData()
    }
  }

  console.error('频道数据加载失败，已达到最大重试次数')
  return false
}

// 等待并验证文件夹数据完全加载
const ensureFolderDataLoaded = async (folderId: string, maxRetries = 3): Promise<boolean> => {
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    console.log(`验证文件夹数据，尝试 ${attempt + 1}/${maxRetries}`)

    // 检查 homeStore 是否有数据
    if (!homeStore.hasData) {
      console.log('homeStore 无数据，重新加载...')
      await homeStore.fetchHomeData()
    }

    // 检查指定文件夹是否存在
    const folderInfo = getFolderInfoFromStore(folderId)
    if (folderInfo && folderInfo.name !== '未知文件夹') {
      console.log('文件夹数据验证成功:', folderInfo)
      return true
    }

    console.warn(`文件夹数据验证失败，尝试 ${attempt + 1}`, {
      folderId,
      folderInfo,
      homeStoreHasData: homeStore.hasData,
      foldersCount: homeStore.folders.length,
      unCategorizedChannelsCount: homeStore.unCategorizedChannels.length,
    })

    // 如果不是最后一次尝试，等待一小段时间后重试
    if (attempt < maxRetries - 1) {
      await new Promise((resolve) => setTimeout(resolve, 500))
      // 强制重新加载数据
      await homeStore.fetchHomeData()
    }
  }

  console.error('文件夹数据加载失败，已达到最大重试次数')
  return false
}

// 去除HTML标签的辅助函数
// 初始化
onMounted(async () => {
  cleanupExpiredPaginationData() // 清理过期数据
  await initializeData()
})

// 监听路由变化
watch(
  () => route.path,
  async () => {
    console.log('路由变化:', route.path)

    // 重置状态
    totalItems.value = 0
    hasNextPage.value = false

    await initializeData()
  },
)

// 将文件夹后端数据转换为 NewsItem 格式
const transformFolderItemToNewsItem = (item: FolderItemDatum, folderName: string): NewsItem => {
  return {
    id: item.id,
    title: item.title,
    // 优先使用接口返回的 channelTitle，如果没有则使用文件夹名称
    source: item.channelTitle || `${folderName} 文件夹`,
    time: item.pubDate,
    image: item.imageUrl, // 直接传递，NewsCard 会处理回退逻辑
    sourceIcon: item.channelIcon, // 使用接口返回的频道图标
    excerpt: item.description, // 直接传递，NewsCard 会处理 HTML 清理
  }
}
</script>

<template>
  <div class="feed-view">
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <div class="source-info">
          <!-- <div class="source-icon" v-if="currentSource?.icon">
            {{ currentSource.icon }}
          </div> -->
          <div class="source-details">
            <h1 class="page-title">{{ pageTitle }}</h1>
            <p v-if="showChannelDescription" class="source-description">
              {{ currentChannelDescription }}
            </p>
          </div>
        </div>
      </div>

      <div class="header-actions">
        <!-- 文件夹管理按钮 - 仅频道页面显示 -->
        <FolderDropdown
          v-if="showFolderManagement && currentSource"
          :channel-id="currentSource.id"
          :current-folder-ids="currentChannelFolders"
          @folders-updated="handleFoldersUpdated"
        />

        <!-- 取消订阅按钮 - 仅频道页面显示 -->
        <button
          v-if="currentSource?.type === 'channel'"
          class="action-btn unsubscribe-btn"
          title="取消订阅"
          @click="handleUnsubscribeChannel"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M22 12h-6l-2-3h-4l-2 3H2" />
            <path
              d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"
            />
            <!-- 取消斜线 -->
            <line x1="4" y1="4" x2="20" y2="20" stroke-width="2.5" />
          </svg>
        </button>

        <!-- 删除文件夹按钮 - 仅文件夹页面显示 -->
        <button
          v-if="currentSource?.type === 'folder'"
          class="action-btn delete-btn"
          title="删除文件夹"
          @click="handleDeleteFolder"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="3,6 5,6 21,6"></polyline>
            <path
              d="M19,6v14a2,2 0 0,1 -2,2H7a2,2 0 0,1 -2,-2V6m3,0V4a2,2 0 0,1 2,-2h4a2,2 0 0,1 2,2v2"
            ></path>
          </svg>
        </button>

        <button class="action-btn" title="刷新" @click="handleRefresh">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M23 4v6h-6" />
            <path d="M1 20v-6h6" />
            <path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15" />
          </svg>
        </button>
      </div>

      <!-- 移动端省略号菜单 -->
      <div class="mobile-menu">
        <DropdownMenu
          :items="headerMenuItems"
          button-icon="more"
          button-title="More options"
          position="right"
          size="medium"
          :z-index="610"
        />
      </div>
    </header>

    <!-- Content -->
    <main class="content">
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else class="news-container">
        <!-- 顶部分页组件 - 频道页面和文件夹页面且有数据时显示 -->
        <PaginationComponent
          v-if="(isChannelPage || isFolderPage) && news.length > 0"
          :current-page="currentPage"
          :page-size="pageSize"
          :has-next-page="hasNextPage"
          :loading="loading"
          @page-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />

        <div class="news-list">
          <NewsCard
            v-for="newsItem in news"
            :key="newsItem.id"
            :news="newsItem"
            @click="navigateToNewsDetail(newsItem.id)"
          />
        </div>

        <!-- 底部分页组件 - 频道页面和文件夹页面且有数据时显示 -->
        <PaginationComponent
          v-if="(isChannelPage || isFolderPage) && news.length > 0"
          :current-page="currentPage"
          :page-size="pageSize"
          :has-next-page="hasNextPage"
          :loading="loading"
          @page-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />

        <!-- 空状态 -->
        <div v-if="!loading && news.length === 0" class="empty-state">
          <div class="empty-icon">📰</div>
          <h3>暂无新闻</h3>
          <p v-if="isChannelPage">该频道暂时没有新闻内容</p>
          <p v-else>暂时没有新闻内容</p>
        </div>
      </div>
    </main>

    <!-- 移动端文件夹管理模态框 -->
    <div v-if="showMobileFolderManager" class="mobile-folder-modal">
      <div class="modal-overlay" @click="closeMobileFolderManager"></div>
      <div class="modal-content">
        <div class="modal-header">
          <h3>管理文件夹</h3>
          <button @click="closeMobileFolderManager" class="close-btn">
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div class="modal-body">
          <div v-if="mobileFolderError" class="error-message">
            {{ mobileFolderError }}
          </div>

          <div v-if="homeStore.folders.length === 0" class="empty-state">
            <span>暂无文件夹</span>
          </div>

          <div v-else class="folder-list">
            <label
              v-for="folder in homeStore.folders"
              :key="folder.id"
              class="folder-item"
              :class="{ disabled: processingMobileFolders.has(folder.id) }"
            >
              <input
                type="checkbox"
                :checked="selectedMobileFolders.has(folder.id)"
                @change="toggleMobileFolder(folder.id, ($event.target as HTMLInputElement).checked)"
                :disabled="processingMobileFolders.has(folder.id)"
              />
              <span class="folder-icon">📁</span>
              <span class="folder-name">{{ folder.name }}</span>
              <span v-if="processingMobileFolders.has(folder.id)" class="processing-indicator">
                <div class="mini-spinner"></div>
              </span>
            </label>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.feed-view {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  background: white;
  border-bottom: 1px solid #e5e5e5;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: var(--z-page-header);
  flex-shrink: 0;
  min-height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

@media (max-width: 767px) {
  .header-left {
    padding: 0 0 0 50px; /* 为了给移动菜单留出空间 */
  }
}

.source-info {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.source-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background: #f5f5f5;
  border-radius: 8px;
}

.source-details {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden; /* 确保子元素不会溢出 */
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin: 0;
  line-height: 1.2;
  flex-shrink: 0;
  /* 限制为一行，超出部分显示省略号 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: min(300px, 50vw); /* 响应式最大宽度 */
}

.source-description {
  font-size: 0.9rem;
  color: #666;
  margin: 0;
  line-height: 1.3;
  flex-shrink: 1;
  /* 限制为一行，超出部分显示省略号 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: min(200px, 30vw); /* 响应式最大宽度 */
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 8px;
  border: none;
  background: none;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
  display: flex;
  /* align-items: center; */
  /* justify-content: center; */
}

.action-btn:hover {
  background-color: #f5f5f5;
  color: #333;
}

.delete-btn:hover {
  background-color: #ffebee;
  color: #d32f2f;
}

.unsubscribe-btn:hover {
  background-color: #fff3e0;
  color: #f57c00;
}

.mobile-menu {
  display: none;
}

.content {
  flex: 1;
  overflow-y: auto;
  background-color: #fafafa;
  display: flex;
  flex-direction: column;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  gap: 16px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #2196f3;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.news-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 24px;
}

.news-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  flex: 1;
  position: relative; /* 为弹层提供定位上下文 */
}

/* 新闻卡片点击效果 */
.news-list .news-card {
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  z-index: 1; /* 基础层级 */
}

.news-list .news-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.news-list .news-card:active {
  transform: translateY(0);
  transition: transform 0.1s ease;
}

/* 确保弹出菜单不被遮挡 */
.news-list .news-card.dropdown-open {
  z-index: 9998;
  position: relative;
}

/* 分页控件样式 */
.news-container :deep(.pagination) {
  margin: 0;
  border-top: none;
  /* border-bottom: 1px solid #e5e5e5; */
}

/* 顶部分页控件 */
.news-container :deep(.pagination:first-child) {
  /* border-bottom: 1px solid #e5e5e5; */
  border-top: none;
  margin-bottom: 16px;
  padding: 0 0 16px 0;
}

/* 底部分页控件 */
.news-container :deep(.pagination:last-child) {
  /* border-top: 1px solid #e5e5e5; */
  border-bottom: none;
  margin-top: 16px;
  padding: 16px 0 0 0;
}

/* 空状态样式 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #666;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h3 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 18px;
  font-weight: 600;
}

.empty-state p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

/* 移动端响应式 */
@media (max-width: 767px) {
  .header {
    padding: 0 16px;
    min-height: 64px;
  }

  .header-actions {
    display: none;
  }

  .mobile-menu {
    display: block;
  }

  .source-details {
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
  }

  .source-description {
    display: none;
  }

  .page-title {
    font-size: 1.25rem;
    max-width: min(200px, 40vw); /* 移动端响应式最大宽度 */
  }

  .source-icon {
    width: 28px;
    height: 28px;
    font-size: 16px;
  }

  .news-container {
    padding: 16px;
  }
}

/* 大屏幕样式 - 显示频道描述 */
@media (min-width: 768px) {
  .source-description {
    display: block;
  }
}

/* 移动端文件夹管理模态框 */
.mobile-folder-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
}

.modal-content {
  position: relative;
  background: white;
  width: 100%;
  /* max-width: 480px; */
  max-height: 80vh;
  border-radius: 16px 16px 0 0;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #e5e5e5;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.close-btn {
  padding: 8px;
  border: none;
  background: none;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background-color: #f5f5f5;
  color: #333;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px 24px;
}

.error-message {
  background: #ffebee;
  color: #d32f2f;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  text-align: center;
}

.folder-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.folder-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  background: #f8f9fa;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.folder-item:hover:not(.disabled) {
  background: #e3f2fd;
}

.folder-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.folder-item input[type='checkbox'] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.folder-item.disabled input[type='checkbox'] {
  cursor: not-allowed;
}

.folder-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.folder-name {
  flex: 1;
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.processing-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
}

.mini-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #2196f3;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #666;
  font-size: 14px;
}
</style>
