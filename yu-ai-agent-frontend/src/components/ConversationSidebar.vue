<template>
  <div class="sidebar" :class="{ collapsed: isCollapsed }">
    <div class="sidebar-toggle" @click="toggle">
      {{ isCollapsed ? '▶' : '◀' }}
    </div>
    <div v-if="!isCollapsed" class="sidebar-content">
      <div class="sidebar-header">
        <span class="sidebar-title">会话列表</span>
        <button class="new-chat-btn" @click="$emit('newChat')">+ 新对话</button>
      </div>
      <div class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: conv.chatId === activeChatId }"
          @click="$emit('select', conv.chatId)"
        >
          <span class="conv-title">{{ conv.title }}</span>
          <button class="conv-delete" @click.stop="$emit('delete', conv.id)">×</button>
        </div>
        <div v-if="conversations.length === 0" class="empty">暂无会话</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  conversations: { type: Array, default: () => [] },
  activeChatId: { type: String, default: '' }
})

defineEmits(['newChat', 'select', 'delete'])

const isCollapsed = ref(false)
const toggle = () => { isCollapsed.value = !isCollapsed.value }
</script>

<style scoped>
.sidebar {
  width: 200px;
  background: #1a1a2e;
  border-right: 1px solid rgba(255,255,255,0.06);
  display: flex;
  flex-direction: row;
  transition: width 0.2s;
  flex-shrink: 0;
}
.sidebar.collapsed {
  width: 32px;
}
.sidebar-toggle {
  width: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255,255,255,0.3);
  cursor: pointer;
  font-size: 12px;
  flex-shrink: 0;
  border-right: 1px solid rgba(255,255,255,0.04);
}
.sidebar-toggle:hover { color: rgba(255,255,255,0.6); }
.sidebar-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sidebar-header {
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.sidebar-title {
  color: rgba(255,255,255,0.6);
  font-size: 13px;
  font-weight: 500;
}
.new-chat-btn {
  background: none;
  border: 1px solid rgba(255,255,255,0.12);
  color: rgba(255,255,255,0.6);
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 4px;
  cursor: pointer;
}
.new-chat-btn:hover { border-color: rgba(255,255,255,0.3); color: #fff; }
.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}
.conv-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  color: rgba(255,255,255,0.55);
  font-size: 13px;
  transition: background 0.15s;
}
.conv-item:hover { background: rgba(255,255,255,0.04); }
.conv-item.active { background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.85); }
.conv-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv-delete {
  background: none;
  border: none;
  color: rgba(255,255,255,0.2);
  font-size: 14px;
  cursor: pointer;
  padding: 0 4px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s;
}
.conv-item:hover .conv-delete { opacity: 1; }
.conv-delete:hover { color: rgba(255,107,107,0.8); }
.empty {
  color: rgba(255,255,255,0.2);
  font-size: 13px;
  text-align: center;
  padding: 20px;
}
</style>
