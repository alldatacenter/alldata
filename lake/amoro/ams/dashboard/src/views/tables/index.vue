<template>
  <div class="tables-wrap">
    <div v-if="!isSecondaryNav" class="tables-content">
      <div class="g-flex-jsb">
        <div class="g-flex-col">
          <div class="g-flex">
            <span :title="baseInfo.tableName" class="table-name g-text-nowrap">{{baseInfo.tableName}}</span>
            <span v-if="!isIceberg" class="create-time">{{ `${$t('createTime')}: ${baseInfo.createTime}` }}</span>
          </div>
          <div class="table-info g-flex-ac">
            <p>{{`${$t('table')}${$t('size')}`}}: <span class="text-color">{{baseInfo.size}}</span></p>
            <a-divider type="vertical" />
            <p>{{$t('file')}}:  <span class="text-color">{{baseInfo.file}}</span></p>
            <a-divider type="vertical" />
            <p>{{$t('averageFileSize')}}: <span class="text-color">{{baseInfo.averageFile}}</span></p>
            <a-divider type="vertical" />
            <p>{{$t('tableFormat')}}: <span class="text-color">{{baseInfo.tableFormat}}</span></p>
          </div>
        </div>
        <!-- <div class="table-edit">
          <edit-outlined @click="editTable" class="g-mr-8" />
          <delete-outlined @click="delTable" />
        </div> -->
      </div>
      <div class="content">
        <a-tabs v-model:activeKey="activeKey" destroyInactiveTabPane @change="onChangeTab">
          <a-tab-pane key="Details" tab="Details">
            <u-details @setBaseDetailInfo="setBaseDetailInfo" />
          </a-tab-pane>
          <a-tab-pane v-if="detailLoaded" key="Files" tab="Files">
            <u-files :hasPartition="baseInfo.hasPartition"/>
          </a-tab-pane>
          <a-tab-pane v-for="tab in tabConfigs" :key="tab.key" :tab="`${tab.label}`">
            <component :is="`U${tab.key}`"></component>
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>
    <!-- Create table secondary page -->
    <router-view v-else @goBack="goBack"></router-view>
  </div>
</template>

<script lang="ts">
import { defineComponent, reactive, toRefs, watch, shallowReactive, computed, onMounted } from 'vue'
// import { EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import UDetails from './components/Details.vue'
import UFiles from './components/Files.vue'
import UOperations from './components/Operations.vue'
import UTransactions from './components/Transactions.vue'
import UOptimized from './components/Optimized.vue'
import { useRoute, useRouter } from 'vue-router'
import useStore from '@/store/index'
import { IBaseDetailInfo, IMap } from '@/types/common.type'

export default defineComponent({
  name: 'Tables',
  components: {
    // EditOutlined,
    // DeleteOutlined,
    UDetails,
    UFiles,
    UOperations,
    UTransactions,
    UOptimized
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const store = useStore()

    const tabConfigs = shallowReactive([
      // { key: 'Details' },
      // { key: 'Files' },
      { key: 'Transactions', label: 'Transactions' },
      { key: 'Optimized', label: 'Optimizing' },
      { key: 'Operations', label: 'Operations' }
    ])

    const state = reactive({
      activeKey: 'Details',
      isSecondaryNav: false,
      baseInfo: {
        tableType: '',
        tableName: '',
        createTime: '',
        size: '',
        file: '',
        averageFile: '',
        tableFormat: '',
        hasPartition: false
      } as IBaseDetailInfo,
      detailLoaded: false
    })

    const isIceberg = computed(() => {
      return state.baseInfo.tableType === 'ICEBERG'
    })

    const setBaseDetailInfo = (baseInfo: IBaseDetailInfo) => {
      state.detailLoaded = true
      state.baseInfo = { ...baseInfo }
    }
    const onChangeTab = (key: string) => {
      const query = { ...route.query }
      query.tab = key
      router.replace({ query: { ...query } })
    }

    const editTable = () => {}
    const delTable = () => {}
    const hideTablesMenu = () => {
      store.updateTablesMenu(false)
    }
    const goBack = () => {
      state.isSecondaryNav = false
      router.back()
    }

    watch(
      () => route.path,
      () => {
        state.isSecondaryNav = !!(route.path.indexOf('create') > -1)
      }, { immediate: true }
    )

    watch(
      () => route.query,
      (value, oldVal) => {
        const { catalog, db, table } = value
        const { catalog: oldCatalog, db: oldDb, table: oldTable } = oldVal
        if (`${catalog}${db}${table}` !== `${oldCatalog}${oldDb}${oldTable}`) {
          state.activeKey = 'Details'
          return
        }
        state.activeKey = value.tab as string
      }
    )

    onMounted(() => {
      state.activeKey = (route.query?.tab as string) || 'Details'
    })

    return {
      ...toRefs(state),
      tabConfigs,
      store,
      isIceberg,
      editTable,
      delTable,
      setBaseDetailInfo,
      hideTablesMenu,
      goBack,
      onChangeTab
    }
  }
})

</script>

<style lang="less" scoped>
.tables-wrap {
  font-size: 14px;
  border: 1px solid #e8e8f0;
  padding: 12px 0;
  min-height: 100%;
  .create-time {
    margin-top: 12px;
  }
  .tables-menu-wrap {
    position: fixed;
    width: 100%;
    height: 100%;
    top: 0;
    left: 200px;
    z-index: 100;
  }
  .table-name {
    font-size: 24px;
    line-height: 1.5;
    margin-right: 16px;
    max-width: 400px;
    padding-left: 24px;
  }
  .table-info {
    padding: 12px 24px 0 24px;
    .text-color {
      color: #7CB305;
    }
    .ant-divider-vertical {
      height: 1.2em;
      margin: 0 12px;
    }
  }
  .table-edit {
    font-size: 18px;
    padding-right: 12px;
  }
  :deep(.ant-tabs-nav) {
    padding-left: 12px;
    margin-bottom: 0;
  }
}
</style>
