<template>
  <el-row :gutter="20">
    <el-col :span="6">
      <el-card class="box-card tree-wrapper" shadow="always">
        <div class="body-wrapper">
          <el-tree
            ref="type"
            :data="typeOptions"
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
                  <i v-hasPerm="['standard:type:add']" v-show="!data.id && data.show" class="el-icon-circle-plus-outline bt-add" @click="() => handleAddType()" />
                  <i v-hasPerm="['standard:type:edit']" v-show="data.id && data.show" class="el-icon-edit-outline bt-edit" @click="() => handleEditType(data)" />
                  <i v-hasPerm="['standard:type:remove']" v-show="data.id && data.show" class="el-icon-delete bt-delete" @click="() => handleDelType(data)" />
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
          <el-form-item label="标准编码" prop="gbCode">
            <el-input
              v-model="queryParams.gbCode"
              placeholder="请输入标准编码"
              clearable
              size="small"
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="标准名称" prop="gbName">
            <el-input
              v-model="queryParams.gbName"
              placeholder="请输入标准名称"
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
                v-hasPerm="['standard:dict:add']"
                type="primary"
                icon="el-icon-plus"
                size="mini"
                @click="handleAdd"
              >新增</el-button>
              <el-button
                v-hasPerm="['standard:dict:refresh']"
                type="warning"
                icon="el-icon-refresh"
                size="mini"
                @click="handleCacheRefresh"
              >刷新缓存</el-button>
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
                  v-hasPerm="['standard:dict:edit']"
                  size="mini"
                  type="text"
                  icon="el-icon-edit-outline"
                  @click="handleEdit(scope.row)"
                >修改</el-button>
                <el-button
                  v-hasPerm="['standard:dict:detail']"
                  size="mini"
                  type="text"
                  icon="el-icon-view"
                  @click="handleDetail(scope.row)"
                >详情</el-button>
                <el-button
                  v-hasPerm="['standard:dict:remove']"
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

    <!-- 数据标准类别对话框 -->
    <el-dialog :title="dialog.title" :visible.sync="dialog.open" width="400px" append-to-body>
      <el-form ref="dialogForm" :model="dialogForm" :rules="dialogRules" label-width="80px">
        <el-form-item label="类别编码" prop="gbTypeCode">
          <el-input v-model="dialogForm.gbTypeCode" placeholder="请输入类别编码" />
        </el-form-item>
        <el-form-item label="类别名称" prop="gbTypeName">
          <el-input v-model="dialogForm.gbTypeName" placeholder="请输入类别名称" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitDialogForm">确 定</el-button>
        <el-button @click="dialog.open = false">取 消</el-button>
      </div>
    </el-dialog>
  </el-row>
</template>

<script>
import { listDataDictType, addDataDictType, updateDataDictType, delDataDictType, pageDataDict, delDataDict, refreshDict } from '@/api/standard/datadict'

export default {
  name: 'DataDictList',
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
        { prop: 'gbTypeCode', label: '标准类别编码', show: true },
        { prop: 'gbTypeName', label: '标准类别名称', show: true },
        { prop: 'gbCode', label: '标准编码', show: true },
        { prop: 'gbName', label: '标准名称', show: true },
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
        typeId: '',
        gbCode: '',
        gbName: ''
      },
      // 左侧树
      typeOptions: [],
      defaultProps: {
        children: 'children',
        label: 'gbTypeName'
      },
      dialog: {
        // 是否显示弹出层
        open: false,
        // 弹出层标题
        title: '数据标准类别'
      },
      dialogForm: {},
      dialogRules: {
        gbTypeCode: [
          { required: true, message: '类别编码不能为空', trigger: 'blur' }
        ],
        gbTypeName: [
          { required: true, message: '类别名称不能为空', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.getDicts('sys_common_status').then(response => {
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
      listDataDictType().then(response => {
        if (response.success) {
          const { data } = response
          const tree = {}
          tree.gbTypeName = '数据标准类别'
          tree.children = data
          this.typeOptions = []
          this.typeOptions.push(tree)
        }
      })
    },
    /** 节点单击事件 */
    handleNodeClick(data) {
      if (data.id) {
        this.queryParams.typeId = data.id
        this.getList()
      }
    },
    handleAddType() {
      this.dialog.open = true
      this.dialogForm = {
        gbTypeCode: '',
        gbTypeName: ''
      }
    },
    submitDialogForm() {
      this.$refs['dialogForm'].validate(valid => {
        if (valid) {
          if (this.dialogForm.id) {
            updateDataDictType(this.dialogForm).then(response => {
              if (response.success) {
                this.$message.success('保存成功')
                this.dialog.open = false
                this.getTree()
              } else {
                this.$message.error('保存失败')
              }
            }).catch(error => {
              this.$message.error(error.msg || '保存失败')
            })
          } else {
            addDataDictType(this.dialogForm).then(response => {
              if (response.success) {
                this.$message.success('保存成功')
                this.dialog.open = false
                this.getTree()
              } else {
                this.$message.error('保存失败')
              }
            }).catch(error => {
              this.$message.error(error.msg || '保存失败')
            })
          }
        }
      })
    },
    /** 树节点鼠标移入移出 */
    mouseenter(data) {
      this.$set(data, 'show', true)
    },
    mouseleave(data) {
      this.$set(data, 'show', false)
    },
    handleEditType(data) {
      this.dialog.open = true
      this.dialogForm = {
        id: data.id,
        gbTypeCode: data.gbTypeCode,
        gbTypeName: data.gbTypeName
      }
    },
    handleDelType(data) {
      this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delDataDictType(data.id).then(response => {
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
      pageDataDict(this.queryParams).then(response => {
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
        typeId: '',
        gbCode: '',
        gbName: ''
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
      if (!this.queryParams.typeId) {
        this.$message.warning('请先选择数据标准类别')
        return
      }
      this.showOptions.data.typeId = this.queryParams.typeId
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
    /** 刷新缓存 */
    handleCacheRefresh() {
      refreshDict().then(response => {
        if (response.success) {
          this.$message.success('刷新缓存成功')
        } else {
          this.$message.error('刷新缓存失败')
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delDataDict(row.id).then(response => {
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
