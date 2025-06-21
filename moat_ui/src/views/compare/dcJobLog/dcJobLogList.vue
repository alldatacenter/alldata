<template>
  <el-card class="box-card" shadow="always">
    <el-form ref="queryForm" :model="queryParams" :inline="true">
      <el-form-item label="jobLogId" prop="jobLogId">
        <el-input
          v-model="queryParams.jobLogId"
          placeholder="jobLogId"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
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
              icon="el-icon-document"
              @click="handleDetail(scope.row)"
            >详情</el-button>
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
    <el-dialog title="调度日志详情" :visible.sync="dialogFormVisible" width="800px">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="150px">
        <el-form-item label="日志编号" prop="jobLogId" required>
          <el-input v-model="temp.jobLogId" style="width: 70%" />
        </el-form-item>
        <el-form-item label="任务名称" prop="jobName" required>
          <el-input v-model="temp.jobName" style="width: 70%" />
        </el-form-item>
        <el-form-item label="源表主键" prop="jobGroup" required>
          <el-input v-model="temp.jobGroup" style="width: 70%" />
        </el-form-item>
        <el-form-item label="调用目标字符串" prop="invokeTarget">
          <el-input v-model="temp.invokeTarget" style="width: 70%" />
        </el-form-item>
        <el-form-item label="日志信息" prop="jobMessage">
          <el-input v-model="temp.jobMessage" style="width: 70%" />
        </el-form-item>

        <el-form-item label="状态" prop="status" required>
          <el-input v-model="temp.status" placeholder="目标表" disabled style="width: 70%" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">
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
import { pageDataModel, delDataModel } from '@/api/compare/dcJobLog/datamodel'
export default {
  name: 'DcJobLogList',
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
      dialogStatus: '',
      temp: {
        jobLogId: '',
        jobName: '',
        jobGroup: '',
        invokeTarget: '',
        jobMessage: '',
        status: ''
      },
      rules: {
        name: [{ required: true, message: '不能为空', trigger: 'blur' }],
        description: [{ required: true, message: '不能为空', trigger: 'blur' }]
      },
      // 表格头
      tableColumns: [
        { prop: 'jobLogId', label: '日志编号', show: true },
        { prop: 'jobName', label: '任务名称', show: true },
        { prop: 'jobGroup', label: '源表主键', show: true },
        {
          prop: 'invokeTarget',
          label: '调用目标字符串',
          show: true
        },
        {
          prop: 'jobMessage',
          label: '日志信息',
          show: true
        },
        { prop: 'status', label: '状态', show: true,
          formatter: function (value, row, index) {
            if (value.status == 0) {
              return '成功'
            } else {
              return '失败'
            }
          } }
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
        jobLogId: ''
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
        // console.log("hello world2")
        // console.log(response.total)
        // console.log(response.code)
        _this.loading = false
        if (response.code === 0) {
          _this.tableDataList = response.rows
          _this.total = response.total
          // console.log("hello world3")
          // console.log(_this.tableDataList)
          // console.log(_this.total)
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
        jobLogId: ''
      }
      this.handleQuery()
    },
    /** 刷新列表 */
    handleRefresh() {
      this.getList()
    },
    /** 详情按钮操作 */
    handleDetail(row) {
      this.temp = Object.assign({}, row) // copy obj
      console.log(row)
      if (row.status == 0) {
        this.temp.status = '成功'
      } else {
        this.temp.status = '失败'
      }
      this.dialogStatus = 'detail'
      this.dialogFormVisible = true
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      console.log('进入到删除按钮操作')
      console.log(row)
      this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delDataModel(row.jobLogId).then(response => {
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
