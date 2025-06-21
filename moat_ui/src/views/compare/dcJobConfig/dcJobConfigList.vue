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
              icon="el-icon-video-play"
              @click="handleRun(scope.row)"
            >运行</el-button>
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
        <el-form-item label="connectName" prop="dbConfigId" required>
          <el-select v-model="temp.dbConfigId" placeholder="请选择数据配置">
            <el-option
              v-for="item in options"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="源表" prop="originTableName" required>
          <el-input v-model="temp.originTableName" placeholder="请填写库名和表名(例如:[db].[table])" style="width: 70%" />
        </el-form-item>
        <el-form-item label="源表主键" prop="originTablePrimary" required>
          <el-input v-model="temp.originTablePrimary" placeholder="源表主键" style="width: 70%" />
        </el-form-item>
        <el-form-item label="源表字段" prop="originTableFields" required>
          <el-input v-model="temp.originTableFields" placeholder="请填写字段名,用逗号分隔" style="width: 70%" />
        </el-form-item>
        <el-form-item label="源表过滤条件" prop="originTableFilter">
          <el-input v-model="temp.originTableFilter" placeholder="源表过滤条件" style="width: 70%" />
        </el-form-item>
        <el-form-item label="源表分组条件" prop="originTableGroup">
          <el-input v-model="temp.originTableGroup" placeholder="源表分组条件" style="width: 70%" />
        </el-form-item>

        <el-form-item label="目标表" prop="toTableName" required>
          <el-input v-model="temp.toTableName" placeholder="目标表" disabled style="width: 70%" />
        </el-form-item>
        <el-form-item label="目标表主键" prop="toTablePrimary" required>
          <el-input v-model="temp.toTablePrimary" placeholder="目标表主键" disabled style="width: 70%" />
        </el-form-item>
        <el-form-item label="目标表字段" prop="toTableFields" required>
          <el-input v-model="temp.toTableFields" placeholder="目标表字段" disabled style="width: 70%" />
        </el-form-item>
        <el-form-item label="目标表过滤条件" prop="toTableFilter">
          <el-input v-model="temp.toTableFilter" placeholder="目标表过滤条件" disabled style="width: 70%" />
        </el-form-item>
        <el-form-item label="目标表分组条件" prop="toTableGroup">
          <el-input v-model="temp.toTableGroup" placeholder="目标表分组条件" disabled style="width: 70%" />
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
        <el-form-item label="源表" prop="originTableName" required>
          <el-input v-model="temp2.originTableName" placeholder="请填写库名和表名(例如:[db].[table])" style="width: 70%" />
        </el-form-item>
        <el-form-item label="源表主键" prop="originTablePrimary" required>
          <el-input v-model="temp2.originTablePrimary" placeholder="源表主键" style="width: 70%" />
        </el-form-item>
        <el-form-item label="源表字段" prop="originTableFields" required>
          <el-input v-model="temp2.originTableFields" placeholder="请填写字段名,用逗号分隔" style="width: 70%" />
        </el-form-item>
        <el-form-item label="源表过滤条件" prop="originTableFilter">
          <el-input v-model="temp2.originTableFilter" placeholder="源表过滤条件" style="width: 70%" />
        </el-form-item>
        <el-form-item label="源表分组条件" prop="originTableGroup">
          <el-input v-model="temp2.originTableGroup" placeholder="源表分组条件" style="width: 70%" />
        </el-form-item>

        <el-form-item label="目标表" prop="toTableName" required>
          <el-input v-model="temp2.toTableName" placeholder="目标表" style="width: 70%" />
        </el-form-item>
        <el-form-item label="目标表主键" prop="toTablePrimary" required>
          <el-input v-model="temp2.toTablePrimary" placeholder="目标表主键" style="width: 70%" />
        </el-form-item>
        <el-form-item label="目标表字段" prop="toTableFields" required>
          <el-input v-model="temp2.toTableFields" placeholder="目标表字段" style="width: 70%" />
        </el-form-item>
        <el-form-item label="目标表过滤条件" prop="toTableFilter">
          <el-input v-model="temp2.toTableFilter" placeholder="目标表过滤条件" style="width: 70%" />
        </el-form-item>
        <el-form-item label="目标表分组条件" prop="toTableGroup">
          <el-input v-model="temp2.toTableGroup" placeholder="目标表分组条件" style="width: 70%" />
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
import { pageDataModel, delDataModel, addDataModel, updateDataModel, getDbConfig, dcJobConfigRun } from '@/api/compare/dcJobConfig/datamodel'
export default {
  name: 'DcJobConfigList',
  data() {
    return {
      options: [],
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
        dbConfigId: '',
        originTableName: '',
        originTablePrimary: '',
        originTableFields: '',
        originTableFilter: '',
        originTableGroup: '',
        toTableName: '',
        toTablePrimary: '',
        toTableFields: '',
        toTableFilter: '',
        toTableGroup: ''
      },
      temp2: {
        dbConfigId: '',
        originTableName: '',
        originTablePrimary: '',
        originTableFields: '',
        originTableFilter: '',
        originTableGroup: '',
        toTableName: '',
        toTablePrimary: '',
        toTableFields: '',
        toTableFilter: '',
        toTableGroup: ''
      },
      rules: {
        name: [{ required: true, message: '不能为空', trigger: 'blur' }],
        description: [{ required: true, message: '不能为空', trigger: 'blur' }]
      },
      // 表格头
      tableColumns: [
        { prop: 'id', label: 'jobconfigId', show: true },
        { prop: 'originTableName', label: '源表', show: true },
        { prop: 'originTablePrimary', label: '源表主键', show: true },
        {
          prop: 'originTableFields',
          label: '对比字段',
          show: true
        },
        {
          prop: 'toTableName',
          label: '目标表',
          show: true
        },
        { prop: 'toTablePrimary', label: '目标主键', show: true },
        { prop: 'toTableFields', label: '目标对比字段', show: true },
        { prop: 'schduleStatus', label: '是否启动调度', show: true,
          formatter: function (value, row, index) {
            console.log(value)
            if (value.schduleStatus == '1') {
              return '是'
            } else {
              return '否'
            }
          } },
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
  watch: {
    temp: {
      handler(newName, oldName) {
        this.temp.dbConfigId = newName.dbConfigId
        this.temp.toTableName = newName.originTableName
        this.temp.toTablePrimary = newName.originTablePrimary
        this.temp.toTableFields = newName.originTableFields
        this.temp.toTableFilter = newName.originTableFilter
        this.temp.toTableGroup = newName.originTableGroup
      },
      immediate: true,
      deep: true
    }
  },
  created() {
    this.getList()
    this.getDbConfig()
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
    // 获取DbconfigId
    getDbConfig() {
      const _this = this
      console.log('添加数据时，获取DbconfigId')
      getDbConfig().then((response) => {
        // console.log("response",response)
        // console.log("total",response.total)
        for (let i = 0; i < response.total; i++) {
          // console.log(response.rows[i].id)
          const obj = { value: '', label: '' }
          console.log(response.rows[i].connectName)
          obj.value = response.rows[i].id
          obj.label = response.rows[i].connectName
          console.log(i)
          console.log(obj)
          _this.options.push(obj)
        }
      })
    },
    // 添加数据前，清空数据
    resetTemp() {
      console.log('添加数据前，先清空之前输入的值')
      this.temp = {
        dbConfigId: '',
        originTableName: '',
        originTablePrimary: '',
        originTableFields: '',
        originTableFilter: '',
        originTableGroup: '',
        toTableName: '',
        toTablePrimary: '',
        toTableFields: '',
        toTableFilter: '',
        toTableGroup: ''
      }
    },
    // 开始添加数据
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
    // 运行按钮操作
    handleRun(data) {
      console.log(data.id)
      dcJobConfigRun(data.id).then((response) => {
        console.log('response', response)
        if (response.code === 0) {
          this.$message.success('运行成功')
          this.getList()
        } else {
          this.$message.success('运行失败')
          this.getList()
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
