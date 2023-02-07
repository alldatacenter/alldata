<template>
  <div class="table-header">
    <span :class="[column.required ? 'required' : '']" class="label">
      <span v-if="column.icon" class="label-icon">
        <cl-icon :icon="column.icon"/>
      </span>
      {{ column.label }}
    </span>

    <cl-table-header-dialog
      v-if="hasDialog"
      :action-status-map="actionStatusMap"
      :column="column"
      :visible="dialogVisible"
      :filter="filterData"
      :sort="sortData"
      @apply="onDialogApply"
      @clear="onDialogClear"
      @cancel="onDialogCancel"
    >
      <template #reference>
        <div class="actions">
          <cl-table-header-action
            v-for="{key, tooltip, isHtml, icon, onClick} in actions"
            :key="key + JSON.stringify(icon)"
            :icon="icon"
            :status="actionStatusMap[key]"
            :tooltip="tooltip"
            :is-html="isHtml"
            @click="onClick"
          />
        </div>
      </template>
    </cl-table-header-dialog>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType, reactive, ref} from 'vue';
import {conditionTypesMap} from '@/components/filter/FilterCondition.vue';
import {ASCENDING, DESCENDING} from '@/constants/sort';
import {FILTER_OP_NOT_SET} from '@/constants/filter';
import {useI18n} from 'vue-i18n';
import {sendEvent} from '@/admin/umeng';

export default defineComponent({
  name: 'TableHeader',
  props: {
    column: {
      type: Object as PropType<TableColumn>,
      required: true,
    },
    index: {
      type: Number,
      required: false,
    },
  },
  emits: [
    'change',
  ],
  setup(props, {emit}) {
    // i18n
    const {t} = useI18n();

    const dialogVisible = ref<boolean>(false);

    const actionStatusMap = reactive<TableHeaderActionStatusMap>({
      filter: {active: false, focused: false},
      sort: {active: false, focused: false},
    });

    const sortData = ref<SortData>();
    const filterData = ref<TableHeaderDialogFilterData>();

    const filterItemsMap = computed<Map<any, string | undefined>>(() => {
      const map = new Map<any, string | undefined>();
      const {column} = props;
      const {filterItems} = column;
      if (!filterItems) return map;
      filterItems.forEach(d => {
        const {label, value} = d;
        map.set(value, label);
      });
      return map;
    });

    const actions = computed<TableColumnButton[]>(() => {
      const {column} = props;

      // sort icon and tooltip
      let sortIcon = ['fa', 'sort-amount-down-alt'];
      let sortTooltip = t('components.table.header.sort.tooltip.sort');
      if (sortData.value?.d === ASCENDING) {
        sortIcon = ['fa', 'sort-amount-up'];
        sortTooltip = t('components.table.header.sort.tooltip.sortAscending');
      } else if (sortData.value?.d === DESCENDING) {
        sortIcon = ['fa', 'sort-amount-down-alt'];
        sortTooltip = t('components.table.header.sort.tooltip.sortDescending');
      }

      // filter tooltip
      let filterTooltip = t('components.table.header.filter.tooltip.filter');
      let filterIsHtml = false;
      if (filterData.value) {
        const {searchString, conditions, items} = filterData.value;

        // search string
        if (searchString) {
          filterTooltip += `<br><span style="color: var(--cl-primary-color)">${t('components.table.header.filter.tooltip.search')}:</span> <span style="color: ${variables.warningColor};">"${searchString}"</span>`;
          filterIsHtml = true;
        }

        // filter conditions
        if (conditions && conditions.length > 0) {
          filterTooltip += '<br>' + conditions.filter(d => d.op !== FILTER_OP_NOT_SET)
            .map(d => `<span style="color: var(--cl-primary-color);margin-right: 5px">${conditionTypesMap[d.op || '']}:</span> <span style="color: ${variables.warningColor};">"${d.value}"</span>`)
            .join('<br>');
          filterIsHtml = true;
        }

        // filter items
        if (items && items.length > 0) {
          const itemsStr = items.map(value => filterItemsMap.value.get(value)).join(', ');
          filterTooltip += `<br><span style="color: var(--cl-primary-color);margin-right: 5px">${t('components.table.header.filter.tooltip.include')}:</span><span style="color: ${variables.warningColor}">` + itemsStr + '</span>';
          filterIsHtml = true;
        }
      }

      // tooltip items
      const items = [];
      if (column.hasSort) {
        items.push({
          key: 'sort',
          tooltip: sortTooltip,
          icon: sortIcon,
          onClick: () => {
            dialogVisible.value = true;
            actionStatusMap.sort.focused = true;

            sendEvent('click_table_header_sort_show');
          }
        });
      }
      if (column.hasFilter) {
        items.push({
          key: 'filter',
          tooltip: filterTooltip,
          isHtml: filterIsHtml,
          icon: ['fa', 'filter'],
          onClick: () => {
            dialogVisible.value = true;
            actionStatusMap.filter.focused = true;

            sendEvent('click_table_header_filter_show');
          }
        });
      }

      return items;
    });

    const hideDialog = () => {
      dialogVisible.value = false;
      actionStatusMap.filter.focused = false;
      actionStatusMap.sort.focused = false;

      sendEvent('click_table_header_dialog_hide');
    };

    const clearDialog = () => {
      const {column} = props as TableHeaderProps;

      // set status
      actionStatusMap.filter.active = false;
      actionStatusMap.sort.active = false;

      // set data
      sortData.value = undefined;
      filterData.value = undefined;

      // hide
      hideDialog();

      // emit
      emit('change', column, undefined, undefined);

      sendEvent('click_table_header_dialog_clear');
    };

    const onDialogCancel = () => {
      hideDialog();
    };

    const onDialogClear = () => {
      clearDialog();
    };

    const onDialogApply = (value: TableHeaderDialogValue) => {
      const {column} = props as TableHeaderProps;
      const {sort, filter} = value;

      // set status
      if (sort) actionStatusMap.sort.active = true;
      if (filter) actionStatusMap.filter.active = true;

      // set data
      sortData.value = sort;
      filterData.value = filter;

      // if no data set, clear
      if (!sortData.value && !filterData.value) {
        clearDialog();
        return;
      }

      // hide
      hideDialog();

      // emit
      emit('change', column, sortData.value, filterData.value);

      sendEvent('click_table_header_dialog_apply');
    };

    const hasDialog = computed<boolean>(() => {
      const {
        hasSort,
        hasFilter,
      } = props.column;

      return !!hasSort || !!hasFilter;
    });

    return {
      dialogVisible,
      actionStatusMap,
      actions,
      sortData,
      filterData,
      onDialogCancel,
      onDialogClear,
      onDialogApply,
      hasDialog,
    };
  },
});
</script>

<style lang="scss" scoped>
.table-header {
  display: flex;
  position: relative;

  .label {
    display: flex;
    align-items: center;

    &.required:before {
      content: "*";
      color: var(--cl-red);
      margin-right: 4px;
    }

    .label-icon {
      color: var(--cl-info-medium-light-color);
      font-size: 10px;
      margin-right: 5px;
    }
  }

  .actions {
    position: absolute;
    right: 0;
    height: 100%;
    align-items: center;
    cursor: pointer;
  }
}
</style>
<style scoped>
.table-header:hover .actions >>> .action {
  display: inline;
}

.table-header .actions >>> .action {
  display: none;
}
</style>
