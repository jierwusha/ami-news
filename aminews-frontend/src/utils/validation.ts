/**
 * 表单验证工具函数
 */

/**
 * 验证用户名格式
 * @param username 用户名
 * @returns 验证结果
 */
export const validateUsername = (username: string): { isValid: boolean; message: string } => {
  if (!username) {
    return { isValid: false, message: '用户名不能为空' }
  }

  if (username.length < 3 || username.length > 20) {
    return { isValid: false, message: '用户名长度必须在3-20之间' }
  }

  if (!/^[a-zA-Z0-9]+$/.test(username)) {
    return { isValid: false, message: '用户名只能包含字母和数字' }
  }

  return { isValid: true, message: '' }
}

/**
 * 验证密码格式
 * @param password 密码
 * @returns 验证结果
 */
export const validatePassword = (password: string): { isValid: boolean; message: string } => {
  if (!password) {
    return { isValid: false, message: '密码不能为空' }
  }

  if (password.length < 6 || password.length > 20) {
    return { isValid: false, message: '密码长度必须在6-20之间' }
  }

  if (!/^(?=.*[a-zA-Z])(?=.*\d).+$/.test(password)) {
    return { isValid: false, message: '密码必须包含字母和数字' }
  }

  return { isValid: true, message: '' }
}

/**
 * 验证邮箱格式
 * @param email 邮箱
 * @returns 验证结果
 */
export const validateEmail = (email: string): { isValid: boolean; message: string } => {
  if (!email) {
    return { isValid: false, message: '邮箱不能为空' }
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email)) {
    return { isValid: false, message: '邮箱格式不正确' }
  }

  return { isValid: true, message: '' }
}

/**
 * 验证邮箱验证码格式
 * @param code 验证码
 * @returns 验证结果
 */
export const validateEmailCode = (code: string): { isValid: boolean; message: string } => {
  if (!code) {
    return { isValid: false, message: '验证码不能为空' }
  }

  if (!/^\d{6}$/.test(code)) {
    return { isValid: false, message: '验证码必须是6位数字' }
  }

  return { isValid: true, message: '' }
}

/**
 * 验证密码确认
 * @param password 密码
 * @param confirmPassword 确认密码
 * @returns 验证结果
 */
export const validatePasswordConfirm = (
  password: string,
  confirmPassword: string,
): { isValid: boolean; message: string } => {
  if (!confirmPassword) {
    return { isValid: false, message: '请确认密码' }
  }

  if (password !== confirmPassword) {
    return { isValid: false, message: '两次输入的密码不一致' }
  }

  return { isValid: true, message: '' }
}
