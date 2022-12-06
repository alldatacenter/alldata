<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.jobDesc" placeholder="任务描述" style="width: 200px;" class="filter-item" />
      <el-select v-model="projectIds" multiple placeholder="所属项目" class="filter-item">
        <el-option v-for="item in jobProjectList" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-button v-waves class="filter-item" type="primary" icon="el-icon-search" @click="fetchData">
        搜索
      </el-button>
      <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="el-icon-edit" @click="handleCreate">
        添加
      </el-button>
      <!-- <el-checkbox v-model="showReviewer" class="filter-item" style="margin-left:15px;" @change="tableKey=tableKey+1">
        reviewer
      </el-checkbox> -->
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
        <template slot-scope="scope">{{ scope.row.id }}</template>
      </el-table-column>
      <el-table-column label="任务描述" align="center">
        <template slot-scope="scope">{{ scope.row.jobDesc }}</template>
      </el-table-column>
      <el-table-column label="所属项目" align="center" width="120">
        <template slot-scope="scope">{{ scope.row.projectName }}</template>
      </el-table-column>
      <el-table-column label="Cron" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.jobCron }}</span>
        </template>
      </el-table-column>
      <el-table-column label="路由策略" align="center">
        <template slot-scope="scope"> {{ routeStrategies.find(t => t.value === scope.row.executorRouteStrategy).label }}</template>
      </el-table-column>
      <el-table-column label="修改用户" align="center" width="80">
        <template slot-scope="scope">{{ scope.row.userName }}</template>
      </el-table-column>
      <el-table-column label="注册节点" align="center">
        <template slot-scope="scope">
          <el-popover
            placement="bottom"
            width="500"
            @show="loadById(scope.row)"
          >
            <el-table :data="registerNode">
              <el-table-column width="150" property="title" label="执行器名称" />
              <el-table-column width="150" property="appName" label="appName" />
              <el-table-column width="150" property="registryList" label="机器地址" />
            </el-table>
            <el-button slot="reference" size="small">查看</el-button>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column label="下次触发时间" align="center">
        <template slot-scope="scope">
          <el-popover
            placement="bottom"
            width="300"
            @show="nextTriggerTime(scope.row)"
          >
            <h5 v-html="triggerNextTimes" />
            <el-button slot="reference" size="small">查看</el-button>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center">
        <template slot-scope="{row}">
          <el-dropdown trigger="click">
            <span class="el-dropdown-link">
              操作<i class="el-icon-arrow-down el-icon--right" />
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item divided @click.native="handlerUpdate(row)">编辑</el-dropdown-item>
              <el-dropdown-item @click.native="handlerDelete(row)">删除</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="listQuery.current" :limit.sync="listQuery.size" @pagination="fetchData" />

    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible" width="1000px" :before-close="handleClose">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="执行器" prop="jobGroup">
              <el-select v-model="temp.jobGroup" placeholder="请选择执行器">
                <el-option v-for="item in executorList" :key="item.id" :label="item.title" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务描述" prop="jobDesc">
              <el-input v-model="temp.jobDesc" size="medium" placeholder="请输入任务描述" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="路由策略" prop="executorRouteStrategy">
              <el-select v-model="temp.executorRouteStrategy" placeholder="请选择路由策略">
                <el-option v-for="item in routeStrategies" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-dialog
              title="提示"
              :visible.sync="showCronBox"
              width="60%"
              append-to-body
            >
              <cron v-model="temp.jobCron" />
              <span slot="footer" class="dialog-footer">
                <el-button @click="showCronBox = false;">关闭</el-button>
                <el-button type="primary" @click="showCronBox = false">确 定</el-button>
              </span>
            </el-dialog>
            <el-form-item label="Cron" prop="jobCron">
              <el-input v-model="temp.jobCron" auto-complete="off" placeholder="请输入Cron表达式">
                <el-button v-if="!showCronBox" slot="append" icon="el-icon-turn-off" title="打开图形配置" @click="showCronBox = true" />
                <el-button v-else slot="append" icon="el-icon-open" title="关闭图形配置" @click="showCronBox = false" />
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="阻塞处理" prop="executorBlockStrategy">
              <el-select v-model="temp.executorBlockStrategy" placeholder="请选择阻塞处理策略">
                <el-option v-for="item in blockStrategies" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报警邮件">
              <el-input v-model="temp.alarmEmail" placeholder="请输入报警邮件，多个用逗号分隔" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务类型" prop="glueType">
              <el-select v-model="temp.glueType" placeholder="任务脚本类型">
                <el-option v-for="item in glueTypes" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="失败重试次数">
              <el-input-number v-model="temp.executorFailRetryCount" :min="0" :max="20" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属项目" prop="projectId">
              <el-select v-model="temp.projectId" placeholder="所属项目" class="filter-item">
                <el-option v-for="item in jobProjectList" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="超时时间(分钟)">
              <el-input-number v-model="temp.executorTimeout" :min="0" :max="120" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="子任务">
              <el-select v-model="temp.childJobId" multiple placeholder="子任务" value-key="id">
                <el-option v-for="item in jobIdList" :key="item.id" :label="item.jobDesc" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12" />
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="JVM启动参数">
              <el-input
                v-model="temp.jvmParam"
                placeholder="-Xms1024m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="dialogStatus==='create'?createData():updateData()">
          确认
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as executor from '@/api/dts/datax-executor'
import Cron from '@/components/Cron'
import * as jobTemp from '@/api/dts/datax-job-template'
import waves from '@/directive/waves' // waves directive
import Pagination from '@/components/Pagination' // secondary package based on el-pagination
import * as datasourceApi from '@/api/dts/datax-jdbcDatasource'
import * as jobProjectApi from '@/api/dts/datax-job-project'
import * as job from '@/api/dts/datax-job-info'

export default {
  name: 'JobTemplate',
  components: { Pagination, Cron },
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
    const validateIncParam = (rule, value, callback) => {
      if (!value) {
        callback(new Error('Increment parameters is required'))
      }
      callback()
    }
    const validatePartitionParam = (rule, value, callback) => {
      if (!this.partitionField) {
        callback(new Error('Partition parameters is required'))
      }
      callback()
    }
    return {
      projectIds: '',
      list: null,
      listLoading: true,
      total: 0,
      listQuery: {
        current: 1,
        size: 10,
        jobGroup: 0,
        triggerStatus: -1,
        jobDesc: '',
        executorHandler: '',
        userId: 0,
        projectIds: ''
      },
      showCronBox: false,
      dialogPluginVisible: false,
      pluginData: [],
      dialogFormVisible: false,
      dialogStatus: '',
      textMap: {
        update: 'Edit',
        create: 'Create'
      },
      rules: {
        jobGroup: [{ required: true, message: 'jobGroup is required', trigger: 'change' }],
        executorRouteStrategy: [{ required: true, message: 'executorRouteStrategy is required', trigger: 'change' }],
        executorBlockStrategy: [{ required: true, message: 'executorBlockStrategy is required', trigger: 'change' }],
        jobDesc: [{ required: true, message: 'jobDesc is required', trigger: 'blur' }],
        jobProject: [{ required: true, message: 'jobProject is required', trigger: 'blur' }],
        jobCron: [{ required: true, message: 'jobCron is required', trigger: 'blur' }],
        incStartId: [{ trigger: 'blur', validator: validateIncParam }],
        replaceParam: [{ trigger: 'blur', validator: validateIncParam }],
        primaryKey: [{ trigger: 'blur', validator: validateIncParam }],
        incStartTime: [{ trigger: 'change', validator: validateIncParam }],
        replaceParamType: [{ trigger: 'change', validator: validateIncParam }],
        partitionField: [{ trigger: 'blur', validator: validatePartitionParam }],
        datasourceId: [{ trigger: 'change', validator: validateIncParam }],
        readerTable: [{ trigger: 'blur', validator: validateIncParam }],
        projectId: [{ required: true, message: 'projectId is required', trigger: 'change' }]
      },
      temp: {
        id: undefined,
        jobGroup: '',
        jobCron: '',
        jobDesc: '',
        executorRouteStrategy: 'RANDOM',
        executorBlockStrategy: 'DISCARD_LATER',
        childJobId: '',
        executorFailRetryCount: '',
        alarmEmail: '',
        executorTimeout: '',
        userId: 0,
        jobConfigId: '',
        executorHandler: 'executorJobHandler',
        glueType: 'BEAN',
        executorParam: '',
        jvmParam: '',
        projectId: '',
        datasourceId: 0,
        readerTable: ''
      },
      resetTemp() {
        this.temp = this.$options.data().temp
      },
      executorList: '',
      jobIdList: '',
      jobProjectList: '',
      dataSourceList: '',
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
      glueTypes: [
        { value: 'BEAN', label: 'FlinkX任务' }
      ],
      triggerNextTimes: '',
      registerNode: []
    }
  },
  created() {
    this.fetchData()
    this.getExecutor()
    this.getJobIdList()
    this.getJobProject()
    this.getDataSourceList()
  },

  methods: {
    handleClose(done) {
      this.$confirm('确认关闭？')
        .then(_ => {
          done()
        })
        .catch(_ => {})
    },
    getExecutor() {
      jobTemp.getExecutorList().then(response => {
        const { content } = response
        this.executorList = content
      })
    },
    getJobIdList() {
      job.getJobIdList().then(response => {
        const { content } = response
        this.jobIdList = content
      })
    },
    getJobProject() {
      jobProjectApi.getJobProjectList().then(response => {
        this.jobProjectList = response
      })
    },
    getDataSourceList() {
      datasourceApi.getDataSourceList().then(response => {
        this.dataSourceList = response
      })
    },
    fetchData() {
      this.listLoading = true
      if (this.projectIds) {
        this.listQuery.projectIds = this.projectIds.toString()
      }
      jobTemp.getList(this.listQuery).then(response => {
        const { content } = response
        this.total = content.recordsTotal
        this.list = content.data
        this.listLoading = false
      })
    },
    handleCreate() {
      this.resetTemp()
      this.dialogStatus = 'create'
      this.dialogFormVisible = true
      this.temp.jobGroup = this.executorList[0]['id']
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    createData() {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          if (this.temp.childJobId) {
            const childJobs = []
            for (const i in this.temp.childJobId) {
              childJobs.push(this.temp.childJobId[i].id)
            }
            this.temp.childJobId = childJobs.toString()
          }
          if (this.partitionField) this.temp.partitionInfo = this.partitionField + ',' + this.timeOffset + ',' + this.timeFormatType
          jobTemp.createJob(this.temp).then(() => {
            this.fetchData()
            this.dialogFormVisible = false
            this.$notify({
              title: 'Success',
              message: 'Created Successfully',
              type: 'success',
              duration: 2000
            })
          })
        }
      })
    },
    handlerUpdate(row) {
      this.resetTemp()
      this.temp = Object.assign({}, row) // copy obj
      this.dialogStatus = 'update'
      this.dialogFormVisible = true
      const arrchildSet = []
      const arrJobIdList = []
      if (this.JobIdList) {
        for (const n in this.JobIdList) {
          if (this.JobIdList[n].id !== this.temp.id) {
            arrJobIdList.push(this.JobIdList[n])
          }
        }
        this.JobIdList = arrJobIdList
      }

      if (this.temp.childJobId) {
        const arrString = this.temp.childJobId.split(',')
        for (const i in arrString) {
          for (const n in this.jobIdList) {
            if (this.jobIdList[n].id === parseInt(arrString[i])) {
              arrchildSet.push(this.jobIdList[n])
            }
          }
        }
        this.temp.childJobId = arrchildSet
      }

      if (this.temp.partitionInfo) {
        const partition = this.temp.partitionInfo.split(',')
        this.partitionField = partition[0]
        this.timeOffset = partition[1]
        this.timeFormatType = partition[2]
      }
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    updateData() {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          if (this.temp.childJobId) {
            const childJobs = []
            for (const i in this.temp.childJobId) {
              childJobs.push(this.temp.childJobId[i].id)
            }
            this.temp.childJobId = childJobs.toString()
          }
          if (this.partitionField) this.temp.partitionInfo = this.partitionField + ',' + this.timeOffset + ',' + this.timeFormatType
          jobTemp.updateJob(this.temp).then(() => {
            this.fetchData()
            this.dialogFormVisible = false
            this.$notify({
              title: 'Success',
              message: 'Update Successfully',
              type: 'success',
              duration: 2000
            })
          })
        }
      })
    },
    handlerDelete(row) {
      this.$confirm('确定删除吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        jobTemp.removeJob(row.id).then(response => {
          this.fetchData()
          this.$notify({
            title: 'Success',
            message: 'Delete Successfully',
            type: 'success',
            duration: 2000
          })
        })
      })

      // const index = this.list.indexOf(row)
    },
    nextTriggerTime(row) {
      jobTemp.nextTriggerTime(row.jobCron).then(response => {
        const { content } = response
        this.triggerNextTimes = content.join('<br>')
      })
    },
    loadById(row) {
      executor.loadById(row.jobGroup).then(response => {
        this.registerNode = []
        const { content } = response
        this.registerNode.push(content)
      })
    }
  }
}
</script>

<style>
  .el-dropdown-link {
    cursor: pointer;
    color: #409EFF;
  }
  .el-dropdown + .el-dropdown {
    margin-left: 15px;
  }
</style>
