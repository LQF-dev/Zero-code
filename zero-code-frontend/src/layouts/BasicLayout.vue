<template>
  <a-layout class="basic-layout">
    <GlobalHeader />
    <a-layout-content class="main-content">
      <router-view />
    </a-layout-content>
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: transparent;
}

.main-content {
  width: min(1240px, calc(100vw - 32px));
  margin: 24px auto 0;
  padding-bottom: 24px;
  background: transparent;
}

@media (max-width: 768px) {
  .main-content {
    width: calc(100vw - 24px);
    margin-top: 16px;
    padding-bottom: 12px;
  }
}
</style>
