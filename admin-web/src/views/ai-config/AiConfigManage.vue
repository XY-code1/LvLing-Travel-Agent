<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules, UploadFile } from 'element-plus';

import PageTable from '../../components/PageTable/PageTable.vue';
import SearchForm from '../../components/SearchForm/SearchForm.vue';
import {
  createAiConfig,
  deleteAiConfig,
  enableAiConfig,
  listAiConfig,
  testAiConfig,
  testVisionAiConfig,
  updateAiConfig
} from '../../api/aiConfig';
import type { AiConfigFormDTO, AiConfigTestDTO, AiConfigTestVO, AiConfigVO } from '../../types';
import { getErrorMessage } from '../../utils';

interface AiRouteMeta {
  aiServiceType?: string;
  aiTitle?: string;
  aiDescription?: string;
  lockedProvider?: string;
}

interface OptionItem {
  label: string;
  value: string;
}

const route = useRoute();
const serviceTypes: OptionItem[] = [
  { label: '本地向量模型', value: 'EMBEDDING' },
  { label: '文本大模型', value: 'LLM' },
  { label: '多模态模型', value: 'VISION' },
  { label: 'ASR 语音识别', value: 'ASR' },
  { label: 'TTS 语音合成', value: 'TTS' }
];
const protocols: OptionItem[] = [
  { label: 'OpenAI-Compatible', value: 'OPENAI_COMPATIBLE' },
  { label: 'Anthropic-Compatible', value: 'ANTHROPIC_COMPATIBLE' }
];
const aliServiceTypes = new Set(['ASR', 'TTS']);
const modelServiceTypes = new Set(['EMBEDDING', 'LLM', 'VISION']);
const pageMeta = computed(() => route.meta as unknown as AiRouteMeta);
const lockedServiceType = computed(() => String(pageMeta.value.aiServiceType || ''));
const lockedProvider = computed(() => String(pageMeta.value.lockedProvider || ''));
const isLockedPage = computed(() => Boolean(lockedServiceType.value));
const pageTitle = computed(() => pageMeta.value.aiTitle || 'AI 服务配置');
const pageDescription = computed(() => pageMeta.value.aiDescription || '维护外部 AI 服务配置；每类能力同一时间仅允许启用一个全局默认配置。');

const rows = ref<AiConfigVO[]>([]);
const serviceType = ref('');
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const testDialogVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<AiConfigFormDTO>(emptyForm());
const testTarget = ref<AiConfigVO | null>(null);
const testForm = reactive<AiConfigTestDTO>({ question: '', imageUrl: '', prompt: '' });
const testImageName = ref('');
const testImageFile = ref<File | null>(null);
const testImagePreview = ref('');
const testResult = ref<AiConfigTestVO | null>(null);
const testLoading = ref(false);

const formTitle = computed(() => `${form.id ? '编辑' : '新增'}${pageTitle.value}`);
const isModelForm = computed(() => modelServiceTypes.has(form.serviceType));
const isAliForm = computed(() => aliServiceTypes.has(form.serviceType));
const formProtocols = computed(() => form.serviceType === 'EMBEDDING'
  ? protocols.filter((item) => item.value === 'OPENAI_COMPATIBLE')
  : protocols);
const testServiceType = computed(() => testTarget.value?.serviceType || lockedServiceType.value);
const isVisionTest = computed(() => testServiceType.value === 'VISION');
const isLlmTest = computed(() => testServiceType.value === 'LLM');
const isEmbeddingTest = computed(() => testServiceType.value === 'EMBEDDING');

const rules: FormRules<AiConfigFormDTO> = {
  serviceType: [{ required: true, message: '请选择服务类型', trigger: 'change' }],
  provider: [{ required: true, message: '请输入服务商', trigger: 'blur' }],
  protocol: [
    {
      validator: (_rule, value, callback) => {
        if (modelServiceTypes.has(form.serviceType) && !value) {
          callback(new Error('请选择协议'));
          return;
        }
        callback();
      },
      trigger: 'change'
    }
  ],
  baseUrl: [
    {
      validator: (_rule, value, callback) => {
        if (modelServiceTypes.has(form.serviceType) && !value) {
          callback(new Error('请输入 Base URL'));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ],
  modelName: [
    {
      validator: (_rule, value, callback) => {
        if (modelServiceTypes.has(form.serviceType) && !value) {
          callback(new Error('请输入模型名'));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ],
  accessKeyId: [
    {
      validator: (_rule, value, callback) => {
        if (aliServiceTypes.has(form.serviceType) && !value && !form.id) {
          callback(new Error('请输入 AccessKeyId'));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ],
  accessKeySecret: [
    {
      validator: (_rule, value, callback) => {
        if (aliServiceTypes.has(form.serviceType) && !value && !form.id) {
          callback(new Error('请输入 AccessKeySecret'));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ],
  appKey: [
    {
      validator: (_rule, value, callback) => {
        if (aliServiceTypes.has(form.serviceType) && !value) {
          callback(new Error('请输入 AppKey / AppId'));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ],
  region: [
    {
      validator: (_rule, value, callback) => {
        if (aliServiceTypes.has(form.serviceType) && !value) {
          callback(new Error('请输入服务区域'));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ]
};

function emptyForm(): AiConfigFormDTO {
  return {
    serviceType: lockedServiceType.value || 'LLM',
    provider: lockedProvider.value,
    protocol: '',
    baseUrl: '',
    apiKey: '',
    accessKeyId: '',
    accessKeySecret: '',
    appKey: '',
    region: '',
    modelName: '',
    datasetId: '',
    extraConfig: '',
    timeoutMs: 10000,
    retryCount: 1,
    enabled: 0,
    remark: ''
  };
}

function serviceLabel(value: string | null | undefined): string {
  return serviceTypes.find((item) => item.value === value)?.label || value || '未设置';
}

function protocolLabel(value: string | null | undefined): string {
  return protocols.find((item) => item.value === value)?.label || value || '无';
}

function credentialText(row: AiConfigVO): string {
  if (aliServiceTypes.has(row.serviceType)) {
    return row.accessKeyId ? `${row.accessKeyId} / ${row.hasSecret ? 'Secret 已配置' : 'Secret 未配置'}` : '未配置';
  }
  return row.apiKey || '未配置';
}

function applyLockedDefaults(): void {
  if (lockedServiceType.value) {
    form.serviceType = lockedServiceType.value;
  }
  if (lockedProvider.value) {
    form.provider = lockedProvider.value;
  }
  if (modelServiceTypes.has(form.serviceType) && !form.protocol) {
    form.protocol = 'OPENAI_COMPATIBLE';
  }
  if (form.serviceType === 'EMBEDDING') {
    form.protocol = 'OPENAI_COMPATIBLE';
  }
  if (aliServiceTypes.has(form.serviceType)) {
    form.protocol = '';
    form.baseUrl = '';
    form.apiKey = '';
    form.modelName = '';
    form.datasetId = '';
  }
}

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    const queryType = lockedServiceType.value || serviceType.value || undefined;
    rows.value = await listAiConfig(queryType);
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

function openCreate(): void {
  Object.assign(form, emptyForm());
  applyLockedDefaults();
  dialogVisible.value = true;
}

function openEdit(row: AiConfigVO): void {
  Object.assign(form, { ...emptyForm(), ...row, apiKey: '', accessKeyId: '', accessKeySecret: '' });
  applyLockedDefaults();
  dialogVisible.value = true;
}

async function submitForm(): Promise<void> {
  applyLockedDefaults();
  const valid = await formRef.value?.validate();
  if (!valid) {
    return;
  }
  saving.value = true;
  try {
    if (form.id) {
      await updateAiConfig({ ...form, id: form.id });
    } else {
      await createAiConfig(form);
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

async function removeRow(row: AiConfigVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除「${row.provider} / ${serviceLabel(row.serviceType)}」？`, '删除确认', { type: 'warning' });
    await deleteAiConfig(row.id);
    ElMessage.success('删除成功');
    await loadData();
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(getErrorMessage(error));
    }
  }
}

async function enableRow(row: AiConfigVO): Promise<void> {
  try {
    await enableAiConfig(row.id);
    ElMessage.success('已设为全局启用配置');
    await loadData();
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

function openTest(row: AiConfigVO): void {
  testTarget.value = row;
  testResult.value = null;
  testLoading.value = false;
  clearVisionImage();
  Object.assign(testForm, { question: '你好', imageUrl: '', imageBase64: '', mimeType: '', prompt: '查看图片' });
  testDialogVisible.value = true;
}

async function submitTest(): Promise<void> {
  if (!testTarget.value) {
    return;
  }
  testLoading.value = true;
  testResult.value = null;
  try {
    if (isVisionTest.value) {
      if (!testImageFile.value) {
        ElMessage.warning('请选择测试图片');
        return;
      }
      testResult.value = await testVisionAiConfig(testTarget.value.id, testImageFile.value, testForm.prompt || '');
    } else {
      testResult.value = await testAiConfig(testTarget.value.id, testForm);
    }
    await loadData();
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    testLoading.value = false;
  }
}

function handleVisionImageChange(file: UploadFile): void {
  const raw = file.raw;
  if (!raw) {
    return;
  }
  if (!raw.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件');
    return;
  }
  if (testImagePreview.value) {
    URL.revokeObjectURL(testImagePreview.value);
  }
  testImageFile.value = raw;
  testImageName.value = raw.name;
  testImagePreview.value = URL.createObjectURL(raw);
}

function clearVisionImage(): void {
  if (testImagePreview.value) {
    URL.revokeObjectURL(testImagePreview.value);
  }
  testImageFile.value = null;
  testImageName.value = '';
  testImagePreview.value = '';
  testForm.imageBase64 = '';
  testForm.mimeType = '';
}

watch(() => form.serviceType, () => {
  applyLockedDefaults();
});

watch(() => route.fullPath, () => {
  serviceType.value = '';
  dialogVisible.value = false;
  testDialogVisible.value = false;
  void loadData();
});

onMounted(() => {
  void loadData();
});

onBeforeUnmount(() => {
  clearVisionImage();
});
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">{{ pageTitle }}</h2>
        <p class="page-description">{{ pageDescription }}</p>
      </div>
      <el-button type="primary" @click="openCreate">新增配置</el-button>
    </header>

    <SearchForm v-if="!isLockedPage">
      <el-form-item label="服务类型">
        <el-select v-model="serviceType" clearable placeholder="全部">
          <el-option v-for="item in serviceTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
    </SearchForm>

    <PageTable>
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column v-if="!isLockedPage" label="类型" width="140">
          <template #default="{ row }">{{ serviceLabel(row.serviceType) }}</template>
        </el-table-column>
        <el-table-column prop="provider" label="服务商" min-width="150" />
        <el-table-column label="协议" width="170">
          <template #default="{ row }">{{ protocolLabel(row.protocol) }}</template>
        </el-table-column>
        <el-table-column prop="modelName" label="模型/实例" min-width="150" show-overflow-tooltip />
        <el-table-column label="凭证" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ credentialText(row) }}</template>
        </el-table-column>
        <el-table-column label="能力" width="90">
          <template #default="{ row }">
            <el-tag :type="row.capabilityVerified === 1 ? 'success' : 'warning'">
              {{ row.capabilityVerified === 1 ? '通过' : '未通过' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="全局启用" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" type="success">当前启用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="verifyMsg" label="校验消息" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="openTest(row)">测试</el-button>
              <el-button v-if="row.isDefault !== 1" link type="primary" @click="enableRow(row)">全局启用</el-button>
              <el-button v-else link type="success" disabled>已启用</el-button>
              <el-button link type="danger" @click="removeRow(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </PageTable>

    <el-dialog v-model="dialogVisible" :title="formTitle" width="860">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="服务类型" prop="serviceType">
            <el-input v-if="isLockedPage" :model-value="serviceLabel(form.serviceType)" disabled />
            <el-select v-else v-model="form.serviceType">
              <el-option v-for="item in serviceTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="服务商" prop="provider">
            <el-input v-model="form.provider" :disabled="Boolean(lockedProvider)" />
          </el-form-item>
          <el-form-item v-if="isModelForm" label="协议" prop="protocol">
            <el-select v-model="form.protocol">
              <el-option v-for="item in formProtocols" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="isModelForm" label="模型名" prop="modelName">
            <el-input v-model="form.modelName" />
          </el-form-item>
          <el-form-item v-if="isModelForm" class="form-grid__wide" label="Base URL" prop="baseUrl">
            <el-input v-model="form.baseUrl" />
          </el-form-item>
          <el-form-item v-if="isModelForm" label="API Key">
            <el-input v-model="form.apiKey" show-password placeholder="编辑时留空表示沿用原值" />
          </el-form-item>
          <el-form-item v-if="isAliForm" label="AccessKeyId" prop="accessKeyId">
            <el-input v-model="form.accessKeyId" show-password placeholder="编辑时留空表示沿用原值" />
          </el-form-item>
          <el-form-item v-if="isAliForm" label="AccessKeySecret" prop="accessKeySecret">
            <el-input v-model="form.accessKeySecret" show-password placeholder="编辑时留空表示沿用原值" />
          </el-form-item>
          <el-form-item v-if="isAliForm" label="AppKey / AppId" prop="appKey">
            <el-input v-model="form.appKey" />
          </el-form-item>
          <el-form-item v-if="isAliForm" label="Region" prop="region">
            <el-input v-model="form.region" />
          </el-form-item>
          <el-form-item label="超时毫秒">
            <el-input-number v-model="form.timeoutMs" controls-position="right" />
          </el-form-item>
          <el-form-item label="重试次数">
            <el-input-number v-model="form.retryCount" controls-position="right" />
          </el-form-item>
          <el-form-item class="form-grid__wide" label="扩展配置 JSON">
            <el-input v-model="form.extraConfig" type="textarea" :rows="3" />
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

    <el-dialog v-model="testDialogVisible" title="连通性测试" width="680">
      <el-form label-position="top">
        <el-form-item v-if="isLlmTest" label="测试问题">
          <el-input :model-value="'你好'" disabled />
        </el-form-item>
        <el-form-item v-if="isEmbeddingTest" label="测试文本">
          <el-input :model-value="'你好'" disabled placeholder="用于测试本地向量模型是否能返回向量" />
        </el-form-item>
        <template v-if="isVisionTest">
          <el-form-item label="测试图片">
            <el-upload
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              :on-change="handleVisionImageChange"
              :on-remove="clearVisionImage"
            >
              <el-button>选择图片</el-button>
            </el-upload>
            <span v-if="testImageName" class="upload-file-name">{{ testImageName }}</span>
            <el-image v-if="testImagePreview" class="upload-preview" :src="testImagePreview" fit="cover" />
          </el-form-item>
          <el-form-item label="视觉提示词">
            <el-input v-model="testForm.prompt" />
          </el-form-item>
        </template>
        <el-alert
          v-if="!isLlmTest && !isVisionTest && !isEmbeddingTest"
          :closable="false"
          type="info"
          show-icon
          title="当前能力使用阿里云凭证完整性校验；真实调用由后端业务链路执行。"
        />
      </el-form>
      <el-alert v-if="testResult" :title="testResult.msg" :type="testResult.success ? 'success' : 'error'" show-icon />
      <el-alert v-if="testLoading" :closable="false" type="warning" show-icon title="测试中，请稍候..." />
      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testLoading" @click="submitTest">测试</el-button>
      </template>
    </el-dialog>
  </section>
</template>
