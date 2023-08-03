/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  defineComponent,
  PropType,
  h,
  ref,
  reactive,
  toRefs,
  onMounted,
  onUnmounted,
  watchEffect,
  nextTick
} from 'vue'
import { NIcon, NLog, NSpace, NButton } from 'naive-ui'
import Modal from '../modal'
import {
  DownloadOutlined,
  FullscreenExitOutlined,
  FullscreenOutlined,
  SyncOutlined,
  VerticalAlignTopOutlined,
  VerticalAlignBottomOutlined
} from '@vicons/antd'
import screenfull from 'screenfull'
import styles from './index.module.scss'
import i18n from '@/locales'

const props = {
  showModalRef: {
    type: Boolean as PropType<boolean>,
    default: false
  },
  logRef: {
    type: String as PropType<string>,
    default: ''
  },
  logLoadingRef: {
    type: Boolean as PropType<boolean>,
    default: false
  },
  row: {
    type: Object as PropType<any>,
    default: {}
  },
  showDownloadLog: {
    type: Boolean as PropType<boolean>,
    default: false
  }
}

export default defineComponent({
  name: 'log-modal',
  props,
  emits: ['confirmModal', 'refreshLogs', 'downloadLogs'],
  setup(props, ctx) {
    const { t } = i18n.global
    const logInstRef = ref()
    const variables = reactive({
      isFullscreen: false
    })

    const change = () => {
      variables.isFullscreen = screenfull.isFullscreen
    }

    const renderIcon = (icon: any) => {
      return () => h(NIcon, null, { default: () => h(icon) })
    }

    const confirmModal = () => {
      variables.isFullscreen = false
      ctx.emit('confirmModal', props.showModalRef)
    }

    const refreshLogs = () => {
      ctx.emit('refreshLogs', props.row)
    }

    const handleFullScreen = () => {
      screenfull.toggle(document.querySelectorAll('.logModalRef')[0])
    }

    const downloadLogs = () => {
      ctx.emit('downloadLogs', props.row)
    }

    const setLogPosition = (position: 'top' | 'bottom') => {
      logInstRef.value?.scrollTo({ position, slient: false })
    }

    onMounted(() => {
      screenfull.on('change', change)
      watchEffect(() => {
        if (props.logRef) {
          nextTick(() => {
            logInstRef.value?.scrollTo({ position: 'bottom', slient: true })
          })
        }
      })
    })

    onUnmounted(() => {
      screenfull.on('change', change)
    })

    const headerLinks:any = ref([
      {
        text: t('project.workflow.download_log'),
        show: props.showDownloadLog,
        action: downloadLogs,
        icon: renderIcon(DownloadOutlined)
      },
      {
        text: t('project.task.refresh'),
        show: true,
        action: refreshLogs,
        icon: renderIcon(SyncOutlined)
      },
      {
        text: variables.isFullscreen
          ? t('project.task.cancel_full_screen')
          : t('project.task.enter_full_screen'),
        show: true,
        action: handleFullScreen,
        icon: variables.isFullscreen
          ? renderIcon(FullscreenExitOutlined)
          : renderIcon(FullscreenOutlined)
      }
    ])

    return {
      t,
      renderIcon,
      confirmModal,
      refreshLogs,
      downloadLogs,
      handleFullScreen,
      ...toRefs(variables),
      logInstRef,
      setLogPosition,
      headerLinks
    }
  },
  render() {
    const {
      t,
      renderIcon,
      refreshLogs,
      downloadLogs,
      isFullscreen,
      handleFullScreen,
      showDownloadLog,
      setLogPosition
    } = this
    return (
      <Modal
        class='logModalRef'
        title={t('project.task.view_log')}
        show={this.showModalRef}
        cancelShow={false}
        onConfirm={this.confirmModal}
        style={{ width: '60%' }}
        confirmText={t('modal.close')}
      >
        <NLog
          rows={30}
          log={this.logRef}
          loading={this.logLoadingRef}
          style={{ height: isFullscreen ? 'calc(100vh - 140px)' : '525px' }}
          ref='logInstRef'
        />
        <NSpace vertical class={styles['btns']}>
          <NButton
            size='small'
            circle
            tertiary
            type='primary'
            onClick={() => void setLogPosition('top')}
          >
            <NIcon size={20}>
              <VerticalAlignTopOutlined />
            </NIcon>
          </NButton>
          <NButton
            size='small'
            circle
            tertiary
            type='primary'
            onClick={() => void setLogPosition('bottom')}
          >
            <NIcon size={20}>
              <VerticalAlignBottomOutlined />
            </NIcon>
          </NButton>
        </NSpace>
      </Modal>
    )
  }
})
