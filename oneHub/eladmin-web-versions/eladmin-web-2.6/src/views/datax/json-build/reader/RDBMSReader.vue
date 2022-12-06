<template>
  <div class="app-container">
    <el-form label-position="right" label-width="120px" :model="readerForm" :rules="rules">
      <el-form-item label="数据库源：" prop="datasourceId">
        <el-select v-model="readerForm.datasourceId" filterable style="width: 300px" @change="rDsChange">
          <el-option
            v-for="item in rDsList"
            :key="item.id"
            :label="item.datasourceName"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-show="dataSource==='postgresql' || dataSource==='oracle' || dataSource==='sqlserver' || dataSource==='hana'" label="Schema：" prop="tableSchema">
        <el-select v-model="readerForm.tableSchema" allow-create default-first-option filterable style="width: 300px" @change="schemaChange">
          <el-option
            v-for="item in schemaList"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="数据库表名：" prop="tableName">
        <el-select v-model="readerForm.tableName" allow-create default-first-option filterable style="width: 300px" @change="rTbChange">
          <el-option v-for="item in rTbList" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="SQL语句：">
        <el-input v-model="readerForm.querySql" :autosize="{ minRows: 3, maxRows: 20}" type="textarea" placeholder="sql查询，一般用于多表关联查询时才用" style="width: 42%" />
        <el-button type="primary" @click.prevent="getColumns('reader')">解析字段</el-button>
      </el-form-item>
      <el-form-item label="切分字段：">
        <el-input v-model="readerForm.splitPk" placeholder="切分主键" style="width: 13%" />
      </el-form-item>
      <el-form-item label="表所有字段：">
        <el-checkbox
          v-model="readerForm.checkAll"
          :indeterminate="readerForm.isIndeterminate"
          @change="rHandleCheckAllChange"
        >全选
        </el-checkbox>
        <div style="margin: 15px 0;" />
        <el-checkbox-group v-model="readerForm.columns" @change="rHandleCheckedChange">
          <el-checkbox v-for="c in rColumnList" :key="c" :label="c">{{ c }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="where条件：" prop="where">
        <el-input v-model="readerForm.where" placeholder="where条件，不需要再加where" type="textarea" style="width: 42%" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
    import * as dsQueryApi from '@/api/dts/metadata-query'
    import { list as jdbcDsList } from '@/api/dts/datax-jdbcDatasource'
    import Bus from '../busReader'

    export default {
        name: 'RDBMSReader',
        data() {
            return {
                jdbcDsQuery: {
                    current: 1,
                    size: 200,
                    ascs: 'datasource_name'
                },
                rDsList: [],
                rTbList: [],
                schemaList: [],
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
                    where: '',
                    querySql: '',
                    checkAll: false,
                    isIndeterminate: true,
                    splitPk: '',
                    tableSchema: ''
                },
                rules: {
                    datasourceId: [{ required: true, message: 'this is required', trigger: 'change' }],
                    tableName: [{ required: true, message: 'this is required', trigger: 'change' }],
                    tableSchema: [{ required: true, message: 'this is required', trigger: 'change' }]
                }
            }
        },
        watch: {
            'readerForm.datasourceId': function(oldVal, newVal) {
                if (this.dataSource === 'postgresql' || this.dataSource === 'oracle' || this.dataSource === 'sqlserver' || this.dataSource === 'hana') {
                    this.getSchema()
                } else {
                    this.getTables('rdbmsReader')
                }
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
                    this.rDsList = records
                    this.loading = false
                })
            },
            // 获取表名
            getTables(type) {
                if (type === 'rdbmsReader') {
                    let obj = {}
                    if (this.dataSource === 'postgresql' || this.dataSource === 'oracle' || this.dataSource === 'sqlserver' || this.dataSource === 'hana') {
                        obj = {
                            datasourceId: this.readerForm.datasourceId,
                            tableSchema: this.readerForm.tableSchema
                        }
                    } else {
                        obj = {
                            datasourceId: this.readerForm.datasourceId
                        }
                    }
                    // 组装
                    dsQueryApi.getTables(obj).then(response => {
                        if (response) {
                            this.rTbList = response
                        }
                    })
                }
            },
            getSchema() {
                const obj = {
                    datasourceId: this.readerForm.datasourceId
                }
                dsQueryApi.getTableSchema(obj).then(response => {
                    this.schemaList = response
                })
            },
            // schema 切换
            schemaChange(e) {
                this.readerForm.tableSchema = e
                // 获取可用表
                this.getTables('rdbmsReader')
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
                    if (this.readerForm.querySql !== '') {
                        this.getColumnsByQuerySql()
                    } else {
                        this.getTableColumns()
                    }
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
