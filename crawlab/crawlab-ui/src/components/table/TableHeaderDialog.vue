<template>
  <el-popover
    :show-arrow="false"
    :visible="visible"
    class="table-header-dialog"
    popper-class="table-header-popper"
    trigger="manual"
  >
    <template #reference>
      <div>
        <slot name="reference"/>
      </div>
    </template>
    <div v-click-outside="onClickOutside" class="content">
      <div class="header">
        <div class="title">{{ column.label }}</div>
      </div>
      <span class="close" @click="onCancel">
        <el-icon name="close"></el-icon>
      </span>
      <div class="body">
        <div class="list">
          <div v-if="column.hasSort" class="item sort">
            <cl-table-header-dialog-sort :value="internalSort?.d" @change="onSortChange"/>
          </div>
          <div v-if="column.hasFilter" class="item filter">
            <cl-table-header-dialog-filter
              :column="column"
              :conditions="conditions"
              :search-string="searchString"
              @change="onFilterChange"
              @enter="onFilterEnter"
            />
          </div>
        </div>
      </div>
      <div class="footer">
        <cl-button
          plain
          :tooltip="t('common.actions.cancel')"
          type="info"
          @click="onCancel"
        >
          {{ t('common.actions.cancel') }}
        </cl-button>
        <cl-button
          plain
          :tooltip="t('common.actions.clear')"
          type="warning"
          @click="onClear"
        >
          {{ t('common.actions.clear') }}
        </cl-button>
        <cl-button
          :disabled="isApplyDisabled"
          :tooltip="t('common.actions.apply')"
          type="primary"
          @click="onApply"
        >
          {{ t('common.actions.apply') }}
        </cl-button>
      </div>
    </div>
  </el-popover>
</template>

<script lang="ts">
import {computed, defineComponent, PropType, ref, watch} from 'vue';
import {plainClone} from '@/utils/object';
import {FILTER_OP_NOT_SET} from '@/constants/filter';
import {ClickOutside} from 'element-plus';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'TableHeaderFilter',
  directives: {
    ClickOutside,
  },
  props: {
    visible: {
      type: Boolean,
      required: false,
      default: false,
    },
    column: {
      type: Object as PropType<TableColumn>,
      required: true,
    },
    actionStatusMap: {
      type: Object as PropType<TableHeaderActionStatusMap>,
      required: true,
    },
    sort: {
      type: Object as PropType<SortData>,
      required: false,
    },
    filter: {
      type: Object as PropType<TableHeaderDialogFilterData>,
      required: false,
    },
  },
  emits: [
    'click',
    'cancel',
    'clear',
    'apply',
  ],
  setup(props: TableHeaderDialogProps, {emit}) {
    // i18n
    const {t} = useI18n();

    const defaultInternalSort = {key: props.column.key} as SortData;
    const internalSort = ref<SortData>();
    const internalFilter = ref<TableHeaderDialogFilterData>();

    const searchString = computed<string | undefined>(() => internalFilter.value?.searchString);

    const conditions = computed<FilterConditionData[]>(() => internalFilter.value?.conditions || []);

    const items = computed<string[]>(() => internalFilter.value?.items || []);

    const trueConditions = computed<FilterConditionData[]>(() => {
      return conditions.value?.filter(d => d.op !== FILTER_OP_NOT_SET);
    });

    const isEmptyFilter = computed<boolean>(() => {
      return !searchString.value && trueConditions.value.length == 0 && items.value.length === 0;
    });

    const isApplyDisabled = computed<boolean>(() => {
      for (const cond of trueConditions.value) {
        if (!cond.value) {
          return true;
        }
      }
      return false;
    });

    const cancel = () => {
      emit('cancel');
    };

    const clear = () => {
      if (!internalSort.value) internalSort.value = plainClone(defaultInternalSort);
      internalSort.value.d = undefined;
      internalFilter.value = undefined;
      emit('clear');
    };

    const apply = () => {
      if (!internalSort.value && isEmptyFilter.value) {
        clear();
        return;
      }
      const value: TableHeaderDialogValue = {
        sort: internalSort.value,
        filter: internalFilter.value,
      };
      emit('apply', value);
    };

    const onClickOutside = () => {
      const {visible} = props;
      if (!visible) return;
      cancel();
    };

    const onCancel = () => {
      cancel();
    };

    const onClear = () => {
      clear();
    };

    const onApply = () => {
      apply();
    };

    const onSortChange = (value: string) => {
      if (!internalSort.value) internalSort.value = plainClone(defaultInternalSort);
      internalSort.value.d = value;
    };

    const onFilterChange = (value: TableHeaderDialogFilterData) => {
      internalFilter.value = value;
    };

    const onFilterEnter = () => {
      apply();
    };

    watch(() => {
      const {visible} = props;
      return visible;
    }, () => {
      const {sort, filter, visible} = props;
      if (visible) {
        internalSort.value = (sort ? plainClone(sort) : plainClone(defaultInternalSort)) as SortData;
        internalFilter.value = plainClone(filter) as TableHeaderDialogFilterData;
      }
    });

    return {
      internalSort,
      searchString,
      conditions,
      isApplyDisabled,
      onClickOutside,
      onCancel,
      onClear,
      onApply,
      onSortChange,
      onFilterChange,
      onFilterEnter,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>
.content {
  position: relative;
  min-width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;

  .close {
    position: absolute;
    top: 0;
    right: 0;
    cursor: pointer;
  }

  .header {
    .title {
      font-size: 16px;
      font-weight: 900;
      color: var(--cl-info-medium-color);
      padding-bottom: 10px;
      margin-bottom: 10px;
      border-bottom: 1px solid var(--cl-info-border-color);
    }
  }

  .body {
    flex: 1;
    display: flex;
    flex-direction: column;

    .list {
      flex: 1;
      min-height: 100%;
      display: flex;
      flex-direction: column;

      .item {
        padding: 10px 0;
        border-bottom: 1px solid var(--cl-info-border-color);

        &:first-child {
          padding-top: 0;
        }

        &:last-child {
          border-bottom: none;
        }

        &.sort {
          flex-basis: 100%;
        }

        &.filter {
          flex: 1;
          display: flex;
          flex-direction: column;
        }
      }
    }
  }

  .footer {
    height: 30px;
    text-align: right;
  }
}
</style>
<style>
.table-header-popper {
  min-width: 320px !important;
  display: flex;
  flex-direction: column;
}
</style>
