<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';

import PageTable from '../../components/PageTable/PageTable.vue';
import SearchForm from '../../components/SearchForm/SearchForm.vue';
import { pageFeedback, replyFeedback } from '../../api/feedback';
import type { AdminFeedbackPageQuery, AdminFeedbackVO, FeedbackReplyDTO } from '../../types';
import { emotionText } from '../../utils/display';
import { getErrorMessage } from '../../utils';

const query = reactive<AdminFeedbackPageQuery>({
  pageNum: 1,
  pageSize: 10,
  score: null,
  emotion: '',
  handleStatus: null,
  keyword: ''
});
const rows = ref<AdminFeedbackVO[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const current = ref<AdminFeedbackVO | null>(null);
const form = reactive<FeedbackReplyDTO>({ id: 0, replyContent: '', handleStatus: 1 });

const rules: FormRules<FeedbackReplyDTO> = {
  handleStatus: [{ required: true, message: '请选择处理状态', trigger: 'change' }]
};

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    const data = await pageFeedback(query);
    rows.value = data.list;
    total.value = data.total;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

function openReply(row: AdminFeedbackVO): void {
  current.value = row;
  Object.assign(form, {
    id: row.id,
    replyContent: row.replyContent ?? '',
    handleStatus: row.handleStatus ?? 1
  });
  dialogVisible.value = true;
}

async function submitReply(): Promise<void> {
  const valid = await formRef.value?.validate();
  if (!valid) {
    return;
  }
  saving.value = true;
  try {
    await replyFeedback(form);
    ElMessage.success('处理成功');
    dialogVisible.value = false;
    await loadData();
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    saving.value = false;
  }
}

function resetSearch(): void {
  Object.assign(query, { pageNum: 1, score: null, emotion: '', handleStatus: null, keyword: '' });
  void loadData();
}

function statusText(value: number | null): string {
  if (value === 1) return '已回复';
  if (value === 2) return '已忽略';
  return '待处理';
}

function statusType(value: number | null): 'success' | 'info' | 'warning' {
  if (value === 1) return 'success';
  if (value === 2) return 'info';
  return 'warning';
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">游客反馈/评论</h2>
        <p class="page-description">查看游客评分、情绪倾向和文字反馈，并记录后台回复处理结果。</p>
      </div>
    </header>

    <SearchForm>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="输入反馈内容" />
      </el-form-item>
      <el-form-item label="评分">
        <el-select v-model="query.score" clearable placeholder="全部">
          <el-option v-for="score in [5, 4, 3, 2, 1]" :key="score" :label="`${score} 分`" :value="score" />
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
      <el-form-item label="处理状态">
        <el-select v-model="query.handleStatus" clearable placeholder="全部">
          <el-option label="待处理" :value="0" />
          <el-option label="已回复" :value="1" />
          <el-option label="已忽略" :value="2" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </SearchForm>

    <PageTable>
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column prop="touristName" label="游客昵称" min-width="120" />
        <el-table-column prop="touristPhone" label="手机号" width="140" />
        <el-table-column prop="score" label="评分" width="80" />
        <el-table-column label="情绪" width="110">
          <template #default="{ row }">{{ emotionText(row.emotion) }}</template>
        </el-table-column>
        <el-table-column prop="content" label="反馈内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="处理状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.handleStatus)">{{ statusText(row.handleStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="replyContent" label="回复内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="提交时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReply(row)">处理</el-button>
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

    <el-dialog v-model="dialogVisible" title="处理游客反馈" width="640">
      <el-alert
        v-if="current"
        class="state-panel"
        :closable="false"
        type="info"
        :title="current.content || '暂无反馈内容'"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="处理状态" prop="handleStatus">
          <el-select v-model="form.handleStatus">
            <el-option label="已回复" :value="1" />
            <el-option label="已忽略" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input v-model="form.replyContent" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReply">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
