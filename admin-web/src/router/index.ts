import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';

import { useAuthStore } from '../stores';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/login/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/workbench/overview',
    children: [
      {
        path: 'workbench/overview',
        name: 'workbench-overview',
        component: () => import('../views/dashboard/DashboardHome.vue'),
        meta: { title: '概览仪表盘' }
      },
      // 已按业务要求删除“待办事项”前端入口；保留注释便于后续确认该模块是主动隐藏。
      // {
      //   path: 'workbench/todo',
      //   name: 'workbench-todo',
      //   component: () => import('../views/feature/FeatureItemManage.vue'),
      //   meta: {
      //     title: '待办事项',
      //     feature: {
      //       moduleType: 'TODO',
      //       title: '待办事项',
      //       description: '集中处理知识库补充、投诉复盘、系统巡检等运营待办。',
      //       createText: '新增待办',
      //       titleLabel: '待办标题',
      //       categoryLabel: '待办类型',
      //       contentLabel: '待办内容'
      //     }
      //   }
      // },
      {
        path: 'workbench/quick',
        name: 'workbench-quick',
        component: () => import('../views/workbench/QuickEntry.vue'),
        meta: { title: '快捷入口' }
      },
      {
        path: 'resource/scenic',
        name: 'resource-scenic',
        component: () => import('../views/scenic/ScenicManage.vue'),
        meta: { title: '景区基础信息' }
      },
      {
        path: 'resource/spot',
        name: 'resource-spot',
        component: () => import('../views/spot/SpotManage.vue'),
        meta: { title: '景点/POI 管理' }
      },
      {
        path: 'resource/facility',
        name: 'resource-facility',
        component: () => import('../views/feature/FeatureItemManage.vue'),
        meta: {
          title: '设施管理',
          feature: {
            moduleType: 'FACILITY',
            title: '设施管理',
            description: '维护厕所、停车场、餐厅、急救点、服务中心等景区服务设施。',
            createText: '新增设施',
            titleLabel: '设施名称',
            categoryLabel: '设施类型',
            contentLabel: '设施说明',
            showScenic: true,
            showLocation: true
          }
        }
      },
      {
        path: 'resource/tag',
        name: 'resource-tag',
        component: () => import('../views/feature/FeatureItemManage.vue'),
        meta: {
          title: '标签/分类管理',
          feature: {
            moduleType: 'TAG',
            title: '景点标签/分类管理',
            description: '维护景点管理可选标签和分类，景点表单只能从这里选择标签。',
            createText: '新增标签',
            titleLabel: '标签名称',
            categoryLabel: '标签分类',
            contentLabel: '标签说明'
          }
        }
      },
      {
        path: 'route/recommend',
        name: 'route-recommend',
        component: () => import('../views/route/RouteManage.vue'),
        meta: { title: '推荐路线列表', routeMode: 'list' }
      },
      {
        path: 'route/builder',
        name: 'route-builder',
        component: () => import('../views/route/RouteManage.vue'),
        meta: { title: '自定义路线工具', routeMode: 'builder' }
      },
      {
        path: 'route/type',
        name: 'route-type',
        component: () => import('../views/feature/FeatureItemManage.vue'),
        meta: {
          title: '路线类型管理',
          feature: {
            moduleType: 'ROUTE_TYPE',
            title: '路线类型管理',
            description: '维护路线类型名称和数字编码，路线列表会按这里显示类型名称。',
            createText: '新增路线类型',
            titleLabel: '类型名称',
            categoryLabel: '类型编码',
            contentLabel: '类型说明'
          }
        }
      },
      {
        path: 'route/interest',
        name: 'route-interest',
        component: () => import('../views/feature/FeatureItemManage.vue'),
        meta: {
          title: '兴趣标签管理',
          feature: {
            moduleType: 'ROUTE_INTEREST',
            title: '兴趣标签管理',
            description: '维护游客画像、路线推荐和路线表单共用的兴趣标签中文名称。',
            createText: '新增兴趣标签',
            titleLabel: '标签名称',
            categoryLabel: '标签编码',
            contentLabel: '标签说明'
          }
        }
      },
      {
        path: 'route/content',
        name: 'route-content',
        component: () => import('../views/feature/FeatureItemManage.vue'),
        meta: {
          title: '路线关联内容',
          feature: {
            moduleType: 'ROUTE_CONTENT',
            title: '路线关联内容',
            description: '为特定路线绑定专属讲解词、任务说明或运营提醒。',
            createText: '新增路线内容',
            titleLabel: '内容名称',
            categoryLabel: '内容类型',
            contentLabel: '内容正文',
            showScenic: true,
            showRoute: true
          }
        }
      },
      {
        path: 'ai/rag',
        name: 'ai-rag',
        component: () => import('../views/knowledge/KnowledgeManage.vue'),
        meta: { title: '本地 RAG 知识库' }
      },
      {
        path: 'ai/embedding',
        name: 'ai-embedding',
        component: () => import('../views/ai-config/AiConfigManage.vue'),
        meta: {
          title: '本地向量模型配置',
          aiServiceType: 'EMBEDDING',
          aiTitle: '本地向量模型配置',
          aiDescription: '配置本地 OpenAI-Compatible Embedding 服务，用于本地 RAG 文档向量化；同一时间仅允许启用一个全局默认配置。',
          lockedProvider: '本地向量模型'
        }
      },
      {
        path: 'ai/llm',
        name: 'ai-llm',
        component: () => import('../views/ai-config/AiConfigManage.vue'),
        meta: {
          title: '文本大模型配置',
          aiServiceType: 'LLM',
          aiTitle: '文本大模型配置',
          aiDescription: '配置 OpenAI-Compatible 或 Anthropic-Compatible 文本模型；同一时间仅允许启用一个全局默认配置。'
        }
      },
      {
        path: 'ai/vision',
        name: 'ai-vision',
        component: () => import('../views/ai-config/AiConfigManage.vue'),
        meta: {
          title: '多模态模型配置',
          aiServiceType: 'VISION',
          aiTitle: '多模态模型配置',
          aiDescription: '配置 OpenAI-Compatible 或 Anthropic-Compatible 多模态识别模型；同一时间仅允许启用一个全局默认配置。'
        }
      },
      {
        path: 'ai/asr',
        name: 'ai-asr',
        component: () => import('../views/ai-config/AiConfigManage.vue'),
        meta: {
          title: 'ASR 语音识别配置',
          aiServiceType: 'ASR',
          aiTitle: 'ASR 语音识别配置',
          aiDescription: '锁定阿里云智能语音交互 ASR，仅维护后端调用所需凭证；同一时间仅允许启用一个全局默认配置。',
          lockedProvider: '阿里云智能语音交互'
        }
      },
      {
        path: 'ai/tts',
        name: 'ai-tts',
        component: () => import('../views/ai-config/AiConfigManage.vue'),
        meta: {
          title: 'TTS 语音合成配置',
          aiServiceType: 'TTS',
          aiTitle: 'TTS 语音合成配置',
          aiDescription: '锁定阿里云智能语音交互 TTS，仅维护后端调用所需凭证；同一时间仅允许启用一个全局默认配置。',
          lockedProvider: '阿里云智能语音交互'
        }
      },
      {
        path: 'ai/avatar',
        name: 'ai-avatar',
        component: () => import('../views/avatar/AvatarManage.vue'),
        meta: { title: '数字人形象管理' }
      },
      {
        path: 'interaction/user',
        name: 'interaction-user',
        component: () => import('../views/user/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'interaction/feedback',
        name: 'interaction-feedback',
        component: () => import('../views/feedback/FeedbackManage.vue'),
        meta: { title: '游客反馈/评论' }
      },
      {
        path: 'interaction/checkin',
        name: 'interaction-checkin',
        component: () => import('../views/feature/FeatureItemManage.vue'),
        meta: {
          title: '打卡/成就系统',
          feature: {
            moduleType: 'ACHIEVEMENT',
            title: '打卡/成就系统',
            description: '配置虚拟勋章、打卡任务和游客成长激励。',
            createText: '新增成就',
            titleLabel: '成就名称',
            categoryLabel: '任务类型',
            contentLabel: '达成规则',
            showScenic: true,
            showMedia: true
          }
        }
      },
      {
        path: 'interaction/push-message',
        name: 'interaction-push-message',
        component: () => import('../views/feature/FeatureItemManage.vue'),
        meta: {
          title: '消息推送管理',
          feature: {
            moduleType: 'PUSH_MESSAGE',
            title: '消息推送管理',
            description: '配置面向全体游客、特定景区或特定场景的弹窗通知。',
            createText: '新增推送',
            titleLabel: '推送标题',
            categoryLabel: '推送范围',
            contentLabel: '推送内容',
            showScenic: true
          }
        }
      },
      {
        path: 'interaction/chat-record',
        name: 'interaction-chat-record',
        component: () => import('../views/chat-record/ChatRecordManage.vue'),
        meta: { title: '对话记录' }
      },
      {
        path: 'analytics/screen',
        name: 'analytics-screen',
        component: () => import('../views/screen/DataScreen.vue'),
        meta: { title: '实时流量监控' }
      },
      {
        path: 'analytics/ai-interaction',
        name: 'analytics-ai-interaction',
        component: () => import('../views/sentiment/SentimentManage.vue'),
        meta: { title: 'AI 交互分析' }
      },
      {
        path: 'analytics/visitor-profile',
        name: 'analytics-visitor-profile',
        component: () => import('../views/analytics/VisitorProfileAnalysis.vue'),
        meta: { title: '游客画像分析' }
      },
      {
        path: 'analytics/funnel',
        name: 'analytics-funnel',
        component: () => import('../views/feature/FeatureItemManage.vue'),
        meta: {
          title: '转化漏斗',
          feature: {
            moduleType: 'FUNNEL_STEP',
            title: '转化漏斗',
            description: '配置浏览、咨询、预约、购买等转化节点，便于后续接入票务或商品数据。',
            createText: '新增漏斗节点',
            titleLabel: '漏斗节点',
            categoryLabel: '节点类型',
            contentLabel: '统计口径'
          }
        }
      },
      {
        path: 'system/log',
        name: 'system-log',
        component: () => import('../views/system/SysLogManage.vue'),
        meta: { title: '操作日志' }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  if (to.meta.public) {
    return true;
  }
  if (!authStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (!authStore.user) {
    try {
      await authStore.loadUser();
    } catch {
      authStore.clearSession();
      return { path: '/login', query: { redirect: to.fullPath } };
    }
  }
  return true;
});

export default router;
