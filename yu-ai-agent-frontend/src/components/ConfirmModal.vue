<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('cancel')">
    <div class="modal-card">
      <div class="modal-title">{{ title }}</div>
      <div class="modal-body">{{ message }}</div>
      <div class="modal-actions">
        <button class="btn-cancel" @click="$emit('cancel')">取消</button>
        <button class="btn-confirm" @click="$emit('confirm')">确认删除</button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  visible: Boolean,
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '确定要执行此操作吗？' }
})
defineEmits(['confirm', 'cancel'])
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}
.modal-card {
  background: #1e1e36;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 14px;
  padding: 28px 32px 24px;
  min-width: 320px;
  max-width: 400px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.4);
}
.modal-title {
  color: rgba(255,255,255,0.9);
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 12px;
}
.modal-body {
  color: rgba(255,255,255,0.5);
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 24px;
}
.modal-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
.btn-cancel {
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.15);
  background: transparent;
  color: rgba(255,255,255,0.6);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-cancel:hover { border-color: rgba(255,255,255,0.3); color: #fff; }
.btn-confirm {
  padding: 8px 20px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #ff4757, #ff6b81);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}
.btn-confirm:hover { opacity: 0.85; }
</style>
