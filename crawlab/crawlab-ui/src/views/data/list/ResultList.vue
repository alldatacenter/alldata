<template>
  <cl-list-layout
    :action-functions="actionFunctions"
    :no-actions="noActions"
    :table-pagination="tablePagination"
    :table-columns="tableColumns"
    :table-data="tableData"
    :table-total="tableTotal"
    :visible-buttons="visibleButtons"
    :embedded="embedded"
    class="result-list"
  />

  <cl-result-cell-dialog/>
</template>

<script lang="ts">
import {computed, defineComponent, h, onBeforeUnmount, PropType, watch} from 'vue';
import {useStore} from 'vuex';
import {TABLE_ACTION_CUSTOMIZE_COLUMNS} from '@/constants';
import {emptyArrayFunc} from "@/utils";
import ResultCell from "@/components/result/ResultCell.vue";
import {getDataFieldIconByType} from "@/utils/dataFields";

export default defineComponent({
  name: 'ResultList',
  props: {
    id: {
      type: String,
      required: true,
    },
    dataSourceId: {
      type: String,
      required: false,
    },
    noActions: {
      type: Boolean,
      default: false,
    },
    embedded: {
      type: Boolean,
      default: false,
    },
    visibleButtons: {
      type: Array as PropType<BuiltInTableActionButtonName[]>,
      default: () => {
        return [
          TABLE_ACTION_CUSTOMIZE_COLUMNS,
        ];
      }
    },
    filter: {
      type: [Array, Function] as PropType<FilterConditionData[] | (() => FilterConditionData[])>,
      default: emptyArrayFunc,
    },
    displayAllFields: {
      type: Boolean,
    },
  },
  setup(props: ResultListProps) {
    // store
    const ns = 'dataCollection';
    const store = useStore();
    const {
      dataCollection: state,
    } = store.state as RootStoreState;

    // data
    const tableData = computed<TableData<Result>>(() => state.resultTableData);

    // total
    const tableTotal = computed<number>(() => state.resultTableTotal);

    // pagination
    const tablePagination = computed<TablePagination>(() => state.resultTablePagination);

    // default fields
    const defaultFields = [
      '_id',
      '_tid',
      '_h',
    ];

    // data fields
    const dataFields = computed<DataField[]>(() => state.form?.fields || []);

    // data fields map
    const dataFieldsMap = computed<Map<string, DataFieldType>>(() => {
      const map = new Map<string, DataFieldType>();
      dataFields.value.forEach((field: DataField) => {
        map.set(field.key as string, field.type);
      });
      return map;
    });

    const getTableColumnIcon = (field: DataField): Icon => {
      const type = dataFieldsMap.value.get(field.key as string);
      return getDataFieldIconByType(type);
    };

    // columns
    const tableColumns = computed<TableColumns<Result>>(() => {
      const fields = store.getters[`${ns}/resultFields`] as DataField[];
      return fields
        .filter(f => props.displayAllFields ? true : !defaultFields.includes(f.key as string))
        .map(f => {
          const {key} = f;
          return {
            key,
            label: key,
            minWidth: '240',
            icon: getTableColumnIcon(f),
            value: (row: Result) => {
              const value = row[key as string];
              const type = dataFieldsMap.value.get(key as string);
              return h(ResultCell, {
                fieldKey: key,
                type,
                value,
              });
            },
          };
        }) as TableColumns<Result>;
    });

    // filter conditions
    const filterConditions = computed<FilterConditionData[]>(() => {
      if (typeof props.filter === 'function') {
        return props.filter() as FilterConditionData[];
      }
      return props.filter || [] as FilterConditionData[];
    });

    // action functions
    const actionFunctions = {
      setPagination: (pagination) => store.commit(`${ns}/setResultTablePagination`, pagination),
      getList: async () => {
        const {id} = props;
        if (!id) return;
        return store.dispatch(`${ns}/getResultData`, {
          id,
          params: {
            data_source_id: props.dataSourceId,
            conditions: filterConditions.value,
            ...tablePagination.value,
          },
        });
      },
      getAll: async () => {
        console.warn('getAll is not implemented');
      },
      deleteList: (ids: string[]) => {
        console.warn('deleteList is not implemented');
      },
      deleteByIdConfirm: (row: BaseModel) => {
        console.warn('deleteByIdConfirm is not implemented');
      },
    } as ListLayoutActionFunctions;

    const {
      getList,
    } = actionFunctions;

    watch(() => props.id, getList);

    watch(() => tablePagination.value, getList);

    onBeforeUnmount(() => {
      store.commit(`${ns}/resetResultTableData`);
      store.commit(`${ns}/resetResultTablePagination`);
      store.commit(`${ns}/resetResultTableFilter`);
    });

    return {
      actionFunctions,
      tableData,
      tableTotal,
      tablePagination,
      tableColumns,
    };
  },
});
</script>

<style lang="scss" scoped>
.result-list {
  .nav-actions {
    border-bottom: none;
  }
}
</style>
