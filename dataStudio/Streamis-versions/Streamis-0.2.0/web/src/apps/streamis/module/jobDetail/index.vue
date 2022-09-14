<template>
  <div>
    <jarDetail
      v-if="detailName === 'jarDetail'"
      :isSql="isSql"
      :jarData="data"
      key="jar"
    />
    <workflow v-if="detailName === 'workflow'" key="workflow" />
  </div>
</template>
<script>
import api from '@/common/service/api'
import jarDetail from '@/apps/streamis/module/jarDetail'
import workflow from '@/apps/streamis/module/workflow'
import moment from 'moment'

export default {
  components: {
    jarDetail: jarDetail.component,
    workflow: workflow.component
  },
  data() {
    return {
      detailName: 'jarDetail',
      isSql: false,
      data: {},
    }
  },
  mounted() {
    console.log(this.$route.params)
    this.getDetail()
  },
  methods: {
    getDetail() {
      const { id, version } = this.$route.params
      const query = `jobId=${id}&version=${version}`
      api
        .fetch(`streamis/streamJobManager/job/jobContent?${query}`, 'get')
        .then(res => {
          console.log(res)
          if (res && res.jobContent) {
            this.detailName = 'jarDetail'
            this.data = res.jobContent
            const {
              sql,
              mainClassJar,
              mainClass,
              dependencyJars,
              resources
            } = res.jobContent
            if (mainClassJar) {
              if (mainClassJar.createTime) {
                const newDate = moment(
                  new Date(mainClassJar.createTime)
                ).format('YYYY-MM-DD HH:mm:ss')
                this.data.mainClassJar.createTime = newDate
              }
              this.data.mainClassJar.mainClass = mainClass
              this.data.mainClassJar = [this.data.mainClassJar]
            }
            if (resources) {
              this.data.resources = resources.map(item => {
                const newItem = { ...item }
                newItem.createTime = moment(
                  new Date(newItem.createTime)
                ).format('YYYY-MM-DD HH:mm:ss')
                return newItem
              })
            }
            if (dependencyJars) {
              this.data.dependencyJars = dependencyJars.map(item => {
                const newItem = { ...item }
                newItem.createTime = moment(
                  new Date(newItem.createTime)
                ).format('YYYY-MM-DD HH:mm:ss')
                return newItem
              })
            }
            this.isSql = !!sql
          }
        })
        .catch(e => console.log(e))
    },
    showVersionInfo(row) {
      console.log(row)
    },
    showDetail(row) {
      console.log(row)
    },
    showLogs(row) {
      console.log(row)
    }
  }
}
</script>
<style lang="scss" scoped>
.itemWrap {
  padding: 10px;
  & > p {
    font-weight: 700;
    font-size: 16px;
  }
  & > div {
    margin-left: 20px;
    margin-top: 10px;
  }
}
.programArguement {
  background: rgba(94, 94, 94, 1);
  color: #fff;
  padding: 10px 20px;
  min-height: 64px;
}
</style>
