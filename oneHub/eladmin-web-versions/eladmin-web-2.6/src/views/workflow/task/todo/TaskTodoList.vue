<template>
  <el-card class="box-card" shadow="always">
    <el-form ref="queryForm" :model="queryParams" :inline="true">
      <el-form-item label="业务类型" prop="businessCode">
        <el-input
          v-model="queryParams.businessCode"
          placeholder="请输入业务类型"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="业务名称" prop="businessName">
        <el-input
          v-model="queryParams.businessName"
          placeholder="请输入业务名称"
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
              v-hasPerm="['workflow:task:caim']"
              v-if="scope.row.assignee === undefined || scope.row.assignee === null || scope.row.assignee === ''"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleClaim(scope.row)"
            >签收</el-button>
            <el-button
              v-hasPerm="['workflow:task:unclaim']"
              v-if="scope.row.assignee && scope.row.assignee === user.id"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleUnClaim(scope.row)"
            >反签收</el-button>
            <el-button
              v-hasPerm="['workflow:task:delegate']"
              v-if="scope.row.assignee && scope.row.assignee === user.id"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleDelegate(scope.row)"
            >委派</el-button>
            <el-button
              v-hasPerm="['workflow:task:assignee']"
              v-if="scope.row.assignee && scope.row.assignee === user.id"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleAssignee(scope.row)"
            >转办</el-button>
            <el-button
              v-hasPerm="['workflow:task:exam']"
              v-if="scope.row.assignee && scope.row.assignee === user.id"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleTask(scope.row)"
            >审核</el-button>
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

    <!-- 任务审核对话框 -->
    <handle-task v-if="dialogHandleTaskVisible" :visible.sync="dialogHandleTaskVisible" :task="currentTask" @handleTaskFinished="getList"></handle-task>
    <handle-user v-if="dialogHandleUserVisible" :visible.sync="dialogHandleUserVisible" :task="currentTask" :action="taskAction" @handleTaskUserFinished="getList"></handle-user>
  </el-card>
</template>

<script>
import { pageTodoTask, executeTask } from '@/api/workflow/task'
import { mapGetters } from 'vuex'
import HandleTask from '../components/HandleTask'
import HandleUser from '../components/HandleUser'

export default {
  name: 'TaskTodoList',
  components: { HandleTask, HandleUser },
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
        { prop: 'name', label: '节点名称', show: true },
        { prop: 'businessName', label: '业务名称', show: true },
        { prop: 'createTime', label: '创建时间', show: true }
      ],
      // 表格数据
      tableDataList: [],
      // 总数据条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        businessCode: '',
        businessName: ''
      },
      dialogHandleTaskVisible: false,
      currentTask: {},
      dialogHandleUserVisible: false,
      taskAction: ''
    }
  },
  created() {
    this.getList()
  },
  computed: {
    ...mapGetters([
      'user'
    ])
  },
  methods: {
    /** 查询数据集列表 */
    getList() {
      this.loading = true
      pageTodoTask(this.queryParams).then(response => {
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
        businessCode: '',
        businessName: ''
      }
      this.handleQuery()
    },
    handleClaim(row) {
      this.$confirm('签收任务？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const data = {
          action: 'claim',
          processInstanceId: row.processInstanceId,
          taskId: row.id,
          userId: '',
          message: '',
          variables: { approved: true }
        }
        executeTask(data).then(response => {
          if (response.success) {
            this.$message.success('任务签收成功')
            this.getList()
          }
        })
      }).catch(() => {
      })
    },
    handleUnClaim(row) {
      this.$confirm('反签收任务？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const data = {
          action: 'unclaim',
          processInstanceId: row.processInstanceId,
          taskId: row.id,
          userId: '',
          message: '',
          variables: { approved: true }
        }
        executeTask(data).then(response => {
          if (response.success) {
            this.$message.success('任务反签收成功')
            this.getList()
          }
        })
      }).catch(() => {
      })
    },
    handleDelegate(row) {
      this.taskAction = 'delegate'
      this.currentTask = Object.assign({}, row)
      this.dialogHandleUserVisible = true
    },
    handleAssignee(row) {
      this.taskAction = 'assignee'
      this.currentTask = Object.assign({}, row)
      this.dialogHandleUserVisible = true
    },
    handleTask(row) {
      this.currentTask = Object.assign({}, row)
      this.dialogHandleTaskVisible = true
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
