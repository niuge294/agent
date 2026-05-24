<template>
  <div class="login-container">
    <div class="cyber-circles">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <div class="login-card">
      <div class="card-header">
        <div class="avatar">
          <span class="avatar-icon">AI</span>
        </div>
        <h2 class="card-title">{{ isLogin ? '欢迎回来' : '创建账号' }}</h2>
        <p class="card-subtitle">{{ isLogin ? '登录后继续使用AI智能体' : '注册即可体验全部AI功能' }}</p>
      </div>

      <form class="login-form" @submit.prevent="handleSubmit">
        <div class="form-group">
          <label class="form-label" for="phone">手机号</label>
          <input
            id="phone"
            v-model="phone"
            type="tel"
            class="form-input"
            placeholder="请输入手机号"
            maxlength="11"
            autocomplete="tel"
            :disabled="loading"
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="password">密码</label>
          <div class="password-wrapper">
            <input
              id="password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              class="form-input"
              placeholder="请输入密码（6-20位）"
              maxlength="20"
              autocomplete="current-password"
              :disabled="loading"
            />
            <button type="button" class="toggle-pwd" @click="showPassword = !showPassword">
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>

        <div v-if="errorMsg" class="form-error">{{ errorMsg }}</div>

        <button
          type="submit"
          class="submit-btn"
          :class="{ loading: loading }"
          :disabled="loading || !phone || !password"
        >
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '处理中...' : (isLogin ? '登 录' : '注 册') }}
        </button>
      </form>

      <div class="card-footer">
        <span class="switch-text">{{ isLogin ? '还没有账号？' : '已有账号？' }}</span>
        <a class="switch-link" href="#" @click.prevent="toggleMode">
          {{ isLogin ? '立即注册' : '去登录' }}
        </a>
      </div>
    </div>

    <AppFooter theme="dark" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useHead } from '@vueuse/head'
import { login, register } from '../api/index.js'
import AppFooter from '../components/AppFooter.vue'

useHead({
  title: '登录 - 鱼皮AI超级智能体应用平台',
  meta: [{ name: 'description', content: '登录鱼皮AI超级智能体应用平台，体验AI恋爱大师和AI超级智能体' }]
})

const router = useRouter()
const route = useRoute()

const isLogin = ref(true)
const phone = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')
const showPassword = ref(false)

const toggleMode = () => {
  isLogin.value = !isLogin.value
  errorMsg.value = ''
}

const handleSubmit = async () => {
  errorMsg.value = ''
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    errorMsg.value = '请输入正确的手机号'
    return
  }
  if (password.value.length < 6) {
    errorMsg.value = '密码长度不能少于6位'
    return
  }

  loading.value = true
  try {
    const api = isLogin.value ? login : register
    const res = await api({ phone: phone.value, password: password.value })
    localStorage.setItem('user', JSON.stringify(res.data))
    const redirect = route.query.redirect || '/love-master'
    router.push(redirect)
  } catch (err) {
    errorMsg.value = err.response?.data?.message || '网络错误，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #111122;
  position: relative;
  overflow: hidden;
  padding: 20px;
}

/* 背景动画圆圈 — 复用 Home 的 */
.cyber-circles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.07;
  filter: blur(60px);
}
.circle-1 { width: 400px; height: 400px; background: linear-gradient(135deg, #00f0ff, #9000ff); top: -100px; right: -100px; animation: float 12s ease-in-out infinite; }
.circle-2 { width: 300px; height: 300px; background: linear-gradient(135deg, #9000ff, #ff00d4); bottom: -80px; left: -80px; animation: float 15s ease-in-out infinite alternate; }
.circle-3 { width: 200px; height: 200px; background: linear-gradient(135deg, #ff00d4, #00f0ff); top: 50%; left: 50%; animation: float 20s ease-in-out infinite reverse; }

@keyframes float {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  33% { transform: translate(30px, -30px) rotate(3deg); }
  66% { transform: translate(-20px, 20px) rotate(-2deg); }
}

/* 卡片 */
.login-card {
  position: relative;
  width: 100%;
  max-width: 400px;
  background: rgba(17, 23, 41, 0.85);
  backdrop-filter: blur(16px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 240, 255, 0.12);
  padding: 40px 32px;
  z-index: 1;
}

/* 头部 */
.card-header {
  text-align: center;
  margin-bottom: 32px;
}
.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #00f0ff, #9000ff);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 0 24px rgba(0, 240, 255, 0.3);
}
.avatar-icon {
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 1px;
}
.card-title {
  color: #edf7ff;
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px;
}
.card-subtitle {
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  margin: 0;
}

/* 表单 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.form-label {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  font-weight: 500;
}
.form-input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  color: #edf7ff;
  font-size: 16px;
  outline: none;
  transition: border-color 0.3s, box-shadow 0.3s;
  box-sizing: border-box;
}
.form-input:focus {
  border-color: #00f0ff;
  box-shadow: 0 0 0 3px rgba(0, 240, 255, 0.12);
}
.form-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.form-input::placeholder {
  color: rgba(255, 255, 255, 0.25);
}

.password-wrapper {
  position: relative;
}
.toggle-pwd {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
}
.toggle-pwd:hover {
  color: rgba(255, 255, 255, 0.7);
}

/* 错误提示 */
.form-error {
  color: #ff6b6b;
  font-size: 14px;
  padding: 10px 14px;
  background: rgba(255, 107, 107, 0.1);
  border-radius: 8px;
  border: 1px solid rgba(255, 107, 107, 0.2);
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(90deg, #0088ff, #00b2ff);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: opacity 0.3s, transform 0.15s;
  position: relative;
  overflow: hidden;
}
.submit-btn:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
}
.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}
.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.submit-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  transition: left 0.5s;
}
.submit-btn:hover:not(:disabled)::before {
  left: 100%;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 底部 */
.card-footer {
  text-align: center;
  margin-top: 24px;
}
.switch-text {
  color: rgba(255, 255, 255, 0.4);
  font-size: 14px;
}
.switch-link {
  color: #00f0ff;
  font-size: 14px;
  text-decoration: none;
  margin-left: 4px;
  transition: color 0.3s;
}
.switch-link:hover {
  color: #00b2ff;
}
</style>
