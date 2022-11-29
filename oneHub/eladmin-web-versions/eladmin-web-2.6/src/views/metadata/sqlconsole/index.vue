<template>
  <div class="app-container">
    <el-card class="box-card" shadow="always">
      <el-row>
        <el-col :span="24">
          <el-form :inline="true" class="demo-form-inline">
            <el-form-item label="数据源">
              <el-select v-model="sqlDataSource" placeholder="请选择数据源">
                <el-option
                  v-for="source in sourceOptions"
                  :key="source.id"
                  :label="source.sourceName"
                  :value="source.id"
                  :disabled="source.status === '0'"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button size="mini" round :disabled="sqlExecuting" @click="runData">运行</el-button>
              <el-button size="mini" round :disabled="!sqlExecuting" @click="stopData">停止</el-button>
              <el-button size="mini" round :disabled="sqlExecuting" @click="formaterSql">格式化</el-button>
              <el-button size="mini" round :disabled="sqlExecuting" @click="refreshData">重置</el-button>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <sql-editor
            ref="sqleditor"
            :value="sqlText"
            style="height: 300px; margin: 20px 0;"
            @changeTextarea="changeTextarea($event)"
          />
        </el-col>
      </el-row>
      <el-row>
        <el-col>
          <div v-if="sqlExecuting" v-loading="sqlExecuting">数据加载中...</div>
          <div v-else>
            <div v-if="sqlConsole.length > 0">
              <el-tabs v-model="activeTabName" type="border-card">
                <el-tab-pane label="信息" name="table0">
                  <pre>{{ executeResultInfo }}</pre>
                </el-tab-pane>
                <el-tab-pane v-for="(item,index) in sqlConsole" :key="(index+1)" :name="'table'+(index+1)" :label="'结果'+(index+1)">
                  <el-table
                    :data="item.dataList"
                    stripe
                    border
                    :max-height="300"
                    style="width: 100%; margin: 15px 0;"
                  >
                    <el-table-column label="序号" width="55" align="center">
                      <template slot-scope="scope">
                        <span>{{ scope.$index + 1 }}</span>
                      </template>
                    </el-table-column>
                    <template v-for="(column, index) in item.columnList">
                      <el-table-column
                        :key="index"
                        :prop="column"
                        :label="column"
                        align="center"
                        show-overflow-tooltip
                      />
                    </template>
                  </el-table>
                </el-tab-pane>
              </el-tabs>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import sqlFormatter from 'sql-formatter'
import SqlEditor from '@/components/SqlEditor'
import { listDataSource } from '@/api/metadata/datasource'
import { runSql, stopSql } from '@/api/metadata/sqlconsole'

export default {
  name: 'SqlConsole',
  components: {
    SqlEditor
  },
  data() {
    return {
      // 数据源数据字典
      sourceOptions: [],
      sqlDataSource: undefined,
      sqlText: undefined,
      sqlExecuting: false,
      activeTabName: 'table0',
      sqlExecutorId: undefined,
      sqlConsole: [],
      executeResultInfo: undefined
    }
  },
  created() {
    this.getDataSourceList()
  },
  methods: {
    getDataSourceList() {
      listDataSource().then(response => {
        if (response.success) {
          this.sourceOptions = response.data
        }
      })
    },
    runData() {
      if (!this.sqlDataSource) {
        this.$message.error('数据源不能为空')
        return
      }
      if (!this.sqlText) {
        this.$message.error('查询SQL不能为空')
        return
      }
      this.sqlExecuting = true
      this.sqlExecutorId = (new Date()).getTime() + Math.ceil(Math.random() * 1000)
      this.sqlConsole = []
      const data = {}
      data.sqlKey = this.sqlExecutorId
      data.sourceId = this.sqlDataSource
      data.sqlText = this.sqlText
      runSql(data).then(response => {
        if (response.success) {
          const { data } = response
          let resultStr = ''
          for (let i = 0; i < data.length; i++) {
            const item = data[i]
            resultStr += item.sql
            resultStr += '\n> 状态：' + ((item.success) ? '成功' : '失败')
            if (item.count && item.count >= 0) {
              resultStr += '\n> 影响行数：' + item.count
            }
            resultStr += '\n> 耗时：' + (item.time || 0) / 1000 + 's'
            resultStr += '\n\n'
          }
          this.executeResultInfo = resultStr
          this.sqlConsole = data
        }
        this.sqlExecuting = false
      })
    },
    stopData() {
      const data = {}
      data.sqlKey = this.sqlExecutorId
      stopSql(data).then(response => {
        if (response.success) {
          this.$message.success('停止成功')
        }
        this.sqlExecuting = false
      })
    },
    changeTextarea(val) {
      this.sqlText = val
    },
    formaterSql() {
      if (!this.sqlText) {
        return
      }
      this.$refs.sqleditor.editor.setValue(sqlFormatter.format(this.$refs.sqleditor.editor.getValue()))
    },
    refreshData() {
      if (!this.sqlText) {
        return
      }
      this.sqlExecuting = false
      this.activeTabName = 'table0'
      this.sqlExecutorId = undefined
      this.sqlText = undefined
      this.$refs.sqleditor.editor.setValue('')
      this.sqlConsole = []
      this.executeResultInfo = undefined
    }
  }
}
</script>

<style lang="scss" scoped>
.el-card ::v-deep .el-card__body {
  height: calc(100vh - 170px);
}
</style>
