<template>
  <el-row :gutter="20">
    <el-col :span="6">
      <el-card class="box-card tree-wrapper" shadow="always">
        <el-tree
          ref="tree"
          :data="treeOptions"
          node-key="id"
          empty-text="加载中，请稍后"
          :props="defaultProps"
          default-expand-all
          highlight-current
          :expand-on-click-node="false"
          @node-click="handleNodeClick"
        >
          <span class="custom-tree-node" slot-scope="{ node, data }">
            <i v-if="node.level === 1" class="iconfont icon-shujuku"></i>
            <i v-else-if="node.level === 2" class="iconfont icon-shujubiao"></i>
            <span>{{ node.label }}</span>
          </span>
        </el-tree>
      </el-card>
    </el-col>
    <el-col :span="18">
      <el-card class="box-card" shadow="always">
        <el-form ref="queryForm" :model="queryParams" :inline="true">
          <el-form-item label="字段名称" prop="columnName">
            <el-input
              v-model="queryParams.columnName"
              placeholder="请输入字段名称"
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
                  <el-checkbox-group
                    v-model="checkedTableColumns"
                    @change="handleCheckedColsChange"
                  >
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
          :data="columnList"
          border
          tooltip-effect="dark"
          :size="tableSize"
          :height="tableHeight"
          style="width: 100%;margin: 15px 0;"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="序号" width="55" align="center">
            <template slot-scope="scope">
              <span>{{ scope.$index + 1 }}</span>
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
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template slot-scope="scope">
              <el-popover
                placement="left"
                trigger="click"
              >
                <el-button
                  v-hasPerm="['metadata:datacolumn:detail']"
                  size="mini"
                  type="text"
                  icon="el-icon-view"
                  @click="handleDetail(scope.row)"
                >详情</el-button>
                <el-button slot="reference">操作</el-button>
              </el-popover>
            </template>
          </el-table-column>
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
    </el-col>
  </el-row>
</template>

<script>
import { pageDataColumn, getDataMetadataTree } from '@/api/metadata/datacolumn'

export default {
  name: 'DataColumnList',
  data() {
    return {
      tableHeight: document.body.offsetHeight - 310 + 'px',
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showDetail: false
      },
      // 遮罩层
      loading: true,
      // 表格头
      tableColumns: [
        { prop: 'columnName', label: '字段名称', show: true },
        { prop: 'columnComment', label: '字段注释', show: true },
        { prop: 'columnKey', label: '是否主键', show: true, formatter: this.keyFormatter },
        { prop: 'columnNullable', label: '是否允许为空', show: true, formatter: this.nullableFormatter },
        { prop: 'dataType', label: '数据类型', show: true },
        { prop: 'dataLength', label: '数据长度', show: true },
        { prop: 'dataPrecision', label: '数据精度', show: true },
        { prop: 'dataScale', label: '数据小数位', show: true },
        { prop: 'dataDefault', label: '数据默认值', show: true }
      ],
      // 默认选择中表格头
      checkedTableColumns: [],
      tableSize: 'medium',
      // 表格数据
      columnList: [],
      // 总数据条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        columnName: '',
        sourceId: '',
        tableId: ''
      },
      // 左侧树
      treeOptions: [],
      defaultProps: {
        children: 'children',
        label: 'label'
      }
    }
  },
  created() {
    this.getList()
    this.getTree()
  },
  mounted() {
    this.initCols()
  },
  methods: {
    /** 查询列表 */
    getList() {
      this.loading = true
      pageDataColumn(this.queryParams).then(response => {
        this.loading = false
        if (response.success) {
          const { data } = response
          this.columnList = data.data
          this.total = data.total
        }
      })
    },
    /** 查询树结构 */
    getTree() {
      getDataMetadataTree('table').then(response => {
        if (response.success) {
          const { data } = response
          this.treeOptions = data
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
        columnName: '',
        sourceId: '',
        tableId: ''
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
      this.showOptions.showDetail = true
      this.$emit('showCard', this.showOptions)
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
    /** 节点单击事件 */
    handleNodeClick(data) {
      if (data.type === 'database') {
        this.queryParams.sourceId = data.id
        this.queryParams.tableId = ''
      } else if (data.type === 'table') {
        this.queryParams.tableId = data.id
        this.queryParams.sourceId = ''
      }
      this.getList()
    },
    keyFormatter(row, column, cellValue, index) {
      if (cellValue === '1') {
        return 'Y'
      } else {
        return 'N'
      }
    },
    nullableFormatter(row, column, cellValue, index) {
      if (cellValue === '1') {
        return 'Y'
      } else {
        return 'N'
      }
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
.tree-wrapper {
  overflow-y: auto;
  ::v-deep .custom-tree-node {
    flex: 1;
    display: flex;
    font-size: 14px;
    i {
      margin-right: 10px;
    }
    .icon-shujuku {
      color: #20a0ff;
    }
    .icon-shujubiao {
      color: #38dcec;
    }
  }
}

</style>
