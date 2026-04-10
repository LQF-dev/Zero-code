<template>
  <a-layout-header class="header">
    <div class="header-inner">
      <RouterLink to="/" class="brand-link">
        <div class="brand">
          <img class="logo" src="@/assets/logo-ai-studio.svg" alt="Zero Code Studio" />
          <div class="brand-text">
            <div class="site-title">Zero Code Studio</div>
            <div class="site-subtitle">AI 应用生成平台</div>
          </div>
        </div>
      </RouterLink>

      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="horizontal"
        :items="menuItems"
        class="menu"
        @click="handleMenuClick"
      />

      <div class="mode-switcher">
        <span class="mode-label">全局模式</span>
        <a-segmented
          v-model:value="globalGenerationMode"
          :options="generationModeOptions"
          size="small"
          @change="onGlobalModeChange"
        />
      </div>

      <div class="user-zone">
        <div v-if="loginUserStore.loginUser.id">
          <a-dropdown>
            <a-space class="user-space">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              {{ loginUserStore.loginUser.userName ?? '无名' }}
            </a-space>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="doLogout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div v-else>
          <a-button type="primary" href="/user/login">登录</a-button>
        </div>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import { LogoutOutlined, HomeOutlined } from '@ant-design/icons-vue'
import {
  getGlobalChatGenMode,
  setGlobalChatGenMode,
  type ChatGenMode,
} from '@/utils/chatGenMode'

const loginUserStore = useLoginUserStore()
const router = useRouter()
const selectedKeys = ref<string[]>([router.currentRoute.value.path])
const globalGenerationMode = ref<ChatGenMode>(getGlobalChatGenMode())
const generationModeOptions = [
  { label: '标准模式', value: 'classic' },
  { label: '工作流模式', value: 'workflow' },
]

router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    label: '应用管理',
    title: '应用管理',
  },
]

const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  if (key.startsWith('/')) {
    router.push(key)
  }
}

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

const onGlobalModeChange = (value: string | number) => {
  const mode: ChatGenMode = value === 'workflow' ? 'workflow' : 'classic'
  globalGenerationMode.value = mode
  setGlobalChatGenMode(mode)
}
</script>

<style scoped>
.header {
  width: min(1240px, calc(100vw - 32px));
  height: 68px;
  line-height: 68px;
  margin: 16px auto 0;
  border-radius: 14px;
  padding: 0 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--zc-border);
  backdrop-filter: blur(8px);
  box-shadow: var(--zc-shadow);
}

.header-inner {
  display: grid;
  grid-template-columns: 280px 1fr auto auto;
  align-items: center;
  gap: 16px;
  height: 100%;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo {
  height: 34px;
  width: 34px;
  border-radius: 8px;
}

.site-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--zc-text-primary);
  line-height: 1.2;
}

.site-subtitle {
  font-size: 12px;
  color: var(--zc-text-secondary);
  line-height: 1.3;
}

.menu {
  background: transparent;
  border-bottom: none !important;
}

.menu :deep(.ant-menu-item),
.menu :deep(.ant-menu-submenu-title) {
  color: var(--zc-text-secondary);
}

.menu :deep(.ant-menu-item-selected) {
  color: var(--zc-primary) !important;
}

.menu :deep(.ant-menu-item-selected::after),
.menu :deep(.ant-menu-item-active::after) {
  border-bottom-color: var(--zc-primary) !important;
}

.user-space {
  color: var(--zc-text-primary);
  cursor: pointer;
}

.mode-switcher {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mode-label {
  font-size: 12px;
  color: var(--zc-text-secondary);
}

@media (max-width: 900px) {
  .header {
    width: calc(100vw - 24px);
    height: auto;
    line-height: normal;
    padding: 12px;
  }

  .header-inner {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .mode-switcher {
    justify-self: end;
  }

  .user-zone {
    justify-self: end;
  }
}
</style>
