import axios from 'axios'

const API_BASE_URL = '/api'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000
})

// 获取当前用户信息（用于判断登录态）
export const getMe = () => request.get('/user/me')

// 封装SSE连接
export const connectSSE = (url, params, onMessage, onError) => {
  // 构建带参数的URL
  const queryString = Object.keys(params)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  
  const fullUrl = `${API_BASE_URL}${url}?${queryString}`
  
  // 创建EventSource
  const eventSource = new EventSource(fullUrl, { withCredentials: true })
  
  eventSource.onmessage = event => {
    let data = event.data
    
    // 检查是否是特殊标记
    if (data === '[DONE]') {
      if (onMessage) onMessage('[DONE]')
    } else {
      // 处理普通消息
      if (onMessage) onMessage(data)
    }
  }
  
  eventSource.onerror = error => {
    if (onError) onError(error)
    eventSource.close()
  }
  
  // 返回eventSource实例，以便后续可以关闭连接
  return eventSource
}

// AI恋爱大师聊天
export const chatWithLoveApp = (message, chatId) => {
  return connectSSE('/ai/love_app/chat/sse', { message, chatId })
}

// AI超级智能体聊天
export const chatWithManus = (message, chatId) => {
  return connectSSE('/ai/manus/chat', { message, chatId })
}

// 用户登录注册
// 会话（LoveApp）
export const getConversations = () => request.get('/conversation/list')
export const createConversation = (chatId, title) => request.post('/conversation/create', null, { params: { chatId, title } })
export const deleteConversation = (id) => request.delete(`/conversation/${id}`)
export const getChatHistory = (chatId) => request.get(`/conversation/${chatId}/history`)

// 会话（Manus）
export const getManusConversations = () => request.get('/conversation/manus/list')
export const getManusChatHistory = (chatId) => request.get(`/conversation/manus/${chatId}/history`)

export const login = (data) => request.post('/user/login', data)
export const register = (data) => request.post('/user/register', data)
export const logout = () => request.post('/user/logout')

export default {
  chatWithLoveApp,
  chatWithManus,
  login,
  register,
  logout,
  getMe
}