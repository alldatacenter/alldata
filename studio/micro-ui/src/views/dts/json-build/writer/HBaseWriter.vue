<template>
  <div>
    <el-form label-position="left" label-width="115px" :model="writerForm" :rules="rules">
      <el-form-item label="数据源" prop="datasourceId">
        <el-select
          v-model="writerForm.datasourceId"
          filterable
          @change="wDsChange"
        >
          <el-option
            v-for="item in wDsList"
            :key="item.id"
            :label="item.datasourceName"
            :value="item.id"
          />
        </el-select>
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="表" prop="fromTableName">
            <el-select
              v-model="fromTableName"
              :disabled="writerForm.ifCreateTable"
              filterable
              @change="wTbChange"
            >
              <el-option
                v-for="item in wTbList"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="rowkeyColumn" prop="rowkeyColumn">
        <el-input v-model="writerForm.rowkeyColumn" :autosize="{ minRows: 5, maxRows: 20}" type="textarea" style="width: 42%" />
      </el-form-item>
      <el-form-item label="versionColumn">
        <el-input v-model="writerForm.versionColumn.index" placeholder="index指定对应reader端column的索引" style="width: 42%" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="writerForm.versionColumn.value" placeholder="value指定时间的值,long值" style="width: 42%" />
      </el-form-item>
      <el-form-item label="nullMode">
        <el-select v-model="writerForm.nullMode" placeholder="null值转换方式">
          <el-option v-for="item in nullModeTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="字段">
        <el-checkbox v-model="writerForm.checkAll" :indeterminate="writerForm.isIndeterminate" @change="wHandleCheckAllChange">全选</el-checkbox>
        <div style="margin: 15px 0;" />
        <el-checkbox-group v-model="writerForm.columns" @change="wHandleCheckedChange">
          <el-checkbox v-for="c in fromColumnList" :key="c" :label="c">{{ c }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import * as dsQueryApi from '@/api/dts/metadata-query'
import { list as jdbcDsList } from '@/api/dts/datax-jdbcDatasource'
import Bus from '../busWriter'
export default {
  name: 'HBaseWriter',
  data() {
    const checkJson = (rule, value, callback) => {
      if (!value) {
        callback(new Error('不能为空'))
      }
      if (typeof value === 'string') {
        try {
          var obj = JSON.parse(value)
          if (typeof obj !== 'object' || !obj) {
            callback(new Error('JSON格式错误'))
          }
          if (!(obj instanceof Array)) {
            callback(new Error('JSON必须为数组'))
          }
        } catch (e) {
          callback(new Error('JSON格式错误'))
        }
      }
    }
    return {
      jdbcDsQuery: {
        current: 1,
        size: 200
      },
      wDsList: [],
      fromTableName: '',
      fromColumnList: [],
      wTbList: [],
      dataSource: '',
      writerForm: {
        datasourceId: undefined,
        tableName: '',
        columns: [],
        checkAll: false,
        isIndeterminate: true,
        ifCreateTable: false,
        mode: 'normal',
        rowkeyColumn: '[{\n' +
          '\t"index": 0,\n' +
          '\t"type": "string"\n' +
          '}]',
        versionColumn: {
          index: '',
          value: ''
        },
        nullMode: ''
      },
      nullModeTypes: [
        { value: 'skip', label: '不向hbase写这列' },
        { value: 'empty', label: '写入new byte [0]' }
      ],
      rules: {
        mode: [{ required: true, message: 'this is required', trigger: 'blur' }],
        datasourceId: [{ required: true, message: 'this is required', trigger: 'blur' }],
        fromTableName: [{ required: true, message: 'this is required', trigger: 'blur' }],
        rowkeyColumn: [{ required: true, trigger: 'blur', validator: checkJson }]
      },
      readerForm: this.getReaderData()
    }
  },
  watch: {
    'writerForm.datasourceId': function(oldVal, newVal) {
      this.getTables('hbaseWriter')
    }
  },
  created() {
    this.getJdbcDs()
  },
  methods: {
    // 获取可用数据源
    getJdbcDs(type) {
      this.loading = true
      jdbcDsList(this.jdbcDsQuery).then(response => {
        const { records } = response
        this.wDsList = records
        this.loading = false
      })
    },
    // 获取表名
    getTables(type) {
      if (type === 'hbaseWriter') {
        const obj = {
          datasourceId: this.writerForm.datasourceId
        }
        // 组装
        dsQueryApi.getTables(obj).then(response => {
          this.wTbList = response
        })
      }
    },
    wDsChange(e) {
      // 清空
      this.writerForm.tableName = ''
      this.writerForm.datasourceId = e
      this.wDsList.find((item) => {
        if (item.id === e) {
          this.dataSource = item.datasource
        }
      })
      Bus.dataSourceId = e
      this.$emit('selectDataSource', this.dataSource)
      // 获取可用表
      this.getTables()
    },
    // 获取表字段
    getColumns() {
      const obj = {
        datasourceId: this.writerForm.datasourceId,
        tableName: this.writerForm.tableName
      }
      dsQueryApi.getColumns(obj).then(response => {
        this.fromColumnList = response
        this.writerForm.columns = response
        this.writerForm.checkAll = true
        this.writerForm.isIndeterminate = false
      })
    },
    // 表切换
    wTbChange(t) {
      this.writerForm.tableName = t
      this.fromColumnList = []
      this.writerForm.columns = []
      this.getColumns('writer')
    },
    wHandleCheckAllChange(val) {
      this.writerForm.columns = val ? this.fromColumnList : []
      this.writerForm.isIndeterminate = false
    },
    wHandleCheckedChange(value) {
      const checkedCount = value.length
      this.writerForm.checkAll = checkedCount === this.fromColumnList.length
      this.writerForm.isIndeterminate = checkedCount > 0 && checkedCount < this.fromColumnList.length
    },
    createTableCheckedChange(val) {
      this.writerForm.tableName = val ? this.readerForm.tableName : ''
      this.fromColumnList = this.readerForm.columns
      this.writerForm.columns = this.readerForm.columns
      this.writerForm.checkAll = true
      this.writerForm.isIndeterminate = false
    },
    getData() {
      if (Bus.dataSourceId) {
        this.writerForm.datasourceId = Bus.dataSourceId
      }
      return this.writerForm
    },
    getReaderData() {
      return this.$parent.getReaderData()
    },
    getTableName() {
      return this.fromTableName
    }
  }
}
</script>
