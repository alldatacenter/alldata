<template>
  <div class="custom-top-bar">
    <div class="version-info">
      <span class="g-mr-8">{{`${$t('version')}:  ${verInfo.version}`}}</span>
      <span class="g-mr-8">{{`${$t('commitTime')}:  ${verInfo.commitTime}`}}</span>
    </div>
    <a-tooltip placement="bottomRight" arrow-point-at-center overlayClassName="topbar-tooltip">
      <template #title>{{$t('userGuide')}}</template>
      <question-circle-outlined class="question-icon" @click="goDocs" />
    </a-tooltip>
  </div>
</template>

<script lang="ts">

import { defineComponent, onMounted, reactive } from 'vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import { getVersionInfo } from '@/services/global.service'

interface IVersion {
  version: string
  commitTime: string
}

export default defineComponent ({
  name: 'Topbar',
  components: {
    QuestionCircleOutlined
  },
  setup() {
    const verInfo = reactive<IVersion>({
      version: '',
      commitTime: ''
    })
    const getVersion = async() => {
      const res = await getVersionInfo()
      if (res) {
        verInfo.version = res.version
        verInfo.commitTime = res.commitTime
      }
    }

    const goDocs = () => {
      window.open('https://arctic.netease.com/')
    }
    onMounted(() => {
      getVersion()
    })

    return {
      verInfo,
      goDocs
    }
  }
})
</script>

<style lang="less">
  .custom-top-bar {
    height: 48px;
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: flex-end;
    background: #fff;
    border-bottom: 1px solid #e8e8e8;
    box-shadow: 0 1px 2px rgb(0 21 41 / 5%);
    padding: 0 12px 0 0;
    font-size: 12px;
    .question-icon {
      font-size: 12px;
      margin-top: -2px;
    }
  }
  .topbar-tooltip .ant-tooltip-inner {
    font-size: 12px;
  }
</style>
