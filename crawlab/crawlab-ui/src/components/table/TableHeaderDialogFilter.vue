<template>
  <div class="table-header-dialog-filter">
    <div class="title">
      <span>{{ t('components.table.header.dialog.filter.title') }}</span>
      <el-input
        v-if="column.allowFilterSearch"
        :model-value="internalSearchString"
        class="search"
        :placeholder="t('components.table.header.dialog.filter.search')"
        :prefix-icon="Search"
        @input="onSearch"
        @clear="onClear"
        @keyup.enter="onEnter"
      />
      <!--      <el-tooltip content="Add Condition">-->
      <!--        <span class="icon" @click="onAddCondition">-->
      <!--          <el-icon name="circle-plus-outline"/>-->
      <!--        </span>-->
      <!--      </el-tooltip>-->
    </div>
    <!--    <el-form>-->
    <!--      <FilterConditionList :conditions="internalConditions" @change="onConditionsChange"/>-->
    <!--    </el-form>-->
    <div v-if="column.allowFilterItems" class="items">
      <template v-if="filteredItems.length > 0">
        <el-checkbox-group v-model="internalItems" class="item-list" @change="onItemsChange">
          <el-checkbox
            v-for="(item, $index) in filteredItems"
            :key="$index"
            :label="item.value"
            class="item"
          >
            {{ item.label }}
          </el-checkbox>
        </el-checkbox-group>
      </template>
      <template v-else>
        <cl-empty/>
      </template>
    </div>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType, ref, watch} from 'vue';
import {getDefaultFilterCondition} from '@/components/filter/FilterCondition.vue';
import {debounce} from '@/utils/debounce';
import {useI18n} from 'vue-i18n';
import {Search} from '@element-plus/icons';

export default defineComponent({
  name: 'TableHeaderDialogFilter',
  props: {
    column: {
      type: Object as PropType<TableColumn>,
      required: false,
    },
    searchString: {
      type: String,
      required: false,
    },
    conditions: {
      type: Array as PropType<FilterConditionData[]>,
      required: false,
      default: () => {
        return [];
      }
    },
  },
  emits: [
    'change',
    'clear',
    'enter',
  ],
  setup(props, {emit}) {
    // i18n
    const {t} = useI18n();

    const internalConditions = ref<FilterConditionData[]>([getDefaultFilterCondition()]);
    const internalSearchString = ref<string>();
    const internalItems = ref<string[]>([]);

    const filterData = computed<TableHeaderDialogFilterData>(() => {
      return {
        searchString: internalSearchString.value,
        conditions: internalConditions.value,
        items: internalItems.value,
      };
    });

    const filteredItems = computed<SelectOption[]>(() => {
      const {column} = props as TableHeaderDialogFilterProps;

      const items = column?.filterItems;

      // undefined items
      if (!items) {
        // console.log('undefined items');
        return [];
      }

      // invalid type of items or empty items
      if (!Array.isArray(items) || items.length === 0) {
        // console.log('invalid type of items or empty items');
        return [];
      }

      // items as an array of SelectOption
      // console.log('items as an array of SelectOption');
      return items.filter(d => filterData.value.searchString ? d.label?.toLowerCase()?.includes(filterData.value.searchString) : true);
    });

    const onAddCondition = () => {
      internalConditions.value.push(getDefaultFilterCondition());
    };

    const onConditionsChange = (newConditions: FilterConditionData[]) => {
      internalConditions.value = newConditions;
      emit('change', filterData.value);
    };

    const onItemsChange = (newItems: string[]) => {
      internalItems.value = newItems;
      emit('change', filterData.value);
    };

    const search = debounce(() => {
      if (internalSearchString.value) {
        internalItems.value = filteredItems.value.map(d => d.value);
      } else {
        internalItems.value = [];
      }
      emit('change', filterData.value);
    }, {delay: 100});

    const onSearch = (value?: string) => {
      internalSearchString.value = value;
      search();
    };

    const onClear = () => {
      emit('clear');
    };

    const onEnter = () => {
      emit('enter');
    };

    watch(() => {
      const {searchString} = props as TableHeaderDialogFilterProps;
      return searchString;
    }, (value) => {
      internalSearchString.value = value;
    });

    watch(() => {
      const {conditions} = props as TableHeaderDialogFilterProps;
      return conditions;
    }, (value) => {
      if (value) {
        internalConditions.value = value;
        if (internalConditions.value.length === 0) {
          internalConditions.value.push(getDefaultFilterCondition());
        }
      }
    });

    return {
      internalSearchString,
      internalConditions,
      internalItems,
      filteredItems,
      onAddCondition,
      onConditionsChange,
      onItemsChange,
      onSearch,
      onClear,
      onEnter,
      Search,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>
.table-header-dialog-filter {
  flex: 1;
  display: flex;
  flex-direction: column;

  .title {
    font-size: 14px;
    font-weight: 900;
    margin-bottom: 10px;
    color: var(--cl-info-medium-color);
    display: flex;
    align-items: center;

    .search {
      margin-left: 5px;
      flex: 1;
    }

    .icon {
      cursor: pointer;
      margin-left: 5px;
    }
  }

  .items {
    overflow: auto;
    flex: 1;
    border: 1px solid var(--cl-info-border-color);
    padding: 10px;

    .item-list {
      list-style: none;
      padding: 0;
      margin: 0;
      height: 100%;
      max-height: 240px;
      display: flex;
      flex-direction: column;

      .item {
        cursor: pointer;

        &:hover {
          text-decoration: underline;
        }

        .label {
          margin-left: 5px;
        }
      }
    }
  }
}
</style>
