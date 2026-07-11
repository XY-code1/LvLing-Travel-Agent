<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';

import PageTable from '../../components/PageTable/PageTable.vue';
import { pageFeatureItem } from '../../api/featureItem';
import { pageTouristUser } from '../../api/user';
import type { FeatureItemVO, TouristUserVO } from '../../types';
import { featureLabelMap, tagLabel, tagText } from '../../utils/display';
import { getErrorMessage } from '../../utils';

const loading = ref(false);
const users = ref<TouristUserVO[]>([]);
const interestItems = ref<FeatureItemVO[]>([]);
const interestLabels = computed(() => featureLabelMap(interestItems.value));

const summary = computed(() => {
  const total = users.value.length;
  const active = users.value.filter((item) => item.status === 1).length;
  const male = users.value.filter((item) => item.gender === 1).length;
  const female = users.value.filter((item) => item.gender === 2).length;
  return [
    { label: '样本用户', value: total },
    { label: '启用用户', value: active },
    { label: '男性游客', value: male },
    { label: '女性游客', value: female }
  ];
});

const tagStats = computed(() => {
  const counts = new Map<string, number>();
  users.value.forEach((user) => {
    (user.interestTags || '')
      .split(/[,，]/)
      .map((item) => item.trim())
      .filter(Boolean)
      .forEach((tag) => counts.set(tag, (counts.get(tag) || 0) + 1));
  });
  return Array.from(counts.entries())
    .map(([tag, count]) => ({ tag, label: tagLabel(tag, interestLabels.value), count }))
    .sort((left, right) => right.count - left.count)
    .slice(0, 10);
});

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    const data = await pageTouristUser({ pageNum: 1, pageSize: 100, phone: '', nickname: '', status: null });
    users.value = data.list;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

async function loadInterestOptions(): Promise<void> {
  try {
    const data = await pageFeatureItem({
      pageNum: 1,
      pageSize: 200,
      moduleType: 'ROUTE_INTEREST',
      scenicName: '',
      keyword: '',
      category: '',
      status: 1
    });
    interestItems.value = data.list;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

function genderText(value: number | null): string {
  if (value === 1) return '男';
  if (value === 2) return '女';
  return '未填';
}

onMounted(() => {
  void loadInterestOptions();
  void loadData();
});
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">游客画像分析</h2>
        <p class="page-description">基于游客账号资料统计性别、启用状态和兴趣标签，为后续精细化运营提供依据。</p>
      </div>
      <el-button :loading="loading" @click="loadData">刷新</el-button>
    </header>

    <div class="report-summary">
      <article v-for="item in summary" :key="item.label" class="metric-card">
        <div class="metric-card__label">{{ item.label }}</div>
        <div class="metric-card__value">{{ item.value }}</div>
      </article>
    </div>

    <div class="dashboard-grid">
      <section class="panel">
        <h3 class="panel-title">兴趣标签 Top 10</h3>
        <el-empty v-if="tagStats.length === 0" description="暂无兴趣标签" />
        <ul v-else class="report-list">
          <li v-for="item in tagStats" :key="item.tag">
            <span>{{ item.label }}</span>
            <strong>{{ item.count }} 人</strong>
          </li>
        </ul>
      </section>

      <section class="panel">
        <h3 class="panel-title">最近用户样本</h3>
        <PageTable>
          <el-table :data="users.slice(0, 8)" row-key="id" size="small">
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column prop="nickname" label="昵称" min-width="120" />
            <el-table-column label="性别" width="80">
              <template #default="{ row }">{{ genderText(row.gender) }}</template>
            </el-table-column>
            <el-table-column label="兴趣标签" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ tagText(row.interestTags, interestLabels) }}</template>
            </el-table-column>
          </el-table>
        </PageTable>
      </section>
    </div>
  </section>
</template>
