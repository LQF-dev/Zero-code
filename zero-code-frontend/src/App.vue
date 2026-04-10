<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
</script>

<template>
  <a-config-provider
    :theme="{
      token: {
        colorPrimary: '#b86a42',
        colorInfo: '#b86a42',
        colorSuccess: '#5f8f63',
        colorWarning: '#c28b3f',
        colorError: '#ff4d4f',
        borderRadius: 12,
        fontFamily: 'PingFang SC, Hiragino Sans GB, Microsoft YaHei, sans-serif',
      },
    }"
  >
    <BasicLayout />
  </a-config-provider>
</template>

<style>
:root {
  --zc-bg-page: #f7f6f3;
  --zc-bg-card: #ffffff;
  --zc-bg-soft: #f3f0ea;
  --zc-text-primary: #2b2925;
  --zc-text-secondary: #8b8478;
  --zc-border: #e7e1d7;
  --zc-primary: #b86a42;
  --zc-shadow: 0 10px 30px rgba(59, 46, 31, 0.08);
}

* {
  box-sizing: border-box;
}

html,
body,
#app {
  margin: 0;
  min-height: 100%;
}

body {
  color: var(--zc-text-primary);
  background:
    radial-gradient(circle at 30% -10%, rgba(196, 150, 120, 0.18), transparent 40%),
    linear-gradient(180deg, #fbfaf8 0%, #f6f3ee 100%);
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
}

a {
  color: inherit;
}
</style>
