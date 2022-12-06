<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.jobId" placeholder="全部" style="width: 200px" />
      <el-select v-model="listQuery.jobGroup" placeholder="执行器">
        <el-option v-for="item in executorList" :key="item.id" :label="item.title" :value="item.id" />
      </el-select>
      <el-select v-model="listQuery.logStatus" placeholder="类型" style="width: 200px">
        <el-option v-for="item in logStatusList" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button v-waves class="filter-item" type="primary" icon="el-icon-search" @click="fetchData">
        搜索
      </el-button>
      <el-button
        class="filter-item"
        style="margin-left: 10px;"
        type="primary"
        icon="el-icon-edit"
        @click="handlerDelete"
      >
        清除
      </el-button>
    </div>
    <el-table
      v-loading="listLoading"
      :data="list"
      element-loading-text="Loading"
      border
      fit
      highlight-current-row
    >
      <el-table-column align="center" label="任务ID" width="80">
        <template slot-scope="scope">{{ scope.row.jobId }}</template>
      </el-table-column>
      <el-table-column align="center" label="任务描述">
        <template slot-scope="scope">{{ scope.row.jobDesc }}</template>
      </el-table-column>
      <el-table-column label="调度时间" align="center">
        <template slot-scope="scope">{{ scope.row.triggerTime }}</template>
      </el-table-column>
      <el-table-column label="执行结果" align="center">
        <template slot-scope="scope"> <span :style="`color:${scope.row.handleCode==500?'red':''}`">{{ statusList.find(t => t.value === scope.row.handleCode).label }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="300">
        <template slot-scope="{row}">
          <el-button type="primary" @click="handleViewJobLog(row)">日志查看</el-button>
          <el-button type="primary" @click="killRunningJob(row)">
            终止任务
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="listQuery.current"
      :limit.sync="listQuery.size"
      @pagination="fetchData"
    />

    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible" width="600px">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="center" label-width="100px">
        <el-row>
          <el-col :span="14" :offset="5">
            <el-form-item label="执行器">
              <el-input size="medium" value="全部" :disabled="true" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="14" :offset="5">
            <el-form-item label="任务">
              <el-input size="medium" value="全部" :disabled="true" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="14" :offset="5">
            <el-form-item label="执行器">
              <el-select v-model="temp.deleteType" placeholder="请选择执行器" style="width: 230px">
                <el-option v-for="item in deleteTypeList" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="deleteLog">
          确定
        </el-button>
      </div>
    </el-dialog>
    <el-dialog title="日志查看" :visible.sync="dialogVisible" width="95%">
      <div class="log-container">
        <pre :loading="logLoading" v-text="logContent" />
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">
          关闭
        </el-button>
        <el-button type="primary" @click="loadLog">
          刷新日志
        </el-button>
      </div>
    </el-dialog>

  </div>

</template>

<script>
import * as log from '@/api/dts/datax-job-log'
import * as job from '@/api/dts/datax-job-info'
import waves from '@/directive/waves' // waves directive
import Pagination from '@/components/Pagination' // secondary package based on el-pagination

export default {
  name: 'JobLog',
  components: { Pagination },
  directives: { waves },
  filters: {
    statusFilter(status) {
      const statusMap = {
        published: 'success',
        draft: 'gray',
        deleted: 'danger'
      }
      return statusMap[status]
    }
  },
  data() {
    return {
      dialogVisible: false,
      list: null,
      listLoading: true,
      total: 0,
      listQuery: {
        current: 1,
        size: 10,
        jobGroup: 0,
        jobId: '',
        logStatus: -1,
        filterTime: ''
      },
      dialogPluginVisible: false,
      pluginData: [],
      dialogFormVisible: false,
      dialogStatus: '',
      executorList: '',
      textMap: {
        create: 'Clear'
      },
      rules: {},
      temp: {
        deleteType: 1,
        jobGroup: 0,
        jobId: 0
      },
      statusList: [
        { value: 500, label: '失败' },
        { value: 502, label: '失败(超时)' },
        { value: 200, label: '成功' },
        { value: 0, label: '无' }
      ],
      deleteTypeList: [
        { value: 1, label: '清理一个月之前日志数据' },
        { value: 2, label: '清理三个月之前日志数据' },
        { value: 3, label: '清理六个月之前日志数据' },
        { value: 4, label: '清理一年之前日志数据' },
        { value: 5, label: '清理一千条以前日志数据' },
        { value: 6, label: '清理一万条以前日志数据' },
        { value: 7, label: '清理三万条以前日志数据' },
        { value: 8, label: '清理十万条以前日志数据' },
        { value: 9, label: '清理所有日志数据' }
      ],
      logStatusList: [
        { value: -1, label: '全部' },
        { value: 1, label: '成功' },
        { value: 2, label: '失败' },
        { value: 3, label: '进行中' }
      ],
      // 日志查询参数
      jobLogQuery: {
        executorAddress: '',
        triggerTime: '',
        id: '',
        fromLineNum: 1
      },
      // 日志内容
      logContent: '',
      // 显示日志
      logShow: false,
      // 日志显示加载中效果
      logLoading: false
    }
  },
  created() {
    this.fetchData()
    this.getExecutor()
  },

  methods: {
    fetchData() {
      this.listLoading = true
      const param = Object.assign({}, this.listQuery)
      const urlJobId = this.$route.query.jobId
      if (urlJobId > 0 && !param.jobId) {
        param.jobId = urlJobId
      } else if (!urlJobId && !param.jobId) {
        param.jobId = 0
      }
      log.getList(param).then(response => {
        const { content } = response
        this.total = content.recordsTotal
        this.list = content.data
        this.listLoading = false
      })
    },
    getExecutor() {
      job.getExecutorList().then(response => {
        const { content } = response
        this.executorList = content
        const defaultParam = { id: 0, title: '全部' }
        this.executorList.unshift(defaultParam)
        this.listQuery.jobGroup = this.executorList[0].id
      })
    },
    handlerDelete() {
      this.dialogStatus = 'create'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    deleteLog() {
      log.clearLog(this.temp.jobGroup, this.temp.jobId, this.temp.deleteType).then(response => {
        this.fetchData()
        this.dialogFormVisible = false
        this.$notify({
          title: 'Success',
          message: 'Delete Successfully',
          type: 'success',
          duration: 2000
        })
      })
      // const index = this.list.indexOf(row)
    },
    // 查看日志
    handleViewJobLog(row) {
      // const str = location.href.split('#')[0]
      // window.open(`${str}#/ router的name `)
      this.dialogVisible = true

      this.jobLogQuery.executorAddress = row.executorAddress
      this.jobLogQuery.id = row.id
      this.jobLogQuery.triggerTime = Date.parse(row.triggerTime)
      if (this.logShow === false) {
        this.logShow = true
      }
      // window.open(`#/data/log?executorAddress=${this.jobLogQuery.executorAddress}&triggerTime=${this.jobLogQuery.triggerTime}&id=${this.jobLogQuery.id}&fromLineNum=${this.jobLogQuery.fromLineNum}`)
      this.loadLog()
    },
    // 获取日志
    loadLog() {
      this.logLoading = true
      log.viewJobLog(this.jobLogQuery.executorAddress, this.jobLogQuery.triggerTime, this.jobLogQuery.id,
        this.jobLogQuery.fromLineNum).then(response => {
        // 判断是否是 '\n'，如果是表示显示完成，不重新加载
        if (response.content.logContent === '\n') {
          // this.jobLogQuery.fromLineNum = response.toLineNum - 20;
          // 重新加载
          // setTimeout(() => {
          //   this.loadLog()
          // }, 2000);
        } else {
          this.logContent = response.content.logContent
        }
        this.logLoading = false
      })
    },
    killRunningJob(row) {
      log.killJob(row).then(response => {
        this.fetchData()
        this.dialogFormVisible = false
        this.$notify({
          title: 'Success',
          message: 'Kill Successfully',
          type: 'success',
          duration: 2000
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
  .log-container {
    margin-bottom: 20px;
    background: #f5f5f5;
    width: 100%;
    height: 400px;
    overflow: scroll;
    pre {
      display: block;
      padding: 10px;
      margin: 0 0 10.5px;
      word-break: break-all;
      word-wrap: break-word;
      color: #334851;
      background-color: #f5f5f5;
      // border: 1px solid #ccd1d3;
      border-radius: 1px;
    }
  }
</style>
