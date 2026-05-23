<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/userStore' // 导入用户存储
import { useHomeStore } from '../stores/homeStore' // 导入首页存储
import { register, sendEmailCode, resetPassword } from '../api/user'
import {
  validateUsername,
  validatePassword,
  validateEmail,
  validateEmailCode,
  validatePasswordConfirm,
} from '../utils/validation'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore() // 使用用户存储
const homeStore = useHomeStore() // 使用首页存储

// 表单数据
const loginForm = ref({
  username: '',
  password: '',
})

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  code: '',
})

const resetPasswordForm = ref({
  email: '',
  password: '',
  confirmPassword: '',
  code: '',
})

// 错误信息
const errorMessage = ref('')

// 成功信息
const successMessage = ref('')

// 界面状态
const isLoginMode = ref(true)
const isRegisterMode = ref(false)
const isResetPasswordMode = ref(false)
const isLoading = ref(false)

// 验证码相关状态
const isCodeSending = ref(false)
const codeCountdown = ref(0)
const codeTimer = ref<number | null>(null)

// 计算属性：验证码按钮文本
const codeButtonText = computed(() => {
  if (isCodeSending.value) return '发送中...'
  if (codeCountdown.value > 0) return `${codeCountdown.value}秒后重试`
  return '获取验证码'
})

// 计算属性：验证码按钮是否禁用
const isCodeButtonDisabled = computed(() => {
  return (
    isCodeSending.value ||
    codeCountdown.value > 0 ||
    !emailValidation.value.isValid ||
    !registerForm.value.email
  )
})

// 实时验证计算属性 - 注册表单
const usernameValidation = computed(() => {
  if (!registerForm.value.username) return { isValid: true, message: '' }
  return validateUsername(registerForm.value.username)
})

const passwordValidation = computed(() => {
  if (!registerForm.value.password) return { isValid: true, message: '' }
  return validatePassword(registerForm.value.password)
})

const confirmPasswordValidation = computed(() => {
  if (!registerForm.value.confirmPassword) return { isValid: true, message: '' }
  return validatePasswordConfirm(registerForm.value.password, registerForm.value.confirmPassword)
})

const emailValidation = computed(() => {
  if (!registerForm.value.email) return { isValid: true, message: '' }
  return validateEmail(registerForm.value.email)
})

const codeValidation = computed(() => {
  if (!registerForm.value.code) return { isValid: true, message: '' }
  return validateEmailCode(registerForm.value.code)
})

// 实时验证计算属性 - 重设密码表单
const resetEmailValidation = computed(() => {
  if (!resetPasswordForm.value.email) return { isValid: true, message: '' }
  return validateEmail(resetPasswordForm.value.email)
})

const resetPasswordValidation = computed(() => {
  if (!resetPasswordForm.value.password) return { isValid: true, message: '' }
  return validatePassword(resetPasswordForm.value.password)
})

const resetConfirmPasswordValidation = computed(() => {
  if (!resetPasswordForm.value.confirmPassword) return { isValid: true, message: '' }
  return validatePasswordConfirm(
    resetPasswordForm.value.password,
    resetPasswordForm.value.confirmPassword,
  )
})

const resetCodeValidation = computed(() => {
  if (!resetPasswordForm.value.code) return { isValid: true, message: '' }
  return validateEmailCode(resetPasswordForm.value.code)
})

// 计算属性：重设密码模式的验证码按钮是否禁用
const isResetCodeButtonDisabled = computed(() => {
  return (
    isCodeSending.value ||
    codeCountdown.value > 0 ||
    !resetEmailValidation.value.isValid ||
    !resetPasswordForm.value.email
  )
})

// 切换登录/注册模式
const toggleMode = () => {
  if (isResetPasswordMode.value) {
    // 从重设密码模式切换到登录模式
    isResetPasswordMode.value = false
    isLoginMode.value = true
    isRegisterMode.value = false
  } else {
    // 在登录和注册模式之间切换
    isLoginMode.value = !isLoginMode.value
    isRegisterMode.value = !isRegisterMode.value
  }

  errorMessage.value = ''
  successMessage.value = ''
  // 清空表单
  loginForm.value = { username: '', password: '' }
  registerForm.value = { username: '', password: '', confirmPassword: '', email: '', code: '' }
  resetPasswordForm.value = { email: '', password: '', confirmPassword: '', code: '' }
  // 清除验证码倒计时
  if (codeTimer.value) {
    clearInterval(codeTimer.value)
    codeTimer.value = null
  }
  codeCountdown.value = 0
}

// 切换到重设密码模式
const switchToResetPassword = () => {
  isResetPasswordMode.value = true
  isLoginMode.value = false
  isRegisterMode.value = false
  errorMessage.value = ''
  successMessage.value = ''
  // 清空表单
  loginForm.value = { username: '', password: '' }
  registerForm.value = { username: '', password: '', confirmPassword: '', email: '', code: '' }
  resetPasswordForm.value = { email: '', password: '', confirmPassword: '', code: '' }
  // 清除验证码倒计时
  if (codeTimer.value) {
    clearInterval(codeTimer.value)
    codeTimer.value = null
  }
  codeCountdown.value = 0
}

// 登录处理
const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    errorMessage.value = '请填写用户名/邮箱和密码'
    successMessage.value = ''
    return
  }

  isLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    // 使用状态存储的登录方法
    const response = await userStore.login({
      usernameOrEmail: loginForm.value.username,
      password: loginForm.value.password,
    })

    if (response.code === 200) {
      console.log('登录成功:', response.data)

      // 登录成功后刷新首页数据
      try {
        await homeStore.refreshData()
        console.log('首页数据已刷新')
      } catch (refreshError) {
        console.warn('刷新首页数据失败:', refreshError)
        // 即使刷新失败也不影响登录流程
      }

      // 获取重定向路径
      const redirectPath = (route.query.redirect as string) || '/'
      console.log('登录成功，重定向到:', redirectPath)

      // 登录成功后跳转到目标页面
      router.push(redirectPath)
    } else {
      errorMessage.value = response.message || '登录失败，请检查用户名/邮箱和密码'
    }
  } catch (error: unknown) {
    console.error('登录失败:', error)
    const errorMsg = error instanceof Error ? error.message : '登录失败，请稍后再试'
    errorMessage.value = errorMsg
  } finally {
    isLoading.value = false
  }
}

// 发送邮箱验证码 - 注册
const handleSendCode = async () => {
  // 验证邮箱格式
  if (!emailValidation.value.isValid) {
    errorMessage.value = emailValidation.value.message
    successMessage.value = ''
    return
  }

  isCodeSending.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const response = await sendEmailCode(registerForm.value.email)

    if (response.code === 200) {
      // 发送成功，启动倒计时
      startCountdown()
      errorMessage.value = ''
      // 可以显示成功提示
      console.log('验证码发送成功')
    } else {
      errorMessage.value = response.message || '发送验证码失败，请稍后再试'
    }
  } catch (error: unknown) {
    console.error('发送验证码失败:', error)
    const errorMsg = error instanceof Error ? error.message : '发送验证码失败，请稍后再试'
    errorMessage.value = errorMsg
  } finally {
    isCodeSending.value = false
  }
}

// 发送邮箱验证码 - 重设密码
const handleSendResetCode = async () => {
  // 验证邮箱格式
  if (!resetEmailValidation.value.isValid) {
    errorMessage.value = resetEmailValidation.value.message
    successMessage.value = ''
    return
  }

  isCodeSending.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const response = await sendEmailCode(resetPasswordForm.value.email)

    if (response.code === 200) {
      // 发送成功，启动倒计时
      startCountdown()
      errorMessage.value = ''
      // 可以显示成功提示
      console.log('重设密码验证码发送成功')
    } else {
      errorMessage.value = response.message || '发送验证码失败，请稍后再试'
    }
  } catch (error: unknown) {
    console.error('发送验证码失败:', error)
    const errorMsg = error instanceof Error ? error.message : '发送验证码失败，请稍后再试'
    errorMessage.value = errorMsg
  } finally {
    isCodeSending.value = false
  }
}

// 启动倒计时
const startCountdown = () => {
  codeCountdown.value = 60
  codeTimer.value = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      if (codeTimer.value) {
        clearInterval(codeTimer.value)
        codeTimer.value = null
      }
    }
  }, 1000)
}

// 注册处理
const handleRegister = async () => {
  // 检查所有验证是否通过
  if (!usernameValidation.value.isValid) {
    errorMessage.value = usernameValidation.value.message
    successMessage.value = ''
    return
  }

  if (!passwordValidation.value.isValid) {
    errorMessage.value = passwordValidation.value.message
    successMessage.value = ''
    return
  }

  if (!confirmPasswordValidation.value.isValid) {
    errorMessage.value = confirmPasswordValidation.value.message
    successMessage.value = ''
    return
  }

  if (!emailValidation.value.isValid) {
    errorMessage.value = emailValidation.value.message
    successMessage.value = ''
    return
  }

  if (!codeValidation.value.isValid) {
    errorMessage.value = codeValidation.value.message
    successMessage.value = ''
    return
  }

  // 检查必填字段
  if (
    !registerForm.value.username ||
    !registerForm.value.password ||
    !registerForm.value.confirmPassword ||
    !registerForm.value.email ||
    !registerForm.value.code
  ) {
    errorMessage.value = '请填写所有必填字段'
    successMessage.value = ''
    return
  }

  isLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    // 调用注册API
    const response = await register({
      username: registerForm.value.username,
      password: registerForm.value.password,
      email: registerForm.value.email,
      code: registerForm.value.code,
    })

    if (response.code === 200) {
      console.log('注册成功:', response)
      // 显示成功消息
      successMessage.value = '注册成功！请使用您的账号登录。'
      errorMessage.value = ''
      // 注册成功后切换到登录模式
      isLoginMode.value = true
      registerForm.value = { username: '', password: '', confirmPassword: '', email: '', code: '' }
      // 清除验证码倒计时
      if (codeTimer.value) {
        clearInterval(codeTimer.value)
        codeTimer.value = null
      }
      codeCountdown.value = 0
    } else {
      errorMessage.value = response.message || '注册失败，请稍后再试'
      successMessage.value = ''
    }
  } catch (error: unknown) {
    console.error('注册失败:', error)
    const errorMsg = error instanceof Error ? error.message : '注册失败，请稍后再试'
    errorMessage.value = errorMsg
  } finally {
    isLoading.value = false
  }
}

// 重设密码处理
const handleResetPassword = async () => {
  // 检查所有验证是否通过
  if (!resetEmailValidation.value.isValid) {
    errorMessage.value = resetEmailValidation.value.message
    successMessage.value = ''
    return
  }

  if (!resetPasswordValidation.value.isValid) {
    errorMessage.value = resetPasswordValidation.value.message
    successMessage.value = ''
    return
  }

  if (!resetConfirmPasswordValidation.value.isValid) {
    errorMessage.value = resetConfirmPasswordValidation.value.message
    successMessage.value = ''
    return
  }

  if (!resetCodeValidation.value.isValid) {
    errorMessage.value = resetCodeValidation.value.message
    successMessage.value = ''
    return
  }

  // 检查必填字段
  if (
    !resetPasswordForm.value.email ||
    !resetPasswordForm.value.password ||
    !resetPasswordForm.value.confirmPassword ||
    !resetPasswordForm.value.code
  ) {
    errorMessage.value = '请填写所有必填字段'
    successMessage.value = ''
    return
  }

  isLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    // 调用重设密码API
    const response = await resetPassword({
      email: resetPasswordForm.value.email,
      password: resetPasswordForm.value.password,
      code: resetPasswordForm.value.code,
    })

    if (response.code === 200) {
      console.log('重设密码成功:', response)
      // 显示成功消息
      successMessage.value = '密码重设成功！请使用新密码登录。'
      errorMessage.value = ''
      // 重设密码成功后切换到登录模式
      isResetPasswordMode.value = false
      isLoginMode.value = true
      isRegisterMode.value = false
      resetPasswordForm.value = { email: '', password: '', confirmPassword: '', code: '' }
      // 清除验证码倒计时
      if (codeTimer.value) {
        clearInterval(codeTimer.value)
        codeTimer.value = null
      }
      codeCountdown.value = 0
    } else {
      errorMessage.value = response.message || '重设密码失败，请稍后再试'
      successMessage.value = ''
    }
  } catch (error: unknown) {
    console.error('重设密码失败:', error)
    const errorMsg = error instanceof Error ? error.message : '重设密码失败，请稍后再试'
    errorMessage.value = errorMsg
  } finally {
    isLoading.value = false
  }
}

// 忘记密码 - 切换到重设密码模式
const handleForgotPassword = () => {
  switchToResetPassword()
}
// const handleSocialLogin = () => {
//   alert('这个功能不会做')
// }
</script>

<template>
  <div class="login-view">
    <div class="login-container">
      <!-- 左侧品牌信息 -->
      <!-- <div class="brand-section">
        <div class="brand-content">
          <div class="brand-logo">
            <div class="logo-icon">📰</div>
            <h1 class="logo-text">AmiNews</h1>
          </div>
          <p class="brand-description">您的个人新闻聚合平台，让信息获取更智能、更高效。</p>
          <div class="feature-list">
            <div class="feature-item">
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M20 6L9 17l-5-5" />
              </svg>
              <span>智能新闻推荐</span>
            </div>
            <div class="feature-item">
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M20 6L9 17l-5-5" />
              </svg>
              <span>多源内容聚合</span>
            </div>
            <div class="feature-item">
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M20 6L9 17l-5-5" />
              </svg>
              <span>个性化阅读体验</span>
            </div>
          </div>
        </div>
      </div> -->

      <!-- 右侧登录表单 -->
      <div class="form-section">
        <div class="form-container">
          <!-- 表单头部 -->
          <div class="form-header">
            <h2 class="form-title">
              {{ isResetPasswordMode ? '重设密码' : isLoginMode ? '登录' : '注册' }}
            </h2>
            <p class="form-subtitle">
              {{
                isResetPasswordMode
                  ? '请输入您的邮箱和新密码来重设密码'
                  : isLoginMode
                    ? '欢迎回来！请登录您的账户'
                    : '创建新账户，开始您的新闻之旅'
              }}
            </p>
          </div>

          <!-- 登录表单 -->
          <form v-if="isLoginMode" @submit.prevent="handleLogin" class="auth-form">
            <div class="input-group">
              <label class="input-label">用户名或邮箱</label>
              <input
                type="text"
                v-model="loginForm.username"
                placeholder="请输入用户名或邮箱"
                class="input-field"
                required
              />
            </div>
            <div class="input-group">
              <label class="input-label">密码</label>
              <input
                type="password"
                v-model="loginForm.password"
                placeholder="请输入密码"
                class="input-field"
                required
              />
            </div>
            <div class="form-actions">
              <button type="button" @click="handleForgotPassword" class="link-button">
                忘记密码？
              </button>
            </div>
            <button type="submit" :disabled="isLoading" class="submit-button">
              <span v-if="isLoading" class="loading-spinner"></span>
              {{ isLoading ? '登录中...' : '登录' }}
            </button>
          </form>

          <!-- 注册表单 - 移除邮箱输入框 -->
          <form v-else-if="!isResetPasswordMode" @submit.prevent="handleRegister" class="auth-form">
            <div class="input-group">
              <label class="input-label">邮箱</label>
              <input
                type="email"
                v-model="registerForm.email"
                placeholder="请输入邮箱地址"
                class="input-field"
                :class="{ 'input-error': !emailValidation.isValid && registerForm.email }"
                required
              />
              <div v-if="!emailValidation.isValid && registerForm.email" class="field-error">
                {{ emailValidation.message }}
              </div>
            </div>
            <div class="input-group">
              <label class="input-label">邮箱验证码</label>
              <div class="verification-code-input">
                <input
                  type="text"
                  v-model="registerForm.code"
                  placeholder="请输入6位验证码"
                  class="input-field code-input"
                  :class="{ 'input-error': !codeValidation.isValid && registerForm.code }"
                  maxlength="6"
                  required
                />
                <button
                  type="button"
                  @click="handleSendCode"
                  :disabled="isCodeButtonDisabled"
                  class="code-button"
                >
                  {{ codeButtonText }}
                </button>
              </div>
              <div v-if="!codeValidation.isValid && registerForm.code" class="field-error">
                {{ codeValidation.message }}
              </div>
            </div>

            <div class="input-group">
              <label class="input-label">用户名</label>
              <input
                type="text"
                v-model="registerForm.username"
                placeholder="请输入用户名（3-20位字母或数字）"
                class="input-field"
                :class="{ 'input-error': !usernameValidation.isValid && registerForm.username }"
                required
              />
              <div v-if="!usernameValidation.isValid && registerForm.username" class="field-error">
                {{ usernameValidation.message }}
              </div>
            </div>
            <div class="input-group">
              <label class="input-label">密码</label>
              <input
                type="password"
                v-model="registerForm.password"
                placeholder="请输入密码（6-20位，包含字母和数字）"
                class="input-field"
                :class="{ 'input-error': !passwordValidation.isValid && registerForm.password }"
                required
              />
              <div v-if="!passwordValidation.isValid && registerForm.password" class="field-error">
                {{ passwordValidation.message }}
              </div>
            </div>
            <div class="input-group">
              <label class="input-label">确认密码</label>
              <input
                type="password"
                v-model="registerForm.confirmPassword"
                placeholder="请再次输入密码"
                class="input-field"
                :class="{
                  'input-error': !confirmPasswordValidation.isValid && registerForm.confirmPassword,
                }"
                required
              />
              <div
                v-if="!confirmPasswordValidation.isValid && registerForm.confirmPassword"
                class="field-error"
              >
                {{ confirmPasswordValidation.message }}
              </div>
            </div>

            <button type="submit" :disabled="isLoading" class="submit-button">
              <span v-if="isLoading" class="loading-spinner"></span>
              {{ isLoading ? '注册中...' : '注册' }}
            </button>
          </form>

          <!-- 重设密码表单 -->
          <form v-else @submit.prevent="handleResetPassword" class="auth-form">
            <div class="input-group">
              <label class="input-label">邮箱</label>
              <input
                type="email"
                v-model="resetPasswordForm.email"
                placeholder="请输入注册时使用的邮箱地址"
                class="input-field"
                :class="{ 'input-error': !resetEmailValidation.isValid && resetPasswordForm.email }"
                required
              />
              <div
                v-if="!resetEmailValidation.isValid && resetPasswordForm.email"
                class="field-error"
              >
                {{ resetEmailValidation.message }}
              </div>
            </div>
            <div class="input-group">
              <label class="input-label">邮箱验证码</label>
              <div class="verification-code-input">
                <input
                  type="text"
                  v-model="resetPasswordForm.code"
                  placeholder="请输入6位验证码"
                  class="input-field code-input"
                  :class="{ 'input-error': !resetCodeValidation.isValid && resetPasswordForm.code }"
                  maxlength="6"
                  required
                />
                <button
                  type="button"
                  @click="handleSendResetCode"
                  :disabled="isResetCodeButtonDisabled"
                  class="code-button"
                >
                  {{ codeButtonText }}
                </button>
              </div>
              <div
                v-if="!resetCodeValidation.isValid && resetPasswordForm.code"
                class="field-error"
              >
                {{ resetCodeValidation.message }}
              </div>
            </div>

            <div class="input-group">
              <label class="input-label">新密码</label>
              <input
                type="password"
                v-model="resetPasswordForm.password"
                placeholder="请输入新密码（6-20位，包含字母和数字）"
                class="input-field"
                :class="{
                  'input-error': !resetPasswordValidation.isValid && resetPasswordForm.password,
                }"
                required
              />
              <div
                v-if="!resetPasswordValidation.isValid && resetPasswordForm.password"
                class="field-error"
              >
                {{ resetPasswordValidation.message }}
              </div>
            </div>
            <div class="input-group">
              <label class="input-label">确认新密码</label>
              <input
                type="password"
                v-model="resetPasswordForm.confirmPassword"
                placeholder="请再次输入新密码"
                class="input-field"
                :class="{
                  'input-error':
                    !resetConfirmPasswordValidation.isValid && resetPasswordForm.confirmPassword,
                }"
                required
              />
              <div
                v-if="!resetConfirmPasswordValidation.isValid && resetPasswordForm.confirmPassword"
                class="field-error"
              >
                {{ resetConfirmPasswordValidation.message }}
              </div>
            </div>

            <button type="submit" :disabled="isLoading" class="submit-button">
              <span v-if="isLoading" class="loading-spinner"></span>
              {{ isLoading ? '重设密码中...' : '重设密码' }}
            </button>
          </form>

          <!-- 成功信息 -->
          <div v-if="successMessage" class="success-message">
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              class="success-icon"
            >
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <polyline points="22,4 12,14.01 9,11.01"></polyline>
            </svg>
            {{ successMessage }}
          </div>

          <!-- 错误信息 -->
          <div v-if="errorMessage" class="error-message">
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              class="error-icon"
            >
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="15" y1="9" x2="9" y2="15"></line>
              <line x1="9" y1="9" x2="15" y2="15"></line>
            </svg>
            {{ errorMessage }}
          </div>

          <!-- 分隔线 -->
          <!-- <div class="divider">
            <span class="divider-text">或者</span>
          </div> -->

          <!-- 社交登录 -->
          <!-- <div class="social-login">
            <button @click="handleSocialLogin()" class="social-button google">
              <svg width="20" height="20" viewBox="0 0 24 24">
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                />
              </svg>
              Google 登录
            </button>
            <button @click="handleSocialLogin()" class="social-button github">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path
                  d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"
                />
              </svg>
              GitHub 登录
            </button>
          </div> -->

          <!-- 切换模式 -->
          <div class="mode-switch">
            <span class="switch-text">
              {{
                isResetPasswordMode ? '记起密码了？' : isLoginMode ? '还没有账户？' : '已有账户？'
              }}
            </span>
            <button @click="toggleMode" class="link-button">
              {{ isResetPasswordMode ? '返回登录' : isLoginMode ? '立即注册' : '立即登录' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-view {
  min-height: 100vh;
  /* background: linear-gradient(135deg, #2abafc 0%, #36f0b5 100%); */
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.login-container {
  width: 100%;
  max-width: 400px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: grid;
  /* grid-template-columns: 1fr 1fr; */
  min-height: 600px;
}

.brand-section {
  background: linear-gradient(135deg, #2abafc 0%, #36f0b5 100%);
  color: white;
  padding: 60px 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-content {
  text-align: center;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}

.logo-icon {
  font-size: 3rem;
  filter: brightness(0) invert(1);
}

.logo-text {
  font-size: 2.5rem;
  font-weight: 700;
  margin: 0;
}

.brand-description {
  font-size: 1.1rem;
  line-height: 1.6;
  margin-bottom: 32px;
  opacity: 0.9;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 1rem;
}

.feature-item svg {
  flex-shrink: 0;
}

.form-section {
  padding: 40px 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-container {
  width: 100%;
  max-width: 400px;
}

.form-header {
  text-align: center;
  margin-bottom: 24px;
}

.form-title {
  font-size: 1.75rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.form-subtitle {
  color: #666;
  margin: 0;
}

.auth-form {
  margin-bottom: 20px;
}

.input-group {
  margin-bottom: 16px;
}

.input-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 6px;
}

.input-field {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 0.2s ease;
}

.input-field:focus {
  outline: none;
  border-color: #2196f3;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}

.input-field.input-error {
  border-color: #d32f2f;
}

.input-field.input-error:focus {
  border-color: #d32f2f;
  box-shadow: 0 0 0 3px rgba(211, 47, 47, 0.1);
}

.field-error {
  color: #d32f2f;
  font-size: 12px;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.form-actions {
  text-align: right;
  margin-bottom: 24px;
}

.link-button {
  background: none;
  border: none;
  color: #2196f3;
  cursor: pointer;
  font-size: 14px;
  text-decoration: none;
  transition: color 0.2s ease;
}

.link-button:hover {
  color: #1976d2;
  text-decoration: underline;
}

.submit-button {
  width: 100%;
  padding: 12px 24px;
  background: #2196f3;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.submit-button:hover:not(:disabled) {
  background: #1976d2;
  transform: translateY(-1px);
}

.submit-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.divider {
  position: relative;
  text-align: center;
  margin: 24px 0;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #e5e5e5;
}

.divider-text {
  background: white;
  color: #666;
  padding: 0 16px;
  font-size: 14px;
  position: relative;
}

.social-login {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.social-button {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  background: white;
  color: #333;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  transition: all 0.2s ease;
}

.social-button:hover {
  background: #f8f9fa;
  transform: translateY(-1px);
}

.social-button.github {
  border-color: #24292e;
}

.social-button.github:hover {
  background: #24292e;
  color: white;
}

.mode-switch {
  text-align: center;
  font-size: 14px;
}

.switch-text {
  color: #666;
  margin-right: 8px;
}

.error-message {
  background-color: #ffebee;
  color: #d32f2f;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.success-message {
  background-color: #e8f5e8;
  color: #2e7d32;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.error-icon,
.success-icon {
  flex-shrink: 0;
}

/* 验证码输入框样式 */
.verification-code-input {
  display: flex;
  gap: 8px;
  align-items: center;
}

.code-input {
  flex: 1;
  margin-bottom: 0;
}

.code-button {
  padding: 12px 16px;
  background: #2196f3;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  min-width: 120px;
  text-align: center;
}

.code-button:hover:not(:disabled) {
  background: #1976d2;
}

.code-button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* 移动端响应式 */
@media (max-width: 767px) {
  .login-container {
    grid-template-columns: 1fr;
    max-width: 400px;
  }

  .brand-section {
    padding: 40px 30px;
  }

  .logo-text {
    font-size: 2rem;
  }

  .brand-description {
    font-size: 1rem;
  }

  .form-section {
    padding: 40px 30px;
  }

  .input-field {
    font-size: 16px; /* 防止iOS缩放 */
  }
}

@media (max-width: 480px) {
  .login-view {
    padding: 10px;
  }

  .brand-section,
  .form-section {
    padding: 30px 20px;
  }
}
</style>
