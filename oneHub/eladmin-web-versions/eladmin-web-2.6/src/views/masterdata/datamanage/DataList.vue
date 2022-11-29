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
          :expand-on-click-node="false"
          @node-click="handleNodeClick"
        >
          <span slot-scope="{ node, data }" class="custom-tree-node">
            <i v-if="node.level === 1" class="iconfont icon-zuzhi tree-folder" />
            <span :class="data.isSync === '0' ? 'no-select' : ''">{{ node.label }}</span>
          </span>
        </el-tree>
      </el-card>
    </el-col>
    <el-col :span="18">
      <el-card class="box-card" shadow="always">
        <div v-if="tableName">
          <el-form ref="queryForm" class="queryForm" :model="queryParams" size="mini" :inline="true">
            <el-form-item v-for="(item, index) in queryParams.conditions" :label="item.columnName" :prop="item.column" :key="index">
              <template v-if="item.dictList && item.dictList.length > 0">
                <el-select v-model.trim="item.value" clearable placeholder="请选择">
                  <el-option
                    v-for="item in item.dictList"
                    :key="item.id"
                    :label="item.label"
                    :value="item.value">
                  </el-option>
                </el-select>
              </template>
              <template v-else>
                <template v-if="item.queryType === 'between'">
                  <template v-if="item.htmlType === 'number'">
                    <el-input-number
                      v-model.trim="item.leftValue"
                      clearable
                      :controls="false"
                      :precision="parseInt(item.columnScale)"
                    ></el-input-number>
                    ~
                    <el-input-number
                      v-model.trim="item.rightValue"
                      clearable
                      :controls="false"
                      :precision="parseInt(item.columnScale)"
                    ></el-input-number>
                  </template>
                  <template v-if="item.htmlType === 'datetime'">
                    <template v-if="item.columnType === 'date'">
                      <el-date-picker
                        v-model.trim="item.leftValue"
                        clearable
                        format="yyyy-MM-dd"
                        value-format="yyyy-MM-dd"
                        type="date"
                        placeholder="选择日期"
                      ></el-date-picker>
                      ~
                      <el-date-picker
                        v-model.trim="item.rightValue"
                        clearable
                        format="yyyy-MM-dd"
                        value-format="yyyy-MM-dd"
                        type="date"
                        placeholder="选择日期"
                      ></el-date-picker>
                    </template>
                    <template v-if="item.columnType === 'time'">
                      <el-time-picker
                        v-model.trim="item.leftValue"
                        clearable
                        format="HH:mm:ss"
                        value-format="HH:mm:ss"
                        placeholder="选择时间点">
                      </el-time-picker>
                      ~
                      <el-time-picker
                        v-model.trim="item.rightValue"
                        clearable
                        format="HH:mm:ss"
                        value-format="HH:mm:ss"
                        placeholder="选择时间点">
                      </el-time-picker>
                    </template>
                    <template v-if="item.columnType === 'year'">
                      <el-date-picker
                        v-model.trim="item.leftValue"
                        clearable
                        format="yyyy"
                        value-format="yyyy"
                        type="year"
                        placeholder="选择年份"
                      ></el-date-picker>
                      ~
                      <el-date-picker
                        v-model.trim="item.rightValue"
                        clearable
                        format="yyyy"
                        value-format="yyyy"
                        type="year"
                        placeholder="选择年份"
                      ></el-date-picker>
                    </template>
                    <template v-if="item.columnType === 'datetime'">
                      <el-date-picker
                        v-model.trim="item.leftValue"
                        clearable
                        format="yyyy-MM-dd HH:mm:ss"
                        value-format="yyyy-MM-dd HH:mm:ss"
                        type="datetime"
                        placeholder="选择日期时间"
                      ></el-date-picker>
                      ~
                      <el-date-picker
                        v-model.trim="item.rightValue"
                        clearable
                        format="yyyy-MM-dd HH:mm:ss"
                        value-format="yyyy-MM-dd HH:mm:ss"
                        type="datetime"
                        placeholder="选择日期时间"
                      ></el-date-picker>
                    </template>
                  </template>
                  <template v-else>
                    <el-input
                      v-model.trim="item.leftValue"
                      clearable
                    ></el-input>
                    ~
                    <el-input
                      v-model.trim="item.rightValue"
                      clearable
                    ></el-input>
                  </template>
                </template>
                <template v-else>
                  <template v-if="item.htmlType === 'number'">
                    <el-input-number
                      v-model.trim="item.value"
                      clearable
                      :controls="false"
                      :precision="parseInt(item.columnScale)"
                    ></el-input-number>
                  </template>
                  <template v-if="item.htmlType === 'datetime'">
                    <template v-if="item.columnType === 'date'">
                      <el-date-picker
                        v-model.trim="item.value"
                        clearable
                        format="yyyy-MM-dd"
                        value-format="yyyy-MM-dd"
                        type="date"
                        placeholder="选择日期"
                      ></el-date-picker>
                    </template>
                    <template v-if="item.columnType === 'time'">
                      <el-time-picker
                        v-model.trim="item.value"
                        clearable
                        format="HH:mm:ss"
                        value-format="HH:mm:ss"
                        placeholder="选择时间点">
                      </el-time-picker>
                    </template>
                    <template v-if="item.columnType === 'year'">
                      <el-date-picker
                        v-model.trim="item.value"
                        clearable
                        format="yyyy"
                        value-format="yyyy"
                        type="year"
                        placeholder="选择年份"
                      ></el-date-picker>
                    </template>
                    <template v-if="item.columnType === 'datetime'">
                      <el-date-picker
                        v-model.trim="item.value"
                        clearable
                        format="yyyy-MM-dd HH:mm:ss"
                        value-format="yyyy-MM-dd HH:mm:ss"
                        type="datetime"
                        placeholder="选择日期时间"
                      ></el-date-picker>
                    </template>
                  </template>
                  <template v-else>
                    <el-input
                      v-model.trim="item.value"
                      clearable
                    ></el-input>
                  </template>
                </template>
              </template>
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
                  v-hasPerm="['masterdata:data:add']"
                  type="primary"
                  icon="el-icon-plus"
                  size="mini"
                  @click="handleAdd"
                >新增</el-button>
              </el-button-group>
            </el-col>
          </el-row>

          <el-table
            v-loading="loading"
            :data="tableDataList"
            border
            tooltip-effect="dark"
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
                :key="index"
                :prop="item.prop"
                :label="item.label"
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
                    v-hasPerm="['masterdata:data:edit']"
                    size="mini"
                    type="text"
                    icon="el-icon-edit-outline"
                    @click="handleEdit(scope.row)"
                  >修改</el-button>
                  <el-button
                    v-hasPerm="['masterdata:data:detail']"
                    size="mini"
                    type="text"
                    icon="el-icon-view"
                    @click="handleDetail(scope.row)"
                  >详情</el-button>
                  <el-button
                    v-hasPerm="['masterdata:data:remove']"
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
        </div>
        <div v-else class="empty-info">从左侧选择数据模型</div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script>
import { listDataModel, getTableParam } from '@/api/masterdata/datamodel'
import { pageData, delData } from '@/api/masterdata/datamanage'

export default {
  name: 'DataList',
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
      // 总数据条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        tableName: '',
        columns: [],
        conditions: []
      },
      // 左侧树
      treeOptions: [],
      defaultProps: {
        children: 'children',
        label: 'modelName'
      },
      // 数据模型ID
      modelId: '',
      tableName: '',
      // 表格头
      tableColumns: [],
      // 表格数据
      tableDataList: [],
      // 查询参数
      tableParamList: []
    }
  },
  created() {
    this.getTree()
  },
  methods: {
    /** 查询树结构 */
    getTree() {
      listDataModel().then(response => {
        if (response.success) {
          const data = response.data
          data.forEach((item, index) => {
            if (item.isSync === '0') {
              item.disabled = true
            }
          })
          const tree = {}
          tree.modelName = '数据模型'
          tree.children = data
          this.treeOptions = []
          this.treeOptions.push(tree)
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
        tableName: JSON.parse(JSON.stringify(this.tableName)),
        columns: JSON.parse(JSON.stringify(this.tableColumns)).map(column => column.prop),
        conditions: JSON.parse(JSON.stringify(this.tableParamList))
      }
      this.handleQuery()
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.showOptions.data = {}
      this.showOptions.data.modelId = this.modelId
      this.showOptions.showList = false
      this.showOptions.showAdd = true
      this.showOptions.showEdit = false
      this.showOptions.showDetail = false
      this.$emit('showCard', this.showOptions)
    },
    /** 修改按钮操作 */
    handleEdit(row) {
      this.showOptions.data.modelId = this.modelId
      this.showOptions.data.id = row.id
      this.showOptions.showList = false
      this.showOptions.showAdd = false
      this.showOptions.showEdit = true
      this.showOptions.showDetail = false
      this.$emit('showCard', this.showOptions)
    },
    /** 详情按钮操作 */
    handleDetail(row) {
      this.showOptions.data.modelId = this.modelId
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
        delData({ id: row.id, tableName: this.tableName, datas: { id: row.id }}).then(response => {
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
    /** 节点单击事件 */
    handleNodeClick(data) {
      if (data.id && data.id !== this.modelId && data.isSync === '1') {
        getTableParam(data.id).then(response => {
          if (response.success) {
            const { data } = response
            this.modelId = data.modelId
            this.tableName = data.tableName
            this.tableColumns = data.columnList
            this.tableParamList = data.queryList
            this.queryParams.tableName = JSON.parse(JSON.stringify(data.tableName))
            this.queryParams.columns = JSON.parse(JSON.stringify(data.columnList)).map(column => column.prop)
            this.queryParams.conditions = JSON.parse(JSON.stringify(data.queryList))
            this.$nextTick(() => {
              // 获取表格数据
              this.getList()
            })
          }
        })
      }
    },
    /** 查询列表 */
    getList() {
      this.loading = true
      pageData(this.queryParams).then(response => {
        this.loading = false
        if (response.success) {
          const { data } = response
          this.tableDataList = data.data
          this.total = data.total
        }
      })
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
    .tree-folder {
      margin-right: 5px;
      color: rgb(246 207 7);
    }
    .no-select {
      text-decoration: line-through dotted red;
    }
  }
}
.empty-info{
  text-align: center;
  font-size: 18px;
  margin-top: 20%;
}
.queryForm{
  .el-input {
    width: unset;
  }
}
</style>
