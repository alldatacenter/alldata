<template>
  <el-row :gutter="20">
    <el-col :span="6">
      <el-card class="box-card tree-wrapper" shadow="always">
        <div class="body-wrapper">
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
            <template slot-scope="{ node, data }">
              <span class="custom-tree-node" @mouseenter="mouseenter(data)" @mouseleave="mouseleave(data)">
                <span><i v-if="node.level === 1" class="iconfont icon-zuzhi tree-folder" />{{ data.name ? node.label + '(' + data.name + ')' : node.label }}</span>
                <span class="tree-bts">
                  <i v-hasPerm="['standard:contrast:add']" v-show="node.level === 1 && data.show" class="el-icon-circle-plus-outline bt-add" @click="() => handleAddContrast()" />
                  <i v-hasPerm="['standard:contrast:edit']" v-show="node.level === 4 && data.show" class="el-icon-edit-outline bt-edit" @click="() => handleEditContrast(data)" />
                  <i v-hasPerm="['standard:contrast:remove']" v-show="node.level === 4 && data.show" class="el-icon-delete bt-delete" @click="() => handleDelContrast(data)" />
                </span>
              </span>
            </template>
          </el-tree>
        </div>
      </el-card>
    </el-col>
    <el-col :span="18">
      <el-card class="box-card" shadow="always">
        <el-form ref="queryForm" :model="queryParams" :inline="true">
          <el-form-item label="字典编码" prop="colCode">
            <el-input
              v-model="queryParams.colCode"
              placeholder="请输入字典编码"
              clearable
              size="small"
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="字典名称" prop="colName">
            <el-input
              v-model="queryParams.colName"
              placeholder="请输入字典名称"
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
              <el-button
                v-hasPerm="['standard:contrast:dict:add']"
                type="primary"
                icon="el-icon-plus"
                size="mini"
                @click="handleAdd"
              >新增</el-button>
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
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template slot-scope="scope">
              <el-popover
                placement="left"
                trigger="click"
              >
                <el-button
                  v-hasPerm="['standard:contrast:dict:edit']"
                  size="mini"
                  type="text"
                  icon="el-icon-edit-outline"
                  @click="handleEdit(scope.row)"
                >修改</el-button>
                <el-button
                  v-hasPerm="['standard:contrast:dict:detail']"
                  size="mini"
                  type="text"
                  icon="el-icon-view"
                  @click="handleDetail(scope.row)"
                >详情</el-button>
                <el-button
                  v-hasPerm="['standard:contrast:dict:remove']"
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

    <!-- 对照表对话框 -->
    <form-contrast v-if="dialogFormContrastVisible" :visible.sync="dialogFormContrastVisible" :data="currentContrast" @handleFormContrastFinished="getTree"></form-contrast>
  </el-row>
</template>

<script>
import { getContrastTree, delContrast } from '@/api/standard/contrast'
import { pageContrastDict, delContrastDict } from '@/api/standard/contrastdict'
import FormContrast from './components/FormContrast'

export default {
  name: 'DictContrastList',
  components: { FormContrast },
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
      // 表格头
      tableColumns: [
        { prop: 'sourceName', label: '数据源', show: true },
        { prop: 'tableName', label: '数据表', show: true },
        { prop: 'columnName', label: '对照字段', show: true },
        { prop: 'gbTypeCode', label: '标准类别编码', show: true },
        { prop: 'gbTypeName', label: '标准类别名称', show: true },
        { prop: 'colCode', label: '字典编码', show: true },
        { prop: 'colName', label: '字典名称', show: true },
        {
          prop: 'status',
          label: '状态',
          show: true,
          formatter: this.statusFormatter
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
        contrastId: '',
        colCode: '',
        colName: ''
      },
      // 左侧树
      treeOptions: [],
      defaultProps: {
        children: 'children',
        label: 'label'
      },
      // 对照表
      dialogFormContrastVisible: false,
      currentContrast: {}
    }
  },
  created() {
    this.getDicts('data_contrast_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
    this.getTree()
    this.getList()
  },
  mounted() {
    this.initCols()
  },
  methods: {
    getTree() {
      getContrastTree().then(response => {
        if (response.success) {
          const { data } = response
          const tree = {}
          tree.label = '对照表'
          tree.children = data
          this.treeOptions = []
          this.treeOptions.push(tree)
        }
      })
    },
    /** 节点单击事件 */
    handleNodeClick(data, node) {
      this.queryParams.contrastId = ''
      if (node.level === 4) {
        this.queryParams.contrastId = data.id
        this.getList()
      }
    },
    /** 树节点鼠标移入移出 */
    mouseenter(data) {
      this.$set(data, 'show', true)
    },
    mouseleave(data) {
      this.$set(data, 'show', false)
    },
    handleAddContrast() {
      this.dialogFormContrastVisible = true
      this.currentContrast = {}
    },
    handleEditContrast(data) {
      this.dialogFormContrastVisible = true
      this.currentContrast = Object.assign({}, data.data)
    },
    handleDelContrast(data) {
      this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delContrast(data.id).then(response => {
          if (response.success) {
            this.$message.success('删除成功')
            this.getTree()
          }
        })
      }).catch(() => {
      })
    },
    /** 查询数据源列表 */
    getList() {
      this.loading = true
      pageContrastDict(this.queryParams).then(response => {
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
        contrastId: '',
        colCode: '',
        colName: ''
      }
      this.handleQuery()
    },
    /** 刷新列表 */
    handleRefresh() {
      this.getList()
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.showOptions.data = {}
      if (!this.queryParams.contrastId) {
        this.$message.warning('请先选择对照表字段')
        return
      }
      this.showOptions.data.contrastId = this.queryParams.contrastId
      this.showOptions.showList = false
      this.showOptions.showAdd = true
      this.showOptions.showEdit = false
      this.showOptions.showDetail = false
      this.$emit('showCard', this.showOptions)
    },
    /** 修改按钮操作 */
    handleEdit(row) {
      this.showOptions.data.id = row.id
      this.showOptions.showList = false
      this.showOptions.showAdd = false
      this.showOptions.showEdit = true
      this.showOptions.showDetail = false
      this.$emit('showCard', this.showOptions)
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
      this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delContrastDict(row.id).then(response => {
          if (response.success) {
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
    },
    statusFormatter(row, column, cellValue, index) {
      const dictLabel = this.selectDictLabel(this.statusOptions, cellValue)
      if (cellValue === '1') {
        return <el-tag type='success'>{dictLabel}</el-tag>
      } else {
        return <el-tag type='warning'>{dictLabel}</el-tag>
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
  .body-wrapper {
    margin: -10px;
    ::v-deep .custom-tree-node {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 14px;
      .tree-folder {
        margin-right: 5px;
        color: #f6cf07;
      }
      .tree-bts {
        .bt-add {
          color: #409eff;
        }
        .bt-edit {
          color: #67c23a;
        }
        .bt-delete {
          color: #f56c6c;
        }
        i {
          margin-right: 10px;
          padding: 0px;
        }
      }
    }
  }
}
</style>
