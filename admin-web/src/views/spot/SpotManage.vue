<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus';

import PageTable from '../../components/PageTable/PageTable.vue';
import SearchForm from '../../components/SearchForm/SearchForm.vue';
import { uploadImage } from '../../api/file';
import { pageFeatureItem } from '../../api/featureItem';
import { pageScenic } from '../../api/scenic';
import { changeSpotStatus, createSpot, deleteSpot, pageSpot, updateSpot } from '../../api/spot';
import type { FeatureItemVO, ScenicVO, SpotFormDTO, SpotPageQuery, SpotVO } from '../../types';
import { featureLabelMap, managedTagOptions, splitCodes, tagText } from '../../utils/display';
import { getErrorMessage } from '../../utils';

const query = reactive<SpotPageQuery>({
  pageNum: 1,
  pageSize: 10,
  scenicId: null,
  scenicName: '',
  name: '',
  tag: '',
  status: null,
  isHot: null
});
const rows = ref<SpotVO[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<SpotFormDTO>(emptyForm());
const scenicOptions = ref<ScenicVO[]>([]);
const tagItems = ref<FeatureItemVO[]>([]);
const selectedTags = ref<string[]>([]);
const spotImageList = computed(() => splitImages(form.images));
const tagOptions = computed(() => managedTagOptions(tagItems.value));
const tagLabels = computed(() => featureLabelMap(tagItems.value));

const rules: FormRules<SpotFormDTO> = {
  scenicId: [{ required: true, message: '请选择景区名称', trigger: 'change' }],
  name: [{ required: true, message: '请输入景点名称', trigger: 'blur' }]
};

function emptyForm(): SpotFormDTO {
  return {
    scenicId: null,
    name: '',
    alias: '',
    intro: '',
    historyCulture: '',
    guideText: '',
    tags: '',
    images: '',
    stayMinutes: null,
    suitCrowd: '',
    isHot: 0,
    longitude: null,
    latitude: null,
    status: 1
  };
}

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    const data = await pageSpot(query);
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

async function loadTagOptions(): Promise<void> {
  try {
    const data = await pageFeatureItem({
      pageNum: 1,
      pageSize: 200,
      moduleType: 'TAG',
      scenicName: '',
      keyword: '',
      category: '',
      status: 1
    });
    tagItems.value = data.list;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

function openCreate(): void {
  Object.assign(form, emptyForm());
  selectedTags.value = [];
  dialogVisible.value = true;
}

function openEdit(row: SpotVO): void {
  Object.assign(form, { ...emptyForm(), ...row });
  selectedTags.value = splitCodes(row.tags);
  dialogVisible.value = true;
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate();
  if (!valid || form.scenicId === null) {
    return;
  }
  form.tags = selectedTags.value.join(',');
  saving.value = true;
  try {
    if (form.id) {
      await updateSpot({ ...form, id: form.id, scenicId: form.scenicId });
    } else {
      await createSpot({ ...form, scenicId: form.scenicId });
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

async function removeRow(row: SpotVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除景点「${row.name}」？`, '删除确认', { type: 'warning' });
    await deleteSpot(row.id);
    ElMessage.success('删除成功');
    await loadData();
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(getErrorMessage(error));
    }
  }
}

async function updateStatus(row: SpotVO, value: string | number | boolean): Promise<void> {
  const status = value ? 1 : 0;
  try {
    await changeSpotStatus({ id: row.id, status });
    row.status = status;
    ElMessage.success('状态已更新');
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

async function uploadSpotImage(options: UploadRequestOptions): Promise<void> {
  try {
    const result = await uploadImage(options.file as File);
    const images = splitImages(form.images);
    images.push(result.url);
    form.images = images.join(',');
    options.onSuccess?.(result);
    ElMessage.success('图片上传成功');
  } catch (error: unknown) {
    options.onError?.(error as Error);
    ElMessage.error(getErrorMessage(error));
  }
}

function splitImages(value: string): string[] {
  return value
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function removeSpotImage(url: string): void {
  form.images = splitImages(form.images).filter((item) => item !== url).join(',');
}

function resetSearch(): void {
  Object.assign(query, { pageNum: 1, scenicId: null, scenicName: '', name: '', tag: '', status: null, isHot: null });
  void loadData();
}

onMounted(() => {
  void loadData();
  void loadScenicOptions();
  void loadTagOptions();
});
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">景点管理</h2>
        <p class="page-description">维护景点讲解词、标签、图片和热门推荐状态。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增景点</el-button>
    </header>

    <SearchForm>
      <el-form-item label="景区名称">
        <el-input v-model="query.scenicName" clearable placeholder="输入景区名称" />
      </el-form-item>
      <el-form-item label="景点名称">
        <el-input v-model="query.name" clearable placeholder="输入名称" />
      </el-form-item>
      <el-form-item label="标签">
        <el-select v-model="query.tag" clearable filterable placeholder="选择标签">
          <el-option
            v-for="item in tagOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="热门">
        <el-select v-model="query.isHot" clearable placeholder="全部">
          <el-option label="热门" :value="1" />
          <el-option label="普通" :value="0" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </SearchForm>

    <PageTable>
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column prop="scenicName" label="景区名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="name" label="景点名称" min-width="150" />
        <el-table-column label="标签" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ tagText(row.tags, tagLabels) }}</template>
        </el-table-column>
        <el-table-column prop="stayMinutes" label="停留分钟" width="110" />
        <el-table-column label="热门" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isHot === 1 ? 'success' : 'info'">
              {{ row.isHot === 1 ? '热门' : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="updateStatus(row, $event)" />
          </template>
        </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑景点' : '新增景点'" width="820">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="景区名称" prop="scenicId">
            <el-select v-model="form.scenicId" filterable placeholder="请选择景区">
              <el-option
                v-for="item in scenicOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="景点名称" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="别名">
            <el-input v-model="form.alias" />
          </el-form-item>
          <el-form-item label="停留分钟">
            <el-input-number v-model="form.stayMinutes" controls-position="right" />
          </el-form-item>
          <el-form-item label="标签">
            <el-select v-model="selectedTags" multiple filterable placeholder="请选择标签">
              <el-option
                v-for="item in tagOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="适合人群">
            <el-input v-model="form.suitCrowd" />
          </el-form-item>
          <el-form-item label="热门">
            <el-select v-model="form.isHot">
              <el-option label="热门" :value="1" />
              <el-option label="普通" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item class="form-grid__wide" label="简介">
            <el-input v-model="form.intro" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item class="form-grid__wide" label="讲解词">
            <el-input v-model="form.guideText" type="textarea" :rows="4" />
          </el-form-item>
          <el-form-item class="form-grid__wide" label="景点图片">
            <el-upload
              :show-file-list="false"
              accept="image/*"
              :http-request="uploadSpotImage"
            >
              <el-button>上传图片</el-button>
            </el-upload>
            <div v-if="spotImageList.length" class="upload-preview-list">
              <el-tag
                v-for="url in spotImageList"
                :key="url"
                closable
                @close="removeSpotImage(url)"
              >
                {{ url }}
              </el-tag>
            </div>
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
