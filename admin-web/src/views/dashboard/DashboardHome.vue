<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';

import { getDashboardOverview } from '../../api/dashboard';
import type { DashboardOverviewVO, TodayStatVO } from '../../types';
import { emotionText } from '../../utils/display';
import { formatPercent, getErrorMessage } from '../../utils';

interface MetricItem {
  label: string;
  value: string | number;
}

const loading = ref(false);
const errorMessage = ref('');
const overview = ref<DashboardOverviewVO | null>(null);

const today = computed<TodayStatVO | null>(() => overview.value?.today ?? null);
const metrics = computed<MetricItem[]>(() => {
  const data = today.value;
  if (!data) {
    return [];
  }
  return [
    { label: '今日服务人次', value: data.serviceCount },
    { label: '文本问答', value: data.qaCount },
    { label: '语音问答', value: data.voiceCount },
    { label: '知识库命中率', value: formatPercent(data.kbHitRate) },
    { label: '拍照识别', value: data.imageCount },
    { label: '数字人播报', value: data.avatarCount },
    { label: '平均耗时', value: `${data.avgCostMs} ms` },
    { label: '异常次数', value: data.errorCount }
  ];
});

async function loadOverview(): Promise<void> {
  loading.value = true;
  errorMessage.value = '';
  try {
    overview.value = await getDashboardOverview();
  } catch (error: unknown) {
    errorMessage.value = getErrorMessage(error);
    ElMessage.error(errorMessage.value);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadOverview();
});
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">运营工作台</h2>
        <p class="page-description">今日导览服务、知识库命中、数字人播报与异常情况。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadOverview">刷新</el-button>
    </header>

    <div v-if="loading" class="state-panel">
      <el-skeleton :rows="6" animated />
    </div>

    <el-alert
      v-else-if="errorMessage"
      :closable="false"
      :title="errorMessage"
      show-icon
      type="error"
    >
      <template #default>
        <el-button type="primary" @click="loadOverview">重试</el-button>
      </template>
    </el-alert>

    <el-empty v-else-if="!overview" class="state-panel" description="暂无运营数据" />

    <div v-else>
      <div class="metric-grid">
        <article v-for="item in metrics" :key="item.label" class="metric-card">
          <div class="metric-card__label">{{ item.label }}</div>
          <div class="metric-card__value">{{ item.value }}</div>
        </article>
      </div>

      <div class="dashboard-grid">
        <section class="panel">
          <h3 class="panel-title">近 7 日趋势</h3>
          <ul class="trend-list">
            <li v-for="item in overview.trend7d" :key="item.date" class="trend-item">
              <span>{{ item.date }}</span>
              <strong>{{ item.serviceCount }} 人次 / {{ formatPercent(item.kbHitRate) }}</strong>
            </li>
          </ul>
        </section>

        <section class="panel">
          <h3 class="panel-title">情绪分布</h3>
          <ul class="rank-list">
            <li v-for="(count, name) in overview.emotionDist" :key="name" class="rank-item">
              <span>{{ emotionText(String(name)) }}</span>
              <strong>{{ count }}</strong>
            </li>
          </ul>
        </section>
      </div>
    </div>
  </section>
</template>
