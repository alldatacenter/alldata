<template>
  <cl-range-picker
    class-name="date-time-range-picker"
    type="datetimerange"
    :model-value="modelValue"
    :options="optionItems"
    @change="value => $emit('change', value)"
  />
</template>

<script lang="ts">
import {computed, defineComponent} from 'vue';
import {getRangeItemOption, rangePickerProps} from '@/components/date/RangePicker.vue';
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

    const optionItems = computed<RangeItemOption[]>(() => [
      getRangeItemOption(t('components.date.dateRangePicker.options.pastNMinutes', {n: 10}), 'past-10m', () => {
        return {
          start: dayjs().subtract(10, 'minute'),
          end: dayjs(),
        };
      }),
      getRangeItemOption(t('components.date.dateRangePicker.options.pastNMinutes', {n: 30}), 'past-30m', () => {
        return {
          start: dayjs().subtract(30, 'minute'),
          end: dayjs(),
        };
      }),
      getRangeItemOption(t('components.date.dateRangePicker.options.pastNHours', {n: 1}), 'past-1h', () => {
        return {
          start: dayjs().subtract(1, 'hour'),
          end: dayjs(),
        };
      }),
      getRangeItemOption(t('components.date.dateRangePicker.options.pastNHours', {n: 3}), 'past-3h', () => {
        return {
          start: dayjs().subtract(3, 'hour'),
          end: dayjs(),
        };
      }),
      getRangeItemOption(t('components.date.dateRangePicker.options.pastNHours', {n: 6}), 'past-6h', () => {
        return {
          start: dayjs().subtract(6, 'hour'),
          end: dayjs(),
        };
      }),
      getRangeItemOption(t('components.date.dateRangePicker.options.pastNHours', {n: 12}), 'past-12h', () => {
        return {
          start: dayjs().subtract(12, 'hour'),
          end: dayjs(),
        };
      }),
      getRangeItemOption(t('components.date.dateRangePicker.options.pastNDays', {n: 1}), 'past-1d', () => {
        return {
          start: dayjs().subtract(1, 'day'),
          end: dayjs(),
        };
      }),
      getRangeItemOption(t('components.date.dateRangePicker.options.pastNDays', {n: 3}), 'past-3d', () => {
        return {
          start: dayjs().subtract(3, 'day'),
          end: dayjs(),
        };
      }),
      getRangeItemOption(t('components.date.dateRangePicker.options.pastNDays', {n: 7}), 'past-7d', () => {
        return {
          start: dayjs().subtract(7, 'day'),
          end: dayjs(),
        };
      }),
      // {
      //   label: t('components.date.dateRangePicker.options.custom'),
      //   value: {
      //     key: 'custom'
      //   },
      // }
    ] as RangeItemOption[]);

    return {
      optionItems,
    };
  }
});
</script>

<style lang="scss" scoped>
</style>
