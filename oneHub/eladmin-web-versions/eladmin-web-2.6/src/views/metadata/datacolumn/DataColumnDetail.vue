<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-form ref="form" :model="form" label-width="100px" size="mini">
        <el-row>
          <el-col :span="8">
            <el-form-item label="字段名称" prop="columnName">
              <el-input v-model="form.columnName" disabled>
                <el-button v-hasPerm="['metadata:changerecord:add']" slot="append" icon="el-icon-edit-outline" @click="changeRecord('columnName')" />
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="字段注释" prop="columnComment">
              <el-input v-model="form.columnComment" disabled>
                <el-button v-hasPerm="['metadata:changerecord:add']" slot="append" icon="el-icon-edit-outline" @click="changeRecord('columnComment')" />
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数据默认值" prop="dataDefault">
              <el-input v-model="form.dataDefault" disabled>
                <el-button v-hasPerm="['metadata:changerecord:add']" slot="append" icon="el-icon-edit-outline" @click="changeRecord('dataDefault')" />
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="是否主键" prop="columnKey">
              <el-input v-model="form.columnKey === '1' ? 'Y' : 'N'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否允许为空" prop="columnNullable">
              <el-input v-model="form.columnNullable === '1' ? 'Y' : 'N'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数据类型" prop="dataType">
              <el-input v-model="form.dataType" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="数据长度" prop="dataLength">
              <el-input v-model="form.dataLength" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数据精度" prop="dataPrecision">
              <el-input v-model="form.dataPrecision" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数据小数位" prop="dataScale">
              <el-input v-model="form.dataScale" disabled />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-divider content-position="center">变更记录</el-divider>
      <el-row>
        <el-col :span="24">
          <el-table
            :data="changeData.dataList"
            stripe
            border
            :max-height="200"
            style="width: 100%; margin: 15px 0;"
          >
            <el-table-column label="序号" align="center">
              <template slot-scope="scope">
                <span>{{ scope.$index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="sourceName" label="数据源" align="center" show-overflow-tooltip />
            <el-table-column prop="tableName" label="数据库表" align="center" show-overflow-tooltip />
            <el-table-column prop="fieldName" label="变更字段" align="center" show-overflow-tooltip />
            <el-table-column prop="objectType" label="变更类型" align="center" show-overflow-tooltip :formatter="fieldNameFormatter" />
            <el-table-column prop="version" label="版本号" align="center" show-overflow-tooltip />
            <el-table-column prop="fieldOldValue" label="原来的值" align="center" show-overflow-tooltip />
            <el-table-column prop="fieldNewValue" label="最新的值" align="center" show-overflow-tooltip />
          </el-table>
          <el-pagination
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :current-page.sync="changeData.pageNum"
            :page-size.sync="changeData.pageSize"
            :total="changeData.dataTotal"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </el-col>
      </el-row>
    </div>

    <!-- 变更弹框 -->
    <el-dialog :title="record.title" :visible.sync="record.open" min-width="600px" append-to-body>
      <el-form ref="form2" :model="form2" :rules="rules2" label-width="80px">
        <el-form-item label="变更类型" prop="objectTypeStr">
          <el-input v-model="form2.objectTypeStr" placeholder="请输入变更字段" :disabled="true" />
        </el-form-item>
        <el-form-item label="字段名称" prop="fieldName">
          <el-input v-model="form2.fieldName" placeholder="请输入版本号" :disabled="true"/>
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="form2.version" placeholder="请输入版本号" />
        </el-form-item>
        <el-form-item label="原来的值" prop="fieldOldValue">
          <el-input v-model="form2.fieldOldValue" placeholder="请输入原来的值" :disabled="true" />
        </el-form-item>
        <el-form-item label="最新的值" prop="fieldNewValue">
          <el-input v-model="form2.fieldNewValue" placeholder="请输入最新的值" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form2.status">
            <el-radio
              v-for="dict in statusOptions"
              :key="dict.id"
              :label="dict.itemText"
            >{{ dict.itemValue }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form2.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitRecordForm">确 定</el-button>
        <el-button @click="record.open = false">取 消</el-button>
      </div>
    </el-dialog>
  </el-card>
</template>

<script>
import { getDataColumn } from '@/api/metadata/datacolumn'
import { pageChangeRecord, addChangeRecord } from '@/api/metadata/changerecord'

export default {
  name: 'DataColumnDetail',
  props: {
    data: {
      type: Object,
      default: function() {
        return {}
      }
    }
  },
  data() {
    return {
      title: '元数据详情（注意：此处变更元数据信息只会保存变更记录，不会真正修改元数据，只是辅助记录作用！）',
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showDetail: false
      },
      // 表单参数
      form: {},
      changeData: {
        dataList: [],
        pageNum: 1,
        pageSize: 20,
        dataTotal: 0
      },
      dicts: new Map([
        ['columnName', '字段名称'],
        ['columnComment', '字段注释'],
        ['dataDefault', '数据默认值'],
        ['columnKey', '是否主键'],
        ['columnNullable', '是否允许为空'],
        ['dataType', '数据类型'],
        ['dataLength', '数据长度'],
        ['dataPrecision', '数据精度'],
        ['dataScale', '数据小数位']
      ]),
      record: {
        // 是否显示弹出层
        open: false,
        // 弹出层标题
        title: '元数据变更'
      },
      form2: {
        status: '1'
      },
      rules2: {
        fieldName: [
          { required: true, message: '字段名称不能为空', trigger: 'blur' }
        ],
        objectTypeStr: [
          { required: true, message: '变更类型不能为空', trigger: 'blur' }
        ],
        // zrx
        /* fieldOldValue: [
          { required: true, message: '原来的值不能为空', trigger: 'blur' }
        ], */
        fieldNewValue: [
          { required: true, message: '最新的值不能为空', trigger: 'blur' }
        ],
        version: [
          { required: true, message: '版本号不能为空', trigger: 'blur' }
        ]
      },
      // 状态数据字典
      statusOptions: []
    }
  },
  created() {
    console.log('id:' + this.data.id)
    this.getDicts('sys_common_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
  },
  mounted() {
    this.getDataColumn(this.data.id)
    this.getChangeRecordList()
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    /** 获取详情 */
    getDataColumn: function(id) {
      getDataColumn(id).then(response => {
        if (response.success) {
          this.form = response.data
        }
      })
    },
    handleSizeChange(val) {
      this.changeData.pageNum = 1
      this.changeData.pageSize = val
      this.getChangeRecordList()
    },
    handleCurrentChange(val) {
      this.changeData.pageNum = val
      this.getChangeRecordList()
    },
    getChangeRecordList() {
      const data = {}
      data.objectId = this.data.id
      data.pageNum = this.changeData.pageNum
      data.pageSize = this.changeData.pageSize
      pageChangeRecord(data).then(response => {
        if (response.success) {
          const { data } = response
          this.changeData.dataList = data.data
          this.changeData.dataTotal = data.total
        }
      })
    },
    fieldNameFormatter(row, column, cellValue, index) {
      return this.dicts.get(cellValue)
    },
    changeRecord(field) {
      this.record.open = true
      this.form2.objectId = this.data.id
      console.log(this.form)
      this.form2.objectType = field
      this.form2.objectTypeStr = this.dicts.get(field)
      this.form2.fieldName = this.form.columnName
      this.form2.fieldOldValue = this.form[field]
      console.log(this.form2)
    },
    submitRecordForm() {
      this.$refs['form2'].validate(valid => {
        if (valid) {
          addChangeRecord(this.form2).then(response => {
            if (response.success) {
              this.$message.success('保存成功（变更记录只会保存记录，并不会真正去变更元数据！）')
              this.getChangeRecordList()
            } else {
              this.$message.error('保存失败')
            }
            this.record.open = false
          })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.el-card ::v-deep .el-card__body {
  height: calc(100vh - 230px);
  overflow-y: auto;
}
</style>
