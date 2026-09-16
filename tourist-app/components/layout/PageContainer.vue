<template>
  <view class="page-container" :class="{ 'page-container--wide': wide }">
    <slot />
  </view>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ wide?: boolean }>(), {
  wide: false
});
</script>

<style scoped lang="scss">
.page-container {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 32px;
  box-sizing: border-box;
}

.page-container--wide {
  max-width: 1440px;
}

@media (max-width: 1199px) {
  .page-container {
    padding: 0 28px;
  }
}

@media (max-width: 767px) {
  .page-container {
    padding: 0 20px;
  }
}
</style>
