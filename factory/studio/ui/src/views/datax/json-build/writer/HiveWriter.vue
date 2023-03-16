<template>
  <div>
    <el-form label-position="left" label-width="105px" :model="writerForm" :rules="rules">
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
        <!--<el-col :span="6">
          <el-form-item>
            <el-button type="primary" :disabled="!this.fromTableName" @click="createTable()">一键生成目标表</el-button>
          </el-form-item>
        </el-col>-->
      </el-row>
      <el-form-item label="path" prop="path">
        <el-input v-model="writerForm.path" :autosize="{ minRows: 2, maxRows: 20}" type="textarea" placeholder="为与hive表关联，请填写hive表在hdfs上的存储路径" style="width: 42%" />
      </el-form-item>
      <el-form-item label="defaultFS" prop="defaultFS">
        <el-input v-model="writerForm.defaultFS" placeholder="Hadoop hdfs文件系统namenode节点地址" style="width: 42%" />
      </el-form-item>
      <el-form-item label="fileName" prop="fileName">
        <el-input v-model="writerForm.fileName" placeholder="HdfsWriter写入时的文件名" style="width: 42%" />
      </el-form-item>
      <el-form-item label="fileType" prop="fileType">
        <el-select v-model="writerForm.fileType" placeholder="文件的类型">
          <el-option v-for="item in fileTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="writeMode" prop="writeMode">
        <el-select v-model="writerForm.writeMode" placeholder="文件的类型">
          <el-option v-for="item in writeModes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="fieldDelimiter" prop="fieldDelimiter">
        <el-input v-model="writerForm.fieldDelimiter" placeholder="与创建表的分隔符一致" style="width: 13%" />
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
  name: 'HiveWriter',
  data() {
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
        defaultFS: '',
        fileType: '',
        path: '',
        fileName: '',
        writeMode: '',
        fieldDelimiter: ''
      },
      rules: {
        path: [{ required: true, message: 'this is required', trigger: 'blur' }],
        defaultFS: [{ required: true, message: 'this is required', trigger: 'blur' }],
        fileName: [{ required: true, message: 'this is required', trigger: 'blur' }],
        fileType: [{ required: true, message: 'this is required', trigger: 'change' }],
        writeMode: [{ required: true, message: 'this is required', trigger: 'change' }],
        fieldDelimiter: [{ required: true, message: 'this is required', trigger: 'blur' }],
        datasourceId: [{ required: true, message: 'this is required', trigger: 'blur' }],
        fromTableName: [{ required: true, message: 'this is required', trigger: 'blur' }]
      },
      readerForm: this.getReaderData(),
      fileTypes: [
        { value: 'text', label: 'text' },
        { value: 'orc', label: 'orc' }
      ],
      writeModes: [
        { value: 'append', label: 'append 写入前不做任何处理' },
        { value: 'nonConflict', label: 'nonConflict 目录下有fileName前缀的文件，直接报错' }
      ]
    }
  },
  watch: {
    'writerForm.datasourceId': function(oldVal, newVal) {
      this.getTables('hiveWriter')
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
        const { records } = response.data
        this.wDsList = records
        this.loading = false
      })
    },
    // 获取表名
    getTables(type) {
      if (type === 'hiveWriter') {
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
    },
    createTable() {
      const tableName = this.fromTableName
      const datasourceId = this.writerForm.datasourceId
      const columns = this.fromColumnList
      const jsonString = {}
      jsonString['datasourceId'] = datasourceId
      jsonString['tableName'] = tableName
      jsonString['columns'] = columns
      console.info(jsonString)
      dsQueryApi.createTable(jsonString).then(response => {
        this.$notify({
          title: 'Success',
          message: 'Create Table Successfully',
          type: 'success',
          duration: 2000
        })
      }).catch(() => console.log('promise catch err'))
    }
  }
}
</script>
