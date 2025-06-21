<template>
  <el-card class="box-card" shadow="always">
    <el-form ref="queryForm" :model="queryParams" :inline="true">
      <el-form-item label="connectName" prop="id">
        <el-input
          v-model="queryParams.connectName"
          placeholder="connectName"
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
        <el-form-item label="connectName" prop="connectName" required>
          <el-input v-model="temp.connectName" placeholder="请输入connectName" style="width: 70%" />
        </el-form-item>
        <el-form-item label="type" prop="type" required>
          <el-select v-model="temp.type" placeholder="请选择数据库类型">
            <el-option
              v-for="item in options"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="url" prop="url" required>
          <el-input v-model="temp.url" placeholder="请输入url" style="width: 70%" />
        </el-form-item>
        <el-form-item label="userName" prop="userName" required>
          <el-input v-model="temp.userName" placeholder="请输入userName" style="width: 70%" />
        </el-form-item>
        <el-form-item label="pwd" prop="pwd">
          <el-input v-model="temp.pwd" placeholder="请输入pwd" style="width: 70%" />
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
import { pageDataModel, delDataModel, addDataModel, updateDataModel } from '@/api/compare/dcDbConfig/datamodel'
export default {
  name: 'DcDbConfigList',
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
      options: [{
        value: 'MySQL',
        label: 'MySQL'
      }, {
        value: 'Hive',
        label: 'Hive'
      }, {
        value: 'Doris',
        label: 'Doris'
      }],
      // 遮罩层
      loading: true,
      dialogFormVisible: false,
      dialogFormVisible2: false,
      textMap: {
        update: '编辑数据库配置',
        create: '添加数据库配置'
      },
      dialogStatus: '',
      temp: {
        connectName: '',
        type: '',
        url: '',
        userName: '',
        pwd: ''
      },
      rules: {
        id: [{ required: true, message: '不能为空', trigger: 'blur' }],
        connectName: [{ required: true, message: '不能为空', trigger: 'blur' }],
        type: [{ required: true, message: '不能为空', trigger: 'blur' }],
        url: [{ required: true, message: '不能为空', trigger: 'blur' }]
      },
      // 表格头
      tableColumns: [
        { prop: 'id', label: 'id', show: true },
        { prop: 'connectName', label: 'connectName', show: true },
        { prop: 'type', label: 'type', show: true },
        {
          prop: 'url',
          label: 'url',
          show: true
        }
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
        connectName: ''
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
        console.log(response)
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
        connectName: '',
        type: '',
        url: '',
        userName: '',
        pwd: ''
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
      this.temp = Object.assign({}, row) // copy obj
      this.dialogStatus = 'update'
      this.dialogFormVisible = true
    },
    updateData() {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          const tempData = Object.assign({}, this.temp)
          console.log(tempData)
          const tempData2 = {}
          tempData2.id = tempData.id
          tempData2.connectName = tempData.connectName
          tempData2.type = tempData.type
          tempData2.url = tempData.url
          tempData2.userName = tempData.userName
          tempData2.pwd = tempData.pwd
          updateDataModel(tempData2).then(() => {
            this.dialogFormVisible = false
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
        connectName: ''
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
