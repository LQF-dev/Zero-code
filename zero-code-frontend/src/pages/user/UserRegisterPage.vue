<template>
  <div id="userRegisterPage">
    <div class="register-card">
      <div class="title-wrap">
        <h2 class="title">创建账号</h2>
        <div class="desc">注册后即可开始使用 Zero Code Studio 生成应用</div>
      </div>
      <a-form :model="formState" name="register" autocomplete="off" @finish="handleSubmit">
        <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input v-model:value="formState.userAccount" size="large" placeholder="请输入账号" />
        </a-form-item>
        <a-form-item
          name="userPassword"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码不能小于 8 位' },
          ]"
        >
          <a-input-password
            v-model:value="formState.userPassword"
            size="large"
            placeholder="请输入密码"
          />
        </a-form-item>
        <a-form-item
          name="checkPassword"
          :rules="[
            { required: true, message: '请确认密码' },
            { min: 8, message: '密码不能小于 8 位' },
            { validator: validateCheckPassword },
          ]"
        >
          <a-input-password
            v-model:value="formState.checkPassword"
            size="large"
            placeholder="请确认密码"
          />
        </a-form-item>
        <div class="tips">
          已有账号？
          <RouterLink to="/user/login">去登录</RouterLink>
        </div>
        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" style="width: 100%">注册</a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { reactive } from 'vue'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
  min-height: calc(100vh - 220px);
  display: grid;
  align-items: center;
}

.register-card {
  width: min(560px, 100%);
  margin: 0 auto;
  padding: 30px;
  border-radius: 20px;
  border: 1px solid var(--zc-border);
  background: #fff;
  box-shadow: var(--zc-shadow);
}

.title-wrap {
  margin-bottom: 18px;
}

.title {
  margin: 0;
  color: var(--zc-text-primary);
  font-size: 30px;
}

.desc {
  margin-top: 6px;
  color: var(--zc-text-secondary);
}

.register-card :deep(.ant-input),
.register-card :deep(.ant-input-affix-wrapper) {
  background: #fff;
  border-color: var(--zc-border);
  color: var(--zc-text-primary);
}

.register-card :deep(.ant-input::placeholder) {
  color: #a19586;
}

.tips {
  margin-bottom: 14px;
  color: var(--zc-text-secondary);
  font-size: 13px;
  text-align: right;
}

.tips a {
  color: var(--zc-primary);
}

@media (max-width: 768px) {
  #userRegisterPage {
    min-height: auto;
  }

  .register-card {
    padding: 18px;
    border-radius: 16px;
  }

  .title {
    font-size: 26px;
  }
}
</style>
