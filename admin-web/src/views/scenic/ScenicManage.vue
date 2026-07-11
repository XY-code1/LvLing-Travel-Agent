<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus';

import PageTable from '../../components/PageTable/PageTable.vue';
import SearchForm from '../../components/SearchForm/SearchForm.vue';
import { uploadImage } from '../../api/file';
import {
  changeScenicStatus,
  createScenic,
  deleteScenic,
  pageScenic,
  updateScenic
} from '../../api/scenic';
import type { ScenicFormDTO, ScenicPageQuery, ScenicVO } from '../../types';
import { getErrorMessage } from '../../utils';

const query = reactive<ScenicPageQuery>({ pageNum: 1, pageSize: 10, name: '', status: null });
const rows = ref<ScenicVO[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<ScenicFormDTO>(emptyForm());

const rules: FormRules<ScenicFormDTO> = {
  name: [{ required: true, message: '请输入景区名称', trigger: 'blur' }]
};

function emptyForm(): ScenicFormDTO {
  return {
    name: '',
    intro: '',
    address: '',
    openTime: '',
    ticketInfo: '',
    trafficInfo: '',
    servicePhone: '',
    notice: '',
    coverImage: '',
    longitude: null,
    latitude: null,
    status: 1
  };
}

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    const data = await pageScenic(query);
    rows.value = data.list;
    total.value = data.total;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

function openCreate(): void {
  Object.assign(form, emptyForm());
  dialogVisible.value = true;
}

function openEdit(row: ScenicVO): void {
  Object.assign(form, { ...emptyForm(), ...row });
  dialogVisible.value = true;
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate();
  if (!valid) {
    return;
  }
  saving.value = true;
  try {
    if (form.id) {
      await updateScenic({ ...form, id: form.id });
    } else {
      await createScenic(form);
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    await loadData();
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    saving.value = false;
  }
}

async function removeRow(row: ScenicVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除景区「${row.name}」？`, '删除确认', { type: 'warning' });
    await deleteScenic(row.id);
    ElMessage.success('删除成功');
    await loadData();
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(getErrorMessage(error));
    }
  }
}

async function updateStatus(row: ScenicVO, value: string | number | boolean): Promise<void> {
  const status = value ? 1 : 0;
  try {
    await changeScenicStatus({ id: row.id, status });
    row.status = status;
    ElMessage.success('状态已更新');
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

async function uploadCoverImage(options: UploadRequestOptions): Promise<void> {
  try {
    const result = await uploadImage(options.file as File);
    form.coverImage = result.url;
    options.onSuccess?.(result);
    ElMessage.success('图片上传成功');
  } catch (error: unknown) {
    options.onError?.(error as Error);
    ElMessage.error(getErrorMessage(error));
  }
}

function resetSearch(): void {
  query.name = '';
  query.status = null;
  query.pageNum = 1;
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
        <h2 class="page-title">景区管理</h2>
        <p class="page-description">维护景区基础信息、开放状态和首页展示素材。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增景区</el-button>
    </header>

    <SearchForm>
      <el-form-item label="景区名称">
        <el-input v-model="query.name" clearable placeholder="输入名称" />
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
        <el-table-column prop="name" label="景区名称" min-width="160" />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="servicePhone" label="服务电话" width="140" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="updateStatus(row, $event)" />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑景区' : '新增景区'" width="720">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="景区名称" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="地址">
            <el-input v-model="form.address" />
          </el-form-item>
          <el-form-item label="服务电话">
            <el-input v-model="form.servicePhone" />
          </el-form-item>
          <el-form-item class="form-grid__wide" label="简介">
            <el-input v-model="form.intro" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item class="form-grid__wide" label="封面图">
            <el-upload
              :show-file-list="false"
              accept="image/*"
              :http-request="uploadCoverImage"
            >
              <el-button>上传图片</el-button>
            </el-upload>
            <el-image
              v-if="form.coverImage"
              class="upload-preview"
              :src="form.coverImage"
              fit="cover"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
