<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus';

import PageTable from '../../components/PageTable/PageTable.vue';
import SearchForm from '../../components/SearchForm/SearchForm.vue';
import { uploadImage } from '../../api/file';
import { pageFeatureItem, createFeatureItem, updateFeatureItem, deleteFeatureItem, changeFeatureItemStatus } from '../../api/featureItem';
import { pageScenic } from '../../api/scenic';
import { pageRoute } from '../../api/route';
import type { FeatureItemFormDTO, FeatureItemPageQuery, FeatureItemVO, RouteVO, ScenicVO } from '../../types';
import { getErrorMessage } from '../../utils';

interface FeatureConfig {
  moduleType: string;
  title: string;
  description: string;
  createText: string;
  titleLabel: string;
  categoryLabel: string;
  contentLabel: string;
  showScenic?: boolean;
  showRoute?: boolean;
  showLocation?: boolean;
  showMedia?: boolean;
}

const route = useRoute();
const config = computed(() => route.meta.feature as FeatureConfig);
const query = reactive<FeatureItemPageQuery>(emptyQuery());
const rows = ref<FeatureItemVO[]>([]);
const scenicOptions = ref<ScenicVO[]>([]);
const routeOptions = ref<RouteVO[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<FeatureItemFormDTO>(emptyForm());

const rules = computed<FormRules<FeatureItemFormDTO>>(() => ({
  title: [{ required: true, message: `请输入${config.value.titleLabel}`, trigger: 'blur' }],
  scenicId: config.value.showScenic
    ? [{ required: true, message: '请选择景区名称', trigger: 'change' }]
    : [],
  relatedId: config.value.showRoute
    ? [{ required: true, message: '请选择关联路线', trigger: 'change' }]
    : []
}));

function emptyQuery(): FeatureItemPageQuery {
  return {
    pageNum: 1,
    pageSize: 10,
    moduleType: '',
    scenicName: '',
    keyword: '',
    category: '',
    status: null
  };
}

function emptyForm(): FeatureItemFormDTO {
  return {
    moduleType: '',
    scenicId: null,
    relatedId: null,
    title: '',
    category: '',
    content: '',
    mediaUrl: '',
    longitude: null,
    latitude: null,
    sortOrder: 0,
    status: 1,
    remark: ''
  };
}

function syncModule(): void {
  query.moduleType = config.value.moduleType;
  form.moduleType = config.value.moduleType;
}

async function loadData(): Promise<void> {
  syncModule();
  loading.value = true;
  try {
    const data = await pageFeatureItem(query);
    rows.value = data.list;
    total.value = data.total;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

async function loadOptions(): Promise<void> {
  try {
    if (config.value.showScenic) {
      const data = await pageScenic({ pageNum: 1, pageSize: 100, name: '', status: 1 });
      scenicOptions.value = data.list;
    }
    if (config.value.showRoute) {
      const data = await pageRoute({ pageNum: 1, pageSize: 100, scenicName: '', name: '', type: null, status: 1 });
      routeOptions.value = data.list;
    }
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

function openCreate(): void {
  Object.assign(form, emptyForm(), { moduleType: config.value.moduleType });
  dialogVisible.value = true;
}

function openEdit(row: FeatureItemVO): void {
  Object.assign(form, {
    ...emptyForm(),
    ...row,
    category: row.category ?? '',
    content: row.content ?? '',
    mediaUrl: row.mediaUrl ?? '',
    remark: row.remark ?? ''
  });
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
      await updateFeatureItem({ ...form, id: form.id });
    } else {
      await createFeatureItem(form);
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

async function removeRow(row: FeatureItemVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除「${row.title}」？`, '删除确认', { type: 'warning' });
    await deleteFeatureItem(row.id);
    ElMessage.success('删除成功');
    await loadData();
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(getErrorMessage(error));
    }
  }
}

async function updateStatus(row: FeatureItemVO, value: string | number | boolean): Promise<void> {
  const status = value ? 1 : 0;
  try {
    await changeFeatureItemStatus({ id: row.id, status });
    row.status = status;
    ElMessage.success('状态已更新');
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

async function uploadMedia(options: UploadRequestOptions): Promise<void> {
  try {
    const result = await uploadImage(options.file as File);
    form.mediaUrl = result.url;
    options.onSuccess?.(result);
    ElMessage.success('图片上传成功');
  } catch (error: unknown) {
    options.onError?.(error as Error);
    ElMessage.error(getErrorMessage(error));
  }
}

function resetSearch(): void {
  Object.assign(query, emptyQuery(), { moduleType: config.value.moduleType });
  void loadData();
}

watch(
  () => route.fullPath,
  () => {
    Object.assign(query, emptyQuery(), { moduleType: config.value.moduleType });
    void loadOptions();
    void loadData();
  }
);

onMounted(() => {
  syncModule();
  void loadOptions();
  void loadData();
});
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">{{ config.title }}</h2>
        <p class="page-description">{{ config.description }}</p>
      </div>
      <el-button type="primary" @click="openCreate">{{ config.createText }}</el-button>
    </header>

    <SearchForm>
      <el-form-item v-if="config.showScenic" label="景区名称">
        <el-input v-model="query.scenicName" clearable placeholder="输入景区名称" />
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable :placeholder="`输入${config.titleLabel}`" />
      </el-form-item>
      <el-form-item :label="config.categoryLabel">
        <el-input v-model="query.category" clearable placeholder="输入分类" />
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
        <el-table-column v-if="config.showScenic" prop="scenicName" label="景区名称" min-width="140" show-overflow-tooltip />
        <el-table-column v-if="config.showRoute" prop="relatedName" label="关联路线" min-width="160" show-overflow-tooltip />
        <el-table-column prop="title" :label="config.titleLabel" min-width="170" show-overflow-tooltip />
        <el-table-column prop="category" :label="config.categoryLabel" min-width="120" show-overflow-tooltip />
        <el-table-column prop="content" :label="config.contentLabel" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑配置' : config.createText" width="760">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item v-if="config.showScenic" label="景区名称" prop="scenicId">
            <el-select v-model="form.scenicId" filterable placeholder="请选择景区">
              <el-option v-for="item in scenicOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="config.showRoute" label="关联路线" prop="relatedId">
            <el-select v-model="form.relatedId" filterable placeholder="请选择路线">
              <el-option v-for="item in routeOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item :label="config.titleLabel" prop="title">
            <el-input v-model="form.title" />
          </el-form-item>
          <el-form-item :label="config.categoryLabel">
            <el-input v-model="form.category" />
          </el-form-item>
          <el-form-item v-if="config.showLocation" label="经度">
            <el-input-number v-model="form.longitude" :precision="6" controls-position="right" />
          </el-form-item>
          <el-form-item v-if="config.showLocation" label="纬度">
            <el-input-number v-model="form.latitude" :precision="6" controls-position="right" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" controls-position="right" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item class="form-grid__wide" :label="config.contentLabel">
            <el-input v-model="form.content" type="textarea" :rows="4" />
          </el-form-item>
          <el-form-item v-if="config.showMedia" class="form-grid__wide" label="图片素材">
            <el-upload :show-file-list="false" accept="image/*" :http-request="uploadMedia">
              <el-button>上传图片</el-button>
            </el-upload>
            <el-image v-if="form.mediaUrl" class="upload-preview" :src="form.mediaUrl" fit="cover" />
          </el-form-item>
          <el-form-item class="form-grid__wide" label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" />
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
