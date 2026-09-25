<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';

import { useAuthStore } from '../../stores';
import { getErrorMessage } from '../../utils';
import { getAdminCaptcha } from '../../api/login';

interface LoginForm {
  username: string;
  password: string;
  captchaId: string;
  captchaCode: string;
}

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive<LoginForm>({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: ''
});
const captchaImage = ref('');

async function refreshCaptcha(): Promise<void> {
  const captcha = await getAdminCaptcha();
  form.captchaId = captcha.captchaId;
  captchaImage.value = captcha.image;
  form.captchaCode = '';
}

onMounted(() => {
  void refreshCaptcha();
});

const rules: FormRules<LoginForm> = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入图片验证码', trigger: 'blur' }]
};

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate();
  if (!valid) {
    return;
  }
  loading.value = true;
  try {
    await authStore.login(form);
    ElMessage.success('登录成功');
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/workbench/overview';
    await router.replace(redirect);
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
    void refreshCaptcha();
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-copy" aria-label="系统说明">
      <h1 class="login-title">景区导览服务 AI 数字人系统</h1>
      <p class="login-subtitle">
        面向景区运营人员的导览内容、知识库、数字人服务与游客体验数据统一管理后台。
      </p>
    </section>

    <section class="login-panel" aria-label="管理员登录">
      <div class="login-card">
        <header class="login-card__header">
          <h2 class="login-card__title">管理员登录</h2>
          <p class="login-card__desc">使用后台账号进入管理端。</p>
        </header>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              autocomplete="current-password"
              show-password
              type="password"
            />
          </el-form-item>
          <el-form-item label="图片验证码" prop="captchaCode">
            <div class="login-captcha">
              <el-input v-model="form.captchaCode" autocomplete="off" placeholder="请输入图片中的字符" />
              <img
                v-if="captchaImage"
                :src="captchaImage"
                alt="点击刷新验证码"
                class="login-captcha__image"
                title="点击刷新验证码"
                @click="refreshCaptcha"
              />
            </div>
          </el-form-item>
          <el-button class="login-submit" type="primary" :loading="loading" @click="handleSubmit">
            登录
          </el-button>
        </el-form>
      </div>
    </section>
  </main>
</template>