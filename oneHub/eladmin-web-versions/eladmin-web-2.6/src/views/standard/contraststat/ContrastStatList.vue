<template>
  <el-card class="box-card" shadow="always">
    <el-form ref="queryForm" :model="queryParams" :inline="true">
      <el-form-item label="数据源" prop="sourceName">
        <el-input
          v-model="queryParams.sourceName"
          placeholder="请输入数据源"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="数据表" prop="tableName">
        <el-input
          v-model="queryParams.tableName"
          placeholder="请输入数据表"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="对照字段" prop="columnName">
        <el-input
          v-model="queryParams.columnName"
          placeholder="请输入对照字段"
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

    <el-row type="flex" justify="space-between">
      <el-col :span="12">
        <el-button-group>
        </el-button-group>
      </el-col>
      <el-col :span="12">
        <div class="right-toolbar">
          <el-tooltip content="密度" effect="dark" placement="top">
            <el-dropdown trigger="click" @command="handleCommand">
              <el-button circle size="mini">
                <svg-icon class-name="size-icon" icon-class="colum-height" />
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="medium">正常</el-dropdown-item>
                <el-dropdown-item command="small">中等</el-dropdown-item>
                <el-dropdown-item command="mini">紧凑</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </el-tooltip>
          <el-tooltip content="刷新" effect="dark" placement="top">
            <el-button circle size="mini" @click="handleRefresh">
              <svg-icon class-name="size-icon" icon-class="shuaxin" />
            </el-button>
          </el-tooltip>
          <el-tooltip content="列设置" effect="dark" placement="top">
            <el-popover placement="bottom" width="100" trigger="click">
              <el-checkbox-group v-model="checkedTableColumns" @change="handleCheckedColsChange">
                <el-checkbox
                  v-for="(item, index) in tableColumns"
                  :key="index"
                  :label="item.prop"
                >{{ item.label }}</el-checkbox>
              </el-checkbox-group>
              <span slot="reference">
                <el-button circle size="mini">
                  <svg-icon class-name="size-icon" icon-class="shezhi" />
                </el-button>
              </span>
            </el-popover>
          </el-tooltip>
        </div>
      </el-col>
    </el-row>

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
      <el-table-column label="序号" width="55" align="center">
        <template slot-scope="scope">
          <span>{{ scope.$index +1 }}</span>
        </template>
      </el-table-column>
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
    </el-table>

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
import { contrastStat } from '@/api/standard/contrast'

export default {
  name: 'ContrastStatList',
  data() {
    return {
      tableHeight: document.body.offsetHeight - 310 + 'px',
      // 展示切换
      showOptions: {
        data: {},
        showList: true
      },
      // 遮罩层
      loading: true,
      // 表格头
      tableColumns: [
        { prop: 'sourceName', label: '数据源', show: true },
        { prop: 'tableName', label: '数据表', show: true },
        { prop: 'columnName', label: '对照字段', show: true },
        { prop: 'gbTypeCode', label: '标准类别编码', show: true },
        { prop: 'gbTypeName', label: '标准类别名称', show: true },
        { prop: 'mappingCount', label: '已对照数量', show: true },
        { prop: 'unMappingCount', label: '未对照数量', show: true },
        { prop: 'mappingPercent', label: '对照比例', show: true, formatter: this.mappingFormatter }
      ],
      // 默认选择中表格头
      checkedTableColumns: [],
      tableSize: 'medium',
      // 表格数据
      tableDataList: [],
      // 总数据条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        sourceName: '',
        tableName: '',
        columnName: ''
      }
    }
  },
  created() {
    this.getList()
  },
  mounted() {
    this.initCols()
  },
  methods: {
    /** 查询数据源列表 */
    getList() {
      this.loading = true
      contrastStat(this.queryParams).then(response => {
        this.loading = false
        if (response.success) {
          const { data } = response
          this.tableDataList = data.data
          this.total = data.total
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
        sourceName: '',
        tableName: '',
        columnName: ''
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
    },
    mappingFormatter(row, column, cellValue, index) {
      return Math.round(parseFloat(row.mappingCount) / (parseFloat(row.mappingCount) + parseFloat(row.unMappingCount)) * 10000) / 100.00 + '%'
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
