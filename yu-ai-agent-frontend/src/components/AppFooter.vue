<template>
  <footer class="app-footer" :class="{ dark: theme === 'dark' }">
    <div class="footer-inner">
      <span class="brand">鱼皮AI超级智能体应用平台</span>
      <span class="sep">|</span>
      <a href="#">用户协议</a>
      <span class="sep">|</span>
      <a href="#">隐私政策</a>
      <span class="sep">|</span>
      <span>友情链接</span>
      <a href="https://www.mianshiya.com" target="_blank">面试鸭</a>
      <a href="#" target="_blank">编程导航</a>
      <a href="#" target="_blank">代码小抄</a>
      <span class="sep">|</span>
      <a href="#">商务合作</a>
      <span class="sep">|</span>
      <span>站长：鱼皮</span>
    </div>
    <div class="footer-bottom">
      © {{ currentYear }} 鱼皮AI超级智能体应用平台
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'

defineProps({
  theme: { type: String, default: 'light' }
})

const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
.app-footer {
  background: transparent;
  padding: 16px 20px 12px;
  text-align: center;
  color: rgba(0, 0, 0, 0.35);
  font-size: 12px;
  line-height: 2;
  width: 100%;
  position: relative;
  z-index: 2;
}

.app-footer.dark {
  color: rgba(255, 255, 255, 0.3);
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
}

.footer-inner a {
  color: rgba(0, 0, 0, 0.35);
  text-decoration: none;
  margin: 0 2px;
  transition: color 0.2s;
}
.app-footer.dark .footer-inner a {
  color: rgba(255, 255, 255, 0.3);
}
.footer-inner a:hover {
  color: #007bff;
}

.sep {
  margin: 0 6px;
  color: rgba(0, 0, 0, 0.12);
}
.app-footer.dark .sep {
  color: rgba(255, 255, 255, 0.12);
}

.footer-bottom {
  margin-top: 6px;
  color: rgba(0, 0, 0, 0.22);
  font-size: 11px;
}
.app-footer.dark .footer-bottom {
  color: rgba(255, 255, 255, 0.18);
}
</style>
