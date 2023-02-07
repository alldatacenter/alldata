<template>
  <div class="metric-progress">
    <el-popover
      width="720px"
    >
      <template #reference>
        <el-progress
          :percentage="percentage"
          :type="type"
          :width="width"
          :format="format"
          :color="color"
        >
          <template #default="{percentage}">
            <div class="label" :style="{color}">
              <span class="label-icon">
                <cl-icon :icon="labelIcon"/>
              </span>
              <span class="label-text">{{ label?.key }}</span>
            </div>
            <div class="value" :style="{color}">
              {{ format(percentage) }}
            </div>
            <div class="status" :style="{color}">
              <el-tooltip :content="computedStatus?.label">
                <cl-icon :icon="computedStatus?.icon"/>
              </el-tooltip>
            </div>
          </template>
        </el-progress>
      </template>

      <div class="detail-metrics">
        <div class="overview">
          <span class="title">
            {{ label?.title }}:
          </span>
          <el-tooltip :content="computedStatus?.label">
            <span class="value" :style="{color}">
              <cl-icon :icon="computedStatus?.icon"/>
              {{ format(percentage) }}
            </span>
          </el-tooltip>
        </div>

        <el-table class="list" :data="detailMetrics" border max-height="480px">
          <el-table-column
            index="label"
            key="label"
            :label="t('components.metric.progress.detail.name')"
            align="right"
          >
            <template #default="{row}">
              {{ t(`components.metric.metrics.${row.label}`) }}
            </template>
          </el-table-column>
          <el-table-column
            index="value"
            key="value"
            :label="t('components.metric.progress.detail.value')"
            align="right"
          >
            <template #default="{row}">
              {{ row.value }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-popover>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import {emptyArrayFunc, emptyObjectFunc} from '@/utils/func';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'MetricProgress',
  props: {
    percentage: {
      type: Number,
    },
    type: {
      type: String as PropType<MetricProgressType>,
    },
    strokeWidth: {
      type: Number,
    },
    textInside: {
      type: Boolean,
    },
    intermediate: {
      type: Boolean,
    },
    duration: {
      type: Number,
    },
    width: {
      type: Number,
    },
    showText: {
      type: Boolean,
    },
    strokeLinecap: {
      type: String as PropType<MetricProgressStrokeLinecap>,
    },
    format: {
      type: Function as PropType<MetricProgressFormat>,
    },
    label: {
      type: [Object, String] as PropType<MetricProgressLabel | string>,
      default: emptyObjectFunc,
    },
    labelIcon: {
      type: [String, Array] as PropType<Icon>,
    },
    detailMetrics: {
      type: Array as PropType<Metric[]>,
      default: emptyArrayFunc,
    },
    status: {
      type: [Object, Function] as PropType<MetricProgressStatus>,
    },
  },
  setup(props: MetricProgressProps, {emit}) {
    const {t} = useI18n();

    const computedStatus = computed<MetricProgressStatusData | undefined>(() => {
      const {status} = props;
      if (typeof status === 'function') {
        return status(props.percentage || null);
      } else {
        return status;
      }
    });

    const color = computed<string | undefined>(() => computedStatus.value?.color);

    const computedLabel = computed<MetricProgressLabel | undefined>(() => {
      const {label} = props;
      if (typeof label === 'string') {
        return {
          title: label,
          key: label,
        };
      }

      return label;
    });

    return {
      t,
      computedStatus,
      color,
      computedLabel,
    };
  }
});
</script>

<style lang="scss" scoped>
.metric-progress {
  display: inline-flex;
  cursor: pointer;
}

.detail-metrics {
  .overview {
    display: flex;
    align-items: center;
    margin-bottom: 10px;
    font-size: 16px;
    font-weight: 600;
    color: var(--cl-info-medium-color);

    .title {
      margin-right: 10px;
    }

    .value {
      cursor: pointer;
      font-size: 20px;

      .status {
        margin-right: 3px;
      }
    }
  }
}
</style>
<style scoped>
.metric-progress >>> .el-progress__text {
  height: 100%;
  display: flex;
  flex-direction: column;
  /*justify-content: center;*/
}

.metric-progress >>> .el-progress__text .label {
  flex: 0 0 20%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 20%;
  font-size: 12px;
}

.metric-progress >>> .el-progress__text .label .label-icon {
  margin-right: 5px;
}

.metric-progress >>> .el-progress__text .value {
  flex: 0 0 40%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.metric-progress >>> .el-progress__text .status {
  flex: 0 0 10%;
  padding-bottom: 10%;
  font-size: 14px;
}
</style>
