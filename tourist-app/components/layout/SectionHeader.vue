<template>
  <view class="section-header" :class="{ 'section-header--center': align === 'center' }">
    <text v-if="eyebrow" class="section-header__eyebrow">{{ eyebrow }}</text>
    <text class="section-header__title">{{ title }}</text>
    <text v-if="subtitle" class="section-header__subtitle">{{ subtitle }}</text>
  </view>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  eyebrow?: string;
  title: string;
  subtitle?: string;
  align?: 'left' | 'center';
}>(), {
  eyebrow: '',
  subtitle: '',
  align: 'left'
});
</script>

<style scoped lang="scss">
.section-header {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.section-header--center {
  align-items: center;
  text-align: center;
}

.section-header__eyebrow {
  color: var(--jade-500);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .14em;
  text-transform: uppercase;
}

.section-header__title {
  color: var(--text-main);
  font-size: 28px;
  font-weight: 800;
  line-height: 1.25;
}

.section-header__subtitle {
  max-width: 680px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

@media (max-width: 767px) {
  .section-header__title {
    font-size: 23px;
  }

  .section-header__subtitle {
    font-size: 13px;
  }
}
</style>
