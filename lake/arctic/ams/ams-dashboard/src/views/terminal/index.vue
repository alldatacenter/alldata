<template>
  <div class="console-wrap">
    <div class="console-content" :class="{'fullscreen': fullscreen }">
      <div :style="{ height: `${sqlResultHeight}px` }" class="sql-wrap">
        <div class="sql-block">
          <div class="top-ops g-flex-jsb">
            <div class="title-left g-flex-ac">
              <div class="select-catalog g-mr-12">
                <span class="label">{{$t('use')}}</span>
                <a-select
                  v-model:value="curCatalog"
                  style="width: 200px"
                  :options="catalogOptions"
                  @change="changeUseCatalog"
                  >
                </a-select>
              </div>
              <a-tooltip v-if="runStatus === 'Running'" :title="$t('pause')" placement="bottom">
                <svg-icon className="icon-svg" icon-class="sqlpause" @click="handleIconClick('pause')" class="g-mr-12" :disabled="readOnly" />
              </a-tooltip>
              <a-tooltip v-else :title="$t('run')" placement="bottom">
                <svg-icon  className="icon-svg" icon-class="sqldebug" @click="handleIconClick('debug')" class="g-mr-12" :disabled="readOnly" />
              </a-tooltip>
              <a-tooltip :title="$t('format')" placement="bottom">
                <svg-icon className="icon-svg" :isStroke="true" icon-class="format" @click="handleIconClick('format')" :disabled="readOnly" />
              </a-tooltip>
            </div>
            <div class="title-right">
              <a-tooltip :title="fullscreen ? $t('recovery') : $t('fullscreen')" placement="bottom" :getPopupContainer="getPopupContainer">
                <svg-icon className="icon-svg" :isStroke="true" :icon-class="fullscreen ? 'sqlinit' : 'sqlmax'" @click="handleFull" :disabled="false" class="g-ml-12" />
              </a-tooltip>
            </div>
          </div>
          <div class="sql-content">
            <div class="sql-raw">
              <sql-editor
                ref="sqlEditorRef"
                :sqlValue="sqlSource"
                v-model:value="sqlSource"
                :readOnly="readOnly"
                :options="{
                  readOnly: readOnly,
                  minimap: { enabled: false }
                }"
              />
            </div>
            <div v-if="runStatus" class="run-status" :style="{background: bgcMap[runStatus]}">
              <template v-if="runStatus === 'Running'">
                <loading-outlined style="color: #1890ff" />
              </template>
              <template v-if="runStatus === 'Canceled' || runStatus === 'Failed'">
                <close-circle-outlined style="color: #ff4d4f" />
              </template>
              <template v-if="runStatus === 'Finished'">
                <check-circle-outlined style="color: #52c41a" />
              </template>
              <template v-if="runStatus === 'Created'">
                <close-circle-outlined style="color:#333" />
              </template>
              <span class="g-ml-12">{{ $t(runStatus) }}</span>
            </div>
          </div>
        </div>
        <div class="sql-shortcuts">
          <div class="shortcuts">{{$t('sqlShortcuts')}}</div>
          <a-button v-for="code in shortcuts" :key="code" type="link" :disabled="runStatus === 'Running'" @click="generateCode(code)" class="code">{{ code }}</a-button>
        </div>
      </div>
      <!-- sql result -->
      <div class="sql-result" :style="{ height: `calc(100% - ${sqlResultHeight}px)` }"  :class="resultFullscreen ? 'result-full' : ''">
        <span class="drag-line" @mousedown="dragMounseDown"><svg-icon class="icon" icon-class="slide"/></span>
        <div class="tab-operation">
          <div class="tab">
            <span :class="{ active: operationActive === 'log' }" @click="operationActive = 'log'" class="tab-item">{{$t('log')}}</span>
            <span v-for="item in resultTabList" :key="item.id" :class="{ active: operationActive === item.id }" @click="operationActive = item.id" class="tab-item">{{item.id}}</span>
          </div>
          <!-- <div v-if="!fullscreen" class="operation">
            <a-tooltip placement="bottom" :title="$t('maximize')">
              <span @click="resultFull"><svg-icon :isStroke="true" className="icon-svg" :icon-class="resultFullscreen ? 'sqlinit' : 'sqlmax'" :disabled="false" /></span>
            </a-tooltip>
          </div> -->
        </div>
        <div class="debug-result">
          <sql-log v-show="operationActive === 'log'" ref="sqlLogRef" />
          <template v-for="item in resultTabList" :key="item.id">
            <sql-result v-if="operationActive === item.id" :info="item" />
          </template>
        </div>
      </div>
    </div>
    <u-loading v-if="loading" />
  </div>
</template>

<script lang="ts">
import { defineComponent, onBeforeUnmount, onMounted, reactive, ref, shallowReactive, watch } from 'vue'
import SqlEditor from '@/components/sql-editor/index.vue'
import SqlResult from './components/sql-result.vue'
import SqlLog from './components/sql-log.vue'
import { ICatalogItem, IDebugResult, ILableAndValue, IMap, debugResultBgcMap } from '@/types/common.type'
import { executeSql, getExampleSqlCode, getJobDebugResult, getLogsResult, getShortcutsList, stopSql, getLastDebugInfo } from '@/services/terminal.service'
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { getCatalogList } from '@/services/table.service'
import { usePlaceholder } from '@/hooks/usePlaceholder'

interface ISessionInfo {
  sessionId: number
  sqlNumber: number
}

interface ILogResult {
  logStatus: string // Created,Running,Finished,Failed,Canceled
  logs: string[]
}

export default defineComponent({
  name: 'Terminal',
  components: {
    SqlEditor,
    SqlResult,
    SqlLog,
    CheckCircleOutlined,
    CloseCircleOutlined,
    LoadingOutlined
  },
  setup() {
    const placeholder = reactive(usePlaceholder())
    const loading = ref<boolean>(false)
    const sqlEditorRef = ref<any>(null)
    const sqlLogRef = ref<any>(null)
    const readOnly = ref<boolean>(false)
    const sqlSource = ref<string>('')
    const showDebug = ref<boolean>(false)
    const runStatus = ref<string>('')
    const sessionId = ref<number>()
    const fullscreen = ref<boolean>(false) // sql fullscreen
    const resultFullscreen = ref<boolean>(false) // result fullscreen
    const operationActive = ref<string>('log')
    const catalogOptions = reactive<ILableAndValue[]>([])
    const shortcuts = reactive<string[]>([])
    const curCatalog = ref<string>('')
    const logInterval = ref<number>()
    const resultTabList = reactive<IDebugResult[]>([])

    const sqlResultHeight = ref<number>(476)

    const bgcMap: IMap<string> = shallowReactive(debugResultBgcMap)
    const storageSqlSourceKey = 'easylake-sql-source'
    const storageUseCatalogKey = 'easylake-use-catalog'

    watch(
      () => readOnly,
      () => {
        sqlEditorRef.value.updateOptions({ readOnly })
      })

    const handleIconClick = (action: string) => {
      if (action === 'debug') {
        handleDebug()
        return
      }
      if (action === 'format') {
        sqlEditorRef.value && sqlEditorRef.value.executeCommand('format')
        return
      }
      if (action === 'pause') {
        stopDebug()
      }
    }

    const getCatalogOps = async() => {
      const res: ICatalogItem[] = await getCatalogList();
      (res || []).forEach((ele: ICatalogItem) => {
        catalogOptions.push({
          value: ele.catalogName,
          label: ele.catalogName
        })
      })
      if (catalogOptions.length) {
        const val = getValueFromStorageByKey(storageUseCatalogKey)
        const index = catalogOptions.findIndex(ele => ele.value === val)
        curCatalog.value = index > -1 ? val : catalogOptions[0].value
      }
    }

    const getShortcuts = async() => {
      const res: string[] = await getShortcutsList()
      shortcuts.push(...(res || []))
    }

    const changeUseCatalog = () => {
      saveValueToStorage(storageUseCatalogKey, curCatalog.value)
    }

    const handleFull = () => {
      fullscreen.value = !fullscreen.value
    }

    const resultFull = () => {
      resultFullscreen.value = !resultFullscreen.value
    }
    const resetResult = () => {
      resultTabList.length = 0
      sqlLogRef.value.initData('')
    }

    const handleDebug = async() => {
      try {
        if (!curCatalog.value) {
          message.error(placeholder.selectClPh)
          return
        }
        showDebug.value = true
        resetResult()
        runStatus.value = 'Running'
        const res:ISessionInfo = await executeSql({
          catalog: curCatalog.value,
          sql: sqlSource.value
        })
        sessionId.value = res.sessionId || 0
        getLogResult()
      } catch (error) {
        runStatus.value = 'Failed'
        message.error((error as Error).message || 'error')
      }
    }

    const stopDebug = async() => {
      if (sessionId.value) {
        try {
          runStatus.value = 'Canceled'
          await stopSql(sessionId.value)
        } catch (error) {
          runStatus.value = 'Failed'
        }
      }
    }

    const getDebugResult = async() => {
      try {
        resultTabList.length = 0
        const res: IDebugResult[] = await getJobDebugResult(sessionId.value || 0)
        if (res && res.length) {
          resultTabList.push(...res)
        }
      } catch (error) {
      }
    }

    const getLogResult = async() => {
      logInterval.value && clearTimeout(logInterval.value)
      if (runStatus.value !== 'Running') {
        return
      }
      if (sessionId.value) {
        const res: ILogResult = await getLogsResult(sessionId.value)
        operationActive.value = 'log'
        const { logStatus, logs } = res || {}
        if (logs?.length) {
          sqlLogRef.value.initData(logs.join('\n'))
        }
        runStatus.value = logStatus
        await getDebugResult()
        if (logStatus === 'Finished' || logStatus === 'Canceled') {
          if (resultTabList.length) {
            operationActive.value = resultTabList[0].id
          }
        } else {
          logInterval.value = setTimeout(() => {
            getLogResult()
          }, 1500)
        }
      }
    }

    const generateCode = async(code: string) => {
      try {
        operationActive.value = 'log'
        if (runStatus.value === 'Running') {
          return
        }
        clearTimeout(logInterval.value)
        loading.value = true
        const res: string = await getExampleSqlCode(code)
        sqlSource.value = res
        showDebug.value = false
        runStatus.value = ''
        resetResult()
      } catch (error) {
        message.error((error as Error).message)
      } finally {
        loading.value = false
      }
    }

    const getPopupContainer = () => {
      return document.body
    }

    const getLastSqlInfo = async() => {
      try {
        if (sqlEditorRef.value) {
          sqlSource.value = getValueFromStorageByKey(storageSqlSourceKey)
        }
        loading.value = true
        const res = await getLastDebugInfo()
        sessionId.value = res.sessionId
        if (res.sessionId > 0) {
          if (sqlEditorRef.value && !sqlSource.value) {
            sqlSource.value = res.sql || ''
          }
          runStatus.value = 'Running'
          showDebug.value = true
          getLogResult()
        }
      } catch (error) {
        message.error((error as Error).message)
      } finally {
        loading.value = false
      }
    }
    const editorSize = {
      topbarHeight: 48,
      optionHeight: 44,
      resultTabHeight: 40,
      runStatusHeight: 32,
      gap: 48
    }
    let mounseDownClientTop = 0
    let mouseDownLeftHeight = 0
    const dragMounseDown = (e: any) => {
      mounseDownClientTop = e.clientY
      mouseDownLeftHeight = sqlResultHeight.value
      window.addEventListener('mousemove', dragMounseMove)
      window.addEventListener('mouseup', dragMounseUp)
    }

    const dragMounseMove = (e: any) => {
      const mounseTop = e.clientY
      const diff = mounseTop - mounseDownClientTop
      const topbarHeight = fullscreen.value ? 0 : editorSize.topbarHeight
      const runStatusHeight = runStatus.value ? editorSize.runStatusHeight : 0
      let top = mouseDownLeftHeight + diff
      top = Math.max(top, editorSize.optionHeight + runStatusHeight)
      top = Math.min(top, window.innerHeight - topbarHeight - (fullscreen.value ? 0 : editorSize.gap) - editorSize.optionHeight - 4)
      sqlResultHeight.value = top
    }

    const dragMounseUp = () => {
      window.removeEventListener('mousemove', dragMounseMove)
      window.removeEventListener('mouseup', dragMounseUp)
    }

    const saveValueToStorage = (key: string, value: string) => {
      localStorage.setItem(key, value)
    }

    const getValueFromStorageByKey = (key: string) => {
      return localStorage.getItem(key) || ''
    }

    onBeforeUnmount(() => {
      clearTimeout(logInterval.value)
      saveValueToStorage(storageSqlSourceKey, sqlSource.value)
      console.log('onBeforeUnmount', sqlSource.value)
    })

    onMounted(() => {
      getLastSqlInfo()
      getShortcuts()
      getCatalogOps()
    })

    return {
      loading,
      bgcMap,
      sqlLogRef,
      sqlEditorRef,
      fullscreen,
      resultFullscreen,
      operationActive,
      resultTabList,
      runStatus,
      shortcuts,
      curCatalog,
      catalogOptions,
      handleIconClick,
      handleFull,
      resultFull,
      showDebug,
      sqlSource,
      readOnly,
      generateCode,
      getPopupContainer,
      sqlResultHeight,
      dragMounseDown,
      changeUseCatalog
    }
  }
})

</script>

<style lang="less" scoped>
.console-wrap {
  height: 100%;
  padding: 16px 24px;
  .console-content {
    background-color: #fff;
    height: 100%;
    width: 100%;
    &.fullscreen {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 99;
      .sql-wrap {
        flex: 1;
        display: flex;
        .sql-block {
          flex: 1;
          display: flex;
          flex-direction: column;
        }
      }
    }
    .sql-wrap {
      user-select: none;
      font-size: 0;
      border: 1px solid #e5e5e5;
      border-bottom: 0;
      .sql-block {
        font-size: @font-size-base;
        display: inline-block;
        width: calc(100% - 200px);
        height: 100%;
        .top-ops {
          padding: 6px 16px;
          align-items: center;
        }
        .icon-svg {
          color: #000;
          cursor: pointer;
          &:hover {
            color: @primary-color;
          }
        }
        .select-catalog {
          .label {
            padding-right: 8px;
          }
        }
        .title-right {
          display: flex;
          justify-content: flex-end;
          height: 32px;
          align-items: center;
        }
        .sql-content {
          // height: 440px;
          height: calc(100% - 44px);
          // height: calc(100% - 76px); // 44 + 32
          // height: 60%;
          border-top: 1px solid #e5e5e5;
          border-bottom: 1px solid #e5e5e5;
          position: relative;
          display: flex;
          flex-direction: column;
          .sql-raw {
            height: 100%;
            overflow: hidden;
            flex: 1;
          }
          .run-status {
            height: 32px;
            flex-shrink: 0;
          }
        }
        .result-full {
          height: auto;
          position: absolute;
          top: 0;
          right: 0;
          left: 0;
          bottom: 0;
        }
      }
      .sql-shortcuts {
        font-size: @font-size-base;
        display: inline-block;
        vertical-align: top;
        width: 200px;
        // height: 484px;
        height: 100%;
        border-left: 1px solid #e5e5e5;
        .shortcuts {
          padding: 0 16px;
          line-height: 44px;
          border-bottom: 1px solid #e5e5e5;
        }
        .code {
          width: 100px;
        }
        .ant-btn {
          text-align: left;
        }
      }
    }
    .sql-result {
      background-color: #fff;
      border: 1px solid #e5e5e5;
      // border-bottom: 0;
      border-top: 0;
      width: 100%;
      padding-bottom: 12px;
      // height: calc(100% - 476px);
      display: flex;
      flex-direction: column;
      position: relative;
      flex: 1;
      .drag-line {
        position: absolute;
        top: -1px;
        left: 0;
        width: 100%;
        height: 6px;
        font-size: 18px;
        border-top: 1px solid #e5e5e5;
        .icon {
          position: absolute;
          top: -12px;
          font-size: 24px;
          left: 50%;
          transform: rotate(90deg);
          z-index: 3;
        }
        &:hover {
          cursor: n-resize;
        }
      }
      .debug-result {
        flex: 1;
        overflow: auto;
      }
    }
  }
  .debug-icon {
    &:hover {
      color: #1890ff;
    }
  }
  .run-status {
    padding: 6px 12px;
    position: absolute;
    left: 0;
    bottom: 0;
    width: 100%;
    z-index: 2;
    background-color: #fff;
  }
  .tab-operation {
    display: flex;
    justify-content: space-between;
    height: 40px;
    line-height: 40px;
    border-bottom: 1px solid #e5e5e5;
    padding: 0 20px 0 16px;
    .tab {
      display: flex;
      .tab-item {
        padding: 0 16px;
        text-align: center;
        cursor: pointer;
        &.active {
          border-bottom: 2px solid @primary-color;
        }
      }
    }
    .operation {
      .icon-svg {
        margin-right: 16px;
        cursor: pointer;
        &:hover {
          color: @primary-color;
        }
      }
    }
  }
  .loading-icon{
    display: block;
    margin-top: 80px;
  }
}
</style>
