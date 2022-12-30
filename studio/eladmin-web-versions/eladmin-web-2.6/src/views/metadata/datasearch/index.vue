<template>
  <div class="app-container">
    <el-card class="box-card" shadow="always">
      <div class="body-wrapper">
        <div v-if="searchExecuting">
          <el-row>
            <el-col :span="12" :offset="6">
              <el-input v-model="keyword" placeholder="请输入内容">
                <el-select slot="prepend" v-model="type" placeholder="请选择" style="width: 100px;" @change="typeSelectChanged">
                  <el-option label="数据库" value="1" />
                  <el-option label="数据表" value="2" />
                  <el-option label="数据元" value="3" />
                </el-select>
                <el-button slot="append" :disabled="btnEnable" icon="el-icon-search" @click="search" />
              </el-input>
            </el-col>
          </el-row>
          <el-divider />
          <el-row>
            <el-col :span="24">
              <source-pane v-if="type === '1'" :data="dataList" />
              <table-pane v-if="type === '2'" :data="dataList" />
              <column-pane v-if="type === '3'" :data="dataList" />
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
        </div>
        <div v-else>
          <div class="search-container">
            <el-input v-model="keyword" placeholder="请输入内容">
              <el-select slot="prepend" v-model="type" placeholder="请选择" style="width: 100px;">
                <el-option label="数据库" value="1" />
                <el-option label="数据表" value="2" />
                <el-option label="数据元" value="3" />
              </el-select>
              <el-button slot="append" icon="el-icon-search" :disabled="btnEnable" @click="search" />
            </el-input>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import SourcePane from './SourcePane'
import TablePane from './TablePane'
import ColumnPane from './ColumnPane'

import { pageDataSource } from '@/api/metadata/datasource'
import { pageDataTable } from '@/api/metadata/datatable'
import { pageDataColumn } from '@/api/metadata/datacolumn'

export default {
  name: 'DataSearch',
  components: { SourcePane, TablePane, ColumnPane },
  data() {
    return {
      searchExecuting: false,
      type: '',
      keyword: '',
      btnEnable: true,
      dataList: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 20
      }
    }
  },
  computed: {
    listenChange() {
      const { type, keyword } = this
      return { type, keyword }
    }
  },
  watch: {
    listenChange(val) {
      if (val.type && val.keyword) {
        this.btnEnable = false
      } else {
        this.btnEnable = true
      }
    }
  },
  methods: {
    search() {
      this.searchExecuting = true
      const data = JSON.parse(JSON.stringify(this.queryParams))
      if (this.type === '1') {
        data.sourceName = this.keyword
        pageDataSource(data).then(response => {
          if (response.success) {
            const { data } = response
            this.dataList = data.data
            this.total = data.total
          }
        })
      } else if (this.type === '2') {
        data.tableName = this.keyword
        pageDataTable(data).then(response => {
          if (response.success) {
            const { data } = response
            this.dataList = data.data
            this.total = data.total
          }
        })
      } else if (this.type === '3') {
        data.columnName = this.keyword
        pageDataColumn(data).then(response => {
          if (response.success) {
            const { data } = response
            this.dataList = data.data
            this.total = data.total
          }
        })
      }
    },
    typeSelectChanged(val) {
      this.dataList = []
      this.total = 0
      this.queryParams.pageNum = 1
      this.queryParams.pageSize = 20
      console.log(val)
    },
    handleSizeChange(val) {
      this.queryParams.pageNum = 1
      this.queryParams.pageSize = val
      this.search()
    },
    handleCurrentChange(val) {
      this.queryParams.pageNum = val
      this.search()
    }
  }
}
</script>

<style lang="scss" scoped>
.el-card ::v-deep .el-card__body {
  height: calc(100vh - 170px);
}
.search-container {
  min-height: 100%;
  width: 100%;
  text-align: center;
  margin-top: 15%;
  .el-input {
    position: relative;
    width: 520px;
    max-width: 100%;
    margin: 0 auto;
  }
}
::v-deep .el-divider {
  margin: 10px 0;
}
</style>
