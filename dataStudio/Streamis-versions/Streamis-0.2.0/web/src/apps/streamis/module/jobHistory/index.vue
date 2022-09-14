<template>
  <div class="tableWrap">
    <Table :columns="columns" :data="tableDatas" border>
      <template slot-scope="{ row }" slot="operation">
        <div style="margin-left: 5px" @click="showVersionInfo(row)">
          <a href="javascript:void(0)">
            {{ $t('message.streamis.jobHistoryColumns.showVersionInfo') }}
          </a>
        </div>
        <div style="margin-left: 5px" @click="showDetail(row)" v-show="false">
          <a href="javascript:void(0)">
            {{ $t('message.streamis.jobHistoryColumns.detail') }}
          </a>
        </div>
        <div style="margin-left: 5px" @click="showLogs(row)">
          <a href="javascript:void(0)">
            {{ $t('message.streamis.jobHistoryColumns.logs') }}
          </a>
        </div>
      </template>
    </Table>
    <versionDetail
      :visible="modalVisible"
      :datas="versionDatas"
      :fromHistory="fromHistory"
      @modalCancel="modalCancel"
    />
    <logDetail
      :visible="logVisible"
      :taskId="taskId"
      @modalCancel="modalCancel"
      ref="logDetail"
    />
  </div>
</template>
<script>
import api from '@/common/service/api'
import versionDetail from '@/apps/streamis/module/versionDetail'
import logDetail from '@/apps/streamis/module/logDetail'
export default {
  components: { versionDetail, logDetail },
  data() {
    return {
      columns: [
        {
          title: this.$t('message.streamis.jobHistoryColumns.taskId'),
          key: 'taskId'
        },
        {
          title: this.$t('message.streamis.jobHistoryColumns.jobName'),
          key: 'jobName'
        },
        {
          title: this.$t('message.streamis.jobHistoryColumns.creator'),
          key: 'creator'
        },
        {
          title: this.$t('message.streamis.jobHistoryColumns.version'),
          key: 'version'
        },
        {
          title: this.$t('message.streamis.jobHistoryColumns.status'),
          key: 'status'
        },
        {
          title: this.$t('message.streamis.jobHistoryColumns.startTime'),
          key: 'startTime'
        },
        {
          title: this.$t('message.streamis.jobHistoryColumns.endTime'),
          key: 'endTime'
        },
        {
          title: this.$t('message.streamis.jobHistoryColumns.runTime'),
          key: 'runTime'
        },
        {
          title: this.$t('message.streamis.jobHistoryColumns.stopCause'),
          key: 'stopCause'
        },
        {
          title: this.$t('message.streamis.jobHistoryColumns.operation'),
          key: 'operation',
          slot: 'operation'
        }
      ],
      tableDatas: [
        // {
        //   id: 10,
        //   name: 'flinkJarTestc',
        //   workspaceName: null,
        //   projectName: 'flinkJarTest3',
        //   jobType: 'flink.jar',
        //   label: 'e,t,y,h,g',
        //   createBy: 'hdfs',
        //   createTime: 1636338541000,
        //   status: "running",
        //   version: 'v00002',
        //   lastVersionTime: 1636339360000,
        //   description: '这是一个FlinkJar测试Job3'
        // }
      ],
      modalVisible: false,
      logVisible: false,
      taskId: 0,
      versionDatas: [],
      jobId: this.$route.params.id,
      fromHistory: true
    }
  },
  mounted() {
    this.getDatas()
    console.log(this.$route.params)
  },
  methods: {
    getDatas() {
      const { id, version } = this.$route.params || {}
      const queries = `?jobId=${id}&version=${version}`
      api
        .fetch('streamis/streamJobManager/job/execute/history' + queries, 'get')
        .then(res => {
          if (res && res.details) {
            this.tableDatas = res.details
          }
        })
        .catch(e => console.log(e))
    },
    showVersionInfo(data) {
      console.log(data)
      this.loading = true
      api
        .fetch(
          'streamis/streamJobManager/job/version?jobId=' +
            this.jobId +
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
    showDetail(row) {
      console.log(row)
    },
    showLogs(row) {
      console.log(row)
      this.$refs['logDetail'].getDatas(row.taskId)
      this.logVisible = true;
      this.taskId = +row.taskId;
    },
    modalCancel() {
      this.modalVisible = false
      this.logVisible = false
    }
  }
}
</script>
<style lang="scss" scoped></style>
