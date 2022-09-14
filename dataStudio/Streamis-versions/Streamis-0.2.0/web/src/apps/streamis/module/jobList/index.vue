<template>
  <div>
    <Spin v-if="baseLoading" fix></Spin>
    <titleCard :title="$t('message.streamis.moduleName.jobList')">
      <div>
        <div>
          <Form v-if="isBatching" inline>
            <FormItem>
              <Button
                type="primary"
                :disabled="!selections.length"
                @click="doRestart(true)"
                style="width:80px;height:30px;background:rgba(22, 155, 213, 1);margin-left: 80px;display: flex;align-items: center;justify-content: center;"
              >
                {{$t('message.streamis.formItems.snapshotRestart')}}
              </Button>
            </FormItem>
            <FormItem>
              <Button
                type="primary"
                :disabled="!selections.length"
                @click="doRestart(false)"
                style="width:80px;height:30px;background:rgba(22, 155, 213, 1);margin-left: 80px;display: flex;align-items: center;justify-content: center;"
              >
                {{$t('message.streamis.formItems.directRestart')}}
              </Button>
            </FormItem>
            <FormItem>
              <Button
                type="primary"
                @click="hideButtons"
                style="width:80px;height:30px;background:rgba(22, 155, 213, 1);margin-left: 80px;display: flex;align-items: center;justify-content: center;"
              >
                {{$t('message.streamis.formItems.cancel')}}
              </Button>
            </FormItem>
          </Form>
          <Form ref="queryForm" inline v-else>
            <FormItem>
              <Input
                search
                v-model="query.jobName"
                :placeholder="$t('message.streamis.formItems.jobName')"
                @on-click="handleNameQuery"
                @on-enter="handleNameQuery"
              >
              </Input>
            </FormItem>
            <FormItem
              :label="$t('message.streamis.formItems.jobStatus')"
              :label-width="120"
            >
              <Select v-model="query.jobStatus" class="select">
                <Option
                  v-for="(item, index) in jobStatus"
                  :value="item"
                  :key="index"
                >
                  {{ $t('message.streamis.jobStatus.' + item) }}
                </Option>
              </Select>
            </FormItem>

            <FormItem>
              <Button
                type="primary"
                @click="handleQuery()"
                style="width:80px;height:30px;background:rgba(22, 155, 213, 1);margin-left: 80px;"
              >
                {{ $t('message.streamis.formItems.queryBtn') }}
              </Button>
            </FormItem>

            <FormItem>
              <Button
                type="primary"
                @click="showButtons"
                style="width:80px;height:30px;background:rgba(22, 155, 213, 1);margin-left: 80px;display: flex;align-items: center;justify-content: center;"
              >
                {{$t('message.streamis.formItems.batchAction')}}
              </Button>
            </FormItem>
          </Form>
          <Table ref="list" :columns="columns" :data="tableDatas" :loading="loading" @on-selection-change="selectionChange" :class="{table: isBatching}">
            <template slot-scope="{ row, index }" slot="jobName">
              <div
                class="jobName"
                v-show="index === 0"
                @click="handleUpload()"
                v-if="false"
              >
                <Icon type="md-add" class="upload" />
                <span>{{
                  $t('message.streamis.jobListTableColumns.upload')
                }}</span>
              </div>
              <div class="jobName" v-show="index === 0">
                <Upload
                  :action="`/api/rest_j/v1/streamis/streamJobManager/job/upload?projectName=${projectName}`"
                  :on-success="jarUploadSuccess"
                  :on-error="jarUploadError"
                  :show-upload-list="false"
                >
                  <Icon type="md-add" class="upload" />
                  <span>{{
                    $t('message.streamis.jobListTableColumns.upload')
                  }}</span></Upload
                >
              </div>
              <div class="jobName" v-show="index !== 0">
                <Dropdown transfer @on-click="name => handleRouter(row, name)">
                  <Icon type="md-more" class="more" />
                  <DropdownMenu slot="list">
                    <DropdownItem
                      v-for="(item, index) in jobMoudleRouter"
                      :name="item"
                      :key="index"
                    >
                      {{ $t('message.streamis.jobMoudleRouter.' + item) }}
                    </DropdownItem>
                  </DropdownMenu>
                </Dropdown>
                <div style="margin-left: 5px" @click="handleRouter(row)">
                  <a href="javascript:void(0)">{{ row.name }} </a>
                </div>
              </div>
            </template>
            <template slot-scope="{ row, index }" slot="version">
              <div v-show="index !== 0" class="versionWrap">
                <div class="version" @click="versionDetail(row)">
                  {{ row.version }}
                </div>
              </div>
            </template>
            <template slot-scope="{ row, index }" slot="operation">
              <div v-show="index !== 0">
                <Button
                  type="primary"
                  v-show="row.status !== 5"
                  :loading="row.buttonLoading"
                  style="height:22px;background:#008000;margin-right: 5px; font-size:10px;"
                  @click="handleAction(row, index)"
                >
                  {{ $t('message.streamis.formItems.startBtn') }}
                </Button>
                <Poptip placement="top" v-model="row.poptipVisible" :disabled="row.buttonLoading">
                  <Button
                    type="primary"
                    v-show="row.status === 5"
                    :disabled="row.buttonLoading"
                    :loading="row.buttonLoading"
                    style="height:22px;background:#ff0000;margin-right: 5px; font-size:10px;"
                  >
                    {{ $t('message.streamis.formItems.stopBtn') }}
                  </Button>
                  <div slot="content">
                    <div class="btn-wrap">
                      <Button
                        class="btn"
                        type="primary"
                        :disabled="row.buttonLoading"
                        style="height:22px;background:#ff0000;margin-right: 5px; font-size:10px;"
                        @click="handleStop(row, 0, index)"
                      >
                        {{ $t('message.streamis.formItems.directStop') }}
                      </Button>
                      <Button
                        class="btn"
                        type="primary"
                        :disabled="row.buttonLoading"
                        style="height:22px;background:#ff0000;margin-right: 5px; font-size:10px;"
                        @click="handleStop(row, 1, index)"
                      >
                        {{ $t('message.streamis.formItems.snapshotAndStop') }}
                      </Button>
                    </div>
                  </div>
                </Poptip>
                    
                <Button
                  type="primary"
                  @click="handleRouter(row, 'jobConfig')"
                  style="height:22px;background:rgba(22, 155, 213, 1);margin-right: 5px; font-size:10px;"
                >
                  {{ $t('message.streamis.formItems.configBtn') }}
                </Button>
        
              </div>
            </template>
          </Table>
          <Page
            :total="pageData.total"
            class="page"
            :page-size="pageData.pageSize"
            show-total
            show-elevator
            show-sizer
            @on-change="handlePageChange"
            @on-page-size-change="handlePageSizeChange"
          />
        </div>
      </div>
    </titleCard>
    <versionDetail
      :visible="modalVisible"
      :datas="versionDatas"
      :projectName="projectName"
      @modalCancel="modalCancel"
    />
    <uploadJobJar
      :visible="uploadVisible"
      @jarModalCancel="jarModalCancel"
      @jarUploadSuccess="jarUploadSuccess"
    />
    <Modal
      v-model="processModalVisable"
      :title="modalTitle"
      width="800"
      footer-hide
      @on-visible-change="onClose"
    >
      <div class="wrap">
        <Spin v-if="modalLoading" fix></Spin>
        <div class="general">
          <div class="bar"></div>
          <div class="text">{{modalContent}}({{orderNum}}/{{selections.length}})</div>
        </div>
        <div class="info" v-if="snapPaths.length">
          <h4>{{$t('message.streamis.jobListTableColumns.snapshotInfo')}}:</h4>
          <div v-for="item in snapPaths" :key="item.taskId" style="margin-bottom: 32px;">
            <span>{{item.taskName}}</span>,
            <span style="margin-right: 32px;">{{item.taskId}}:</span>
            <span>{{item.info}}</span>
          </div>
        </div>
        <div class="info" v-if="failTasks.length">
          <h4>{{$t('message.streamis.jobListTableColumns.failInfo')}}:</h4>
          <div v-for="item in failTasks" :key="item.taskId" style="margin-bottom: 32px;">
            <span>{{item.taskName}}</span>,
            <span style="margin-right: 32px;">{{item.taskId}}:</span>
            <span>{{item.info}}</span>
          </div>
        </div>
        <div v-if="isFinish" style="text-align: center;">
          <Button
            type="primary"
            @click="onClose()"
          >
            {{ $t('message.streamis.formItems.confirmBtn') }}
          </Button>
        </div>
      </div>
    </Modal>
    <Modal
      v-model="snapModalVisable"
      :title="snapTitle"
      width="800"
      footer-hide
    >
      <div class="wrap">
        <div class="info">
          <h4>{{$t('message.streamis.jobListTableColumns.snapshotInfo')}}</h4>
          <div>
            {{snapshotPath}}
          </div>
        </div>
      </div>
    </Modal>
  </div>
</template>
<script>
import api from '@/common/service/api'
import titleCard from '@/apps/streamis/components/titleCard'
import versionDetail from '@/apps/streamis/module/versionDetail'
import uploadJobJar from '@/apps/streamis/module/uploadJobJar'
import { allJobStatuses } from '@/apps/streamis/common/common'
import moment from 'moment'

/**
 * 渲染特殊表头
 */
function renderSpecialHeader(h, params) {
  return h('div', [
    h('strong', params.column.title),
    h(
      'span',
      {
        style: {
          color: 'red'
        }
      },
      '*'
    )
  ])
}

export default {
  components: { titleCard, versionDetail, uploadJobJar },
  data() {
    return {
      query: {
        jobName: '',
        jobStatus: 'all'
      },
      jobStatus: ['all'].concat(allJobStatuses.map(item => item.name)),

      tableDatas: [
        {},
        // {
        //   id: 10,
        //   name: 'flinkJarTestc',
        //   workspaceName: null,
        //   projectName: 'flinkJarTest3',
        //   jobType: 'flink.jar',
        //   label: 'e,t,y,h,g',
        //   createBy: 'hdfs',
        //   createTime: 1636338541000,
        //   status: 0,
        //   version: 'v00002',
        //   lastVersionTime: 1636339360000,
        //   description: '这是一个FlinkJar测试Job3'
        // },
        // {
        //   id: 9,
        //   name: 'flinkSqlTesta',
        //   workspaceName: null,
        //   projectName: 'flinkSqlTestD',
        //   jobType: 'flink.sql',
        //   label: 'a,b,c',
        //   createBy: 'hdfs',
        //   createTime: 1636338055000,
        //   status: 0,
        //   version: 'v00001',
        //   lastVersionTime: 1636338055000,
        //   description: '这是一个FlinkSql测试JobD'
        // }
      ],
      baseLoading: false,
      modalLoading: false,
      failTasks: [],
      snapPaths: [],
      timer: null,
      isBatching: false,
      selections: [],
      processModalVisable: false,
      isFinish: false,
      modalTitle: '',
      snapTitle: '',
      snapshotPath: '',
      snapModalVisable: false,
      modalContent: '',
      orderNum: 0,
      columns: [
        {
          title: this.$t('message.streamis.jobListTableColumns.jobName'),
          key: 'name',
          renderHeader: renderSpecialHeader,
          slot: 'jobName'
        },
        {
          title: this.$t('message.streamis.jobListTableColumns.taskStatus'),
          key: 'status',
          renderHeader: renderSpecialHeader,
          render: (h, params) => {
            const hitStatus = allJobStatuses.find(
              item => item.code === params.row.status
            )
            if (hitStatus) {
              return h('div', [
                h(
                  'strong',
                  {
                    style: {
                      color: hitStatus.color
                    }
                  },
                  this.$t('message.streamis.jobStatus.' + hitStatus.name)
                )
              ])
            }
          }
        },
        {
          title: this.$t('message.streamis.jobListTableColumns.jobType'),
          key: 'jobType',
          renderHeader: renderSpecialHeader
        },

        {
          title: this.$t('message.streamis.jobListTableColumns.label'),
          key: 'label',
          renderHeader: renderSpecialHeader,
          render: (h, params) => {
            return h(
              'span',
              {
                style: {
                  background:
                    params.index === 0 ? 'fff' : 'rgba(204, 204, 255, 1)',
                  display: 'inline-block',
                  padding: '2px'
                }
              },
              params.row.label
            )
          }
        },
        {
          title: this.$t('message.streamis.jobListTableColumns.version'),
          key: 'version',
          slot: 'version'
        },
        {
          title: this.$t('message.streamis.jobListTableColumns.lastRelease'),
          key: 'createBy',
          renderHeader: renderSpecialHeader
        },
        {
          title: this.$t(
            'message.streamis.jobListTableColumns.lastReleaseTime'
          ),
          key: 'lastVersionTime',
          renderHeader: renderSpecialHeader
        },
        {
          title: this.$t('message.streamis.jobListTableColumns.description'),
          key: 'description'
        },
        {
          title: this.$t('message.streamis.jobListTableColumns.operation'),
          key: 'operation',
          slot: 'operation'
        }
      ],
      jobMoudleRouter: [
        'paramsConfiguration',
        'alertConfiguration',
        'runningHistory',
        'runningLogs',
        'savepoint'
      ],
      pageData: {
        total: 0,
        current: 1,
        pageSize: 10
      },
      loading: false,
      buttonLoading: false,
      poptipVisible: false,
      choosedRowId: '',
      modalVisible: false,
      versionDatas: [],
      uploadVisible: false,
      projectName: this.$route.query.projectName
    }
  },
  mounted() {
    this.getJobList()
  },
  methods: {
    getJobList() {
      if (this.loading) {
        return
      }
      this.loading = true
      const { current, pageSize } = this.pageData
      const params = {
        pageNow: current,
        pageSize,
        projectName: this.projectName
      }
      const { jobName, jobStatus } = this.query
      if (jobName) {
        params.jobName = jobName
      }
      if (jobStatus !== 'all') {
        const hitStatus = allJobStatuses.find(item => item.name === jobStatus)
        params.jobStatus = hitStatus.code
      }

      const queries = Object.entries(params)
        .filter(item => !!item[1] || item[1] === 0)
        .map(item => item.join('='))
        .join('&')
      api
        .fetch('streamis/streamJobManager/job/list?' + queries, 'get')
        .then(res => {
          console.log(res)
          if (res) {
            const datas = res.tasks || []
            datas.forEach(item => {
              if (item.lastVersionTime) {
                const newDate = moment(new Date(item.lastVersionTime)).format(
                  'YYYY-MM-DD HH:mm:ss'
                )
                item.lastVersionTime = newDate
              }
            })
            datas.unshift({})
            this.tableDatas = datas.map(r => ({...r, poptipVisible: false}))
            // console.log(JSON.stringify(datas))
            this.pageData.total = parseInt(res.totalPage)
            this.loading = false
          }
        })
        .catch(e => {
          console.log(e)
          this.loading = false
        })
    },
    handleNameQuery() {
      console.log(this.query.jobName)
    },
    handleQuery() {
      this.pageData.current = 1
      this.getJobList()
    },
    handleUpload() {
      this.uploadVisible = true
    },
    handleStop(data, type, index) {
      console.log(data, type)
      const { id } = data
      const path = 'streamis/streamJobManager/job/stop'
      data.buttonLoading = true
      this.choosedRowId = id
      data.poptipVisible = false
      this.$set(this.tableDatas, index, data)
      api
        .fetch(path, { jobId: id, snapshot: !!type }, 'get')
        .then(res => {
          console.log(res)
          data.buttonLoading = false
          this.choosedRowId = ''
          if (res) {
            this.$emit('refreshCoreIndex')
            this.loading = false
            this.getJobList()
          }
        })
        .catch(e => {
          console.log(e.message)
          this.loading = false
          data.buttonLoading = false
          this.choosedRowId = ''
          this.getJobList()
        })
    },
    handleAction(data, index) {
      console.log(data)
      const { id } = data
      const path = 'streamis/streamJobManager/job/execute'
      const second = { jobId: id }
      data.buttonLoading = true
      this.choosedRowId = id
      this.$set(this.tableDatas, index, data)
      api
        .fetch(path, second)
        .then(res => {
          console.log(res)
          data.buttonLoading = false
          this.choosedRowId = ''
          if (res) {
            this.$emit('refreshCoreIndex')
            this.loading = false
            this.getJobList()
          }
        })
        .catch(e => {
          console.log(e.message)
          this.loading = false
          data.buttonLoading = false
          this.choosedRowId = ''
          this.getJobList()
        })
    },
    handleConfig(data) {
      console.log(data)
    },
    stopSavepoint(data) {
      console.log(data)
      this.loading = true
      api
        .fetch(
          'streamis/streamJobManager/job/snapshot/' + data.id,
          {},
          'put'
        )
        .then(res => {
          console.log(res)
          if (res) {
            this.loading = false
            this.snapModalVisable = true;
            this.snapTitle = this.$t('message.streamis.jobListTableColumns.snapTitle');
            this.snapshotPath = res.path;
            this.getJobList();
          }
        })
        .catch(e => {
          console.log(e)
          this.loading = false
        })
    },
    handleRouter(rowData, moduleName) {
      if(moduleName === 'savepoint'){
        this.stopSavepoint(rowData);
        return;
      }
      console.log(rowData)
      console.log(moduleName)
      const moduleMap = {
        paramsConfiguration: 'jobConfig',
        alertConfiguration: 'jobConfig',
        runningHistory: 'jobHistory',
        runningLogs: 'jobHistory'
      }
      this.$router.push({
        name: 'JobDetail',
        params: {
          id: rowData.id,
          module: moduleName
            ? moduleMap[moduleName] || moduleName
            : 'jobSummary',
          name: rowData.name,
          version: rowData.version,
          status: rowData.status,
          jobType: rowData.jobType,
          projectName: rowData.projectName || this.projectName
        }
      })
    },
    handlePageChange(page) {
      console.log(page)
      this.pageData.current = page
      this.getJobList()
    },
    handlePageSizeChange(pageSize) {
      console.log(pageSize)
      this.pageData.pageSize = pageSize
      this.pageData.current = 1
      this.getJobList()
    },
    versionDetail(data) {
      console.log(data)
      this.loading = true
      api
        .fetch(
          'streamis/streamJobManager/job/version?jobId=' +
            data.id +
            '&version=' +
            data.version,
          'get'
        )
        .then(res => {
          console.log(res)
          if (res) {
            this.loading = false
            this.modalVisible = true
            this.versionDatas = [res.detail]
          }
        })
        .catch(e => {
          console.log(e)
          this.loading = false
        })
    },
    modalCancel() {
      this.modalVisible = false
    },
    jarModalCancel() {
      this.uploadVisible = false
    },
    jarUploadSuccess(res) {
      if (res && res.status !== 0 && res.message) {
        this.$Message.error(res.message)
      }
      this.getJobList()
    },
    jarUploadError(err, res) {
      if (res && res.status !== 0 && res.message) {
        this.$Message.error(res.message)
      }
    },
    showButtons(val) {
      console.log('showButtons', val)
      this.columns.unshift({
        type: 'selection',
        width: 60,
        align: 'center'
      });
      this.isBatching = true;
    },
    hideButtons(val) {
      console.log('hideButtons', val)
      this.$refs.list.selectAll(false);
      this.selections = [];
      this.columns = this.columns.filter(col => col.type !== 'selection');
      this.isBatching = false;
    },
    selectionChange(val) {
      const selections = (val || []).filter(item => item.id);
      console.log('selectionChange', selections);
      this.selections = selections;
    },
    async doRestart(snapshot) {
      console.log('doRestart', snapshot);
      this.processModalVisable = true;
      this.modalTitle = this.$t('message.streamis.jobListTableColumns.stopTaskTitle');
      this.modalContent = this.$t('message.streamis.jobListTableColumns.stopTaskContent');
      this.restartTasks(snapshot);
    },
    async restartTasks(snapshot) {
      console.log('restartTasks');
      try {
        // this.modalLoading = true;
        const bulk_sbj = this.selections.map(item => +item.id);
        const res = await api.fetch('streamis/streamJobManager/job/bulk/pause', { bulk_sbj, snapshot });
        console.log('pause result', res);
        if (!res.result || !res.result.Success || !res.result.Failed) throw new Error('后台返回异常');
        // this.modalLoading = false;
        if (snapshot) {
          this.snapPaths = res.result.Success.data.map(item => ({
            taskId: item.jobId,
            taskName: item.scheduleId,
            info: item.snapshotPath,
          }));
        }
        this.failTasks = res.result.Failed.data.map(item => ({
          taskId: item.jobId,
          taskName: item.scheduleId,
          info: item.message,
        }));
        this.orderNum = this.selections.length - this.failTasks.length;
        if (this.failTasks.length) {
          this.isFinish = true;
          this.modalTitle = this.$t('message.streamis.jobListTableColumns.endTaskTitle');
          this.modalContent = this.$t('message.streamis.jobListTableColumns.endTaskTitle');
          return;
        }
        const result = await api.fetch('streamis/streamJobManager/job/bulk/execution', { bulk_sbj });
        console.log('start result', result);
        this.queryProcess(bulk_sbj);
      } catch (error) {
        console.warn(error);
        // this.modalLoading = false
      }
    },
    async queryProcess(id_list) {
      console.log('queryProcess');
      this.modalTitle = this.$t('message.streamis.jobListTableColumns.startTaskTitle');
      this.modalContent = this.$t('message.streamis.jobListTableColumns.startTaskContent');
      this.orderNum = 0;
      this.failTasks = [];
      try {
        // this.modalLoading = true;
        const res = await api.fetch('streamis/streamJobManager/job/status', { id_list }, 'put');
        if (!res.result || !Array.isArray(res.result)) throw new Error('后台返回异常');
        res.result.forEach((item) => {
          if ([1, 5, 6, 7].includes(item.statusCode)) this.orderNum++;
        })
        this.failTasks = res.result.filter(item => [6, 7].includes(item.statusCode)).map(item => ({
          taskId: item.jobId,
          taskName: item.status,
          info: item.message,
        }));
        if (this.orderNum < this.selections.length) {
          this.timer = setTimeout(() => {
            this.queryProcess(id_list);
          }, 2500);
        } else {
          // this.modalLoading = false;
          this.timer = null;
          this.isFinish = true;
          this.modalTitle = this.$t('message.streamis.jobListTableColumns.endTaskTitle');
          this.modalContent = this.$t('message.streamis.jobListTableColumns.endTaskTitle');
        }
      } catch (error) {
        console.warn(error)
        // this.modalLoading = false
      }
    },
    onClose(status) {
      if (status) return;
      this.processModalVisable = false;
      this.isFinish = false;
      clearTimeout(this.timer);
      this.failTasks = [];
      this.orderNum = 0;
      this.hideButtons();
      this.getJobList()
    }
  }
}
</script>
<style lang="scss" scoped>
.select {
  width: 200px;
}
.jobName {
  display: flex;
  cursor: pointer;
}
.more {
  font-size: 20px;
}
.upload {
  font-size: 24px;
}
.page {
  margin-top: 20px;
}
.versionWrap {
  display: flex;
  justify-content: flex-start;
  align-items: center;
}
.version {
  background-color: #008000;
  text-align: center;
  color: #ffffff;
  font-size: 16px;
  cursor: pointer;
  padding: 0px 3px;
}
.btn-wrap {
  display: flex;
  flex-direction: column;
  .btn {
    display: block;
    margin-bottom: 5px;
  }
}
.wrap {
  .general {
    margin-bottom: 32px;
    .text {
      font-weight: bold;
    }
  }
}
.table {
  /deep/ .ivu-table-tbody {
    tr:first-child {
      td:first-child {
        div:first-child {
          visibility: hidden;
        }
      }
    }
  }
}
</style>
