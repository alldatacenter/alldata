<template>
  <el-card class="box-card" shadow="always">
    <el-form ref="queryForm" :model="queryParams" :inline="true">
      <el-form-item label="jobconfigId" prop="id">
        <el-input
          v-model="queryParams.jobconfigId"
          placeholder="请输入JobconfigId"
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
          <el-button
            size="medium"
            type="text"
            icon="el-icon-document"
            @click="handleDetail(scope.row)"
          >详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <!--    模态框：查看详情-->
    <el-dialog title="Job实例列表详细" :visible.sync="dialogFormVisible" width="1000px">
      <div style="text-align: center;font-size: 18pt;margin-bottom: 20px">量级对比</div>
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="180px">
        <div style="display: flex;align-items: center;justify-content: center">
          <el-form-item style="width: 70%;" label="源表" prop="originTableName" required />
          <el-form-item style="width: 70%" label="目标表" prop="originTablePrimary" required />
        </div>

        <div style="display: flex">
          <el-form-item style="width: 70%" label="originTablePv" prop="originTablePv" required>
            <el-input v-model="temp.originTablePv" style="width: 85%" disabled />
          </el-form-item>
          <el-form-item style="width: 70%" label="toTablePv" prop="toTablePv" required>
            <el-input v-model="temp.toTablePv" style="width: 85%" disabled />
          </el-form-item>
        </div>
        <div style="display: flex">
          <el-form-item style="width: 70%" label="originTableUv" prop="originTableUv" required>
            <el-input v-model="temp.originTableUv" style="width: 85%" disabled />
          </el-form-item>
          <el-form-item style="width: 70%" label="toTableUv" prop="toTableUv" required>
            <el-input v-model="temp.toTableUv" style="width: 85%" disabled />
          </el-form-item>
        </div>

        <div style="display: flex">
          <el-form-item style="width: 70%" label="pvDiff" prop="pvDiff" required>
            <el-input v-model="temp.pvDiff" style="width: 85%" disabled />
          </el-form-item>
          <el-form-item style="width: 70%" label="uvDiff" prop="uvDiff" required>
            <el-input v-model="temp.uvDiff" style="width: 85%" disabled />
          </el-form-item>
        </div>
        <div style="display: flex">
          <el-form-item style="width: 95%" label="magnitudeSql" prop="magnitudeSql" required>
            <el-input
              v-model="temp.magnitudeSql"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 8}"
              disabled
              height="150"
            />
          </el-form-item>
        </div>

        <div style="display: flex">
          <el-form-item style="width: 95%" label="量级是否通过" prop="magnitudeSql" required>
            <el-input
              disabled
              height="150"
              style="color:red"
              :value="temp.pvDiff==0 &&temp.uvDiff==0 ? 'true':'false'"
            />
          </el-form-item>
        </div>

        <!--一致性对比-->
        <div style="text-align: center;font-size: 18pt;margin-bottom: 20px">一致性对比</div>
        <div style="display: flex">
          <el-form-item style="width: 70%" label="originTableCount" prop="originTableCount" required>
            <el-input v-model="temp.originTableCount" style="width: 85%" disabled />
          </el-form-item>
          <el-form-item style="width: 70%" label="uvDiff" prop="uvDiff" required>
            <el-input v-model="temp.uvDiff" style="width: 85%" disabled />
          </el-form-item>
        </div>

        <div style="display: flex">
          <el-form-item style="width: 50%" label="countDiff" prop="countDiff" required>
            <el-input v-model="temp.countDiff" style="width: 85%" disabled />
          </el-form-item>
        </div>
        <div style="display: flex">
          <el-form-item style="width: 95%" label="magnitudeSql" prop="magnitudeSql" required>
            <el-input
              v-model="temp.consistencySql"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 8}"
              disabled
              height="150"
            />
          </el-form-item>
        </div>

        <div style="display: flex">
          <el-form-item style="width: 95%" label="一致性是否通过" prop="magnitudeSql" required>
            <el-input
              disabled
              height="150"
              style="color:red"
              :value="temp.countDiff == temp.originTableCount && temp.countDiff == temp.toTableCount ? 'true':'false'"
            />
          </el-form-item>
        </div>
      </el-form>
      <div style="text-align: center;margin: 10px 0">
        <el-button style="font-size: 16pt;background-color: #13ce66;border-radius: 5px" @click="getDiffDetail(temp.id)">
          查看差异
        </el-button>
      </div>
    </el-dialog>

    <!--    模态框：查看差异-->
    <el-dialog title="差异case" :visible.sync="dialogFormVisible2" width="1000px">
      <el-table
        :data="tableData"
        style="width: 100%;"
        height="250"
        stripe
      >
        <el-table-column
          prop="base_dict_code"
          label="base_dict_code"
          width="140"
        />
        <el-table-column
          prop="base_dict_label"
          label="base_dict_label"
          width="140"
        />
        <el-table-column
          prop="verify_dict_label"
          label="verify_dict_label"
          width="140"
        />
        <el-table-column
          prop="dict_label_is_pass"
          label="dict_label_is_pass"
          width="140"
        />
        <el-table-column
          prop="base_dict_value"
          label="base_dict_value"
          width="140"
        />
        <el-table-column
          prop="verify_dict_value"
          label="verify_dict_value"
          width="140"
        />
        <el-table-column
          prop="dict_value_is_pass"
          label="dict_value_is_pass"
          width="140"
        />
      </el-table>
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
import { pageDataModel, getDiffDetail } from '@/api/compare/dcJobInstance/datamodel'
export default {
  name: 'DcJobInstanceList',
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
        detail: 'Jobconfig详情'
      },
      dialogStatus: '',
      // 表单提交内容
      temp: {
        id: '',
        originTablePv: '',
        toTablePv: '',
        originTableUv: '',
        toTableUv: '',
        pvDiff: '',
        uvDiff: '',
        magnitudeSql: '',
        countDiff: '',
        originTableCount: '',
        consistencySql: ''
      },
      // 规则
      rules: {
        name: [{ required: true, message: '不能为空', trigger: 'blur' }],
        description: [{ required: true, message: '不能为空', trigger: 'blur' }]
      },
      // 表格头
      tableColumns: [
        { prop: 'id', label: 'id', show: true },
        { prop: 'jobconfigId', label: 'jobconfigId', show: true },
        { prop: 'originTableName', label: 'originTableName', show: true },
        {
          prop: 'toTableName',
          label: 'toTableName',
          show: true
        },
        { prop: 'countDiff', label: '量级校验通过', show: true,
          formatter: function (value, row, index) {
            if (value.pvDiff == '0' && value.uvDiff == '0') {
              return '是'
            } else {
              return '否'
            }
          }
        },
        { prop: 'countDiff', label: '一致性校验通过', show: true,
          formatter: function (value, row, index) {
            if (value.countDiff == value.originTableCount && value.countDiff == value.toTableCount) {
              return '是'
            } else {
              return '否'
            }
          }
        },
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
        jobconfigId: ''
      },
      // 流程状态数据字典
      flowStatusOptions: [],
      // 查看差异数组
      tableData: []
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
    // 查看差异
    getDiffDetail(id) {
      console.log('查看差异', id)
      const _this = this
      // console.log("hello world1")
      _this.loading = true
      getDiffDetail(id).then(response => {
        _this.loading = false
        console.log('response', response)
        if (response.code === 0) {
          for (let i = 0; i < response.total; i++) {
            const array = {
              base_dict_code: '--',
              base_dict_label: '--',
              verify_dict_label: '--',
              dict_label_is_pass: '--',
              base_dict_value: '--',
              verify_dict_value: '--',
              dict_value_is_pass: '--'
            }
            array.base_dict_code = response.rows[i].base_dict_code || '--'
            array.base_dict_label = response.rows[i].base_dict_label || '--'
            array.verify_dict_label = response.rows[i].verify_dict_label || '--'
            array.dict_label_is_pass = response.rows[i].dict_label_is_pass || '--'
            array.base_dict_value = response.rows[i].base_dict_value || '--'
            array.verify_dict_value = response.rows[i].verify_dict_value || '--'
            array.dict_value_is_pass = response.rows[i].dict_value_is_pass || '--'
            console.log('array', array)
            _this.tableData.push(array)
          }
          this.dialogFormVisible2 = true
        }
      })
    },
    /** 详情按钮操作 */
    handleDetail(row) {
      console.log(row)
      console.log('详情展开')
      this.temp = Object.assign({}, row)
      this.dialogStatus = 'detail'
      this.dialogFormVisible = true
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
        jobconfigId: ''
      }
      this.handleQuery()
    },
    /** 刷新列表 */
    handleRefresh() {
      this.getList()
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
.el-table .warning-row {
  background: oldlace;
}

.el-table .success-row {
  background: #f0f9eb;
}
</style>
