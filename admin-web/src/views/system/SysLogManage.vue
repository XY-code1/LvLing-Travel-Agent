<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';

import PageTable from '../../components/PageTable/PageTable.vue';
import SearchForm from '../../components/SearchForm/SearchForm.vue';
import { pageSysLog } from '../../api/log';
import type { SysLogPageQuery, SysLogVO } from '../../types';
import { getErrorMessage } from '../../utils';

const query = reactive<SysLogPageQuery>({
  pageNum: 1,
  pageSize: 10,
  logType: '',
  success: null,
  operator: '',
  startTime: '',
  endTime: ''
});
const rows = ref<SysLogVO[]>([]);
const total = ref(0);
const loading = ref(false);
const timeRange = ref<[string, string] | null>(null);

async function loadData(): Promise<void> {
  if (timeRange.value) {
    query.startTime = `${timeRange.value[0]} 00:00:00`;
    query.endTime = `${timeRange.value[1]} 23:59:59`;
  } else {
    query.startTime = '';
    query.endTime = '';
  }
  loading.value = true;
  try {
    const data = await pageSysLog(query);
    rows.value = data.list;
    total.value = data.total;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

function resetSearch(): void {
  Object.assign(query, { pageNum: 1, logType: '', success: null, operator: '', startTime: '', endTime: '' });
  timeRange.value = null;
  void loadData();
}

function logTypeText(value: string): string {
  if (value === 'LOGIN') return '登录';
  if (value === 'OPERATION') return '操作';
  if (value === 'LLM') return '大模型';
  if (value === 'VISION') return '视觉识别';
  if (value === 'TTS') return '语音合成';
  return value || '未知';
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">操作日志</h2>
        <p class="page-description">追踪管理员登录、配置修改、AI 调用和异常信息，便于问题追溯。</p>
      </div>
    </header>

    <SearchForm>
      <el-form-item label="日志类型">
        <el-select v-model="query.logType" clearable placeholder="全部">
          <el-option label="登录" value="LOGIN" />
          <el-option label="操作" value="OPERATION" />
          <el-option label="大模型" value="LLM" />
          <el-option label="视觉识别" value="VISION" />
          <el-option label="语音合成" value="TTS" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作人">
        <el-input v-model="query.operator" clearable placeholder="输入操作人" />
      </el-form-item>
      <el-form-item label="结果">
        <el-select v-model="query.success" clearable placeholder="全部">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间">
        <el-date-picker
          v-model="timeRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </SearchForm>

    <PageTable>
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="日志类型" width="110">
          <template #default="{ row }">{{ logTypeText(row.logType) }}</template>
        </el-table-column>
        <el-table-column prop="bizDesc" label="业务说明" min-width="170" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="110" />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="requestSummary" label="请求摘要" min-width="220" show-overflow-tooltip />
        <el-table-column prop="costMs" label="耗时 ms" width="100" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="发生时间" width="180" />
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
  </section>
</template>
