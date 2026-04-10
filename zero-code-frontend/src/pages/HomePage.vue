<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const userPrompt = ref('')
const creating = ref(false)

const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

const promptIdeas = [
  {
    label: '个人博客网站',
    prompt:
      '创建一个现代化的个人博客网站，包含文章列表、详情页、分类标签、搜索功能、评论系统和个人简介页面。采用简洁设计，支持响应式布局。',
  },
  {
    label: '企业官网',
    prompt:
      '设计一个专业的企业官网，包含公司介绍、产品服务展示、新闻资讯、联系我们等页面，采用商务简约风格并支持移动端。',
  },
  {
    label: '在线商城',
    prompt:
      '构建一个在线商城，包含商品展示、购物车、用户登录注册、订单管理、支付结算等功能，界面清晰且易于浏览。',
  },
  {
    label: '作品展示网站',
    prompt:
      '制作一个作品展示网站，包含作品画廊、项目详情、个人介绍和联系方式，风格简洁高级，支持分类筛选。',
  },
]

const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({ initPrompt: userPrompt.value.trim() })
    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      const appId = String(res.data.data)
      await router.push(`/app/chat/${appId}`)
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

const loadFeaturedApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  }
}

const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

onMounted(() => {
  loadMyApps()
  loadFeaturedApps()
})
</script>

<template>
  <div id="homePage">
    <section class="hero-section">
      <h1 class="hero-title">AI 应用生成平台</h1>
      <p class="hero-subtitle">一句话轻松创建网站应用</p>

      <div class="prompt-wrapper">
        <a-textarea
          v-model:value="userPrompt"
          placeholder="帮我创建个人博客网站"
          :rows="5"
          :maxlength="1000"
          class="prompt-input"
        />
        <div class="prompt-submit">
          <a-button type="primary" size="large" :loading="creating" @click="createApp">开始生成</a-button>
        </div>
      </div>

      <div class="quick-actions">
        <a-button v-for="item in promptIdeas" :key="item.label" @click="setPrompt(item.prompt)">
          {{ item.label }}
        </a-button>
      </div>
    </section>

    <section class="content-section">
      <div class="section-head">
        <h2>我的作品</h2>
      </div>
      <div class="app-grid">
        <AppCard
          v-for="app in myApps"
          :key="app.id"
          :app="app"
          @view-chat="viewChat"
          @view-work="viewWork"
        />
      </div>
      <div class="pagination-wrapper">
        <a-pagination
          v-model:current="myAppsPage.current"
          v-model:page-size="myAppsPage.pageSize"
          :total="myAppsPage.total"
          :show-size-changer="false"
          :show-total="(total: number) => `共 ${total} 个应用`"
          @change="loadMyApps"
        />
      </div>
    </section>

    <section class="content-section">
      <div class="section-head">
        <h2>精选案例</h2>
      </div>
      <div class="app-grid">
        <AppCard
          v-for="app in featuredApps"
          :key="app.id"
          :app="app"
          :featured="true"
          @view-chat="viewChat"
          @view-work="viewWork"
        />
      </div>
      <div class="pagination-wrapper">
        <a-pagination
          v-model:current="featuredAppsPage.current"
          v-model:page-size="featuredAppsPage.pageSize"
          :total="featuredAppsPage.total"
          :show-size-changer="false"
          :show-total="(total: number) => `共 ${total} 个案例`"
          @change="loadFeaturedApps"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
#homePage {
  width: 100%;
  min-height: calc(100vh - 180px);
}

.hero-section {
  text-align: center;
  padding: 46px 20px 24px;
  background:
    linear-gradient(180deg, rgba(246, 241, 233, 0.82) 0%, rgba(250, 248, 244, 0.86) 100%),
    repeating-linear-gradient(
      0deg,
      rgba(159, 146, 128, 0.11) 0,
      rgba(159, 146, 128, 0.11) 1px,
      transparent 1px,
      transparent 36px
    ),
    repeating-linear-gradient(
      90deg,
      rgba(159, 146, 128, 0.11) 0,
      rgba(159, 146, 128, 0.11) 1px,
      transparent 1px,
      transparent 36px
    );
  border-radius: 18px;
  border: 1px solid var(--zc-border);
}

.hero-title {
  margin: 0;
  font-size: clamp(42px, 6vw, 74px);
  font-weight: 700;
  line-height: 1.15;
  color: #2f2b27;
}

.hero-subtitle {
  margin: 10px 0 24px;
  font-size: 22px;
  color: var(--zc-text-secondary);
}

.prompt-wrapper {
  width: min(980px, 100%);
  margin: 0 auto;
  padding: 14px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 14px 30px rgba(74, 56, 35, 0.1);
}

.prompt-input {
  border-radius: 18px;
  border: 1px solid var(--zc-border);
  background: #ffffff;
  font-size: 18px;
  color: var(--zc-text-primary);
}

.prompt-submit {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.quick-actions {
  margin-top: 18px;
  display: flex;
  gap: 14px;
  justify-content: center;
  flex-wrap: wrap;
}

.quick-actions .ant-btn {
  height: 46px;
  border-radius: 999px;
  padding: 0 30px;
  font-size: 16px;
  border-color: var(--zc-border);
  color: #5e574d;
  background: rgba(255, 255, 255, 0.95);
}

.content-section {
  margin-top: 18px;
  background: var(--zc-bg-card);
  border: 1px solid var(--zc-border);
  border-radius: 16px;
  padding: 18px;
  box-shadow: var(--zc-shadow);
}

.section-head h2 {
  margin: 0 0 14px;
  color: var(--zc-text-primary);
  font-size: 20px;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .hero-section {
    padding: 28px 14px 16px;
  }

  .hero-title {
    font-size: 34px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .prompt-wrapper {
    padding: 10px;
    border-radius: 16px;
  }

  .prompt-input {
    font-size: 16px;
  }

  .quick-actions .ant-btn {
    height: 38px;
    font-size: 14px;
    padding: 0 16px;
  }

  .content-section {
    padding: 14px;
  }
}
</style>
