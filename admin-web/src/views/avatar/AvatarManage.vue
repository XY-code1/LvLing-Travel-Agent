<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';

import PageTable from '../../components/PageTable/PageTable.vue';
import { enableAvatar, listAvatar, testAvatar } from '../../api/avatar';
import type { AvatarConfigVO, AvatarTestVO } from '../../types';
import { getErrorMessage } from '../../utils';

const rows = ref<AvatarConfigVO[]>([]);
const loading = ref(false);
const togglingId = ref<number | null>(null);
const testDialogVisible = ref(false);
const testTarget = ref<AvatarConfigVO | null>(null);
const testText = ref('欢迎来到景区，我是你的 AI 导游。');
const testResult = ref<AvatarTestVO | null>(null);

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    rows.value = await listAvatar();
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

async function setEnabled(row: AvatarConfigVO, enabled: number): Promise<void> {
  togglingId.value = row.id;
  try {
    await enableAvatar(row.id, enabled);
    ElMessage.success(enabled === 1 ? '已启用，游客端可以选择该形象' : '已停用，游客端不再展示该形象');
    await loadData();
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  } finally {
    togglingId.value = null;
  }
}

function openTest(row: AvatarConfigVO): void {
  testTarget.value = row;
  testResult.value = null;
  testDialogVisible.value = true;
}

async function submitTest(): Promise<void> {
  if (!testTarget.value) {
    return;
  }
  try {
    testResult.value = await testAvatar(testTarget.value.id, testText.value);
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error));
  }
}

function genderText(value: string | null | undefined): string {
  if (value === 'MALE') {
    return '男性';
  }
  if (value === 'FEMALE') {
    return '女性';
  }
  return '未设置';
}

function voiceText(row: AvatarConfigVO): string {
  if (row.voice === 'xiaogang') {
    return '沉稳男声';
  }
  if (row.voice === 'xiaoyun') {
    return '温柔女声';
  }
  return row.voice || '未设置';
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h2 class="page-title">数字人形象管理</h2>
        <p class="page-description">固定形象库仅支持启用或停用；启用后的形象会出现在游客端，由游客自行选择。</p>
      </div>
    </header>

    <PageTable>
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column prop="name" label="形象名称" min-width="150" />
        <el-table-column label="性别" width="90">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="outfit" label="服饰" min-width="130" />
        <el-table-column label="音色" width="120">
          <template #default="{ row }">{{ voiceText(row) }}</template>
        </el-table-column>
        <el-table-column label="形象图" width="96">
          <template #default="{ row }">
            <el-image
              v-if="row.avatarImage"
              class="table-avatar"
              :src="row.avatarImage"
              fit="contain"
              preview-teleported
              :preview-src-list="[row.avatarImage]"
            />
            <span v-else>未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="游客端可选" width="120">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? '已启用' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openTest(row)">试听</el-button>
              <el-button
                v-if="row.enabled === 1"
                link
                type="warning"
                :loading="togglingId === row.id"
                @click="setEnabled(row, 0)"
              >
                停用
              </el-button>
              <el-button
                v-else
                link
                type="success"
                :loading="togglingId === row.id"
                @click="setEnabled(row, 1)"
              >
                启用
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </PageTable>

    <el-dialog v-model="testDialogVisible" title="试听数字人音色" width="640">
      <el-form label-position="top">
        <el-form-item label="测试文本">
          <el-input v-model="testText" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="testResult"
        :title="testResult.msg"
        :type="testResult.audioUrl ? 'success' : 'warning'"
        show-icon
      />
      <audio v-if="testResult?.audioUrl" class="test-audio" :src="testResult.audioUrl" controls />
      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="submitTest">试听</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.table-avatar {
  width: 46px;
  height: 58px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: #f8fafc;
}

.test-audio {
  width: 100%;
  margin-top: 16px;
}
</style>
