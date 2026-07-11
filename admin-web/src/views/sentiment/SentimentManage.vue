<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';

import { downloadSentimentDocx, generateSentiment } from '../../api/sentiment';
import type { SentimentReportVO } from '../../types';
import { getErrorMessage } from '../../utils';

const today = formatDate(new Date());
const loading = ref(false);
const downloading = ref(false);
const report = ref<SentimentReportVO | null>(null);

const summary = computed(() => {
  if (!report.value) {
    return [];
  }
  return [
    { label: '正向', value: report.value.positiveCount },
    { label: '中性', value: report.value.neutralCount },
    { label: '负向', value: report.value.negativeCount },
    { label: '投诉', value: report.value.complaintCount }
  ];
});

const hotQuestions = computed(() => parseReportItems(report.value?.hotQuestions, 'question'));
const hotSpots = computed(() => parseReportItems(report.value?.hotSpots, 'spotName'));
const unanswered = computed(() => parseReportItems(report.value?.unanswered, 'question'));

async function loadCurrentReport(): Promise<void> {
  loading.value = true;
  try {
    report.value = await generateSentiment(today);
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

async function downloadReport(): Promise<void> {
  downloading.value = true;
  try {
    const blob = await downloadSentimentDocx(today);
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `感受度分析报告-${today}.docx`;
    link.click();
    URL.revokeObjectURL(url);
    await loadCurrentReport();
    ElMessage.success('Word 报告已生成');
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    downloading.value = false;
  }
}

function parseReportItems(value: string | null | undefined, nameKey: string): Array<{ name: string; count?: number }> {
  if (!value) {
    return [];
  }
  try {
    const parsed = JSON.parse(value) as Array<Record<string, unknown>>;
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed.map((item) => ({
      name: String(item[nameKey] ?? item.question ?? item.spotName ?? ''),
      count: typeof item.count === 'number' ? item.count : undefined
    })).filter((item) => item.name);
  } catch {
    return [{ name: value }];
  }
}

function formatDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

onMounted(() => {
  void loadCurrentReport();
});
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">感受度分析</h2>
        <p class="page-description">当前报告日期：{{ today }}。页面直接展示今日报告，生成按钮下载 Word 文档。</p>
      </div>
      <el-button type="primary" :loading="downloading" @click="downloadReport">生成 Word 报告</el-button>
    </header>

    <el-empty v-if="!report && !loading" class="state-panel" description="暂无今日报告" />

    <div v-else-if="report">
      <div class="report-summary">
        <article v-for="item in summary" :key="item.label" class="metric-card">
          <div class="metric-card__label">{{ item.label }}</div>
          <div class="metric-card__value">{{ item.value }}</div>
        </article>
      </div>

      <div class="dashboard-grid">
        <section class="panel">
          <h3 class="panel-title">热点问题</h3>
          <el-empty v-if="hotQuestions.length === 0" description="暂无" />
          <ul v-else class="report-list">
            <li v-for="item in hotQuestions" :key="item.name">
              <span>{{ item.name }}</span>
              <strong v-if="item.count !== undefined">{{ item.count }} 次</strong>
            </li>
          </ul>
        </section>
        <section class="panel">
          <h3 class="panel-title">热点景点</h3>
          <el-empty v-if="hotSpots.length === 0" description="暂无" />
          <ul v-else class="report-list">
            <li v-for="item in hotSpots" :key="item.name">
              <span>{{ item.name }}</span>
              <strong v-if="item.count !== undefined">{{ item.count }} 次</strong>
            </li>
          </ul>
        </section>
        <section class="panel">
          <h3 class="panel-title">未回答问题</h3>
          <el-empty v-if="unanswered.length === 0" description="暂无" />
          <ul v-else class="report-list">
            <li v-for="item in unanswered" :key="item.name">
              <span>{{ item.name }}</span>
            </li>
          </ul>
        </section>
        <section class="panel">
          <h3 class="panel-title">运营建议</h3>
          <p class="detail-text">{{ report.aiSuggestion }}</p>
        </section>
      </div>
    </div>
  </section>
</template>
