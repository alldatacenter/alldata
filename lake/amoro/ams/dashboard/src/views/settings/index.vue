<template>
  <div class="setting-wrap">
    <a-tabs v-model:activeKey="activeKeyTab" @change="onChangeTab">
      <a-tab-pane :key="tabMap['system'].key" :tab="tabMap['system'].title">
        <div class="system-setting">
          <a-table
            rowKey="key"
            :columns="basicColumns"
            v-if="systemSettingArray.length"
            :data-source="systemSettingArray"
            :pagination="false"
          />
        </div>
      </a-tab-pane>
      <a-tab-pane :key="tabMap['container'].key" :tab="tabMap['container'].title">
        <div class="container-setting">
          <a-collapse v-model:activeKey="activeKey">
            <a-collapse-panel v-for="container in containerSetting" :key="container.name" :header="container.name">
              <ul class="content">
                <li class="item">
                  <h3 class="left">{{$t('name')}}</h3>
                  <span class="right">{{container.name}}</span>
                </li>
                <li v-if="container.classpath" class="item">
                  <h3 class="left">{{$t('implementation')}}</h3>
                  <span class="right">{{container.classpath}}</span>
                </li>
              </ul>
              <h3 class="g-mb-12 g-mt-12">{{$t('properties')}}</h3>
                <a-table
                  rowKey="key"
                  :columns="basicColumns"
                  :data-source="container.propertiesArray"
                  :pagination="false"
                />
              <h3 class="g-mb-12 g-mt-12">{{$t('optimizerGroups')}}</h3>
              <a-collapse>
                <a-collapse-panel v-for="innerGroup in container.optimizeGroup" :key="innerGroup.name" :header="innerGroup.name">
                  <a-table
                    rowKey="name"
                    :columns="basicColumns"
                    :data-source="innerGroup.innerPropertiesArray"
                    :pagination="false"
                  />
                </a-collapse-panel>
              </a-collapse>
            </a-collapse-panel>
          </a-collapse>
        </div>
      </a-tab-pane>
    </a-tabs>
  </div>
  <u-loading v-if="loading" />
</template>

<script lang="ts" setup>
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { IColumns, IKeyAndValue, IContainerSetting } from '@/types/common.type'
import { useI18n } from 'vue-i18n'
import { getSystemSetting, getContainersSetting } from '@/services/setting.services'
const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const loading = ref<boolean>(false)
const systemSettingArray = reactive<IKeyAndValue>([])
const containerSetting = reactive<IContainerSetting[]>([])
const optimzeGroupColumns: IColumns[] = reactive([
  { title: t('name'), dataIndex: 'name', width: 340, ellipsis: true },
  { title: t('propertiesMemory', { type: 'taskmanager' }), dataIndex: 'tmMemory', width: '50%', ellipsis: true },
  { title: t('propertiesMemory', { type: 'jobmanager' }), dataIndex: 'jmMemory', width: '50%', ellipsis: true }
])
const basicColumns: IColumns[] = reactive([
  { title: t('key'), dataIndex: 'key', width: 340, ellipsis: true },
  { title: t('value'), dataIndex: 'value' }
])
const activeKey = ref<string[]>([])
const tabMap = {
  system: { title: t('systemSetting'), key: 'system' },
  container: { title: t('containerSetting'), key: 'container' }
}
const activeKeyTab = ref<string>(tabMap.system.key)
watch(
  () => route,
  (val) => {
    activeKeyTab.value = val.query?.tab as string || tabMap.system.key
    onChangeTab(activeKeyTab.value)
  }, {
    immediate: true
  }
)
async function getSystemSettingInfo() {
  try {
    loading.value = true
    const res = await getSystemSetting()
    if (!res) { return }
    systemSettingArray.length = 0
    Object.keys(res).forEach(key => {
      systemSettingArray.push({
        key: key,
        value: res[key]
      })
    })
  } finally {
    loading.value = false
  }
}
async function getContainersSettingInfo() {
  try {
    loading.value = true
    const res = await getContainersSetting()
    activeKey.value = []
    containerSetting.length = 0;
    (res || []).forEach((ele, index) => {
      ele.propertiesArray = []
      activeKey.value.push(ele.name)
      containerSetting.push(ele)
      Object.keys(ele.properties || {}).forEach(key => {
        containerSetting[index].propertiesArray.push({
          key: key,
          value: ele.properties[key]
        })
      });
      (ele.optimizeGroup || []).forEach(group => {
        group.innerPropertiesArray = []
        Object.keys(group.properties || {}).forEach(key => {
          group.innerPropertiesArray.push({
            key: key,
            value: group.properties[key]
          })
        })
      })
    })
  } finally {
    loading.value = false
  }
}
function onChangeTab(key: string) {
  const query = { ...route.query }
  query.tab = key
  router.replace({ query: { ...query } })
  getSettingInfo()
}
function getSettingInfo() {
  if (activeKeyTab.value === tabMap.system.key) {
    getSystemSettingInfo()
  } else {
    getContainersSettingInfo()
  }
}
</script>

<style lang="less" scoped>
.setting-wrap {
  height: 100%;
  overflow: auto;
  padding: 16px 24px;
  h1,h2,h3 {
    font-weight: 500;
  }
  h1 {
    font-size: 16px;
  }
  h3 {
    font-size: 14px;
  }
  .container-setting {
    :deep(.ant-collapse) {
      > .ant-collapse-item > .ant-collapse-header {
        font-weight: 500;
        .ant-collapse-arrow {
          vertical-align: 1px;
        }
      }
      .ant-collapse-content > .ant-collapse-content-box {
        padding: 6px 16px 32px;
      }
      .ant-collapse-item:last-child .ant-collapse-content > .ant-collapse-content-box {
        padding-bottom: 16px;
      }
    }
  }
  .content {
    .item {
      padding: 6px 0;
      display: flex;
      word-break: break-all;
    }
    .left {
      width: 320px;
      flex-shrink: 0;
      margin-right: 16px;
    }
  }
}
</style>
