<template>
  <view class="digital-human-panel">
    <view class="digital-human-panel__status"><view class="status-dot" />数字人服务未连接 · Preview Mode</view>
    <view class="digital-human-panel__subtitle">{{ subtitle || '我可以帮你找景点、规划路线。' }}</view>
    <view class="digital-human-panel__actions"><button @tap="emit('text')">文字对话</button><button @tap="emit('voice')">语音入口</button></view>
  </view>
</template>
<script setup lang="ts">
defineProps<{ subtitle?: string }>();
const emit = defineEmits<{ text: []; voice: [] }>();
</script>
<style scoped lang="scss">
.digital-human-panel { margin-top: 12rpx; padding: 16rpx; border-top: 1rpx solid rgba(255,255,255,.22); color: #fff; }
.digital-human-panel__status { color: rgba(255,255,255,.72); font-size: 20rpx; display:flex; align-items:center; gap:8rpx; }
.status-dot { width:10rpx; height:10rpx; border-radius:50%; background:#f2b84b; }
.digital-human-panel__subtitle { margin-top:12rpx; font-size:22rpx; line-height:1.5; }
.digital-human-panel__actions { display:flex; gap:12rpx; margin-top:14rpx; }
button { margin:0; padding:0 18rpx; height:54rpx; line-height:54rpx; border:1rpx solid rgba(255,255,255,.35); border-radius:28rpx; background:rgba(255,255,255,.12); color:#fff; font-size:20rpx; }
</style>
