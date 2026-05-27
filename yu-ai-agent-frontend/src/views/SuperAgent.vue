<template>
  <div class="super-agent-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">AI超级智能体</h1>
      <div></div>
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
          ai-type="super"
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
import { chatWithManus, getConversations, deleteConversation, createConversation, getChatHistory } from '../api'

useHead({
  title: 'AI超级智能体 - 鱼皮AI超级智能体应用平台',
  meta: [
    {
      name: 'description',
      content: 'AI超级智能体是鱼皮AI超级智能体应用平台的全能助手，能解答各类专业问题，提供精准建议和解决方案'
    },
    {
      name: 'keywords',
      content: 'AI超级智能体,智能助手,专业问答,AI问答,专业建议,鱼皮,AI智能体'
    }
  ]
})

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const conversations = ref([])
const connectionStatus = ref('disconnected')
let eventSource = null

const generateChatId = () => 'agent_' + Math.random().toString(36).substring(2, 10)

const addMessage = (content, isUser, type = '') => {
  messages.value.push({ content, isUser, type, time: new Date().getTime() })
}

const sendMessage = (message) => {
  addMessage(message, true, 'user-question')
  if (eventSource) eventSource.close()

  connectionStatus.value = 'connecting'
  let messageBuffer = []
  let lastBubbleTime = Date.now()
  let isFirstResponse = true
  const chineseEndPunctuation = ['。', '！', '？', '…']
  const minBubbleInterval = 800

  const createBubble = (content, type = 'ai-answer') => {
    if (!content.trim()) return
    const now = Date.now()
    const timeSinceLastBubble = now - lastBubbleTime
    if (isFirstResponse) {
      addMessage(content, false, type)
      isFirstResponse = false
    } else if (timeSinceLastBubble < minBubbleInterval) {
      setTimeout(() => { addMessage(content, false, type) }, minBubbleInterval - timeSinceLastBubble)
    } else {
      addMessage(content, false, type)
    }
    lastBubbleTime = now
    messageBuffer = []
  }

  eventSource = chatWithManus(message)

  eventSource.onmessage = (event) => {
    const data = event.data
    if (data && data !== '[DONE]') {
      messageBuffer.push(data)
      const combinedText = messageBuffer.join('')
      const lastChar = data.charAt(data.length - 1)
      const hasCompleteSentence = chineseEndPunctuation.includes(lastChar) || data.includes('\n\n')
      if (hasCompleteSentence || combinedText.length > 40) {
        createBubble(combinedText)
      }
    }
    if (data === '[DONE]') {
      if (messageBuffer.length > 0) {
        createBubble(messageBuffer.join(''), 'ai-final')
      }
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
  // 如果已有一个没发消息的"新对话"，直接切过去，不重复创建
  const empty = conversations.value.find(c => c.title === '新对话')
  if (empty) {
    chatId.value = empty.chatId
    messages.value = []
    addMessage('你好，我是AI超级智能体。我可以解答各类问题，提供专业建议，请问有什么可以帮助你的吗？', false)
    return
  }
  chatId.value = generateChatId()
  messages.value = []
  addMessage('你好，我是AI超级智能体。我可以解答各类问题，提供专业建议，请问有什么可以帮助你的吗？', false)
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
      addMessage('你好，我是AI超级智能体。我可以解答各类问题，提供专业建议，请问有什么可以帮助你的吗？', false)
    }
  } catch (e) {
    addMessage('你好，我是AI超级智能体。我可以解答各类问题，提供专业建议，请问有什么可以帮助你的吗？', false)
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
  addMessage('你好，我是AI超级智能体。我可以解答各类问题，提供专业建议，请问有什么可以帮助你的吗？', false)
  loadConversations()
})

onBeforeUnmount(() => {
  if (eventSource) eventSource.close()
})
</script>

<style scoped>
.super-agent-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f9fbff;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #3f51b5;
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
.back-button:before { content: '←'; margin-right: 8px; }

.title { font-size: 20px; font-weight: bold; margin: 0; }

.main-area { display: flex; flex: 1; overflow: hidden; }

.chat-area { flex: 1; overflow: hidden; position: relative; }

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
