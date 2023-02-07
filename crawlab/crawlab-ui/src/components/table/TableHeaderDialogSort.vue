<template>
  <div class="table-header-dialog-sort">
    <div class="title">
      <span>{{ t('components.table.header.dialog.sort.title') }}</span>
      <el-tooltip
        v-if="value"
        :content="t('components.table.header.dialog.sort.clearSort')"
      >
        <span class="icon" @click="onClear">
          <el-icon name="circle-close"/>
        </span>
      </el-tooltip>
    </div>
    <el-radio-group :model-value="value" type="primary" @change="onChange">
      <el-radio-button :label="ASCENDING" class="sort-btn">
        <font-awesome-icon :icon="['fa', 'sort-amount-up']"/>
        {{ t('components.table.header.dialog.sort.ascending') }}
      </el-radio-button>
      <el-radio-button :label="DESCENDING" class="sort-btn">
        <font-awesome-icon :icon="['fa', 'sort-amount-down-alt']"/>
        {{ t('components.table.header.dialog.sort.descending') }}
      </el-radio-button>
    </el-radio-group>
  </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {ASCENDING, DESCENDING, UNSORTED} from '@/constants/sort';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'TableHeaderDialogSort',
  props: {
    value: {
      type: String,
      required: false,
    },
  },
  emits: [
    'change'
  ],
  setup(props, {emit}) {
    // i18n
    const {t} = useI18n();

    const onChange = (value: SortDirection) => {
      if (value === UNSORTED) {
        emit('change', undefined);
        return;
      }
      emit('change', value);
    };

    const onClear = () => {
      emit('change');
    };

    return {
      onChange,
      onClear,
      UNSORTED,
      ASCENDING,
      DESCENDING,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>
.table-header-dialog-sort {
  .title {
    font-size: 14px;
    font-weight: 900;
    margin-bottom: 10px;
    color: var(--cl-info-medium-color);
    display: flex;
    align-items: center;

    .icon {
      cursor: pointer;
      margin-left: 5px;
    }
  }

  .el-radio-group {
    width: 100%;
    display: flex;

    .sort-btn.el-radio-button {
      &:not(.unsorted) {
        flex: 1;
      }

      &.unsorted {
        flex-basis: 20px;
      }
    }
  }
}
</style>
<style scoped>
.table-header-dialog-sort >>> .el-radio-group .el-radio-button .el-radio-button__inner {
  width: 100%;
}
</style>
