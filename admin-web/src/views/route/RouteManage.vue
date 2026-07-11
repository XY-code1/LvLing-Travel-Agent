<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';

import PageTable from '../../components/PageTable/PageTable.vue';
import SearchForm from '../../components/SearchForm/SearchForm.vue';
import { pageFeatureItem } from '../../api/featureItem';
import {
  changeRouteStatus,
  createRoute,
  deleteRoute,
  getRouteDetail,
  pageRoute,
  updateRoute,
  updateRouteSpots
} from '../../api/route';
import { pageScenic } from '../../api/scenic';
import { pageSpot } from '../../api/spot';
import type { FeatureItemVO, RouteFormDTO, RoutePageQuery, RouteVO, ScenicVO, SpotVO } from '../../types';
import {
  featureLabelMap,
  managedTagOptions,
  routeTypeOptions,
  routeTypeText,
  splitCodes,
  tagText
} from '../../utils/display';
import { getErrorMessage } from '../../utils';

const routeMeta = useRoute();
const query = reactive<RoutePageQuery>({ pageNum: 1, pageSize: 10, scenicName: '', name: '', type: null, status: null });
const rows = ref<RouteVO[]>([]);
const scenicOptions = ref<ScenicVO[]>([]);
const spotOptions = ref<SpotVO[]>([]);
const routeTypeItems = ref<FeatureItemVO[]>([]);
const interestItems = ref<FeatureItemVO[]>([]);
const total = ref(0);
const loading = ref(false);
const loadingSpots = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const spotsDialogVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<RouteFormDTO>(emptyForm());
const currentRoute = ref<RouteVO | null>(null);
const selectedSpotIds = ref<number[]>([]);
const selectedInterestTags = ref<string[]>([]);
const builderRouteId = ref<number | null>(null);

const isBuilderMode = computed(() => routeMeta.meta.routeMode === 'builder');
const pageTitle = computed(() => isBuilderMode.value ? '自定义路线工具' : '推荐路线列表');
const pageDescription = computed(() => isBuilderMode.value
  ? '选择路线后直接编排景点顺序，用于运营人员生成自定义游览路线。'
  : '维护推荐路线、适配人群、兴趣标签和路线景点顺序。');
const createButtonText = computed(() => isBuilderMode.value ? '新建自定义路线' : '新增路线');
const typeOptions = computed(() => routeTypeOptions(routeTypeItems.value));
const interestOptions = computed(() => managedTagOptions(interestItems.value));
const interestLabels = computed(() => featureLabelMap(interestItems.value));

const rules: FormRules<RouteFormDTO> = {
  scenicId: [{ required: true, message: '请选择所属景区', trigger: 'change' }],
  name: [{ required: true, message: '请输入路线名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择路线类型', trigger: 'change' }]
};

function emptyForm(): RouteFormDTO {
  return {
    scenicId: null,
    name: '',
    type: 1,
    intro: '',
    estimateMinutes: null,
    suitCrowd: '',
    interestTags: '',
    recommendReason: '',
    notice: '',
    status: 1
  };
}

async function loadScenicOptions(): Promise<void> {
  try {
    const data = await pageScenic({ pageNum: 1, pageSize: 1000, name: '', status: null });
    scenicOptions.value = data.list;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

async function loadSpotOptions(scenicId: number): Promise<void> {
  loadingSpots.value = true;
  try {
    const data = await pageSpot({
      pageNum: 1,
      pageSize: 1000,
      scenicId,
      name: '',
      tag: '',
      status: null,
      isHot: null
    });
    spotOptions.value = data.list;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    loadingSpots.value = false;
  }
}

async function loadManagedOptions(): Promise<void> {
  try {
    const [typeData, interestData] = await Promise.all([
      pageFeatureItem({
        pageNum: 1,
        pageSize: 200,
        moduleType: 'ROUTE_TYPE',
        scenicName: '',
        keyword: '',
        category: '',
        status: 1
      }),
      pageFeatureItem({
        pageNum: 1,
        pageSize: 200,
        moduleType: 'ROUTE_INTEREST',
        scenicName: '',
        keyword: '',
        category: '',
        status: 1
      })
    ]);
    routeTypeItems.value = typeData.list;
    interestItems.value = interestData.list;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    const data = await pageRoute(query);
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
  selectedInterestTags.value = [];
  dialogVisible.value = true;
}

function openEdit(row: RouteVO): void {
  Object.assign(form, { ...emptyForm(), ...row });
  selectedInterestTags.value = splitCodes(row.interestTags);
  dialogVisible.value = true;
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate();
  if (!valid || form.scenicId === null || form.type === null) {
    return;
  }
  form.interestTags = selectedInterestTags.value.join(',');
  saving.value = true;
  try {
    if (form.id) {
      await updateRoute({ ...form, id: form.id, scenicId: form.scenicId, type: form.type });
    } else {
      await createRoute({ ...form, scenicId: form.scenicId, type: form.type });
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

async function openSpots(row: RouteVO): Promise<void> {
  try {
    const detail = await getRouteDetail(row.id);
    currentRoute.value = detail;
    selectedSpotIds.value = detail.spots?.map((item) => item.spotId) ?? [];
    await loadSpotOptions(detail.scenicId);
    spotsDialogVisible.value = true;
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

async function saveSpots(): Promise<void> {
  if (!currentRoute.value) {
    return;
  }
  if (!selectedSpotIds.value.length) {
    ElMessage.warning('请选择景点');
    return;
  }
  await updateRouteSpots(currentRoute.value.id, selectedSpotIds.value);
  ElMessage.success('路线景点已更新');
  spotsDialogVisible.value = false;
  await loadData();
}

async function loadBuilderRoute(routeId: number | null): Promise<void> {
  if (!routeId) {
    currentRoute.value = null;
    selectedSpotIds.value = [];
    spotOptions.value = [];
    return;
  }
  try {
    const detail = await getRouteDetail(routeId);
    currentRoute.value = detail;
    selectedSpotIds.value = detail.spots?.map((item) => item.spotId) ?? [];
    await loadSpotOptions(detail.scenicId);
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

async function saveBuilderSpots(): Promise<void> {
  if (!currentRoute.value) {
    ElMessage.warning('请选择路线');
    return;
  }
  if (!selectedSpotIds.value.length) {
    ElMessage.warning('请选择景点');
    return;
  }
  await updateRouteSpots(currentRoute.value.id, selectedSpotIds.value);
  ElMessage.success('自定义路线点位已保存');
  await loadData();
  await loadBuilderRoute(currentRoute.value.id);
}

async function removeRow(row: RouteVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除路线「${row.name}」？`, '删除确认', { type: 'warning' });
    await deleteRoute(row.id);
    ElMessage.success('删除成功');
    await loadData();
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(getErrorMessage(error));
    }
  }
}

async function updateStatus(row: RouteVO, value: string | number | boolean): Promise<void> {
  const status = value ? 1 : 0;
  try {
    await changeRouteStatus({ id: row.id, status });
    row.status = status;
    ElMessage.success('状态已更新');
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

function resetSearch(): void {
  Object.assign(query, { pageNum: 1, scenicName: '', name: '', type: null, status: null });
  void loadData();
}

onMounted(() => {
  void loadScenicOptions();
  void loadManagedOptions();
  void loadData();
});

watch(builderRouteId, (value) => {
  void loadBuilderRoute(value);
});
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">{{ pageTitle }}</h2>
        <p class="page-description">{{ pageDescription }}</p>
      </div>
      <el-button type="primary" @click="openCreate">{{ createButtonText }}</el-button>
    </header>

    <section v-if="isBuilderMode" class="panel">
      <h3 class="panel-title">路线点位编排</h3>
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="选择路线">
            <el-select v-model="builderRouteId" filterable clearable placeholder="请选择要编排的路线">
              <el-option
                v-for="item in rows"
                :key="item.id"
                :label="`${item.scenicName || '未设置景区'} · ${item.name}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="路线类型">
            <el-input :model-value="routeTypeText(currentRoute?.type, typeOptions)" disabled />
          </el-form-item>
          <el-form-item class="form-grid__wide" label="景点顺序">
            <el-select
              v-model="selectedSpotIds"
              multiple
              filterable
              :disabled="!currentRoute"
              :loading="loadingSpots"
              placeholder="按游览顺序选择景点"
            >
              <el-option
                v-for="item in spotOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <div class="table-actions">
        <el-button type="primary" @click="saveBuilderSpots">保存点位顺序</el-button>
        <el-button @click="openCreate">新建路线</el-button>
      </div>
    </section>

    <SearchForm>
      <el-form-item label="景区名称">
        <el-input v-model="query.scenicName" clearable placeholder="输入景区名称" />
      </el-form-item>
      <el-form-item label="路线名称">
        <el-input v-model="query.name" clearable placeholder="输入名称" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.type" clearable placeholder="全部">
          <el-option
            v-for="item in typeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </SearchForm>

    <PageTable>
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column prop="scenicName" label="景区名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="name" label="路线名称" min-width="150" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ routeTypeText(row.type, typeOptions) }}</template>
        </el-table-column>
        <el-table-column prop="estimateMinutes" label="预计分钟" width="110" />
        <el-table-column label="兴趣标签" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ tagText(row.interestTags, interestLabels) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="updateStatus(row, $event)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="openSpots(row)">景点</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑路线' : '新增路线'" width="780">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="所属景区" prop="scenicId">
            <el-select v-model="form.scenicId" filterable placeholder="选择景区">
              <el-option
                v-for="item in scenicOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="路线名称" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="类型" prop="type">
            <el-select v-model="form.type">
              <el-option
                v-for="item in typeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="预计分钟">
            <el-input-number v-model="form.estimateMinutes" controls-position="right" />
          </el-form-item>
          <el-form-item label="适合人群">
            <el-input v-model="form.suitCrowd" />
          </el-form-item>
          <el-form-item label="兴趣标签">
            <el-select v-model="selectedInterestTags" multiple filterable placeholder="请选择兴趣标签">
              <el-option
                v-for="item in interestOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item class="form-grid__wide" label="简介">
            <el-input v-model="form.intro" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item class="form-grid__wide" label="推荐理由">
            <el-input v-model="form.recommendReason" type="textarea" :rows="3" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="spotsDialogVisible" title="配置路线景点" width="560">
      <el-form label-position="top">
        <el-form-item label="所属景区">
          <el-input :model-value="currentRoute?.scenicName ?? ''" disabled />
        </el-form-item>
        <el-form-item label="路线名称">
          <el-input :model-value="currentRoute?.name ?? ''" disabled />
        </el-form-item>
        <el-form-item label="路线景点顺序">
          <el-select
            v-model="selectedSpotIds"
            multiple
            filterable
            :loading="loadingSpots"
            placeholder="按顺序选择景点名称"
          >
            <el-option
              v-for="item in spotOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="spotsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSpots">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
