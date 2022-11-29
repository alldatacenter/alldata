<template>
  <el-row :gutter="20">
    <el-col :span="6">
      <el-card class="box-card tree-wrapper" shadow="always">
        <div class="body-wrapper">
          <el-tree
            ref="category"
            :data="categoryOptions"
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
                <span><i v-if="node.level === 1" class="iconfont icon-zuzhi tree-folder" />{{ node.label }}</span>
                <span class="tree-bts">
                  <i v-hasPerm="['workflow:definition:type:add']" v-show="!data.id && data.show" class="el-icon-circle-plus-outline bt-add" @click="() => handleAddCategory()" />
                  <i v-hasPerm="['workflow:definition:type:edit']" v-show="data.id && data.show" class="el-icon-edit-outline bt-edit" @click="() => handleEditCategory(data)" />
                  <i v-hasPerm="['workflow:definition:type:remove']" v-show="data.id && data.show" class="el-icon-delete bt-delete" @click="() => handleDelCategory(data)" />
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
          <el-form-item label="流程定义名称" prop="name">
            <el-input
              v-model="queryParams.name"
              placeholder="请输入流程定义名称"
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
                v-hasPerm="['workflow:definition:import']"
                type="primary"
                icon="el-icon-plus"
                size="mini"
                @click="handleImport"
              >导入</el-button>
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
                  v-hasPerm="['workflow:definition:resource']"
                  size="mini"
                  type="text"
                  icon="el-icon-view"
                  @click="handleResource(scope.row)"
                >流程图</el-button>
                <el-button
                  v-hasPerm="['workflow:definition:activate']"
                  v-if="scope.row.suspensionState === 2"
                  size="mini"
                  type="text"
                  icon="el-icon-view"
                  @click="handleActivate(scope.row)"
                >激活</el-button>
                <el-button
                  v-hasPerm="['workflow:definition:suspend']"
                  v-if="scope.row.suspensionState === 1"
                  size="mini"
                  type="text"
                  icon="el-icon-view"
                  @click="handleSuspend(scope.row)"
                >挂起</el-button>
                <el-button
                  v-hasPerm="['workflow:definition:remove']"
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

    <!-- 流程分类对话框 -->
    <flow-category v-if="dialogFlowCategoryVisible" :visible.sync="dialogFlowCategoryVisible" :data="currentCategory" @handleFlowCategoryFinished="getTree"></flow-category>
    <!-- 流程定义对话框 -->
    <flow-definition v-if="dialogFlowDefinitionVisible" :visible.sync="dialogFlowDefinitionVisible" :category="queryParams.categoryId" @handleFlowDefinitionFinished="getList"></flow-definition>
    <!-- 流程资源对话框 -->
    <flow-resource v-if="dialogFlowResourceVisible" :visible.sync="dialogFlowResourceVisible" :processDefinitionId="currentProcessDefinitionId" />
  </el-row>
</template>

<script>
import { listCategory, delCategory, pageDefinition, delDefinition, activateDefinition, suspendDefinition } from '@/api/workflow/definition'
import FlowResource from './components/FlowResource'
import FlowCategory from './components/FlowCategory'
import FlowDefinition from './components/FlowDefinition'

export default {
  name: 'DefinitionList',
  components: { FlowResource, FlowCategory, FlowDefinition },
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
        { prop: 'id', label: '流程定义ID', show: true },
        { prop: 'name', label: '流程名称', show: true },
        { prop: 'version', label: '版本', show: true },
        {
          prop: 'suspensionState',
          label: '状态',
          show: true,
          formatter: this.statusFormatter
        }
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
        name: '',
        categoryId: ''
      },
      // 左侧树
      categoryOptions: [],
      defaultProps: {
        children: 'children',
        label: 'name'
      },
      // 流程图
      dialogFlowResourceVisible: false,
      currentProcessDefinitionId: '',
      // 流程分类
      dialogFlowCategoryVisible: false,
      currentCategory: {},
      // 流程定义
      dialogFlowDefinitionVisible: false
    }
  },
  created() {
    this.getTree()
    this.getList()
  },
  mounted() {
    this.initCols()
  },
  methods: {
    getTree() {
      listCategory().then(response => {
        if (response.success) {
          const { data } = response
          const tree = {}
          tree.name = '流程分类'
          tree.children = data
          this.categoryOptions = []
          this.categoryOptions.push(tree)
        }
      })
    },
    /** 节点单击事件 */
    handleNodeClick(data) {
      if (data.id) {
        this.queryParams.categoryId = data.id
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
    handleAddCategory() {
      this.dialogFlowCategoryVisible = true
      this.currentCategory = {}
    },
    handleEditCategory(data) {
      this.dialogFlowCategoryVisible = true
      this.currentCategory = Object.assign({}, data)
    },
    handleDelCategory(data) {
      this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delCategory(data.id).then(response => {
          if (response.success) {
            this.$message.success('删除成功')
            this.getTree()
          }
        })
      }).catch(() => {
      })
    },
    handleImport() {
      if (this.queryParams.categoryId) {
        this.dialogFlowDefinitionVisible = true
      } else {
        this.$message.warning('请先选择流程分类')
      }
    },
    /** 查询数据源列表 */
    getList() {
      this.loading = true
      pageDefinition(this.queryParams).then(response => {
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
        name: '',
        categoryId: ''
      }
      this.handleQuery()
    },
    /** 刷新列表 */
    handleRefresh() {
      this.getList()
    },
    handleResource(row) {
      this.currentProcessDefinitionId = row.id
      this.dialogFlowResourceVisible = true
    },
    handleActivate(row) {
      this.$confirm('激活流程定义？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        activateDefinition(row.id).then(response => {
          if (response.success) {
            this.$message.success('删除成功')
            this.getList()
          }
        })
      }).catch(() => {
      })
    },
    handleSuspend(row) {
      this.$confirm('挂起流程定义？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        suspendDefinition(row.id).then(response => {
          if (response.success) {
            this.$message.success('删除成功')
            this.getList()
          }
        })
      }).catch(() => {
      })
    },
    handleDelete(row) {
      this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delDefinition(row.deploymentId).then(response => {
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
      if (cellValue === 1) {
        return <el-tag type='success'>激活</el-tag>
      } else if (cellValue === 2) {
        return <el-tag type='warning'>挂起</el-tag>
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
