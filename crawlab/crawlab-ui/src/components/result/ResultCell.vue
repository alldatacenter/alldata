<template>
  <el-tooltip>
    <!--tooltip-->
    <template #content>
      <div v-html="tooltip"/>
    </template>
    <!--./tooltip-->

    <!--content-->
    <div :class="cls" @click="onClick">
      <template v-if="type === DATA_FIELD_TYPE_IMAGE">
        <a class="result-cell-image" :href="value" target="_blank">
          <img :src="value"/>
        </a>
      </template>

      <template v-else-if="type === DATA_FIELD_TYPE_URL">
        <div class="result-cell-url">
          <font-awesome-icon class="icon" :icon="['fa', 'link']"/>
          <a :href="value" target="_blank">
            {{ value }}
          </a>
        </div>
      </template>

      <template v-else-if="type === DATA_FIELD_TYPE_HTML">
        <div v-html="value"/>
      </template>

      <template v-else-if="type === DATA_FIELD_TYPE_LONG_TEXT">
        <div class="result-cell-long-text">
          {{ value }}
        </div>
      </template>

      <template v-else>
        {{ value }}
      </template>
    </div>
    <!--./content-->

  </el-tooltip>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import {
  DATA_FIELD_TYPE_GENERAL,
  DATA_FIELD_TYPE_IMAGE,
  DATA_FIELD_TYPE_URL,
  DATA_FIELD_TYPE_HTML,
  DATA_FIELD_TYPE_LONG_TEXT, DATA_FIELD_TYPE_TIME,
} from "@/constants/dataFields";
import {translate} from "@/utils";
import {useStore} from 'vuex';
import dayjs from 'dayjs';
import {getI18n} from "@/i18n";
import TimeAgo from 'javascript-time-ago';

const t = translate;

export default defineComponent({
  name: 'ResultCell',
  props: {
    fieldKey: {
      type: String,
    },
    type: {
      type: String as PropType<DataFieldType>,
      default: DATA_FIELD_TYPE_GENERAL,
    },
    value: {
      type: [String, Number, Boolean, Array, Object],
    },
  },
  emits: ['click'],
  setup(props: ResultCellProps, {emit}) {
    // store
    const ns = 'dataCollection';
    const store = useStore();

    // tooltip
    const tooltipValue = computed<string>(() => {
      switch (props.type) {
        case DATA_FIELD_TYPE_LONG_TEXT:
          return props.value.substring(0, 200) + '...';
        case DATA_FIELD_TYPE_TIME:
          const time = dayjs(props.value)
          const timeAgo = new TimeAgo(getI18n().global.locale.value === 'zh' ? 'zh' : 'en');
          const ago = timeAgo.format(time.toDate()) as string;
          return `${time.format('YYYY-MM-DD HH:mm:ssZ')} (${ago})`;
        default:
          return typeof props.value === 'string' ? props.value : JSON.stringify(props.value);
      }
    });
    const tooltip = computed<string>(() => {
      return `
<label style="margin-right: 5px;">${t('components.result.form.dataType')}:</label>
<div style="color:var(--cl-primary-color);font-weight:600;display:inline-block;max-width:800px;">${t('components.result.types.' + props.type)}</div><br> ${tooltipValue.value}`
    });

    const onClick = async () => {
      emit('click');

      switch (props.type) {
        case DATA_FIELD_TYPE_LONG_TEXT:
        case DATA_FIELD_TYPE_HTML:
          store.commit(`${ns}/setResultDialogVisible`, true);
          store.commit(`${ns}/setResultDialogContent`, props.value);
          store.commit(`${ns}/setResultDialogType`, props.type);
          store.commit(`${ns}/setResultDialogKey`, props.fieldKey);
      }
    };

    const cls = computed<string>(() => {
      const cls = ['result-cell'];
      cls.push(props.type);
      return cls.join(' ');
    })

    return {
      tooltip,
      onClick,
      DATA_FIELD_TYPE_GENERAL,
      DATA_FIELD_TYPE_IMAGE,
      DATA_FIELD_TYPE_URL,
      DATA_FIELD_TYPE_HTML,
      DATA_FIELD_TYPE_LONG_TEXT,
      cls,
    };
  }
});
</script>

<style lang="scss" scoped>
.result-cell {
  cursor: pointer;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-height: 240px;
  max-width: 240px;

  &.html,
  &.long-text {
    overflow: auto;
    white-space: inherit;
  }

  &:hover {
    text-decoration: underline;
  }

  .icon {
    margin-right: 3px;
  }

  .result-cell-image {
    img {
      max-height: 100%;
      max-width: 100%;
    }
  }

  .result-cell-url {
    color: var(--cl-primary-color);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .result-cell-long-text {
    overflow-y: auto;
  }
}
</style>
