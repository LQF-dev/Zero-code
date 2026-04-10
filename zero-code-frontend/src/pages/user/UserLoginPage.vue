<template>
  <div id="userLoginPage">
    <div class="login-shell">
      <aside class="login-brand">
        <div class="brand-tag">Zero Code Studio</div>
        <h1>欢迎回来</h1>
        <p>
          继续你的 AI 应用创作流程。登录后可以查看历史作品、继续对话生成并部署站点。
        </p>
        <ul class="brand-points">
          <li>自然语言生成页面结构</li>
          <li>可视化迭代与即时预览</li>
          <li>一键部署与代码下载</li>
        </ul>
      </aside>

      <section class="login-panel">
        <h2>账号登录</h2>
        <div class="desc">请输入你的账号和密码</div>
        <a-form :model="formState" name="login" autocomplete="off" @finish="handleSubmit">
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input v-model:value="formState.userAccount" size="large" placeholder="请输入账号" />
          </a-form-item>
          <a-form-item
            name="userPassword"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              size="large"
              placeholder="请输入密码"
            />
          </a-form-item>
          <div class="tips">
            没有账号？
            <RouterLink to="/user/register">去注册</RouterLink>
          </div>
          <a-form-item>
            <a-button type="primary" html-type="submit" size="large" style="width: 100%">
              登录
            </a-button>
          </a-form-item>
        </a-form>
      </section>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive } from 'vue'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const router = useRouter()
const loginUserStore = useLoginUserStore()

const handleSubmit = async (values: API.UserLoginRequest) => {
  const res = await userLogin(values)
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({
      path: '/',
      replace: true,
    })
  } else {
    message.error('登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userLoginPage {
  min-height: calc(100vh - 220px);
  display: grid;
  align-items: center;
}

.login-shell {
  display: grid;
  grid-template-columns: minmax(320px, 1.2fr) minmax(320px, 1fr);
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid var(--zc-border);
  background: #fff;
  box-shadow: var(--zc-shadow);
}

.login-brand {
  padding: 34px;
  background:
    radial-gradient(circle at 8% 12%, rgba(205, 167, 137, 0.3), transparent 42%),
    radial-gradient(circle at 100% 100%, rgba(224, 197, 163, 0.2), transparent 40%),
    #f7f3ed;
}

.brand-tag {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #825136;
  background: #f0dfcf;
}

.login-brand h1 {
  margin: 18px 0 10px;
  font-size: 34px;
  color: var(--zc-text-primary);
}

.login-brand p {
  margin: 0;
  color: var(--zc-text-secondary);
  line-height: 1.65;
}

.brand-points {
  margin: 28px 0 0;
  padding-left: 18px;
  color: #7b7164;
  line-height: 1.8;
}

.login-panel {
  padding: 34px;
  background: #fff;
}

.login-panel h2 {
  margin: 0;
  font-size: 28px;
  color: var(--zc-text-primary);
}

.desc {
  margin: 6px 0 18px;
  color: var(--zc-text-secondary);
}

.login-panel :deep(.ant-input),
.login-panel :deep(.ant-input-affix-wrapper) {
  background: #fff;
  border-color: var(--zc-border);
  color: var(--zc-text-primary);
}

.login-panel :deep(.ant-input::placeholder) {
  color: #a19586;
}

.tips {
  text-align: right;
  color: var(--zc-text-secondary);
  font-size: 13px;
  margin-bottom: 14px;
}

.tips a {
  color: var(--zc-primary);
}

@media (max-width: 900px) {
  #userLoginPage {
    min-height: auto;
  }

  .login-shell {
    grid-template-columns: 1fr;
    border-radius: 16px;
  }

  .login-brand,
  .login-panel {
    padding: 20px;
  }
}
</style>
