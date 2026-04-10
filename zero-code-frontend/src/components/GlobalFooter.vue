<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <span class="brand">Zero Code Studio</span>
      <span class="divider">|</span>
      <span class="text">AI 驱动的网站应用构建平台</span>
      <span class="divider">|</span>
      <span class="text">© {{ currentYear }} All rights reserved.</span>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
const currentYear = new Date().getFullYear()
</script>

<style scoped>
.footer {
  width: min(1240px, calc(100vw - 32px));
  margin: 0 auto 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--zc-border);
  text-align: center;
  padding: 10px 16px;
}

.footer-content {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  color: var(--zc-text-secondary);
  font-size: 13px;
  flex-wrap: wrap;
}

.brand {
  color: var(--zc-text-primary);
  font-weight: 600;
}

.divider {
  opacity: 0.4;
}

@media (max-width: 768px) {
  .footer {
    width: calc(100vw - 24px);
  }

  .divider {
    display: none;
  }
}
</style>
