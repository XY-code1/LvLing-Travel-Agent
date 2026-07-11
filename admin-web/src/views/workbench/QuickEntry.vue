<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();
const entries = [
  { title: '新增景点', desc: '维护 POI、讲解词和图片素材', path: '/resource/spot' },
  { title: '配置推荐路线', desc: '维护官方推荐路线和关联景点', path: '/route/recommend' },
  { title: '上传本地 RAG', desc: '同步本地文档和测试召回效果', path: '/ai/rag' },
  { title: '查看游客反馈', desc: '处理评分、投诉和评论回复', path: '/interaction/feedback' },
  { title: '生成感受度报告', desc: '下载今日 Word 分析报告', path: '/analytics/ai-interaction' },
  { title: '查看操作日志', desc: '追踪后台关键操作', path: '/system/log' }
];

function go(path: string): void {
  void router.push(path);
}
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">快捷入口</h2>
        <p class="page-description">运营人员高频使用的功能入口集中在这里。</p>
      </div>
    </header>
    <div class="quick-entry-grid">
      <article v-for="item in entries" :key="item.path" class="quick-entry-card" @click="go(item.path)">
        <h3>{{ item.title }}</h3>
        <p>{{ item.desc }}</p>
      </article>
    </div>
  </section>
</template>
