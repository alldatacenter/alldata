<template>
  <div class="tableWrap">
    <Table :columns="columns" :data="tableDatas" border>
      <template slot-scope="{ row }" slot="operation">
        <div style="margin-left: 5px" @click="showVersionInfo(row)">
          <a href="javascript:void(0)">
            {{ $t('message.streamis.jobHistoryColumns.showVersionInfo') }}
          </a>
        </div>
        <div
          style="margin-left: 5px"
          @click="showDetail(row)"
          v-show="row.status === 'running'"
        >
          <a href="javascript:void(0)">
            {{ $t('message.streamis.jobHistoryColumns.detail') }}
          </a>
        </div>
        <div
          style="margin-left: 5px"
          @click="showLogs(row)"
          v-show="row.status !== 'running'"
        >
          <a href="javascript:void(0)">
            {{ $t('message.streamis.jobHistoryColumns.logs') }}
          </a>
        </div>
      </template>
    </Table>
  </div>
</template>
<script>
import api from '@/common/service/api'
import moment from 'moment'
export default {
  data() {
    return {
      columns: [
        {
          title: 'id',
          key: 'id'
        },
        {
          title: this.$t('message.streamis.versionDetail.createTime'),
          key: 'createTime'
        },
        {
          title: this.$t('message.streamis.jobConfig.formItems.alertLevel'),
          key: 'alertLevel'
        },
        {
          title: this.$t('message.streamis.jobConfig.formItems.alertUser'),
          key: 'alertUser'
        },
        {
          title: this.$t('message.streamis.jobAlert.alertMsg'),
          key: 'alertMsg'
        },
        {
          title: this.$t('message.streamis.jobAlert.errorMsg'),
          key: 'errorMsg'
        }
      ],
      tableDatas: [
        // {
        //   id: 1,
        //   jobId: 55,
        //   alertLevel: 'critical',
        //   alertUser: 'johnnwang',
        //   alertMsg: '请求LinkisManager失败，Linkis集群出现异常，请关注！',
        //   jobVersionId: 96,
        //   taskId: 214,
        //   createTime: 1636646400000,
        //   status: 0,
        //   errorMsg: null
        // }
      ]
    }
  },
  mounted() {
    this.getDatas()
  },
  methods: {
    getDatas() {
      const { id, version } = this.$route.params || {}
      const queries = `?jobId=${id}&version=${version}`
      api
        .fetch('streamis/streamJobManager/job/alert' + queries, 'get')
        .then(res => {
          if (res && res.list) {
            res.list.forEach(item => {
              if (item.createTime) {
                const newDate = moment(new Date(item.createTime)).format(
                  'YYYY-MM-DD HH:mm:ss'
                )
                item.createTime = newDate
              }
            })
            this.tableDatas = res.list
          }
        })
        .catch(e => console.log(e))
    }
  }
}
</script>
<style lang="scss" scoped></style>
