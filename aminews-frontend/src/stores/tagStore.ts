import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getUserTags, type Tag } from '../api/tag'
import { isAuthenticated } from '../api/user'

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

export const useTagStore = defineStore('tag', () => {
  // 状态
  const tags = ref<Tag[]>([])
  const loading = ref(false)
  const error = ref('')

  // 计算属性
  const tagCount = computed(() => tags.value.length)

  // 获取标签列表
  const fetchTags = async () => {
    // 检查用户是否已登录
    if (!isAuthenticated()) {
      console.log('用户未登录，跳过获取标签列表')
      // 清空数据状态
      tags.value = []
      error.value = ''
      loading.value = false
      return
    }

    try {
      loading.value = true
      error.value = ''

      const response = await getUserTags()
      if (response.code === 200 && response.data) {
        tags.value = response.data
      } else if (response.code === 401) {
        error.value = response.message || '请先登录'
        console.warn('需要登录:', response.message)
        // 清空数据
        tags.value = []
      } else if (
        response.code === 500 &&
        response.message &&
        response.message.includes('获取用户信息失败')
      ) {
        error.value = '登录已过期，请重新登录'
        console.warn('Token已失效:', response.message)
        // 清空数据
        tags.value = []
        // 调用登出操作清除本地token
        await handleTokenExpired()
      } else {
        error.value = response.message || '获取标签列表失败'
      }
    } catch (err: unknown) {
      console.error('获取标签列表失败:', err)
      error.value = err instanceof Error ? err.message : '获取标签列表失败，请稍后再试'
    } finally {
      loading.value = false
    }
  }

  // 刷新标签列表
  const refreshTags = async () => {
    await fetchTags()
  }

  // 添加标签到本地状态
  const addTag = (tag: Tag) => {
    tags.value.push(tag)
  }

  // 从本地状态删除标签
  const removeTag = (tagId: number) => {
    const index = tags.value.findIndex((tag) => tag.id === tagId)
    if (index > -1) {
      tags.value.splice(index, 1)
    }
  }

  // 更新标签
  const updateTag = (updatedTag: Tag) => {
    const index = tags.value.findIndex((tag) => tag.id === updatedTag.id)
    if (index > -1) {
      tags.value[index] = updatedTag
    }
  }

  // 更新标签的项目数量
  const updateTagItemCount = (tagId: number, increment: number = 1) => {
    const tag = tags.value.find((t) => t.id === tagId)
    if (tag) {
      // 优先更新itemCount，如果没有则更新count（保持兼容性）
      if (tag.itemCount !== undefined) {
        tag.itemCount = Math.max(0, tag.itemCount + increment)
      } else if (tag.count !== undefined) {
        tag.count = Math.max(0, tag.count + increment)
      } else {
        tag.itemCount = Math.max(0, increment)
      }
    }
  }

  // 重置状态
  const resetState = () => {
    tags.value = []
    loading.value = false
    error.value = ''
  }

  return {
    // 状态
    tags,
    loading,
    error,
    // 计算属性
    tagCount,
    // 方法
    fetchTags,
    refreshTags,
    addTag,
    removeTag,
    updateTag,
    updateTagItemCount,
    resetState,
  }
})
