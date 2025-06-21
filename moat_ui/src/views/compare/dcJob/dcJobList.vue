<template>
  <el-card class="box-card" shadow="always">
    <el-form ref="queryForm" :model="queryParams" :inline="true">
      <el-form-item label="JobId" prop="id">
        <el-input
          v-model="queryParams.id"
          placeholder="请输入JobId"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        <el-button
          type="primary"
          icon="el-icon-plus"
          size="mini"
          @click="handleCreate"
        >新增</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-loading="loading"
      :data="tableDataList"
      border
      tooltip-effect="dark"
      :size="tableSize"
      :height="tableHeight"
      style="width: 100%;margin: 15px 0;"
    >
      <el-table-column type="selection" width="55" align="center" />
      <template v-for="(item, index) in tableColumns">
        <el-table-column
          v-if="item.show"
          :key="index"
          :prop="item.prop"
          :label="item.label"
          :formatter="item.formatter"
          align="center"
          show-overflow-tooltip
        />
      </template>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-popover
            placement="left"
            trigger="click"
          >
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit-outline"
              @click="handleUpdate(scope.row)"
            >修改</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
            >删除</el-button>
            <el-button slot="reference">操作</el-button>
          </el-popover>
        </template>
      </el-table-column>
    </el-table>
    <!--    模态框：添加数据-->
    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible" width="800px">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="150px">
        <el-form-item label="任务编号" prop="jobId" required>
          <el-input v-model="temp.jobId" placeholder="请输入任务编号" style="width: 70%" />
        </el-form-item>
        <el-form-item label="任务名称" prop="jobName" required>
          <el-input v-model="temp.jobName" placeholder="请输入任务名称" style="width: 70%" />
        </el-form-item>
        <el-form-item label="任务分组" prop="jobGroup" required>
          <el-input v-model="temp.jobGroup" placeholder="请输入任务分组" style="width: 70%" />
        </el-form-item>
        <el-form-item label="调用目标字符串" prop="invokeTarget">
          <el-input v-model="temp.invokeTarget" placeholder="请输入任务目标字符串" style="width: 70%" />
        </el-form-item>
        <el-form-item label="执行表达式" prop="cronExpression">
          <el-input v-model="temp.cronExpression" placeholder="请输入执行表达式" style="width: 70%" />
        </el-form-item>

        <el-form-item label="任务状态" prop="status" required>
          <el-input v-model="temp.status" placeholder="任务状态" style="width: 70%" />
        </el-form-item>
        <el-form-item label="创建时间" prop="createTime" required>
          <el-input v-model="temp.createTime" placeholder="创建时间" style="width: 70%" />
        </el-form-item>
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

    <!--    模态框：修改数据-->
    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible2" width="800px">
      <el-form ref="dataForm2" :rules="rules" :model="temp2" label-position="left" label-width="150px">
        <el-form-item label="任务编号" prop="jobId" required>
          <el-input v-model="temp2.jobId" placeholder="请输入任务编号" style="width: 70%" />
        </el-form-item>
        <el-form-item label="任务名称" prop="jobName" required>
          <el-input v-model="temp2.jobName" placeholder="请输入任务名称" style="width: 70%" />
        </el-form-item>
        <el-form-item label="任务分组" prop="jobGroup" required>
          <el-input v-model="temp2.jobGroup" placeholder="请输入任务分组" style="width: 70%" />
        </el-form-item>
        <el-form-item label="调用目标字符串" prop="invokeTarget">
          <el-input v-model="temp2.invokeTarget" placeholder="请输入任务目标字符串" style="width: 70%" />
        </el-form-item>
        <el-form-item label="执行表达式" prop="cronExpression">
          <el-input v-model="temp2.cronExpression" placeholder="请输入执行表达式" style="width: 70%" />
        </el-form-item>

        <el-form-item label="任务状态" prop="status" required>
          <el-input v-model="temp2.status" placeholder="任务状态" style="width: 70%" />
        </el-form-item>
        <el-form-item label="创建时间" prop="createTime" required>
          <el-input v-model="temp2.createTime" placeholder="创建时间" style="width: 70%" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible2 = false">
          取消
        </el-button>
        <el-button type="primary" @click="dialogStatus==='create'?createData():updateData()">
          确认
        </el-button>
      </div>
    </el-dialog>

    <el-pagination
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      :current-page.sync="queryParams.pageNum"
      :page-size.sync="queryParams.pageSize"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </el-card>
</template>

<script>
import { pageDataModel, delDataModel, addDataModel, updateDataModel } from '@/api/compare/dcJob/datamodel'
export default {
  name: 'DcJobList',
  data() {
    return {
      tableHeight: document.body.offsetHeight - 310 + 'px',
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false
      },
      // 遮罩层
      loading: true,
      dialogFormVisible: false,
      dialogFormVisible2: false,
      textMap: {
        update: '编辑Job配置',
        create: '添加Job配置'
      },
      dialogStatus: '',
      temp: {
        jobId: '',
        jobName: '',
        jobGroup: '',
        invokeTarget: '',
        cronExpression: '',
        status: '',
        createTime: ''
      },
      temp2: {
        jobId: '',
        jobName: '',
        jobGroup: '',
        invokeTarget: '',
        cronExpression: '',
        status: '',
        createTime: ''
      },
      rules: {
        jobId: [{ required: true, message: '不能为空', trigger: 'blur' }],
        jobName: [{ required: true, message: '不能为空', trigger: 'blur' }],
        jobGroup: [{ required: true, message: '不能为空', trigger: 'blur' }],
        invokeTarget: [{ required: true, message: '不能为空', trigger: 'blur' }],
        cronExpression: [{ required: true, message: '不能为空', trigger: 'blur' }],
        status: [{ required: true, message: '不能为空', trigger: 'blur' }],
        createTime: [{ required: true, message: '不能为空', trigger: 'blur' }]
      },
      // 表格头
      tableColumns: [
        { prop: 'jobId', label: '任务编号', show: true },
        { prop: 'jobName', label: '任务名称', show: true },
        { prop: 'jobGroup', label: '任务分组', show: true },
        {
          prop: 'invokeTarget',
          label: '调用目标字符串',
          show: true
        },
        {
          prop: 'cronExpression',
          label: '执行表达式',
          show: true
        },
        { prop: 'status', label: '任务状态', show: true },
        { prop: 'createTime', label: '创建时间', show: true }
      ],
      // 默认选择中表格头
      checkedTableColumns: [],
      tableSize: 'medium',
      // 状态数据字典
      statusOptions: [],
      // 表格数据
      tableDataList: [],
      // 总数据条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        id: ''
      },
      // 流程状态数据字典
      flowStatusOptions: []
    }
  },
  created() {
    this.getDicts('sys_common_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
    this.getDicts('sys_flow_status').then(response => {
      if (response.success) {
        this.flowStatusOptions = response.data
      }
    })
    this.getList()
  },
  mounted() {
    this.initCols()
  },
  methods: {
    /** 查询数据源列表 */
    getList() {
      const _this = this
      // console.log("hello world1")
      _this.loading = true
      pageDataModel(this.queryParams).then(response => {
        _this.loading = false
        if (response.code === 0) {
          _this.tableDataList = response.rows
          _this.total = response.total
        }
      })
    },
    initCols() {
      this.checkedTableColumns = this.tableColumns.map(col => col.prop)
    },
    handleCheckedColsChange(val) {
      this.tableColumns.forEach(col => {
        if (!this.checkedTableColumns.includes(col.prop)) {
          col.show = false
        } else {
          col.show = true
        }
      })
    },
    handleCommand(command) {
      this.tableSize = command
    },
    resetTemp() {
      console.log('添加数据前，先清空之前输入的值')
      this.temp = {
        jobId: '',
        jobName: '',
        jobGroup: '',
        invokeTarget: '',
        cronExpression: '',
        status: '',
        createTime: ''
      }
    },
    createData() {
      console.log('开始添加数据')
      this.$refs['dataForm'].validate((valid) => {
        console.log(valid)
        if (valid) {
          console.log(this.temp)
          addDataModel(this.temp).then((response) => {
            console.log(response)
            this.dialogFormVisible = false
            this.$notify({
              title: '成功',
              message: '添加成功',
              type: 'success',
              duration: 2000
            })
            this.getList()
          })
        }
      })
    },
    // 新增按钮操作
    handleCreate() {
      this.resetTemp()
      this.dialogStatus = 'create'
      this.dialogFormVisible = true
      console.log('新增数据前。看是否已清空之前的数据', this.temp)
    },
    // 修改按钮操作
    handleUpdate(row) {
      // console.log(row)
      this.temp2 = Object.assign({}, row) // copy obj
      this.dialogStatus = 'update'
      this.dialogFormVisible2 = true
    },
    updateData() {
      this.$refs['dataForm2'].validate((valid) => {
        if (valid) {
          const tempData = Object.assign({}, this.temp2)
          console.log(tempData)
          const tempData2 = {}
          tempData2.id = tempData.id
          tempData2.originTableName = tempData.originTableName
          tempData2.originTablePrimary = tempData.originTablePrimary
          tempData2.originTableFields = tempData.originTableFields
          tempData2.originTableFilter = tempData.originTableFilter
          tempData2.originTableGroup = tempData.originTableGroup
          tempData2.toTableName = tempData.toTableName
          tempData2.toTablePrimary = tempData.toTablePrimary
          tempData2.toTableFields = tempData.toTableFields
          tempData2.toTableFilter = tempData.toTableFilter
          tempData2.toTableGroup = tempData.toTableGroup
          updateDataModel(tempData2).then(() => {
            this.dialogFormVisible2 = false
            this.$notify({
              title: '成功',
              message: '修改成功',
              type: 'success',
              duration: 2000
            })
            this.getList()
          })
        }
      })
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 20,
        id: ''
      }
      this.handleQuery()
    },
    /** 刷新列表 */
    handleRefresh() {
      this.getList()
    },
    /** 详情按钮操作 */
    handleDetail(row) {
      this.showOptions.data.id = row.id
      this.showOptions.showList = false
      this.showOptions.showAdd = false
      this.showOptions.showEdit = false
      this.showOptions.showDetail = true
      this.$emit('showCard', this.showOptions)
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      console.log('进入到删除按钮操作')
      console.log(row.id)
      this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delDataModel(row.id).then(response => {
          console.log(response)
          if (response.code === 0) {
            this.$message.success('删除成功')
            this.getList()
          }
        })
      }).catch(() => {
      })
    },
    /** 删除全部，按钮操作 */
    handleDelAll(row) {
      console.log('进入到删除按钮操作')
      console.log(row)
      this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delDataModel(row.id).then(response => {
          console.log(response)
          if (response.code === 0) {
            this.$message.success('删除成功')
            this.getList()
          }
        })
      }).catch(() => {
      })
    },
    handleSizeChange(val) {
      console.log(`每页 ${val} 条`)
      this.queryParams.pageNum = 1
      this.queryParams.pageSize = val
      this.getList()
    },
    handleCurrentChange(val) {
      console.log(`当前页: ${val}`)
      this.queryParams.pageNum = val
      this.getList()
    }
  }
}
</script>

<style lang="scss" scoped>
.right-toolbar {
  float: right;
}
.el-card ::v-deep .el-card__body {
  height: calc(100vh - 170px);
}
</style>
