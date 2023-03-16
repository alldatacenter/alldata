<template>
  <div class="app-container">
    <el-form label-position="left" label-width="105px" :model="readerForm" :rules="rules">
      <el-form-item label="数据源" prop="datasourceId">
        <el-select v-model="readerForm.datasourceId" filterable @change="rDsChange">
          <el-option
            v-for="item in rDsList"
            :key="item.id"
            :label="item.datasourceName"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="表" prop="tableName">
        <el-select v-model="readerForm.tableName" filterable @change="rTbChange">
          <el-option v-for="item in rTbList" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="mode" prop="mode">
        <el-select v-model="readerForm.mode" placeholder="读取hbase的模式">
          <el-option v-for="item in modeTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="maxVersion">
        <el-input v-model="readerForm.maxVersion" placeholder="多版本模式下读取的版本数,取值只能为－1或者大于1的数字" style="width: 50%" />
      </el-form-item>
      <el-form-item label="range">
        <el-input v-model="readerForm.range.startRowkey" placeholder="startRowkey指定开始rowkey" style="width: 50%" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="readerForm.range.endRowkey" placeholder="endRowkey指定结束rowkey" style="width: 50%" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="readerForm.range.isBinaryRowkey" placeholder="转换方式">
          <el-option v-for="item in binaryRowkeyTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="字段">
        <el-checkbox
          v-model="readerForm.checkAll"
          :indeterminate="readerForm.isIndeterminate"
          @change="rHandleCheckAllChange"
        >全选</el-checkbox>
        <div style="margin: 15px 0;" />
        <el-checkbox-group v-model="readerForm.columns" @change="rHandleCheckedChange">
          <el-checkbox v-for="c in rColumnList" :key="c" :label="c">{{ c }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import * as dsQueryApi from '@/api/dts/metadata-query'
import { list as jdbcDsList } from '@/api/dts/datax-jdbcDatasource'
import Bus from '../busReader'

export default {
  name: 'HBaseReader',
  data() {
    return {
      jdbcDsQuery: {
        current: 1,
        size: 200
      },
      rDsList: [],
      rTbList: [],
      rColumnList: [],
      loading: false,
      active: 1,
      customFields: '',
      customType: '',
      customValue: '',
      dataSource: '',
      readerForm: {
        datasourceId: undefined,
        tableName: '',
        columns: [],
        checkAll: false,
        isIndeterminate: true,
        mode: '',
        maxVersion: '',
        range: {
          startRowkey: '',
          endRowkey: '',
          isBinaryRowkey: ''
        }
      },
      modeTypes: [
        { value: 'normal', label: 'normal' },
        { value: 'multiVersionFixedColumn', label: 'multiVersionFixedColumn' }
      ],
      binaryRowkeyTypes: [
        { value: 'true', label: '调用Bytes.toBytesBinary(rowkey)' },
        { value: 'false', label: '调用Bytes.toBytes(rowkey)' }
      ],
      rules: {
        mode: [{ required: true, message: 'this is required', trigger: 'blur' }],
        datasourceId: [{ required: true, message: 'this is required', trigger: 'blur' }],
        tableName: [{ required: true, message: 'this is required', trigger: 'blur' }]
      }
    }
  },
  watch: {
    'readerForm.datasourceId': function(oldVal, newVal) {
      this.getTables('hbaseReader')
    }
  },
  created() {
    this.getJdbcDs()
  },
  methods: {
    // 获取可用数据源
    getJdbcDs() {
      this.loading = true
      jdbcDsList(this.jdbcDsQuery).then(response => {
        const { records } = response.data
        this.rDsList = records
        this.loading = false
      })
    },
    // 获取表名
    getTables(type) {
      if (type === 'hbaseReader') {
        const obj = {
          datasourceId: this.readerForm.datasourceId
        }
        // 组装
        dsQueryApi.getTables(obj).then(response => {
          this.rTbList = response
        })
      }
    },
    // reader 数据源切换
    rDsChange(e) {
      // 清空
      this.readerForm.tableName = ''
      this.readerForm.datasourceId = e
      this.rDsList.find((item) => {
        if (item.id === e) {
          this.dataSource = item.datasource
        }
      })
      Bus.dataSourceId = e
      this.$emit('selectDataSource', this.dataSource)
      // 获取可用表
      this.getTables('reader')
    },
    getTableColumns() {
      const obj = {
        datasourceId: this.readerForm.datasourceId,
        tableName: this.readerForm.tableName
      }
      dsQueryApi.getColumns(obj).then(response => {
        this.rColumnList = response
        this.readerForm.columns = response
        this.readerForm.checkAll = true
        this.readerForm.isIndeterminate = false
      })
    },
    getColumnsByQuerySql() {
      const obj = {
        datasourceId: this.readerForm.datasourceId,
        querySql: this.readerForm.querySql
      }
      dsQueryApi.getColumnsByQuerySql(obj).then(response => {
        this.rColumnList = response
        this.readerForm.columns = response
        this.readerForm.checkAll = true
        this.readerForm.isIndeterminate = false
      })
    },
    // 获取表字段
    getColumns(type) {
      if (type === 'reader') {
        this.getTableColumns()
      }
    },
    // 表切换
    rTbChange(t) {
      this.readerForm.tableName = t
      this.rColumnList = []
      this.readerForm.columns = []
      this.getColumns('reader')
    },
    rHandleCheckAllChange(val) {
      this.readerForm.columns = val ? this.rColumnList : []
      this.readerForm.isIndeterminate = false
    },
    rHandleCheckedChange(value) {
      const checkedCount = value.length
      this.readerForm.checkAll = checkedCount === this.rColumnList.length
      this.readerForm.isIndeterminate = checkedCount > 0 && checkedCount < this.rColumnList.length
    },
    getData() {
      if (Bus.dataSourceId) {
        this.readerForm.datasourceId = Bus.dataSourceId
      }
      return this.readerForm
    }
  }
}
</script>
