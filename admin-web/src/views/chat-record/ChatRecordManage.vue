<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';

import PageTable from '../../components/PageTable/PageTable.vue';
import SearchForm from '../../components/SearchForm/SearchForm.vue';
import { getChatDetail, markChatSupplement, pageChat } from '../../api/chat';
import type { AdminChatMessageVO, AdminChatPageQuery } from '../../types';
import { emotionText, inputTypeText } from '../../utils/display';
import { getErrorMessage } from '../../utils';

const query = reactive<AdminChatPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  inputType: '',
  emotion: '',
  success: null
});
const rows = ref<AdminChatMessageVO[]>([]);
const total = ref(0);
const loading = ref(false);
const detailVisible = ref(false);
const detail = ref<AdminChatMessageVO | null>(null);

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    const data = await pageChat(query);
    rows.value = data.list;
    total.value = data.total;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

async function openDetail(row: AdminChatMessageVO): Promise<void> {
  try {
    detail.value = await getChatDetail(row.id);
    detailVisible.value = true;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

async function markRow(row: AdminChatMessageVO): Promise<void> {
  try {
    await markChatSupplement(row.id);
    row.needSupplement = 1;
    ElMessage.success('已标记待补充');
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

function resetSearch(): void {
  Object.assign(query, { pageNum: 1, keyword: '', inputType: '', emotion: '', success: null });
  void loadData();
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">对话记录</h2>
        <p class="page-description">查看游客问答、知识库命中、情绪标签和失败原因。</p>
      </div>
    </header>

    <SearchForm>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="问题或回答" />
      </el-form-item>
      <el-form-item label="输入类型">
        <el-select v-model="query.inputType" clearable placeholder="全部">
          <el-option label="文本" value="TEXT" />
          <el-option label="语音" value="VOICE" />
          <el-option label="图片" value="IMAGE" />
        </el-select>
      </el-form-item>
      <el-form-item label="情绪">
        <el-select v-model="query.emotion" clearable placeholder="全部">
          <el-option label="正向" value="POSITIVE" />
          <el-option label="中性" value="NEUTRAL" />
          <el-option label="负向" value="NEGATIVE" />
          <el-option label="投诉" value="COMPLAINT" />
        </el-select>
      </el-form-item>
      <el-form-item label="成功">
        <el-select v-model="query.success" clearable placeholder="全部">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </SearchForm>

    <PageTable>
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ inputTypeText(row.inputType) }}</template>
        </el-table-column>
        <el-table-column label="情绪" width="110">
          <template #default="{ row }">{{ emotionText(row.emotion) }}</template>
        </el-table-column>
        <el-table-column prop="hitKb" label="命中" width="80" />
        <el-table-column prop="costMs" label="耗时" width="90" />
        <el-table-column label="补库" width="90">
          <template #default="{ row }">
            <el-tag :type="row.needSupplement === 1 ? 'warning' : 'info'">
              {{ row.needSupplement === 1 ? '待补' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button link type="primary" @click="markRow(row)">标记</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @change="loadData"
        />
      </div>
    </PageTable>

    <el-dialog v-model="detailVisible" title="对话详情" width="760">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="会话号">{{ detail.sessionNo }}</el-descriptions-item>
        <el-descriptions-item label="游客 ID">{{ detail.touristUserId }}</el-descriptions-item>
        <el-descriptions-item label="输入类型">{{ inputTypeText(detail.inputType) }}</el-descriptions-item>
        <el-descriptions-item label="情绪">{{ emotionText(detail.emotion) }}</el-descriptions-item>
        <el-descriptions-item label="问题" :span="2">
          <div class="detail-text">{{ detail.question }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="回答" :span="2">
          <div class="detail-text">{{ detail.answer }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="来源" :span="2">
          <div class="detail-text">{{ detail.sources }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </section>
</template>
