<template>
  <div class="app-container">
    <div class="build-container">
      <el-steps :active="active" finish-status="success">
        <el-step title="步骤 1" description="构建reader">1</el-step>
        <el-step title="步骤 2" description="构建writer">2</el-step>
        <el-step title="步骤 3" description="表映射">3</el-step>
        <el-step title="步骤 4" description="批量创建">4</el-step>
      </el-steps>

      <div v-show="active===1" class="step1">
        <Reader ref="reader" />
      </div>
      <div v-show="active===2" class="step2">
        <Writer ref="writer" />
      </div>
      <div v-show="active===3" class="step3">
        <Mapper ref="mapper" />
      </div>
      <div v-show="active===4" class="step4">
        <el-button type="primary" @click="handleJobTemplateSelectDrawer">{{ jobTemplate ? jobTemplate : "1.选择模板" }}</el-button>
        <el-button type="primary" @click="createJob">2.批量创建任务</el-button>
        (步骤：选择模板->批量创建任务)
        <el-drawer
          ref="jobTemplateSelectDrawer"
          title="选择模板"
          :visible.sync="jobTemplateSelectDrawer"
          direction="rtl"
          size="50%"
        >
          <el-table
            v-loading="listLoading"
            :data="list"
            element-loading-text="Loading"
            border
            fit
            highlight-current-row
            destroy-on-close="true"
            @current-change="handleCurrentChange"
          >
            <el-table-column align="center" label="任务ID" width="80">
              <template slot-scope="scope">{{ scope.row.id }}</template>
            </el-table-column>
            <el-table-column label="任务描述" align="center">
              <template slot-scope="scope">{{ scope.row.jobDesc }}</template>
            </el-table-column>
            <el-table-column label="所属项目" align="center">
              <template slot-scope="scope">{{ scope.row.jobProject }}</template>
            </el-table-column>
            <el-table-column label="Cron" align="center">
              <template slot-scope="scope"><span>{{ scope.row.jobCron }}</span></template>
            </el-table-column>
            <el-table-column label="路由策略" align="center">
              <template slot-scope="scope"> {{ routeStrategies.find(t => t.value === scope.row.executorRouteStrategy).label }}</template>
            </el-table-column>
          </el-table>
          <pagination v-show="total>0" :total="total" :page.sync="listQuery.current" :limit.sync="listQuery.size" @pagination="fetchData" />
        </el-drawer>
        <div style="margin-bottom: 20px;" />
      </div>

      <el-button :disabled="active===1" style="margin-top: 12px;" @click="last">上一步</el-button>
      <el-button v-show="active!==4" type="primary" style="margin-top: 12px;margin-bottom: 12px;" @click="next">下一步</el-button>
    </div>
  </div>
</template>

<script>
import * as jobTemplate from '@/api/dtsdatax-job-template'
import * as job from '@/api/dts/datax-job-info'
import Pagination from '@/components/Pagination'
import Reader from './reader'
import Writer from './writer'
import clip from '@/utils/clipboard'
import Mapper from './mapper'

export default {
  name: 'JsonBuild',
  components: { Reader, Writer, Pagination, Mapper },
  data() {
    return {
      configJson: '',
      active: 1,
      jobTemplate: '',
      jobTemplateSelectDrawer: false,
      list: null,
      currentRow: null,
      listLoading: true,
      total: 0,
      listQuery: {
        current: 1,
        size: 10,
        jobGroup: 0,
        triggerStatus: -1,
        jobDesc: '',
        executorHandler: '',
        userId: 0
      },
      blockStrategies: [
        { value: 'SERIAL_EXECUTION', label: '单机串行' },
        { value: 'DISCARD_LATER', label: '丢弃后续调度' },
        { value: 'COVER_EARLY', label: '覆盖之前调度' }
      ],
      routeStrategies: [
        { value: 'FIRST', label: '第一个' },
        { value: 'LAST', label: '最后一个' },
        { value: 'ROUND', label: '轮询' },
        { value: 'RANDOM', label: '随机' },
        { value: 'CONSISTENT_HASH', label: '一致性HASH' },
        { value: 'LEAST_FREQUENTLY_USED', label: '最不经常使用' },
        { value: 'LEAST_RECENTLY_USED', label: '最近最久未使用' },
        { value: 'FAILOVER', label: '故障转移' },
        { value: 'BUSYOVER', label: '忙碌转移' }
        // { value: 'SHARDING_BROADCAST', label: '分片广播' }
      ],
      triggerNextTimes: '',
      registerNode: [],
      jobJson: '',
      temp: {
        id: undefined,
        jobGroup: '',
        jobCron: '',
        jobDesc: '',
        executorRouteStrategy: '',
        executorBlockStrategy: '',
        childJobId: '',
        executorFailRetryCount: '',
        alarmEmail: '',
        executorTimeout: '',
        userId: 0,
        jobConfigId: '',
        executorHandler: 'executorJobHandler',
        glueType: 'BEAN',
        jobJson: '',
        executorParam: '',
        replaceParam: '',
        jvmParam: '',
        incStartTime: '',
        templateId: 0
      }
    }
  },
  created() {
    // this.getJdbcDs()
  },
  methods: {
    next() {
      const fromTableList = this.$refs.reader.getData().tables
      const toTableList = this.$refs.writer.getData().tables
      // const fromTableName = this.$refs.reader.getData().tableName
      // 第一步 reader 判断是否已选字段
      if (this.active === 1) {
        // 实现第一步骤读取的表和字段直接带到第二步骤
        // this.$refs.writer.sendTableNameAndColumns(fromTableName, fromColumnList)
        // 取子组件的数据
        // console.info(this.$refs.reader.getData())
        this.active++
      } else {
        // 将第一步和第二步得到的字段名字发送到第三步
        if (this.active === 2) {
          this.$refs.mapper.sendTables(fromTableList, toTableList)
        }
        if (this.active !== 4) {
          this.active++
        }
      }
    },
    last() {
      if (this.active > 1) {
        this.active--
      }
    },
    // Create job
    createJob() {
      const readerData = this.$refs.reader.getData()
      const writeData = this.$refs.writer.getData()
      const readerTables = this.$refs.mapper.getLTables()
      const writerTables = this.$refs.mapper.getRTables()
      const rdbmsReader = {
        readerSplitPk: readerData.splitPk
      }
      const rdbmsWriter = {}
      const obj = {
        readerDatasourceId: readerData.datasourceId,
        readerTables: readerTables,
        writerDatasourceId: writeData.datasourceId,
        writerTables: writerTables,
        rdbmsReader: rdbmsReader,
        rdbmsWriter: rdbmsWriter,
        templateId: this.temp.templateId
      }
      // 调api
      job.batchAddJob(obj).then(response => {
        this.$notify({
          title: 'Success',
          message: 'Created Successfully',
          type: 'success',
          duration: 2000
        })
        // 切回第一步
        this.active = 1
      })
    },
    handleCopy(text, event) {
      clip(this.configJson, event)
      this.$message({
        message: 'copy success',
        type: 'success'
      })
    },
    handleJobTemplateSelectDrawer() {
      this.jobTemplateSelectDrawer = !this.jobTemplateSelectDrawer
      if (this.jobTemplateSelectDrawer) {
        this.fetchData()
        this.getExecutor()
      }
    },
    getReaderData() {
      return this.$refs.reader.getData()
    },
    getExecutor() {
      jobTemplate.getExecutorList().then(response => {
        const { content } = response
        this.executorList = content
      })
    },
    fetchData() {
      this.listLoading = true
      jobTemplate.getList(this.listQuery).then(response => {
        const { content } = response
        this.total = content.recordsTotal
        this.list = content.data
        this.listLoading = false
      })
    },
    handleCurrentChange(val) {
      this.temp = Object.assign({}, val)
      this.temp.id = undefined
      this.temp.jobDesc = this.getReaderData().tableName
      this.$refs.jobTemplateSelectDrawer.closeDrawer()
      this.jobTemplate = val.id + '(' + val.jobDesc + ')'
      this.temp.templateId = val.id
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
