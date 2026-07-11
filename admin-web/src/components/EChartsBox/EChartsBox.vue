<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { LineChart, PieChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { init, use } from 'echarts/core';
import type { ECharts, EChartsCoreOption } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';

use([LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

const props = defineProps<{
  option: EChartsCoreOption;
}>();

const elRef = ref<HTMLDivElement>();
const chart = ref<ECharts | null>(null);

function renderChart(): void {
  if (!chart.value) {
    return;
  }
  chart.value.setOption(props.option, true);
}

function resizeChart(): void {
  chart.value?.resize();
}

onMounted(async () => {
  await nextTick();
  if (elRef.value) {
    chart.value = init(elRef.value);
    renderChart();
    window.addEventListener('resize', resizeChart);
  }
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart);
  chart.value?.dispose();
});

watch(
  () => props.option,
  () => renderChart(),
  { deep: true }
);
</script>

<template>
  <div ref="elRef" class="chart-box" />
</template>
