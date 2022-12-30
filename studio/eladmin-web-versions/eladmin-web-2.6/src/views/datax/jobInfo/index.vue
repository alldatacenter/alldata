<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.jobDesc" placeholder="任务名称" style="width: 200px;" class="filter-item" />
      <el-select v-model="projectIds" multiple placeholder="所属项目" class="filter-item">
        <el-option v-for="item in jobProjectList" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="listQuery.glueType" placeholder="任务类型" style="width: 200px" class="filter-item">
        <el-option v-for="item in glueTypes" :key="item.value" :label="item.label" :value="item.value" />
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
      style="width: 100%"
      size="medium"
    >
      <el-table-column align="center" label="ID" width="80">
        <template slot-scope="scope">{{ scope.row.id }}</template>
      </el-table-column>
      <el-table-column label="任务名称" align="center">
        <template slot-scope="scope">{{ scope.row.jobDesc }}</template>
      </el-table-column>
      <el-table-column label="所属项目" align="center" width="120">
        <template slot-scope="scope">{{ scope.row.projectName }}</template>
      </el-table-column>
      <el-table-column label="Cron" align="center" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.jobCron }}</span>
        </template>
      </el-table-column>
      <el-table-column label="路由策略" align="center" width="130">
        <template slot-scope="scope"> {{ routeStrategies.find(t => t.value === scope.row.executorRouteStrategy).label }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="150">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.triggerStatus"
            active-color="#00A854"
            active-text="启动"
            :active-value="1"
            inactive-color="#F04134"
            inactive-text="停止"
            :inactive-value="0"
            @change="changeSwitch(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="注册节点" align="center" width="100">
        <template slot-scope="scope">
          <el-popover
            placement="bottom"
            width="500"
            @show="loadById(scope.row)"
          >
            <el-table :data="registerNode">
              <el-table-column width="150" property="title" label="执行器名称" />
              <el-table-column width="150" property="appName" label="分组标识" />
              <el-table-column width="150" property="registryList" label="机器地址" />
            </el-table>
            <el-button slot="reference" size="small">查看</el-button>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column label="下次触发时间" align="center" width="120">
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
      <el-table-column label="执行状态" align="center" width="80">
        <template slot-scope="scope"> {{ statusList.find(t => t.value === scope.row.lastHandleCode).label }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" fixed="right">
        <template slot-scope="{row}">
          <!-- <el-dropdown type="primary" size="small"> -->
          <!-- 操作 -->
          <el-dropdown trigger="click">
            <span class="el-dropdown-link">
              操作<i class="el-icon-arrow-down el-icon--right" />
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item @click.native="handlerExecute(row)">执行一次</el-dropdown-item>
              <el-dropdown-item @click.native="handlerViewLog(row)">查询日志</el-dropdown-item>
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
            <el-form-item label="任务名称" prop="jobDesc">
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
        <el-row v-if="temp.glueType==='BEAN'" :gutter="20">
          <el-col :span="12">
            <el-form-item label="辅助参数" prop="incrementType">
              <el-select v-model="temp.incrementType" placeholder="请选择参数类型" value="">
                <el-option v-for="item in incrementTypes" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="temp.glueType==='BEAN' && temp.incrementType === 1" :gutter="20">
          <el-col :span="12">
            <el-form-item label="增量主键开始ID" prop="incStartId">
              <el-input v-model="temp.incStartId" placeholder="首次增量使用" style="width: 56%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="ID增量参数" prop="replaceParam">
              <el-input v-model="temp.replaceParam" placeholder="-DstartId='%s' -DendId='%s'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="reader数据源" prop="datasourceId">
              <el-select v-model="temp.datasourceId" placeholder="reader数据源" class="filter-item">
                <el-option v-for="item in dataSourceList" :key="item.id" :label="item.datasourceName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="reader表" prop="readerTable">
              <el-input v-model="temp.readerTable" placeholder="读表的表名" />
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="主键" label-width="40px" prop="primaryKey">
              <el-input v-model="temp.primaryKey" placeholder="请填写主键字段名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="temp.glueType==='BEAN' && temp.incrementType === 2" :gutter="20">
          <el-col :span="12">
            <el-form-item label="增量开始时间" prop="incStartTime">
              <el-date-picker
                v-model="temp.incStartTime"
                type="datetime"
                placeholder="首次增量使用"
                format="yyyy-MM-dd HH:mm:ss"
                style="width: 57%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="增量时间字段" prop="replaceParam">
              <el-input v-model="temp.replaceParam" placeholder="-DlastTime='%s' -DcurrentTime='%s'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="增量时间格式" prop="replaceParamType">
              <el-select v-model="temp.replaceParamType" placeholder="增量时间格式" @change="incStartTimeFormat">
                <el-option v-for="item in replaceFormatTypes" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>

        </el-row>
        <el-row v-if="temp.glueType==='BEAN' && temp.incrementType === 3" :gutter="20">
          <el-col :span="12">
            <el-form-item label="分区字段" prop="partitionField">
              <el-input v-model="partitionField" placeholder="请输入分区字段" style="width: 56%" />
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="分区时间">
              <el-select v-model="timeFormatType" placeholder="分区时间格式">
                <el-option v-for="item in timeFormatTypes" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-input-number v-model="timeOffset" :min="-20" :max="0" style="width: 65%" />
          </el-col>
        </el-row>
        <el-row v-if="temp.glueType==='BEAN'" :gutter="20">
          <el-col :span="24">
            <el-form-item label="JVM启动参数">
              <el-input v-model="temp.jvmParam" placeholder="-Xms1024m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <json-editor v-if="temp.glueType==='BEAN'" ref="jsonEditor" v-model="jobJson" />
      <shell-editor v-if="temp.glueType==='GLUE_SHELL'" ref="shellEditor" v-model="glueSource" />
      <python-editor v-if="temp.glueType==='GLUE_PYTHON'" ref="pythonEditor" v-model="glueSource" />
      <powershell-editor v-if="temp.glueType==='GLUE_POWERSHELL'" ref="powershellEditor" v-model="glueSource" />
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="dialogStatus==='create'?createData():updateData()">
          确定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as executor from '@/api/dts/datax-executor'
import * as job from '@/api/dts/datax-job-info'
import waves from '@/directive/waves' // waves directive
import Cron from '@/components/Cron'
import Pagination from '@/components/Pagination' // secondary package based on el-pagination
import JsonEditor from '@/components/JsonEditor'
import ShellEditor from '@/components/ShellEditor'
import PythonEditor from '@/components/PythonEditor'
import PowershellEditor from '@/components/PowershellEditor'
import * as datasourceApi from '@/api/dts/datax-jdbcDatasource'
import * as jobProjectApi from '@/api/dts/datax-job-project'
import { isJSON } from '@/utils/validate'

export default {
  name: 'JobInfo',
  components: { Pagination, JsonEditor, ShellEditor, PythonEditor, PowershellEditor, Cron },
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
        projectIds: '',
        triggerStatus: -1,
        jobDesc: '',
        glueType: ''
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
        glueType: [{ required: true, message: 'jobType is required', trigger: 'change' }],
        projectId: [{ required: true, message: 'projectId is required', trigger: 'change' }],
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
        readerTable: [{ trigger: 'blur', validator: validateIncParam }]
      },
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
        executorHandler: '',
        glueType: '',
        glueSource: '',
        jobJson: '',
        executorParam: '',
        replaceParam: '',
        replaceParamType: 'Timestamp',
        jvmParam: '',
        incStartTime: '',
        partitionInfo: '',
        incrementType: 0,
        incStartId: '',
        primaryKey: '',
        projectId: '',
        datasourceId: '',
        readerTable: ''
      },
      resetTemp() {
        this.temp = this.$options.data().temp
        this.jobJson = ''
        this.glueSource = ''
        this.timeOffset = 0
        this.timeFormatType = 'yyyy-MM-dd'
        this.partitionField = ''
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
        { value: 'BEAN', label: 'FlinkX任务' },
        { value: 'GLUE_SHELL', label: 'Shell任务' },
        { value: 'GLUE_PYTHON', label: 'Python任务' },
        { value: 'GLUE_POWERSHELL', label: 'PowerShell任务' }
      ],
      incrementTypes: [
        { value: 0, label: '无' },
        { value: 1, label: '主键自增' },
        { value: 2, label: '时间自增' },
        { value: 3, label: 'HIVE分区' }
      ],
      triggerNextTimes: '',
      registerNode: [],
      jobJson: '',
      glueSource: '',
      timeOffset: 0,
      timeFormatType: 'yyyy-MM-dd',
      partitionField: '',
      timeFormatTypes: [
        { value: 'yyyy-MM-dd', label: 'yyyy-MM-dd' },
        { value: 'yyyyMMdd', label: 'yyyyMMdd' },
        { value: 'yyyy/MM/dd', label: 'yyyy/MM/dd' }
      ],
      replaceFormatTypes: [
        { value: 'yyyy/MM/dd', label: 'yyyy/MM/dd' },
        { value: 'yyyy-MM-dd', label: 'yyyy-MM-dd' },
        { value: 'HH:mm:ss', label: 'HH:mm:ss' },
        { value: 'yyyy/MM/dd HH:mm:ss', label: 'yyyy/MM/dd HH:mm:ss' },
        { value: 'yyyy-MM-dd HH:mm:ss', label: 'yyyy-MM-dd HH:mm:ss' },
        { value: 'Timestamp', label: '时间戳' }
      ],
      statusList: [
        { value: 500, label: '失败' },
        { value: 502, label: '失败(超时)' },
        { value: 200, label: '成功' },
        { value: 0, label: '无' }
      ]
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
      job.getExecutorList().then(response => {
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

      job.getList(this.listQuery).then(response => {
        const { content } = response
        this.total = content.recordsTotal
        this.list = content.data
        this.listLoading = false
      })
    },
    incStartTimeFormat(vData) {
    },
    handleCreate() {
      this.resetTemp()
      this.dialogStatus = 'create'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    createData() {
      if (this.temp.glueType === 'BEAN' && !isJSON(this.jobJson)) {
        this.$notify({
          title: 'Fail',
          message: 'json格式错误',
          type: 'error',
          duration: 2000
        })
        return
      }
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          if (this.temp.childJobId) {
            const auth = []
            for (const i in this.temp.childJobId) {
              auth.push(this.temp.childJobId[i].id)
            }
            this.temp.childJobId = auth.toString()
          }
          this.temp.jobJson = this.jobJson
          this.temp.glueSource = this.glueSource
          this.temp.executorHandler = this.temp.glueType === 'BEAN' ? 'executorJobHandler' : ''
          if (this.partitionField) this.temp.partitionInfo = this.partitionField + ',' + this.timeOffset + ',' + this.timeFormatType
          job.createJob(this.temp).then(() => {
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
      if (this.temp.jobJson) this.jobJson = JSON.parse(this.temp.jobJson)
      this.glueSource = this.temp.glueSource
      const arrchildSet = []
      const arrJobIdList = []
      if (this.jobIdList) {
        for (const n in this.jobIdList) {
          if (this.jobIdList[n].id !== this.temp.id) {
            arrJobIdList.push(this.jobIdList[n])
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
      this.dialogStatus = 'update'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    updateData() {
      this.temp.jobJson = typeof (this.jobJson) !== 'string' ? JSON.stringify(this.jobJson) : this.jobJson
      if (this.temp.glueType === 'BEAN' && !isJSON(this.temp.jobJson)) {
        this.$notify({
          title: 'Fail',
          message: 'json格式错误',
          type: 'error',
          duration: 2000
        })
        return
      }
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          if (this.temp.childJobId) {
            const auth = []
            for (const i in this.temp.childJobId) {
              auth.push(this.temp.childJobId[i].id)
            }
            this.temp.childJobId = auth.toString()
          }
          this.temp.executorHandler = this.temp.glueType === 'BEAN' ? 'executorJobHandler' : ''
          this.temp.glueSource = this.glueSource
          if (this.partitionField) this.temp.partitionInfo = this.partitionField + ',' + this.timeOffset + ',' + this.timeFormatType
          job.updateJob(this.temp).then(() => {
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
        job.removeJob(row.id).then(response => {
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
    handlerExecute(row) {
      this.$confirm('确定执行吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const param = {}
        param.jobId = row.id
        param.executorParam = row.executorParam
        job.triggerJob(param).then(response => {
          this.$notify({
            title: 'Success',
            message: 'Execute Successfully',
            type: 'success',
            duration: 2000
          })
        })
      })
    },
    // 查看日志
    handlerViewLog(row) {
      this.$router.push({ path: '/data/log', query: { jobId: row.id }})
    },
    handlerStart(row) {
      job.startJob(row.id).then(response => {
        this.$notify({
          title: 'Success',
          message: 'Start Successfully',
          type: 'success',
          duration: 2000
        })
      })
    },
    handlerStop(row) {
      job.stopJob(row.id).then(response => {
        this.$notify({
          title: 'Success',
          message: 'Start Successfully',
          type: 'success',
          duration: 2000
        })
      })
    },
    changeSwitch(row) {
      row.triggerStatus === 1 ? this.handlerStart(row) : this.handlerStop(row)
    },
    nextTriggerTime(row) {
      job.nextTriggerTime(row.jobCron).then(response => {
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
