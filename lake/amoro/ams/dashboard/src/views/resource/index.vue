<template>
  <div class="border-wrap">
    <div class="resource-wrap">
      <div class="content">
        <a-tabs
          v-model:activeKey="activeTab"
          destroyInactiveTabPane
          @change="onChangeTab"
        >
          <a-tab-pane
            key="tables"
            :tab="t('tables')"
            :class="[activeTab === 'tables' ? 'active' : '']"
          >
            <TableList curGroupName="all" type="tables" />
          </a-tab-pane>
          <a-tab-pane
            key="optimizers"
            :tab="t('optimizers')"
            :class="[activeTab === 'optimizers' ? 'active' : '']"
          >
            <List type="optimizers" />
          </a-tab-pane>
          <a-tab-pane
            key="optimizergroup"
            :tab="t('optimizergroup')"
            :class="[activeTab === 'optimizergroup' ? 'active' : '']"
          >
            <a-button type="primary" class="g-mb-16" @click="editGroup(null)">
              {{ t("addgroup") }}
            </a-button>
            <List
              :key="groupKeyCount"
              type="optimizergroup"
              @editGroup="editGroup"
            />
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>
    <GroupModal
      :edit="groupEdit"
      :editRecord="groupEditRecord"
      v-if="showGroupModal"
      @cancel="showGroupModal = false"
      @refresh="
        groupKeyCount++;
        showGroupModal = false;
      "
    ></GroupModal>
  </div>
</template>

<script lang="ts">
import { ILableAndValue, IIOptimizeGroupItem } from '@/types/common.type'
import {
  defineComponent,
  onMounted,
  reactive,
  shallowReactive,
  toRefs,
  watch
} from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { usePlaceholder } from '@/hooks/usePlaceholder'
import { usePagination } from '@/hooks/usePagination'
import List from './components/List.vue'
import TableList from '../optimize/components/List.vue'
import GroupModal from '@/views/resource/components/GroupModal.vue'

export default defineComponent({
  name: 'Resource',
  components: {
    List,
    GroupModal,
    TableList
  },
  setup() {
    const { t } = useI18n()
    const router = useRouter()
    const route = useRoute()
    const tabConfig: ILableAndValue[] = shallowReactive([
      { label: t('optimizergroup'), value: 'optimizergroup' },
      { label: t('optimizers'), value: 'optimizers' }
    ])
    const placeholder = reactive(usePlaceholder())
    const pagination = reactive(usePagination())
    const state = reactive({
      activeTab: 'optimizergroup' as string,
      showGroupModal: false as boolean,
      groupEdit: false,
      groupEditRecord: {},
      groupKeyCount: 1,
      showTab: false as boolean
    })

    watch(
      () => route.query,
      (value) => {
        state.activeTab = (value.tab as string) || 'tables'
      },
      {
        immediate: true
      }
    )

    const editGroup = (editRecord: IIOptimizeGroupItem | null) => {
      if (editRecord) {
        state.groupEdit = true
        state.groupEditRecord = { ...editRecord }
      } else {
        state.groupEdit = false
      }
      state.showGroupModal = true
    }

    const onChangeTab = (key: string) => {
      const query = { ...route.query }
      query.tab = key
      router.replace({ query: { ...query } })
    }

    onMounted(() => {
      state.showTab = true
    })

    return {
      placeholder,
      pagination,
      ...toRefs(state),
      tabConfig,
      onChangeTab,
      editGroup,
      t
    }
  }
})
</script>

<style lang="less" scoped>
.border-wrap {
  padding: 16px 24px;
  height: 100%;
}
.resource-wrap {
  height: 100%;
  overflow-y: auto;
  .status-icon {
    width: 8px;
    height: 8px;
    border-radius: 8px;
  }
  .tabs {
    height: 32px;
    display: flex;
    align-items: center;
    margin-bottom: 12px;
    padding: 0 12px;
    border: 1px solid #e5e5e5;
  }
  :deep(.ant-tabs-content-holder) {
    // padding: 0 24px;
  }
  :deep(.ant-tabs-nav) {
    // padding: 0 12px;
  }
  .table-name {
    color: @primary-color;
    &:hover {
      cursor: pointer;
    }
  }
}
</style>
