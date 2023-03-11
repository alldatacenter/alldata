<template>
  <div class="hive-tables-wrap">
    <div v-if="!isSecondaryNav" class="tables-content">
      <div class="g-flex-jsb table-top">
        <span :title="tableName" class="table-name g-text-nowrap">{{tableName}}</span>
        <div class="right-btn">
          <a-button type="primary" :disabled="status === upgradeStatus.upgrading" @click="upgradeTable">{{displayStatus}}</a-button>
          <p v-if="status === upgradeStatus.failed" @click="showErrorMsg = true" class="fail-msg">{{$t('lastUpgradingFailed')}}</p>
        </div>
      </div>
      <div class="content">
        <a-tabs v-model:activeKey="activeKey">
          <a-tab-pane key="Details" tab="Details">
            <u-details :partitionColumnList="partitionColumnList" :schema="schema" />
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>
    <u-loading v-if="loading" />
    <!-- upgrade table secondary page -->
    <router-view v-if="isSecondaryNav" @goBack="goBack" @refresh="refresh"></router-view>
    <error-msg v-if="showErrorMsg" :msg="errorMessage" @cancle="showErrorMsg = false" />
  </div>
</template>

<script lang="ts">
import { computed, defineComponent, onBeforeUnmount, onMounted, reactive, ref, toRefs, watch } from 'vue'
import UDetails from './components/Details.vue'
import errorMsg from './components/ErrorMsg.vue'
import { useRoute, useRouter } from 'vue-router'
import { DetailColumnItem, upgradeStatusMap } from '@/types/common.type'
import { getHiveTableDetail, getUpgradeStatus } from '@/services/table.service'
import { useI18n } from 'vue-i18n'

export default defineComponent({
  name: 'Tables',
  components: {
    UDetails,
    errorMsg
  },
  setup() {
    const upgradeStatus = upgradeStatusMap
    const statusInterval = ref<number>()
    const router = useRouter()
    const route = useRoute()
    const { t } = useI18n()

    const isSecondaryNav = computed(() => {
      return !!(route.path.indexOf('upgrade') > -1)
    })

    const state = reactive({
      loading: false,
      showErrorMsg: false,
      activeKey: 'Details',
      status: '', // failed、upgrading、success、none
      displayStatus: '',
      errorMessage: '',
      tableName: 'tableName',
      partitionColumnList: [] as DetailColumnItem[],
      schema: [] as DetailColumnItem[]
    })

    const goBack = () => {
      router.back()
    }

    const params = computed(() => {
      return {
        ...route.query
      }
    })

    const getTableUpgradeStatus = async(hideLoading = false) => {
      try {
        statusInterval.value && clearTimeout(statusInterval.value)
        const { catalog, db, table } = params.value
        if (!catalog || !db || !table) {
          return
        }
        !hideLoading && (state.loading = true)
        const result = await getUpgradeStatus({
          ...params.value
        })
        const { status, errorMessage } = result
        state.status = status
        state.displayStatus = status === upgradeStatusMap.upgrading ? t('upgrading') : t('upgrade')
        state.errorMessage = errorMessage || ''
        if (status === upgradeStatusMap.upgrading) {
          statusInterval.value = setTimeout(() => {
            getTableUpgradeStatus(true)
          }, 1500)
        } else {
          if (status === upgradeStatusMap.none) {
            getHiveTableDetails()
          } else if (status === upgradeStatusMap.success) {
            router.replace({
              path: '/tables',
              query: {
                ...route.query
              }
            })
          } else if (status === upgradeStatusMap.failed) {
            getHiveTableDetails()
          }
        }
      } finally {
        !hideLoading && (state.loading = false)
      }
    }

    const getHiveTableDetails = async() => {
      try {
        const { catalog, db, table } = params.value
        if (!catalog || !db || !table) {
          return
        }
        state.loading = true
        const result = await getHiveTableDetail({
          ...params.value
        })
        const { partitionColumnList = [], schema, tableIdentifier } = result

        state.tableName = tableIdentifier?.tableName || ''

        state.partitionColumnList = partitionColumnList || []
        state.schema = schema || []
      } catch (error) {
      } finally {
        state.loading = false
      }
    }

    const init = async() => {
      await getTableUpgradeStatus()
    }

    const upgradeTable = () => {
      router.push({
        path: '/hive-tables/upgrade',
        query: {
          ...route.query
        }
      })
    }
    const refresh = () => {
      init()
    }

    watch(
      () => route.query,
      (val, old) => {
        const { catalog, db, table } = val
        if (route.path === '/hive-tables' && (catalog !== old.catalog || db !== old.db || table !== old.table)) {
          init()
        }
      }
    )

    onBeforeUnmount(() => {
      clearTimeout(statusInterval.value)
    })

    onMounted(() => {
      init()
    })

    return {
      ...toRefs(state),
      isSecondaryNav,
      upgradeStatus,
      upgradeTable,
      goBack,
      refresh
    }
  }
})

</script>

<style lang="less" scoped>
.hive-tables-wrap {
  border: 1px solid #e8e8f0;
  padding: 12px 0;
  display: flex;
  height: 100%;
  flex: 1;
  flex-direction: column;
  .table-top {
    padding: 0 12px;
    .right-btn {
      position: relative;
    }
    .fail-msg {
      position: absolute;
      bottom: -30px;
      right: 0;
      z-index: 1;
      font-size: 12px;
      width: 90px;
      color: #ff4d4f;
      text-align: center;
      text-decoration-line: underline;
      cursor: pointer;
    }
  }
  .table-name {
    font-size: 24px;
    line-height: 1.5;
    margin-right: 16px;
    max-width: 400px;
    padding-left: 12px;
  }
  :deep(.ant-tabs-nav) {
    padding-left: 24px;
    margin-bottom: 0;
  }
}
</style>
