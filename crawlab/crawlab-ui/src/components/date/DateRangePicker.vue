<template>
  <cl-range-picker
    class-name="date-range-picker"
    type="daterange"
    :model-value="modelValue"
    :options="optionItems"
    @change="value => $emit('change', value)"
  />
</template>

<script lang="ts">
import {computed, defineComponent} from 'vue';
import {rangePickerProps} from '@/components/date/RangePicker.vue';
import {useI18n} from 'vue-i18n';
import dayjs from 'dayjs';

export default defineComponent({
  name: 'DateRangePicker',
  props: {
    ...rangePickerProps,
  },
  emits: [
    'change',
  ],
  setup(props: DateRangePickerProps, {emit}) {
    const {t} = useI18n();

    const optionItems = computed<RangeItem[]>(() => [
      {
        key: t('components.date.dateRangePicker.options.today'),
        value: () => {
          return {
            start: dayjs(),
            end: dayjs(),
          };
        }
      },
      {
        key: t('components.date.dateRangePicker.options.yesterday'),
        value: () => {
          return {
            start: dayjs().subtract(1, 'day'),
            end: dayjs().subtract(1, 'day'),
          };
        }
      },
      {
        key: t('components.date.dateRangePicker.options.pastNDays', {n: 7}),
        value: () => {
          return {
            start: dayjs().subtract(7, 'day'),
            end: dayjs(),
          };
        }
      },
      {
        key: t('components.date.dateRangePicker.options.pastNDays', {n: 14}),
        value: () => {
          return {
            start: dayjs().subtract(14, 'day'),
            end: dayjs(),
          };
        }
      },
      {
        key: t('components.date.dateRangePicker.options.pastNDays', {n: 30}),
        value: () => {
          return {
            start: dayjs().subtract(30, 'day'),
            end: dayjs(),
          };
        }
      },
    ] as RangeItem[]);

    return {
      optionItems,
    };
  }
});
</script>

<style lang="scss" scoped>
</style>
