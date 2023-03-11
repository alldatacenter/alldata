<template>
  <div class="border-wrap">
    <div class="optimize-wrap">
      <div class="optimize-group g-flex-ac">
        <div class="left-group">
          <span class="g-mr-16">{{$t('optimzerGroup')}}</span>
          <a-select
            v-model:value="curGroupName"
            :showSearch="true"
            :options="groupList"
            :placeholder="placeholder.selectOptGroupPh"
            @change="onChangeGroup"
            style="width: 240px"
          />
        </div>
        <div class="btn-wrap">
          <span class="g-ml-16 f-shink-0">{{$t('resourceOccupation')}}  <span class="text-color">{{groupInfo.occupationCore}}</span> {{$t('core')}} <span class="text-color">{{groupInfo.occupationMemory}}</span> {{groupInfo.unit}}</span>
          <a-button type="primary" @click="expansionJob" class="g-ml-8">{{$t('scaleOut')}}</a-button>
        </div>
      </div>
      <div class="content">
        <a-tabs v-if="showTab" v-model:activeKey="activeTab" destroyInactiveTabPane @change="onChangeTab">
          <a-tab-pane
            v-for="tab in tabConfig"
            :key="tab.value"
            :tab="tab.label"
            :class="[activeTab === tab.value ? 'active' : '']"
            >
            <List :curGroupName="curGroupName" :type="tab.value" @refreshCurGroupInfo="refreshCurGroupInfo" />
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>
    <scale-out-modal
      v-if="showScaleOutModal"
      :visible="showScaleOutModal"
      :resourceGroup="curGroupName === 'all' ? '' : curGroupName"
      @cancel="showScaleOutModal = false"
      @refreshOptimizersTab="refreshOptimizersTab"
    />
  </div>
</template>

<script lang="ts">
import { IGroupItem, IGroupItemInfo, ILableAndValue, IMap } from '@/types/common.type'
import { computed, defineComponent, nextTick, onMounted, reactive, shallowReactive, toRefs, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { usePlaceholder } from '@/hooks/usePlaceholder'
import { usePagination } from '@/hooks/usePagination'
import { getOptimizerGroups, getQueueResourceInfo } from '@/services/optimize.service'
import ScaleOutModal from './components/ScaleOut.vue'
import List from './components/List.vue'
import { mbToSize } from '@/utils'

export default defineComponent({
  name: 'Optimize',
  components: {
    List,
    ScaleOutModal
  },
  setup() {
    const { t } = useI18n()
    const router = useRouter()
    const route = useRoute()
    const tabConfig: ILableAndValue[] = shallowReactive([
      { label: t('optimizers'), value: 'optimizers' },
      { label: t('tables'), value: 'tables' }
    ])
    const placeholder = reactive(usePlaceholder())
    const pagination = reactive(usePagination())
    const state = reactive({
      curGroupName: 'all' as string,
      groupList: [
        {
          label: t('allGroups'),
          value: 'all'
        }
      ] as IMap<string | number>[],
      groupInfo: {
        occupationCore: 0,
        occupationMemory: 0,
        unit: ''
      } as IGroupItemInfo,
      activeTab: 'tables' as string,
      showScaleOutModal: false as boolean,
      showTab: false as boolean
    })

    const isTableTab = computed(() => {
      return (state.activeTab === 'tables')
    })

    watch(() => route.query,
      (value) => {
        state.activeTab = (value.tab as string) || 'tables'
      }, {
        immediate: true
      }
    )

    const onChangeGroup = () => {
      getCurGroupInfo()
    }

    const refreshCurGroupInfo = () => {
      getCurGroupInfo()
    }

    const getCompactQueues = async() => {
      const result = await getOptimizerGroups();
      (result || []).forEach((item: IGroupItem) => {
        state.groupList.push({
          label: item.optimizerGroupName,
          value: item.optimizerGroupName
        })
      })
    }

    const getCurGroupInfo = async() => {
      const result = await getQueueResourceInfo(state.curGroupName || '')
      const memory = mbToSize(result.occupationMemory || 0)
      const memoryArr = memory.split(' ')
      state.groupInfo = {
        occupationCore: result.occupationCore,
        occupationMemory: memoryArr[0],
        unit: memoryArr[1] || ''
      }
    }

    const expansionJob = () => {
      state.showScaleOutModal = true
    }

    const refreshOptimizersTab = () => {
      onChangeTab('optimizers')
      state.showTab = false
      nextTick(() => {
        state.showTab = true
      })
      getCurGroupInfo()
    }

    const onChangeTab = (key: string) => {
      const query = { ...route.query }
      query.tab = key
      router.replace({ query: { ...query } })
    }

    onMounted(() => {
      state.showTab = true
      getCompactQueues()
      getCurGroupInfo()
    })

    return {
      isTableTab,
      placeholder,
      pagination,
      ...toRefs(state),
      tabConfig,
      onChangeGroup,
      refreshCurGroupInfo,
      expansionJob,
      refreshOptimizersTab,
      onChangeTab
    }
  }
})

</script>

<style lang="less" scoped>
.border-wrap {
  padding: 16px 24px;
  height: 100%;
}
.optimize-wrap {
  border: 1px solid #e5e5e5;
  padding: 12px 0;
  height: 100%;
  overflow-y: auto;
  .optimize-group {
    justify-content: space-between;
    padding: 0 24px;
    .f-shink-0 {
      flex-shrink: 0;
    }
    .text-color {
      color: #0ad787;
    }
  }
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
    padding: 0 24px;
  }
  :deep(.ant-tabs-nav) {
    padding: 0 12px;
  }
  .table-name {
    color: @primary-color;
    &:hover {
      cursor: pointer;
    }
  }
}
</style>
