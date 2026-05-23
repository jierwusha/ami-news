<script setup lang="ts">
import { ref } from 'vue'

// 设置数据
const settings = ref({
  theme: 'light',
  language: 'zh-CN',
  autoRefresh: true,
  refreshInterval: 30,
  notifications: {
    desktop: true,
    email: false,
    sound: true,
  },
  reading: {
    fontSize: 'medium',
    fontFamily: 'system',
    lineHeight: 'normal',
  },
  privacy: {
    analytics: false,
    crashReports: true,
  },
})

// 保存设置
const saveSettings = () => {
  // TODO: 实现保存到后端或本地存储
  console.log('保存设置:', settings.value)
  alert('设置已保存！')
}

// 重置设置
const resetSettings = () => {
  if (confirm('确定要重置所有设置吗？')) {
    settings.value = {
      theme: 'light',
      language: 'zh-CN',
      autoRefresh: true,
      refreshInterval: 30,
      notifications: {
        desktop: true,
        email: false,
        sound: true,
      },
      reading: {
        fontSize: 'medium',
        fontFamily: 'system',
        lineHeight: 'normal',
      },
      privacy: {
        analytics: false,
        crashReports: true,
      },
    }
  }
}
</script>

<template>
  <div class="settings-view">
    <main class="main-content">
      <!-- Header -->
      <header class="header">
        <div class="header-left">
          <h1 class="page-title">设置</h1>
          <!-- <p class="page-subtitle">管理您的AmiNews偏好设置</p> -->
        </div>
        <div class="header-actions">
          <button class="btn btn-secondary" @click="resetSettings">重置设置</button>
          <button class="btn btn-primary" @click="saveSettings">保存设置</button>
        </div>
      </header>

      <!-- Content -->
      <div class="content">
        <div class="settings-grid">
          <!-- 外观设置 -->
          <div class="settings-section">
            <h2 class="section-title">外观</h2>
            <div class="setting-item">
              <label class="setting-label">主题</label>
              <select v-model="settings.theme" class="setting-select">
                <option value="light">浅色</option>
                <option value="dark">深色</option>
                <option value="auto">跟随系统</option>
              </select>
            </div>
            <div class="setting-item">
              <label class="setting-label">语言</label>
              <select v-model="settings.language" class="setting-select">
                <option value="zh-CN">简体中文</option>
                <option value="zh-TW">繁体中文</option>
                <option value="en-US">English</option>
              </select>
            </div>
          </div>

          <!-- 阅读设置 -->
          <div class="settings-section">
            <h2 class="section-title">阅读体验</h2>
            <div class="setting-item">
              <label class="setting-label">字体大小</label>
              <select v-model="settings.reading.fontSize" class="setting-select">
                <option value="small">小</option>
                <option value="medium">中</option>
                <option value="large">大</option>
              </select>
            </div>
            <div class="setting-item">
              <label class="setting-label">字体</label>
              <select v-model="settings.reading.fontFamily" class="setting-select">
                <option value="system">系统默认</option>
                <option value="serif">衬线字体</option>
                <option value="sans-serif">无衬线字体</option>
                <option value="monospace">等宽字体</option>
              </select>
            </div>
            <div class="setting-item">
              <label class="setting-label">行高</label>
              <select v-model="settings.reading.lineHeight" class="setting-select">
                <option value="compact">紧凑</option>
                <option value="normal">标准</option>
                <option value="relaxed">宽松</option>
              </select>
            </div>
          </div>

          <!-- 刷新设置 -->
          <div class="settings-section">
            <h2 class="section-title">内容刷新</h2>
            <div class="setting-item">
              <label class="setting-label">
                <input type="checkbox" v-model="settings.autoRefresh" class="setting-checkbox" />
                自动刷新内容
              </label>
            </div>
            <div class="setting-item" v-if="settings.autoRefresh">
              <label class="setting-label">刷新间隔（分钟）</label>
              <input
                type="number"
                v-model="settings.refreshInterval"
                min="5"
                max="120"
                class="setting-input"
              />
            </div>
          </div>

          <!-- 通知设置 -->
          <div class="settings-section">
            <h2 class="section-title">通知</h2>
            <div class="setting-item">
              <label class="setting-label">
                <input
                  type="checkbox"
                  v-model="settings.notifications.desktop"
                  class="setting-checkbox"
                />
                桌面通知
              </label>
            </div>
            <div class="setting-item">
              <label class="setting-label">
                <input
                  type="checkbox"
                  v-model="settings.notifications.email"
                  class="setting-checkbox"
                />
                邮件通知
              </label>
            </div>
            <div class="setting-item">
              <label class="setting-label">
                <input
                  type="checkbox"
                  v-model="settings.notifications.sound"
                  class="setting-checkbox"
                />
                声音提醒
              </label>
            </div>
          </div>

          <!-- 隐私设置 -->
          <div class="settings-section">
            <h2 class="section-title">隐私</h2>
            <div class="setting-item">
              <label class="setting-label">
                <input
                  type="checkbox"
                  v-model="settings.privacy.analytics"
                  class="setting-checkbox"
                />
                允许匿名使用统计
              </label>
              <p class="setting-description">帮助我们改进产品，不会收集个人信息</p>
            </div>
            <div class="setting-item">
              <label class="setting-label">
                <input
                  type="checkbox"
                  v-model="settings.privacy.crashReports"
                  class="setting-checkbox"
                />
                自动发送崩溃报告
              </label>
              <p class="setting-description">帮助我们修复bug和提升稳定性</p>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.settings-view {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-content {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  background: white;
  border-bottom: 1px solid #e5e5e5;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: var(--z-header);
  flex-shrink: 0;
  height: 80px;
}

.header-left h1 {
  font-size: 1.75rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 0.875rem;
  color: #666;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.btn-primary {
  background: #2196f3;
  color: white;
  border-color: #2196f3;
}

.btn-primary:hover {
  background: #1976d2;
  border-color: #1976d2;
}

.btn-secondary {
  background: white;
  color: #666;
  border-color: #e5e5e5;
}

.btn-secondary:hover {
  background: #f5f5f5;
  color: #333;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
  background: #fafafa;
}

.settings-grid {
  max-width: 800px;
  margin: 0 auto;
  display: grid;
  gap: 24px;
}

.settings-section {
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e5e5;
  padding: 24px;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.setting-item {
  margin-bottom: 20px;
}

.setting-item:last-child {
  margin-bottom: 0;
}

.setting-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
  cursor: pointer;
}

.setting-label input[type='checkbox'] {
  margin-right: 8px;
}

.setting-select,
.setting-input {
  width: 100%;
  max-width: 200px;
  padding: 8px 12px;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  transition: border-color 0.2s ease;
}

.setting-select:focus,
.setting-input:focus {
  outline: none;
  border-color: #2196f3;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}

.setting-checkbox {
  width: 16px;
  height: 16px;
  accent-color: #2196f3;
}

.setting-description {
  font-size: 12px;
  color: #666;
  margin: 4px 0 0 24px;
}

/* 移动端响应式 */
@media (max-width: 767px) {
  .header {
    padding: 0 16px;
    height: 70px;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    justify-content: center;
  }

  .header-actions {
    align-self: flex-end;
  }

  .content {
    padding: 16px;
  }

  .btn {
    padding: 6px 12px;
    font-size: 13px;
  }

  .settings-section {
    padding: 20px;
  }

  .setting-select,
  .setting-input {
    max-width: 100%;
  }
}

@media (min-width: 768px) and (max-width: 1199px) {
  .content {
    padding: 24px;
  }
}
</style>
