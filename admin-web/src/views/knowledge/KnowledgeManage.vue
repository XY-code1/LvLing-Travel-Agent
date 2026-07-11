<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { UploadFile, UploadRawFile } from 'element-plus';

import PageTable from '../../components/PageTable/PageTable.vue';
import SearchForm from '../../components/SearchForm/SearchForm.vue';
import { pageScenic } from '../../api/scenic';
import {
  changeKnowledgeStatus,
  deleteKnowledge,
  pageKnowledge,
  syncKnowledge,
  testKnowledge,
  uploadKnowledge
} from '../../api/knowledge';
import type { KnowledgeDocumentVO, KnowledgePageQuery, KnowledgeTestVO, ScenicVO } from '../../types';
import { getErrorMessage } from '../../utils';

const query = reactive<KnowledgePageQuery>({
  pageNum: 1,
  pageSize: 10,
  scenicId: null,
  scenicName: '',
  syncStatus: null
});
const rows = ref<KnowledgeDocumentVO[]>([]);
const total = ref(0);
const loading = ref(false);
const uploading = ref(false);
const testing = ref(false);
const syncingIds = ref<Set<number>>(new Set());
const uploadDialogVisible = ref(false);
const testDialogVisible = ref(false);
const uploadScenicId = ref<number | null>(null);
const scenicOptions = ref<ScenicVO[]>([]);
const selectedFile = ref<File | null>(null);
const question = ref('');
const testResult = ref<KnowledgeTestVO | null>(null);

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    const data = await pageKnowledge(query);
    rows.value = data.list;
    total.value = data.total;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

async function loadScenicOptions(): Promise<void> {
  try {
    const data = await pageScenic({ pageNum: 1, pageSize: 100, name: '', status: 1 });
    scenicOptions.value = data.list;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

function handleFileChange(file: UploadFile): void {
  selectedFile.value = file.raw ? (file.raw as UploadRawFile) : null;
}

function clearFile(): void {
  selectedFile.value = null;
}

async function submitUpload(): Promise<void> {
  if (!selectedFile.value) {
    ElMessage.warning('请选择知识库文档');
    return;
  }
  uploading.value = true;
  try {
    await uploadKnowledge(selectedFile.value, uploadScenicId.value);
    ElMessage.success('上传成功');
    uploadDialogVisible.value = false;
    clearFile();
    await loadData();
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    uploading.value = false;
  }
}

async function syncRow(row: KnowledgeDocumentVO): Promise<void> {
  syncingIds.value.add(row.id);
  try {
    const result = await syncKnowledge(row.id);
    ElMessage.success(result.syncMsg || '同步完成');
    await loadData();
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    syncingIds.value.delete(row.id);
  }
}

async function removeRow(row: KnowledgeDocumentVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除文档「${row.fileName}」？`, '删除确认', { type: 'warning' });
    await deleteKnowledge(row.id);
    ElMessage.success('删除成功');
    await loadData();
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(getErrorMessage(error));
    }
  }
}

async function updateStatus(row: KnowledgeDocumentVO, value: string | number | boolean): Promise<void> {
  const status = value ? 1 : 0;
  try {
    await changeKnowledgeStatus({ id: row.id, status });
    row.status = status;
    ElMessage.success('状态已更新');
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

async function submitTest(): Promise<void> {
  if (!question.value.trim()) {
    ElMessage.warning('请输入测试问题');
    return;
  }
  testing.value = true;
  try {
    testResult.value = await testKnowledge(question.value.trim());
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    testing.value = false;
  }
}

function resetSearch(): void {
  Object.assign(query, { pageNum: 1, scenicId: null, scenicName: '', syncStatus: null });
  void loadData();
}

async function batchResync(): Promise<void> {
  const failedRows = rows.value.filter(r => r.embedStatus !== null && r.embedStatus !== 2);
  if (failedRows.length === 0) {
    ElMessage.info('所有文档已生成向量，无需重新同步');
    return;
  }
  ElMessage.info('开始重新同步 ' + failedRows.length + ' 个文档...');
  for (const row of failedRows) {
    syncingIds.value.add(row.id);
    try {
      await syncKnowledge(row.id);
    } catch (error: unknown) {
      console.error('重新同步失败：', row.fileName, error);
    } finally {
      syncingIds.value.delete(row.id);
    }
  }
  await loadData();
  ElMessage.success('全部重新同步完成');
}

function syncType(status: number): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 2) {
    return 'success';
  }
  if (status === 3) {
    return 'danger';
  }
  if (status === 1) {
    return 'warning';
  }
  return 'info';
}

function syncText(status: number): string {
  if (status === 2) {
    return '已同步';
  }
  if (status === 3) {
    return '失败';
  }
  if (status === 1) {
    return '同步中';
  }
  return '待同步';
}

function displayScenicName(row: KnowledgeDocumentVO): string {
  return row.scenicName || '全局知识库';
}

function displaySyncMsg(row: KnowledgeDocumentVO): string {
  if (row.syncMsg) {
    return row.syncMsg;
  }
  return syncText(row.syncStatus);
}

onMounted(() => {
  void loadData();
  void loadScenicOptions();
});
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">本地 RAG 知识库</h2>
        <p class="page-description">上传本地知识素材，写入 MySQL kb_chunk 分块，并使用 LocalRagService 测试召回效果。</p>
      </div>
      <div class="table-actions">
        <el-button @click="testDialogVisible = true">RAG 测试</el-button>
        <el-button :loading="syncingIds.size > 0" @click="batchResync">全部重新同步</el-button>
        <el-button type="primary" @click="uploadDialogVisible = true">上传文档</el-button>
      </div>
    </header>

    <SearchForm>
      <el-form-item label="景区名称">
        <el-input v-model="query.scenicName" clearable placeholder="输入景区名称" />
      </el-form-item>
      <el-form-item label="同步状态">
        <el-select v-model="query.syncStatus" clearable placeholder="全部">
          <el-option label="待同步" :value="0" />
          <el-option label="同步中" :value="1" />
          <el-option label="已同步" :value="2" />
          <el-option label="失败" :value="3" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </SearchForm>

    <PageTable>
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
        <el-table-column label="景区名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ displayScenicName(row) }}</template>
        </el-table-column>
        <el-table-column prop="fileType" label="类型" width="100" />
        <el-table-column label="同步状态" width="120">
          <template #default="{ row }">
            <el-tag :type="syncType(row.syncStatus)">{{ syncText(row.syncStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="向量化" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.embedStatus === 2" type="success">已生成</el-tag>
            <el-tag v-else-if="row.embedStatus === 3" type="danger">失败</el-tag>
            <el-tag v-else type="info">待处理</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="同步消息" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ displaySyncMsg(row) }}</template>
        </el-table-column>
        <el-table-column label="启用" width="110">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="updateStatus(row, $event)" />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
	              <el-button link type="primary" :loading="syncingIds.has(row.id)" @click="syncRow(row)">同步</el-button>
              <el-button link type="danger" @click="removeRow(row)">删除</el-button>
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

    <el-dialog v-model="uploadDialogVisible" title="上传本地 RAG 文档" width="560" @closed="clearFile">
      <el-form label-position="top">
        <el-form-item label="景区名称">
          <el-select v-model="uploadScenicId" clearable filterable placeholder="请选择景区">
            <el-option
              v-for="item in scenicOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文档文件">
          <el-upload :auto-upload="false" :limit="1" :on-change="handleFileChange" :on-remove="clearFile">
            <el-button>选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="testDialogVisible" title="本地 RAG 测试" width="720">
      <el-form label-position="top">
        <el-form-item label="测试问题">
          <el-input v-model="question" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <el-alert v-if="testResult" :closable="false" type="success" show-icon>
        <template #title>
          命中：{{ testResult.hit === 1 ? '是' : '否' }} / 耗时：{{ testResult.costMs }} ms
        </template>
        <p>{{ testResult.answer }}</p>
      </el-alert>
      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testing" @click="submitTest">测试</el-button>
      </template>
    </el-dialog>
  </section>
</template>
