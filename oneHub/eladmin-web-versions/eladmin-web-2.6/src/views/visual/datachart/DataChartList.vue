<template>
  <div class="chart-container">
    <el-row>
      <el-col>
        <el-pagination
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :current-page.sync="queryParams.pageNum"
          :page-size.sync="queryParams.pageSize"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </el-col>
    </el-row>
    <el-divider />
    <el-row :gutter="20">
      <el-col class="box-card-col" v-hasPerm="['visual:chart:add']">
        <el-card :body-style="{ padding: '0px' }" class="box-card-item">
          <div class="box-card-item-add" @click="handleAdd">
            <div class="icon-block">
              <i class="el-icon-plus" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col v-for="(item, index) in tableDataList" :key="item.id" class="box-card-col">
        <el-card :body-style="{ padding: '0px' }" class="box-card-item">
          <div class="box-card-item-body" @mouseenter="mouseEnter(item)" @mouseleave="mouseLeave(item)">
            <el-image :src="item.chartThumbnail ? item.chartThumbnail : ''">
              <div slot="error" class="image-slot">
                <i class="el-icon-picture-outline" />
              </div>
            </el-image>
            <div class="box-card-item-edit" :style="{display: (item.show ? 'block' : 'none')}">
              <el-button v-hasPerm="['visual:chart:build']" type="primary" @click="handleConfig(item)">编辑</el-button>
            </div>
          </div>
          <div class="box-card-item-footer">
            <span class="box-card-item-footer-text">{{ item.chartName }}</span>
            <div class="clearfix">
              <i class="el-icon-edit-outline" v-hasPerm="['visual:chart:edit']" @click="handleEdit(item)" />
              <i class="el-icon-delete" v-hasPerm="['visual:chart:remove']" @click="handleDelete(item)" />
              <i class="el-icon-copy-document" v-hasPerm="['visual:chart:copy']" @click="handleCopy(item)" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <chart-form v-if="dialogFormVisible" :visible.sync="dialogFormVisible" :data="currentChart" @handleChartFormFinished="getList" />
  </div>
</template>

<script>
import { pageDataChart, delDataChart, copyDataChart } from '@/api/visual/datachart'
import ChartForm from './components/ChartForm'

export default {
  name: 'DataChartList',
  components: { ChartForm },
  data() {
    return {
      // 表格数据
      tableDataList: [],
      // 总数据条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10
      },
      dialogFormVisible: false,
      currentChart: {}
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询数据列表 */
    getList() {
      pageDataChart(this.queryParams).then(response => {
        if (response.success) {
          const { data } = response
          this.tableDataList = data.data
          this.total = data.total
        }
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
    mouseEnter(data) {
      this.$set(data, 'show', true)
    },
    mouseLeave(data) {
      this.$set(data, 'show', false)
    },
    handleAdd() {
      this.dialogFormVisible = true
      this.currentChart = {}
    },
    handleConfig(data) {
      const route = this.$router.resolve({ path: `/visual/chart/build/${data.id}` })
      window.open(route.href, '_blank')
    },
    handleEdit(data) {
      this.dialogFormVisible = true
      this.currentChart = Object.assign({}, data)
    },
    handleDelete(data) {
      this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delDataChart(data.id).then(response => {
          if (response.success) {
            this.$message.success('删除成功')
            this.getList()
          }
        })
      }).catch(() => {
      })
    },
    handleCopy(data) {
      this.$confirm('确认拷贝当前图表, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        copyDataChart(data.id).then(response => {
          if (response.success) {
            this.$message.success('拷贝成功')
            this.getList()
          }
        })
      }).catch(() => {
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.el-pagination {
  text-align: center;
}
.box-card-col {
  width: 260px;
  height: 185px;
  padding-left: 0px;
  padding-right: 0px;
  margin-right: 10px;
  margin-bottom: 10px;
  .box-card-item {
    width: 260px;
    height: 185px;
    .box-card-item-body {
      display: flex;
      justify-content: center;
      align-items: center;
      .box-card-item-edit {
        width: 260px;
        height: 150px;
        text-align: center;
        position: absolute;
        background: rgba(4, 11, 28, 0.7);
        opacity: 0.8;
        button {
          margin-top: 55px;
        }
      }
    }
    .el-image{
      width: 260px;
      height: 150px;
      display: block;
      ::v-deep .image-slot {
        display: flex;
        justify-content: center;
        align-items: center;
        width: 100%;
        height: 100%;
        background: #f5f7fa;
        color: #909399;
      }
    }
    .box-card-item-add {
      width: 260px;
      height: 185px;
      display: flex;
      justify-content: center;
      align-items: center;
      cursor: pointer;
      i {
        font-size: 30px;
      }
    }
    .box-card-item-footer {
      padding: 8px 5px;
      background-color: #dcdcdc;
      display: flex;
      justify-content: space-between;
      .box-card-item-footer-text {
        width: 150px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      i {
        margin-right: 5px;
        cursor: pointer;
      }
    }
  }
}
</style>
