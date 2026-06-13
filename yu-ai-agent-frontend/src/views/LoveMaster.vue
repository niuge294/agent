<template>
  <div class="love-master-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">AI恋爱大师</h1>
      <div class="header-right"></div>
    </div>

    <div class="main-area">
      <ConversationSidebar
        :conversations="conversations"
        :activeChatId="chatId"
        @newChat="handleNewChat"
        @select="handleSelect"
        @delete="handleDelete"
      />
      <div class="chat-area">
        <ChatRoom
          :messages="messages"
          :connection-status="connectionStatus"
          ai-type="love"
          @send-message="sendMessage"
        />
      </div>
    </div>

    <div class="footer-container">
      <AppFooter />
    </div>

    <ConfirmModal
      :visible="deleteModalVisible"
      title="删除会话"
      message="删除后聊天记录也将清除，确定删除？"
      @confirm="confirmDelete"
      @cancel="deleteModalVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import ConversationSidebar from '../components/ConversationSidebar.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import AppFooter from '../components/AppFooter.vue'
import { chatWithLoveApp, getConversations, deleteConversation, createConversation, getChatHistory } from '../api'

useHead({
  title: 'AI恋爱大师 - 鱼皮AI超级智能体应用平台',
  meta: [
    {
      name: 'description',
      content: 'AI恋爱大师是鱼皮AI超级智能体应用平台的专业情感顾问，帮你解答各种恋爱问题，提供情感建议'
    },
    {
      name: 'keywords',
      content: 'AI恋爱大师,情感顾问,恋爱咨询,AI聊天,情感问题,鱼皮,AI智能体'
    }
  ]
})

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const conversations = ref([])
const connectionStatus = ref('disconnected')
let eventSource = null

const generateChatId = () => 'love_' + Math.random().toString(36).substring(2, 10)

const addMessage = (content, isUser) => {
  messages.value.push({ content, isUser, time: new Date().getTime() })
}

const sendMessage = (message) => {
  addMessage(message, true)
  if (eventSource) eventSource.close()

  const aiMessageIndex = messages.value.length
  addMessage('', false)

  connectionStatus.value = 'connecting'
  eventSource = chatWithLoveApp(message, chatId.value)

  eventSource.onmessage = (event) => {
    const data = event.data
    if (data && data !== '[DONE]') {
      if (aiMessageIndex < messages.value.length) {
        messages.value[aiMessageIndex].content += data
      }
    }
    if (data === '[DONE]') {
      connectionStatus.value = 'disconnected'
      eventSource.close()
      loadConversations()
    }
  }
  eventSource.onerror = (error) => {
    console.error('SSE Error:', error)
    connectionStatus.value = 'error'
    eventSource.close()
  }
}

const loadConversations = async () => {
  try {
    const res = await getConversations()
    conversations.value = res.data
  } catch (e) { /* ignore */ }
}

const handleNewChat = async () => {
  // 关闭当前 SSE 连接
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  // 先拉最新列表，避免用脏数据判断"新对话"是否已存在
  await loadConversations()
  const empty = conversations.value.find(c => c.title === '新对话')
  if (empty) {
    chatId.value = empty.chatId
    messages.value = []
    addMessage('欢迎来到AI恋爱大师，请告诉我你的恋爱问题，我会尽力给予帮助和建议。', false)
    return
  }
  chatId.value = generateChatId()
  messages.value = []
  addMessage('欢迎来到AI恋爱大师，请告诉我你的恋爱问题，我会尽力给予帮助和建议。', false)
  await createConversation(chatId.value, '新对话')
  loadConversations()
}

const handleSelect = async (chatIdVal) => {
  chatId.value = chatIdVal
  messages.value = []
  try {
    const res = await getChatHistory(chatIdVal)
    if (res.data && res.data.length > 0) {
      res.data.forEach(msg => {
        messages.value.push({
          content: msg.content,
          isUser: msg.role === 'user',
          time: new Date().getTime()
        })
      })
    } else {
      addMessage('欢迎来到AI恋爱大师，请告诉我你的恋爱问题，我会尽力给予帮助和建议。', false)
    }
  } catch (e) {
    addMessage('欢迎来到AI恋爱大师，请告诉我你的恋爱问题，我会尽力给予帮助和建议。', false)
  }
}

const deleteTargetId = ref(null)
const deleteModalVisible = ref(false)

const handleDelete = (id) => {
  deleteTargetId.value = id
  deleteModalVisible.value = true
}

const confirmDelete = async () => {
  await deleteConversation(deleteTargetId.value)
  deleteModalVisible.value = false
  loadConversations()
}

const goBack = () => router.push('/')

onMounted(() => {
  chatId.value = generateChatId()
  addMessage('欢迎来到AI恋爱大师，请告诉我你的恋爱问题，我会尽力给予帮助和建议。', false)
  loadConversations()
})

onBeforeUnmount(() => {
  if (eventSource) eventSource.close()
})
</script>

<style scoped>
.love-master-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #fff9f9;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #ff6b8b;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: opacity 0.2s;
}
.back-button:hover { opacity: 0.8; }
.back-button:before {
  content: '←';
  margin-right: 8px;
}

.title {
  font-size: 20px;
  font-weight: bold;
  margin: 0;
}
.header-right { width: 60px; }

.main-area {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.chat-area {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.footer-container { margin-top: auto; }

@media (max-width: 768px) {
  .header { padding: 12px 16px; }
  .title { font-size: 18px; }
}

@media (max-width: 480px) {
  .header { padding: 10px 12px; }
  .title { font-size: 16px; }
}
</style>
