<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';

import PageTable from '../../components/PageTable/PageTable.vue';
import SearchForm from '../../components/SearchForm/SearchForm.vue';
import { changeTouristUserStatus, pageTouristUser } from '../../api/user';
import type { TouristUserPageQuery, TouristUserVO } from '../../types';
import { getErrorMessage } from '../../utils';

const query = reactive<TouristUserPageQuery>({
  pageNum: 1,
  pageSize: 10,
  phone: '',
  nickname: '',
  status: null
});
const rows = ref<TouristUserVO[]>([]);
const total = ref(0);
const loading = ref(false);

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    const data = await pageTouristUser(query);
    rows.value = data.list;
    total.value = data.total;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

async function updateStatus(row: TouristUserVO, value: string | number | boolean): Promise<void> {
  const status = value ? 1 : 0;
  try {
    await changeTouristUserStatus({ id: row.id, status });
    row.status = status;
    ElMessage.success('状态已更新');
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

function resetSearch(): void {
  Object.assign(query, { pageNum: 1, phone: '', nickname: '', status: null });
  void loadData();
}

function genderText(value: number | null): string {
  if (value === 1) {
    return '男';
  }
  if (value === 2) {
    return '女';
  }
  return '未填';
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-description">游客就是用户，这里管理游客端注册登录账号和启用状态。</p>
      </div>
    </header>

    <SearchForm>
      <el-form-item label="手机号">
        <el-input v-model="query.phone" clearable placeholder="输入手机号" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="query.nickname" clearable placeholder="输入昵称" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </SearchForm>

    <PageTable>
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column label="性别" width="90">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="interestTags" label="兴趣标签" min-width="180" show-overflow-tooltip />
        <el-table-column prop="lastLoginTime" label="最后登录" width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="updateStatus(row, $event)" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
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
