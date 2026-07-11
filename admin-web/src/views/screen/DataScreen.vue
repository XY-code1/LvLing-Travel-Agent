<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import type { EChartsCoreOption } from 'echarts/core';

import { getDashboardOverview } from '../../api/dashboard';
import EChartsBox from '../../components/EChartsBox/EChartsBox.vue';
import type { DashboardOverviewVO } from '../../types';
import { emotionText } from '../../utils/display';
import { formatPercent, getErrorMessage } from '../../utils';

const loading = ref(false);
const overview = ref<DashboardOverviewVO | null>(null);

const kpis = computed(() => {
  const today = overview.value?.today;
  if (!today) {
    return [];
  }
  return [
    { label: '服务人次', value: today.serviceCount },
    { label: '问答次数', value: today.qaCount },
    { label: '数字人播报', value: today.avatarCount },
    { label: '知识命中率', value: formatPercent(today.kbHitRate) }
  ];
});

const trendOption = computed<EChartsCoreOption>(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'axis' },
  grid: { left: 36, right: 24, top: 32, bottom: 28 },
  xAxis: {
    type: 'category',
    data: overview.value?.trend7d.map((item) => item.date) ?? [],
    axisLabel: { color: 'rgb(203 213 225)' }
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: 'rgb(203 213 225)' },
    splitLine: { lineStyle: { color: 'rgb(51 65 85)' } }
  },
  series: [
    {
      name: '服务人次',
      type: 'line',
      smooth: true,
      data: overview.value?.trend7d.map((item) => item.serviceCount) ?? [],
      areaStyle: { opacity: 0.16 },
      lineStyle: { color: 'rgb(34 211 238)' },
      itemStyle: { color: 'rgb(34 211 238)' }
    }
  ]
}));

const emotionOption = computed<EChartsCoreOption>(() => ({
  tooltip: { trigger: 'item' },
  series: [
    {
      name: '情绪',
      type: 'pie',
      radius: ['46%', '70%'],
      data: Object.entries(overview.value?.emotionDist ?? {}).map(([name, value]) => ({ name: emotionText(name), value }))
    }
  ]
}));

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    overview.value = await getDashboardOverview();
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <section class="screen-page" v-loading="loading">
    <header class="screen-header">
      <div>
        <h2 class="screen-title">景区导览运营大屏</h2>
        <p class="screen-subtitle">实时呈现今日服务、知识命中、数字人播报和游客情绪。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadData">刷新</el-button>
    </header>

    <div class="screen-kpi-grid">
      <article v-for="item in kpis" :key="item.label" class="screen-card">
        <div class="screen-card__label">{{ item.label }}</div>
        <div class="screen-card__value">{{ item.value }}</div>
      </article>
    </div>

    <div class="screen-grid">
      <section class="screen-panel">
        <h3 class="panel-title">近 7 日服务趋势</h3>
        <EChartsBox :option="trendOption" />
      </section>
      <section class="screen-panel">
        <h3 class="panel-title">游客情绪分布</h3>
        <EChartsBox :option="emotionOption" />
      </section>
    </div>
  </section>
</template>
