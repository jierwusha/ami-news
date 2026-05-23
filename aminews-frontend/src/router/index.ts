import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/userStore'
import HomeView from '../views/HomeView.vue'
import SearchView from '../views/SearchView.vue'
import SettingsView from '../views/SettingsView.vue'
import LoginView from '../views/LoginView.vue'
import FeedView from '../views/FeedView.vue'
import NewsDetailView from '../views/NewsDetailView.vue'
import LaterView from '@/views/LaterView.vue'
import TagsView from '@/views/TagsView.vue'
import TagDetailView from '@/views/TagDetailView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/feeds/all',
      name: 'feeds-all',
      component: FeedView,
      meta: { requiresAuth: true },
    },
    {
      path: '/feeds/bookmarks',
      name: 'feeds-bookmarks',
      component: FeedView,
      meta: { requiresAuth: true },
    },
    {
      path: '/feeds/folder/:id',
      name: 'feeds-folder',
      component: FeedView,
      meta: { requiresAuth: true },
    },
    {
      path: '/feeds/channel/:id',
      name: 'feeds-channel',
      component: FeedView,
      meta: { requiresAuth: true },
    },
    {
      path: '/search',
      name: 'search',
      component: SearchView,
      meta: { requiresAuth: true },
    },
    {
      path: '/tags',
      name: 'tags',
      component: TagsView,
      meta: { requiresAuth: true },
    },
    {
      path: '/tags/:id',
      name: 'tag-detail',
      component: TagDetailView,
      meta: { requiresAuth: true },
    },
    {
      path: '/later',
      name: 'later',
      component: LaterView,
      meta: { requiresAuth: true },
    },
    {
      path: '/settings',
      name: 'settings',
      component: SettingsView,
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { requiresAuth: false },
    },
    {
      path: '/news/:id',
      name: 'news-detail',
      component: NewsDetailView,
      meta: { requiresAuth: true },
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('../views/ChatView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/chat/:id',
      name: 'chat-detail',
      component: () => import('../views/ChatDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
      meta: { requiresAuth: false },
    },
  ],
})

// 路由守卫：检查用户登录状态
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 如果路由需要认证
  if (to.meta.requiresAuth) {
    // 检查用户是否已登录
    if (!userStore.isLoggedIn) {
      console.log('用户未登录，重定向到登录页面')
      // 保存用户想要访问的页面，登录后可以重定向回去
      const redirectPath = to.fullPath !== '/login' ? to.fullPath : '/'
      next({
        name: 'login',
        query: { redirect: redirectPath },
      })
      return
    }
  }

  // 如果已登录用户访问登录页面，重定向到首页
  if (to.name === 'login' && userStore.isLoggedIn) {
    console.log('已登录用户访问登录页面，重定向到首页')
    next({ name: 'home' })
    return
  }

  next()
})

export default router
