<template>
  <cl-tag
    v-if="!iconOnly"
    :key="data"
    :icon="data.icon"
    :label="data.label"
    :size="size"
    :spinning="data.spinning"
    :type="data.type"
    class="schedule-cron"
    @click="$emit('click')"
  >
    <template #tooltip>
      <div v-html="data.tooltip"/>
    </template>
  </cl-tag>
  <div v-else :class="[isValid ? 'valid' : 'invalid']" class="schedule-cron">
    <div class="row">
      <span class="title">
        <el-tooltip :content="t('components.schedule.cron.title.cronDescription')">
          <font-awesome-icon :icon="['fa', 'info-circle']" class="description"/>
        </el-tooltip>
      </span>
      <span class="value description">
        {{ isValid ? description : t('common.status.invalid') }}
      </span>
    </div>
    <div class="row">
      <span class="title">
        <el-tooltip :content="t('components.schedule.cron.title.nextRun')">
          <font-awesome-icon :icon="['fa', 'arrow-right']" class="next"/>
        </el-tooltip>
      </span>
      <span class="value next">
        {{ isValid ? next : t('common.status.invalid') }}
      </span>
    </div>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import {CronExpression, parseExpression} from 'cron-parser';
import cronstrue from 'cronstrue/i18n';
import dayjs from 'dayjs';
import en from 'dayjs/locale/en.js';
import zh from 'dayjs/locale/zh.js';
import localizedFormat from 'dayjs/plugin/localizedFormat';
import {useI18n} from 'vue-i18n';
import {getI18n} from '@/i18n';

dayjs.extend(localizedFormat);

export default defineComponent({
  name: 'ScheduleCron',
  props: {
    cron: {
      type: String,
      required: false,
    },
    size: {
      type: String as PropType<BasicSize>,
      required: false,
      default: 'default',
    },
    iconOnly: {
      type: Boolean,
      required: false,
      default: false,
    },
  },
  setup(props: ScheduleCronProps, {emit}) {
    // i18n
    const {t} = useI18n();

    const isValid = computed((): boolean => {
      const {cron} = props;
      if (!cron) return false;
      try {
        parseExpression(cron);
      } catch (e) {
        return false;
      }
      try {
        cronstrue.toString(cron);
      } catch (e) {
        return false;
      }
      return true;
    });

    const interval = computed<CronExpression | undefined>(() => {
      const {cron} = props;
      if (!cron) return;
      try {
        return parseExpression(cron);
      } catch (e) {
        return;
      }
    });

    const next = computed<string | undefined>(() => {
      if (!interval.value || !isValid.value) {
        return t('common.status.unknown');
      }
      if (getI18n().global.locale.value === 'zh') {
        return dayjs(interval.value.next().getTime()).locale(zh)?.format('llll');
      } else {
        return dayjs(interval.value.next().getTime())?.format('llll');
      }
    });

    const description = computed<string | undefined>(() => {
      const {cron} = props;
      if (!cron) return;
      try {
        return cronstrue.toString(cron, {
          locale: getI18n().global.locale.value === 'zh' ? 'zh_CN' : 'en',
        });
      } catch (e) {
        return t('components.schedule.rules.message.invalidCronExpression');
      }
    });

    const tooltip = computed<string>(() => `<span class="title">${t('components.schedule.cron.title.cron')}: </span><span style="color: var(--cl-blue)">${props.cron}</span><br>
<span class="title">${t('components.schedule.cron.title.description')}: </span><span style="color: var(--cl-orange)">${description.value}</span><br>
<span class="title">${t('components.schedule.cron.title.next')}: </span><span style="color: var(--cl-green)">${next.value}</span>`);

    const data = computed<TagData>(() => {
      const {cron} = props;
      if (!isValid.value) {
        return {
          label: t('common.status.unknown'),
          tooltip: t('common.status.unknown'),
          type: 'info',
        };
      }

      return {
        label: cron,
        tooltip: tooltip.value,
        type: 'primary',
      };
    });

    return {
      data,
      next,
      description,
      isValid,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>
.schedule-cron {
  .row {
    min-height: 20px;

    .title {
      display: inline-block;
      width: 18px;
      text-align: right;
      font-size: 14px;
      margin-right: 10px;
    }

    .value {
      font-size: 14px;
    }

    .description {
      color: var(--cl-warning-color);
    }

    .next {
      color: var(--cl-success-color);
    }
  }

  &.invalid {
    .description,
    .next {
      color: var(--cl-info-medium-color);
    }
  }
}
</style>
