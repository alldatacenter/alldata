<template>
  <el-card class="box-card" shadow="always">
    <el-form ref="queryForm" :model="queryParams" :inline="true">
      <el-form-item label="流程实例名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入流程实例名称"
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
              v-hasPerm="['workflow:instance:track']"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleTrack(scope.row)"
            >流程追踪</el-button>
            <el-button
              v-hasPerm="['workflow:instance:running:activate']"
              v-if="scope.row.suspensionState === 2"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleActivate(scope.row)"
            >激活</el-button>
            <el-button
              v-hasPerm="['workflow:instance:running:suspend']"
              v-if="scope.row.suspensionState === 1"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleSuspend(scope.row)"
            >挂起</el-button>
            <el-button
              v-hasPerm="['workflow:instance:running:remove']"
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

    <!-- 流程图对话框 -->
    <flow-image v-if="dialogFlowImageVisible" :visible.sync="dialogFlowImageVisible" :processInstanceId="currentProcessInstanceId"></flow-image>
  </el-card>
</template>

<script>
import { pageRunningInstance, delInstance, activateInstance, suspendInstance } from '@/api/workflow/instance'
import FlowImage from '../components/FlowImage'

export default {
  name: 'InstanceList',
  components: { FlowImage },
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
        { prop: 'processDefinitionId', label: '流程定义ID', show: true },
        { prop: 'processDefinitionName', label: '流程定义名称', show: true },
        { prop: 'name', label: '流程实列名称', show: true },
        {
          prop: 'suspensionState',
          label: '状态',
          show: true,
          formatter: this.statusFormatter
        },
        { prop: 'startTime', label: '创建时间', show: true }
      ],
      // 表格数据
      tableDataList: [],
      // 总数据条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        name: ''
      },
      // 流程图
      dialogFlowImageVisible: false,
      currentProcessInstanceId: ''
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询数据集列表 */
    getList() {
      this.loading = true
      pageRunningInstance(this.queryParams).then(response => {
        this.loading = false
        if (response.success) {
          const { data } = response
          this.tableDataList = data.data
          this.total = data.total
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
        name: ''
      }
      this.handleQuery()
    },
    handleTrack(row) {
      this.currentProcessInstanceId = row.id
      this.dialogFlowImageVisible = true
    },
    handleActivate(row) {
      this.$confirm('激活流程实例？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        activateInstance(row.id).then(response => {
          if (response.success) {
            this.$message.success('激活流程实例成功')
            this.getList()
          }
        })
      }).catch(() => {
      })
    },
    handleSuspend(row) {
      this.$confirm('挂起流程实例？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        suspendInstance(row.id).then(response => {
          if (response.success) {
            this.$message.success('挂起流程实例成功')
            this.getList()
          }
        })
      }).catch(() => {
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delInstance(row.id).then(response => {
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
</style>
