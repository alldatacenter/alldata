<template>
  <el-card class="box-card" shadow="always">
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
              v-hasPerm="['quality:job:pause']"
              v-if="scope.row.status === '1'"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handlePause(scope.row)"
            >任务暂停</el-button>
            <el-button
              v-hasPerm="['quality:job:resume']"
              v-if="scope.row.status === '0'"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleResume(scope.row)"
            >任务恢复</el-button>
            <el-button
              v-hasPerm="['quality:job:run']"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleRun(scope.row)"
            >立即执行</el-button>
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
</template>

<script>
import { pageCheckJob, pauseCheckJob, resumeCheckJob, runCheckJob } from '@/api/quality/checkjob'

export default {
  name: 'CheckJobList',
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
        { prop: 'jobName', label: '任务名称', show: true },
        { prop: 'beanName', label: 'bean名称', show: true },
        { prop: 'methodName', label: '方法名称', show: true },
        { prop: 'methodParams', label: '方法参数', show: true },
        { prop: 'cronExpression', label: 'cron表达式', show: true },
        {
          prop: 'status',
          label: '状态',
          show: true,
          formatter: this.statusFormatter
        }
      ],
      // 状态数据字典
      statusOptions: [],
      // 数据集表格数据
      tableDataList: [],
      // 总数据条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20
      }
    }
  },
  created() {
    this.getDicts('sys_job_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
    this.getList()
  },
  methods: {
    /** 查询数据集列表 */
    getList() {
      this.loading = true
      pageCheckJob(this.queryParams).then(response => {
        this.loading = false
        if (response.success) {
          const { data } = response
          this.tableDataList = data.data
          this.total = data.total
        }
      })
    },
    handlePause(row) {
      this.$confirm('是否暂停该任务', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        pauseCheckJob(row.id).then(response => {
          if (response.success) {
            this.$message.success('任务暂停成功')
            this.getList()
          }
        })
      }).catch(() => {
      })
    },
    handleResume(row) {
      this.$confirm('是否恢复该任务', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        resumeCheckJob(row.id).then(response => {
          if (response.success) {
            this.$message.success('任务恢复成功')
            this.getList()
          }
        })
      }).catch(() => {
      })
    },
    /* zrx add*/
    handleRun(row) {
      this.$confirm('是否执行该任务', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        runCheckJob(row.id).then(response => {
          if (response.success) {
            this.$message.success('任务执行成功，在任务日志可查看执行情况')
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
</style>
