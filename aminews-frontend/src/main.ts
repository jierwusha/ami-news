import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/userStore'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

app.mount('#app')

// 在应用启动后，设置定期检查token有效性
const userStore = useUserStore()

// 初始化用户状态
userStore.initializeUserState()

// 每3分钟检查一次token有效性（缩短间隔以更快发现过期token）
setInterval(
  async () => {
    if (userStore.isLoggedIn) {
      console.log('定期检查token有效性...')
      await userStore.checkAuthValidity()
    }
  },
  3 * 60 * 1000,
)

// 页面获得焦点时也检查一次（用户可能在其他标签页停留很久）
window.addEventListener('focus', async () => {
  if (userStore.isLoggedIn) {
    console.log('页面获得焦点，检查token有效性...')
    await userStore.checkAuthValidity()
  }
})

// 页面即将卸载时的清理
window.addEventListener('beforeunload', () => {
  // 这里可以添加一些清理逻辑
  console.log('页面即将卸载')
})
