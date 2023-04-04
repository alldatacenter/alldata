<template>
  <div class="tables-menu">
    <div class="select-catalog g-flex-ac">
      <span class="label">{{$t('catalog')}}</span>
      <a-select
        v-model:value="curCatalog"
        :options="catalogOptions"
        @change="catalogChange"
        :loading="catalogLoading"
        :getPopupContainer="getPopupContainer"
        class="theme-dark"
        />
    </div>
    <div class="tables-wrap g-flex">
      <div class="database-list">
        <div class="list-wrap">
          <div class="add g-flex-jsb">
            <span class="label">{{$t('database', 2)}}</span>
            <!-- <plus-outlined @click="addDatabase" class="icon" /> -->
          </div>
          <div class="filter-wrap">
            <a-input-search
              v-model:value="DBSearchInput"
              :placeholder="placeholder.filterDBPh"
              @change="(val) => handleSearch('db',val)"
              class="theme-dark"
            >
              <template #prefix>
                <SearchOutlined />
              </template>
              <template #suffix v-if="DBSearchInput">
                <CloseCircleOutlined @click="clearSearch('db')" class="input-clear-icon" />
              </template>
            </a-input-search>
          </div>
          <u-loading v-if="loading" />
          <virtual-recycle-scroller :loading="loading" :items="databaseList" :activeItem="database" :itemSize="40" @handleClickTable="handleClickDb" iconName="database" />
        </div>
      </div>
      <div class="table-list">
        <div class="list-wrap">
          <div class="add g-flex-jsb">
            <span class="label">{{$t('table', 2)}}</span>
            <!-- <plus-outlined @click="createTable" class="icon" /> -->
          </div>
          <div class="filter-wrap">
            <a-input-search
              v-model:value="tableSearchInput"
              :placeholder="placeholder.filterTablePh"
              @change="(val) => handleSearch('table', val)"
              class="theme-dark"
            >
              <template #prefix>
                <SearchOutlined />
              </template>
              <template #suffix v-if="tableSearchInput">
                <CloseCircleOutlined @click="clearSearch('table')" class="input-clear-icon" />
              </template>
            </a-input-search>
          </div>
          <u-loading v-if="tableLoading" />
          <virtual-recycle-scroller :loading="tableLoading" :items="tableList" :activeItem="tableName" :itemSize="40" @handleClickTable="handleClickTable" iconName="tableOutlined" />
        </div>
      </div>
    </div>
  </div>
  <createDB-modal :visible="showCreateDBModal" :catalogOptions="catalogOptions" @cancel="cancel"></createDB-modal>
</template>

<script lang="ts">
import { defineComponent, onBeforeMount, reactive, toRefs } from 'vue'
import {
  // PlusOutlined,
  SearchOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue'
import CreateDBModal from './CreateDB.vue'
import { useRoute, useRouter } from 'vue-router'
import useStore from '@/store/index'
import { getCatalogList, getDatabaseList, getTableList } from '@/services/table.service'
import { ICatalogItem, ILableAndValue, IMap } from '@/types/common.type'
import { debounce } from '@/utils/index'
import { usePlaceholder } from '@/hooks/usePlaceholder'
import virtualRecycleScroller from '@/components/VirtualRecycleScroller.vue'

interface IDatabaseItem {
  id: string
  label: string
}
interface ITableItem {
  name: string
  type: string
}

export default defineComponent({
  name: 'TablesMenu',
  components: {
    // PlusOutlined,
    SearchOutlined,
    CloseCircleOutlined,
    CreateDBModal,
    virtualRecycleScroller
  },
  emits: ['goCreatePage'],
  setup(props, { emit }) {
    const router = useRouter()
    const route = useRoute()
    const store = useStore()
    const state = reactive({
      catalogLoading: false as boolean,
      DBSearchInput: '' as string,
      tableSearchInput: '' as string,
      curCatalog: '',
      database: '',
      tableName: '',
      type: '',
      catalogOptions: [] as ILableAndValue[],
      showCreateDBModal: false,
      loading: false,
      tableLoading: false,
      databaseList: [] as IMap<string>[],
      tableList: [] as IMap<string>[]
    })
    const storageTableKey = 'easylake-menu-catalog-db-table'
    const storageCataDBTable = JSON.parse(localStorage.getItem(storageTableKey) || '{}')

    const placeholder = reactive(usePlaceholder())

    const handleSearch = (type: string) => {
      type === 'table' ? getSearchTableList() : getSearchDBList()
    }
    const clearSearch = (type: string) => {
      if (type === 'table') {
        state.tableSearchInput = ''
        getSearchTableList()
      } else {
        state.DBSearchInput = ''
        getSearchDBList()
      }
    }

    const getSearchTableList = debounce(() => {
      getAllTableList()
    })

    const getSearchDBList = debounce(() => {
      getAllDatabaseList(true)
    })

    const handleClickDb = (item: IDatabaseItem) => {
      if (state.database === item.id) {
        return
      }
      state.database = item.id
      state.tableName = ''
      getAllTableList()
    }

    const getPopupContainer = (triggerNode: Element) => {
      return triggerNode.parentNode
    }

    const clickDatabase = () => {

    }
    const catalogChange = (value: string) => {
      state.curCatalog = value
      state.databaseList.length = 0
      state.tableList.length = 0
      getAllDatabaseList()
    }
    const addDatabase = () => {
      state.showCreateDBModal = true
    }
    const cancel = () => {
      state.showCreateDBModal = false
    }
    const createTable = () => {
      emit('goCreatePage')
    }
    const handleClickTable = (item: IMap<string>) => {
      state.tableName = item.label
      state.type = item.type
      localStorage.setItem(storageTableKey, JSON.stringify({
        catalog: state.curCatalog,
        database: state.database,
        tableName: item.label
      }))
      store.updateTablesMenu(false)
      const path = item.type === 'HIVE' ? '/hive-tables' : '/tables'
      const pathQuery = {
        path,
        query: {
          catalog: state.curCatalog,
          db: state.database,
          table: state.tableName,
          type: state.type
        }
      }
      if (route.path.indexOf('tables') > -1) {
        router.replace(pathQuery)
        return
      }
      router.push(pathQuery)
    }

    const getCatalogOps = () => {
      state.catalogLoading = true
      getCatalogList().then((res: ICatalogItem[]) => {
        if (!res) {
          return
        }
        state.catalogOptions = (res || []).map((ele: ICatalogItem) => ({
          value: ele.catalogName,
          label: ele.catalogName
        }))
        if (state.catalogOptions.length) {
          const index = state.catalogOptions.findIndex(ele => ele.value === storageCataDBTable.catalog)
          const query = route.query
          state.curCatalog = index > -1 ? storageCataDBTable.catalog : (query?.catalog)?.toString() || state.catalogOptions[0].value
        }
        getAllDatabaseList()
      }).finally(() => {
        state.catalogLoading = false
      })
    }

    const getAllDatabaseList = (isSearch = false) => {
      if (!state.curCatalog) {
        return
      }
      state.loading = true
      getDatabaseList({
        catalog: state.curCatalog,
        keywords: state.DBSearchInput
      }).then((res: string[]) => {
        state.databaseList = (res || []).map((ele: string) => ({
          id: ele,
          label: ele
        }))
        if (state.databaseList.length && !isSearch) {
          const index = state.databaseList.findIndex(ele => ele.id === storageCataDBTable.database)
          state.database = index > -1 ? storageCataDBTable.database : (route.query?.db)?.toString() || state.databaseList[0].id || ''
          getAllTableList()
        }
      }).finally(() => {
        state.loading = false
      })
    }

    const getAllTableList = () => {
      if (!state.curCatalog || !state.database) {
        return
      }
      state.tableLoading = true
      state.tableList.length = 0
      getTableList({
        catalog: state.curCatalog,
        db: state.database,
        keywords: state.tableSearchInput
      }).then((res: ITableItem[]) => {
        state.tableList = (res || []).map((ele: ITableItem) => ({
          id: ele.name,
          label: ele.name,
          type: ele.type
        }))
      }).finally(() => {
        state.tableLoading = false
      })
    }
    onBeforeMount(() => {
      const { database, tableName } = storageCataDBTable
      state.database = database
      state.tableName = tableName
      getCatalogOps()
    })

    return {
      ...toRefs(state),
      placeholder,
      handleClickDb,
      getPopupContainer,
      clickDatabase,
      catalogChange,
      addDatabase,
      cancel,
      createTable,
      handleClickTable,
      handleSearch,
      clearSearch
    }
  }
})
</script>

<style lang="less" scoped>
  .tables-menu {
    box-sizing: border-box;
    height: 100%;
    width: 512px;
    background-color: @dark-bg-color;
    color: #fff;
    box-shadow: rgb(0 21 41 / 8%) 2px 0 6px;
    .tables-wrap {
      height: calc(100% - 40px);
    }
    .filter-wrap {
      padding: 4px 4px 0;
      .input-clear-icon {
        font-size: 12px;
      }
    }
    :deep(.ant-input-group-addon) {
      display: none;
    }
    .database-list,
    .table-list {
      flex: 1;
      padding-top: 8px;
    }
    .table-list,
    .database-list .list-wrap {
      border-right: 1px solid rgb(255 255 255 / 12%);
    }
    .list-wrap {
      height: calc(100% - 12px);
      position: relative;
      .u-loading {
        background: transparent;
        justify-content: flex-start;
        padding-top: 200px;
      }
    }
    .select-catalog,
    .add {
      align-items: center;
      height: 40px;
      padding: 0 12px;
    }
    .database-list .select-catalog {
      padding-right: 4px;
    }
    .add {
      margin: 4px 4px 0;
      background-color: @dark-bg-primary-color;
    }
    :deep(.select-catalog .ant-select) {
      width: 240px;
      margin-left: 12px;
    }
    .icon {
      cursor: pointer;
    }
    .select-catalog {
      padding-top: 8px;
      border-right: 1px solid rgb(255 255 255 / 12%);
    }
  }
</style>
