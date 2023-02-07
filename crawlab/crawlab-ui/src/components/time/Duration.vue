<template>
  <div v-if="!isTag" class="duration">
    {{ label }}
  </div>
  <cl-tag
    v-else
    class-name="duration"
    :icon="icon"
    :label="label"
    :size="size"
    :tooltip="tooltip"
    :type="type"
  />
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import humanizeDuration from 'humanize-duration';
import {getLanguage} from '@/utils/i18n';

export default defineComponent({
  name: 'Duration',
  props: {
    duration: {
      type: Number,
      required: false,
    },
    isTag: {
      type: Boolean,
    },
    size: {
      type: String as PropType<BasicSize>,
      default: 'default',
    },
    tooltip: {
      type: String,
    },
    icon: {
      type: [String, Array] as PropType<Icon>,
      default: () => {
        return ['fa', 'stopwatch'];
      }
    },
    type: {
      type: String as PropType<BasicType>,
      default: 'primary',
    }
  },
  setup(props: DurationProps, {emit}) {
    const label = computed<string>(() => {
      const {duration} = props;

      const d = Math.ceil((duration as number) / 1000) * 1000;

      const language = getLanguage();

      return humanizeDuration(d, {
        spacer: ' ',
        language,
      });
    });

    return {
      label,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
