<template>
  <div class="spider-stat">
    <cl-tag
      :icon="['fa', 'tasks']"
      :label="labels.tasks"
      :tooltip="tooltips.tasks"
      type="primary"
      clickable
      @click="() => $emit('tasks-click')"
    />
    <cl-tag
      :icon="['fa', 'database']"
      :label="labels.results"
      :tooltip="tooltips.results"
      type="success"
      clickable
      @click="() => $emit('results-click')"
    />
    <cl-tag
      :icon="['fa', 'stopwatch']"
      :label="labels.duration"
      type="warning"
      clickable
      @click="() => $emit('duration-click')"
    >
      <template #tooltip>
        <div v-html="tooltips.duration"/>
      </template>
    </cl-tag>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import humanizeDuration from 'humanize-duration';
import {useI18n} from 'vue-i18n';
import {getLanguage} from '@/utils/i18n';

export default defineComponent({
  name: 'SpiderStats',
  props: {
    stat: {
      type: Object as PropType<SpiderStat>,
      required: false,
    },
  },
  emits: [
    'tasks-click',
    'results-click',
    'duration-click',
  ],
  setup(props: SpiderStatProps, {emit}) {
    // i18n
    const {t} = useI18n();

    const labels = computed<SpiderStatLabels>(() => {
      const {stat} = props;
      const {
        tasks,
        results,
        // average_wait_duration,
        // average_runtime_duration,
        average_total_duration,
      } = stat as SpiderStat;

      return {
        tasks: `${tasks}`,
        results: `${results}`,
        duration: `${average_total_duration}`
      };
    });

    const tooltips = computed<SpiderStatTooltips>(() => {
      const {stat} = props;
      const {
        tasks,
        results,
        average_wait_duration,
        average_runtime_duration,
        average_total_duration,
      } = stat as SpiderStat;

      const language = getLanguage();

      return {
        tasks: `${t('components.spider.stat.totalTasks')}: ${tasks}`,
        results: `${t('components.spider.stat.totalResults')}: ${results}`,
        duration: `
<span class="label">${t('components.spider.stat.averageWaitDuration')}:</span>
<span class="value" style="color: var(--cl-blue)">${humanizeDuration(average_wait_duration * 1000, {
          spacer: ' ',
          language
        })}</span><br>
<span class="label">${t('components.spider.stat.averageRuntimeDuration')}:</span>
<span class="value" style="color: var(--cl-orange)">${humanizeDuration(average_runtime_duration * 1000, {
          spacer: ' ',
          language
        })}</span><br>
<span class="label">${t('components.spider.stat.averageRuntimeDuration')}:</span>
<span class="value" style="color: var(--cl-white)">${humanizeDuration(average_total_duration * 1000, {
          spacer: ' ',
          language
        })}</span><br>
`,
      };
    });

    return {
      tooltips,
      labels,
    };
  },
});
</script>

<style lang="scss" scoped>
.spider-stat {
}
</style>

<style scoped>
</style>
