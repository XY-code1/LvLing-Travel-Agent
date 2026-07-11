<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';

import { useAuthStore } from '../stores';

interface MenuItem {
  path: string;
  title: string;
}

interface MenuGroup {
  index: string;
  title: string;
  icon: string;
  children: MenuItem[];
}

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// Lucide/Heroicons 风格 16×16 线性图标 SVG
const ICONS = {
  dashboard: `<svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><path d="M14 17.5h7M17.5 14v7"/></svg>`,
  scenic:    `<svg viewBox="0 0 24 24"><path d="M3 20 L8 10 L12 15 L16 8 L21 20Z"/><circle cx="17" cy="5" r="2"/></svg>`,
  route:     `<svg viewBox="0 0 24 24"><circle cx="5" cy="6" r="2"/><circle cx="19" cy="18" r="2"/><path d="M5 8v3a4 4 0 0 0 4 4h6a4 4 0 0 1 4 4"/></svg>`,
  user:      `<svg viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`,
  aiconfig:  `<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>`,
  chat:      `<svg viewBox="0 0 24 24"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>`,
  sentiment: `<svg viewBox="0 0 24 24"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>`,
  system:    `<svg viewBox="0 0 24 24"><path d="M12 15.5A3.5 3.5 0 1 0 12 8a3.5 3.5 0 0 0 0 7.5Z"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06A1.7 1.7 0 0 0 15 19.4a1.7 1.7 0 0 0-1 .6V20a2 2 0 1 1-4 0v-.08a1.7 1.7 0 0 0-1-.6 1.7 1.7 0 0 0-1.88.34l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-.6-1H4a2 2 0 1 1 0-4h.08a1.7 1.7 0 0 0 .6-1 1.7 1.7 0 0 0-.34-1.88l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-.6V4a2 2 0 1 1 4 0v.08a1.7 1.7 0 0 0 1 .6 1.7 1.7 0 0 0 1.88-.34l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.7 1.7 0 0 0 19.4 9a1.7 1.7 0 0 0 .6 1H20a2 2 0 1 1 0 4h-.08a1.7 1.7 0 0 0-.52 1Z"/></svg>`,
};

const menuGroups: MenuGroup[] = [
  {
    index: 'workbench',
    title: '运营工作台',
    icon: ICONS.dashboard,
    children: [
      { path: '/workbench/overview', title: '概览仪表盘' },
      // 已按业务要求删除“待办事项”前端菜单；保留注释说明该入口是主动隐藏。
      // { path: '/workbench/todo', title: '待办事项' },
      { path: '/workbench/quick', title: '快捷入口' },
    ],
  },
  {
    index: 'resource',
    title: '景区资源管理',
    icon: ICONS.scenic,
    children: [
      { path: '/resource/scenic', title: '景区基础信息' },
      { path: '/resource/spot', title: '景点/POI管理' },
      { path: '/resource/facility', title: '设施管理' },
      { path: '/resource/tag', title: '景点标签/分类管理' },
    ],
  },
  {
    index: 'route',
    title: '游览路线规划',
    icon: ICONS.route,
    children: [
      { path: '/route/recommend', title: '推荐路线列表' },
      { path: '/route/builder', title: '自定义路线工具' },
      { path: '/route/type', title: '路线类型管理' },
      { path: '/route/interest', title: '兴趣标签管理' },
      { path: '/route/content', title: '路线关联内容' },
    ],
  },
  {
    index: 'ai',
    title: 'AI 与数字人中心',
    icon: ICONS.aiconfig,
    children: [
      { path: '/ai/rag', title: '本地 RAG' },
      { path: '/ai/embedding', title: '本地向量模型' },
      { path: '/ai/llm', title: '文本大模型' },
      { path: '/ai/vision', title: '多模态模型' },
      { path: '/ai/asr', title: 'ASR 语音识别' },
      { path: '/ai/tts', title: 'TTS 语音合成' },
      { path: '/ai/avatar', title: '数字人形象管理' },
    ],
  },
  {
    index: 'interaction',
    title: '用户与互动管理',
    icon: ICONS.user,
    children: [
      { path: '/interaction/user', title: '用户管理' },
      { path: '/interaction/feedback', title: '游客反馈/评论' },
      { path: '/interaction/checkin', title: '打卡/成就系统' },
      { path: '/interaction/push-message', title: '消息推送管理' },
      { path: '/interaction/chat-record', title: '对话记录' },
    ],
  },
  {
    index: 'analytics',
    title: '数据分析大屏',
    icon: ICONS.sentiment,
    children: [
      { path: '/analytics/screen', title: '实时流量监控' },
      { path: '/analytics/visitor-profile', title: '游客画像分析' },
      { path: '/analytics/ai-interaction', title: 'AI交互分析' },
      { path: '/analytics/funnel', title: '转化漏斗' },
    ],
  },
  {
    index: 'system',
    title: '系统设置',
    icon: ICONS.system,
    children: [
      { path: '/system/log', title: '操作日志' },
    ],
  },
];

const activePath = computed(() => route.path);
const pageTitle  = computed(() => String(route.meta.title ?? '管理后台'));
const displayName = computed(
  () => authStore.user?.realName || authStore.user?.username || '管理员'
);
const displayInitial = computed(() => displayName.value.slice(0, 1));

async function handleLogout(): Promise<void> {
  await ElMessageBox.confirm('确认退出当前管理账号？', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning',
  });
  await authStore.logout();
  ElMessage.success('已退出登录');
  await router.replace('/login');
}
</script>

<template>
  <el-container class="admin-shell">
    <!-- 侧边栏 -->
    <el-aside class="admin-sidebar">
      <!-- 品牌区 -->
      <div class="admin-brand">
        <div class="admin-brand__logo">
          <!-- 地图定位图标 -->
          <svg viewBox="0 0 24 24" stroke="white" fill="none" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/>
            <circle cx="12" cy="9" r="2.5"/>
          </svg>
        </div>
        <div class="admin-brand__info">
          <span class="admin-brand__name">Guido Scenic AI</span>
          <span class="admin-brand__meta">景区导览管理端</span>
        </div>
      </div>

      <!-- 导航菜单 -->
      <el-menu class="admin-menu" :default-active="activePath" router unique-opened>
        <el-sub-menu v-for="group in menuGroups" :key="group.index" :index="group.index">
          <template #title>
            <span class="menu-icon" v-html="group.icon" />
            <span>{{ group.title }}</span>
          </template>
          <el-menu-item
            v-for="item in group.children"
            :key="item.path"
            :index="item.path"
          >
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 主体 -->
    <el-container class="admin-main">
      <!-- 顶栏 -->
      <el-header class="admin-topbar">
        <h1 class="admin-topbar__title">{{ pageTitle }}</h1>
        <div class="admin-topbar__user">
          <el-avatar :size="32" style="background: linear-gradient(135deg,#6366f1,#4f46e5);color:#fff">
            {{ displayInitial }}
          </el-avatar>
          <el-dropdown>
            <el-button text style="font-size:13.5px;color:var(--color-text-secondary)">
              {{ displayName }} ▾
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 页面内容 -->
      <el-main class="admin-content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>
