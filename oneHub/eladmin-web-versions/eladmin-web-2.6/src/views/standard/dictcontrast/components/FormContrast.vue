<template>
  <el-dialog title="对照表" width="50%" :visible.sync="dialogVisible">
    <el-form ref="form" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="数据源" prop="sourceId">
        <el-select v-model="form.sourceId" placeholder="请选择数据源" @change="sourceSelectChanged">
          <el-option
            v-for="source in sourceOptions"
            :key="source.id"
            :label="source.sourceName"
            :value="source.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="数据表" prop="tableId">
        <el-select v-model="form.tableId" placeholder="请选择数据表" @change="tableSelectChanged">
          <el-option
            v-for="table in tableOptions"
            :key="table.id"
            :label="table.tableName"
            :value="table.id">
            <span style="float: left">{{ table.tableName + '(' + (table.tableComment ? table.tableComment : "") + ')' }}</span>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="对照字段" prop="columnId">
        <el-select v-model="form.columnId" placeholder="请选择对照字段" @change="columnSelectChanged">
          <el-option
            v-for="column in columnOptions"
            :key="column.id"
            :label="column.columnName"
            :value="column.id">
            <span style="float: left">{{ column.columnName + '(' + (column.columnComment ? column.columnComment : "") + ')' }}</span>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="标准类别" prop="gbTypeId">
        <el-select v-model="form.gbTypeId" placeholder="请选择标准类别">
          <el-option
            v-for="type in gbTypeOptions"
            :key="type.id"
            :label="type.gbTypeName"
            :value="type.id">
            <span style="float: left">{{ type.gbTypeName + '(' + type.gbTypeCode + ')' }}</span>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="绑定类别" prop="bindGbColumn">
        <el-select v-model="form.bindGbColumn" placeholder="请选择标准字典字段">
          <el-option
            v-for="item in gbColumnOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value">
          </el-option>
        </el-select>
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitForm">确定</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { addContrast, updateContrast } from '@/api/standard/contrast'
import { listDataSource } from '@/api/metadata/datasource'
import { listDataTable } from '@/api/metadata/datatable'
import { listDataColumn } from '@/api/metadata/datacolumn'
import { listDataDictType } from '@/api/standard/datadict'

export default {
  name: 'FormContrast',
  props: {
    visible: {
      type: Boolean,
      default: function() {
        return false
      }
    },
    data: {
      type: Object,
      default: function() {
        return {}
      }
    }
  },
  data() {
    return {
      form: {
        sourceId: undefined,
        sourceName: undefined,
        tableId: undefined,
        tableName: undefined,
        columnId: undefined,
        columnName: undefined,
        gbTypeId: undefined,
        bindGbColumn: undefined
      },
      rules: {
        sourceId: [
          { required: true, message: '数据源不能为空', trigger: 'change' }
        ],
        tableId: [
          { required: true, message: '数据表不能为空', trigger: 'change' }
        ],
        columnId: [
          { required: true, message: '对照字段不能为空', trigger: 'change' }
        ],
        gbTypeId: [
          { required: true, message: '标准类别不能为空', trigger: 'change' }
        ],
        bindGbColumn: [
          { required: true, message: '绑定类别不能为空', trigger: 'change' }
        ]
      },
      sourceOptions: [],
      tableOptions: [],
      columnOptions: [],
      gbTypeOptions: [],
      // 标准字典字段数据字典
      gbColumnOptions: [
        { value: 'gb_code', label: '标准编码' },
        { value: 'gb_name', label: '标准名称' }
      ]
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    }
  },
  created() {
    console.log(this.data)
    this.form = Object.assign({}, this.data)
    this.getDataSourceList()
    if (this.data.sourceId) {
      // zrx 查询表
      listDataTable({ sourceId: this.data.sourceId }).then(response => {
        if (response.success) {
          this.tableOptions = response.data
        }
      })
    }
    if (this.data.sourceId && this.data.tableId) {
      // zrx 查询字段
      listDataColumn({ sourceId: this.data.sourceId, tableId: this.data.tableId }).then(response => {
        if (response.success) {
          this.columnOptions = response.data
        }
      })
    }
    this.getDictTypeList()
  },
  methods: {
    getDictTypeList() {
      listDataDictType().then(response => {
        if (response.success) {
          this.gbTypeOptions = response.data
        }
      })
    },
    getDataSourceList() {
      listDataSource().then(response => {
        if (response.success) {
          this.sourceOptions = response.data
        }
      })
    },
    sourceSelectChanged(val) {
      listDataTable({ sourceId: val }).then(response => {
        if (response.success) {
          this.tableOptions = response.data
          this.columnOptions = []
          const source = this.sourceOptions.find(function(item) {
            return item.id === val
          })
          this.form.sourceName = source.sourceName
          this.form.tableId = ''
          this.form.tableName = ''
          this.form.columnId = ''
          this.form.columnName = ''
        }
      })
    },
    tableSelectChanged(val) {
      listDataColumn({ sourceId: this.form.sourceId, tableId: val }).then(response => {
        if (response.success) {
          this.columnOptions = response.data
          const table = this.tableOptions.find(function(item) {
            return item.id === val
          })
          this.form.tableName = table.tableName
          this.form.tableComment = table.tableComment
          this.form.columnId = ''
          this.form.columnName = ''
          this.form.columnComment = ''
        }
      })
    },
    columnSelectChanged(val) {
      const column = this.columnOptions.find(function(item) {
        return item.id === val
      })
      this.form.columnName = column.columnName
      this.form.columnComment = column.columnComment
      this.$forceUpdate()
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          if (this.form.id) {
            updateContrast(this.form).then(response => {
              if (response.success) {
                this.$message.success('保存成功')
                this.dialogVisible = false
                this.$emit('handleFormContrastFinished')
              }
            }).catch(error => {
              this.$message.error(error.msg || '保存失败')
            })
          } else {
            addContrast(this.form).then(response => {
              if (response.success) {
                this.$message.success('保存成功')
                this.dialogVisible = false
                this.$emit('handleFormContrastFinished')
              }
            }).catch(error => {
              this.$message.error(error.msg || '保存失败')
            })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
