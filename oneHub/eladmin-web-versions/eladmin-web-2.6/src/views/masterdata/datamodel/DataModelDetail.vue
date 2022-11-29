<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button v-hasPerm="['masterdata:model:submit']" v-if="form.flowStatus === '1' || form.flowStatus === '6'" size="mini" icon="el-icon-s-data" round @click="submit">提交</el-button>
        <el-button v-hasPerm="['masterdata:model:create']" v-if="form.flowStatus === '4'" :disabled="form.isSync === '1'" size="mini" icon="el-icon-s-data" round @click="createTable">建模</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-tabs v-model="activeName">
        <el-tab-pane label="基本信息" name="first">
          <el-form ref="form" :model="form" label-width="80px" disabled>
            <el-form-item label="模型名称">
              <el-input v-model="form.modelName" placeholder="请输入模型名称" />
            </el-form-item>
            <el-form-item label="逻辑表">
              <el-input v-model="form.modelLogicTable" placeholder="请输入逻辑表" />
            </el-form-item>
            <el-form-item label="物理表">
              <el-input v-model="form.modelPhysicalTable" placeholder="请输入逻辑表" />
            </el-form-item>
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in statusOptions"
                  :key="dict.id"
                  :label="dict.itemText"
                >{{ dict.itemValue }}</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="字段信息" name="second">
          <el-form ref="secondTable" :model="form" size="mini" disabled>
            <el-table :data="form.modelColumns" border style="width: 100%; margin: 15px 0;">
              <el-table-column label="序号" type="index" align="center" width="55" />
              <el-table-column label="列名称">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-input v-model="scope.row.columnName" clearable placeholder="请输入列名称" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="列描述">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-input v-model="scope.row.columnComment" clearable placeholder="请输入列描述" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="列类型">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-select v-model="scope.row.columnType" clearable placeholder="请选择">
                      <el-option
                        v-for="item in columnTypeOptions"
                        :key="item.id"
                        :label="item.itemValue"
                        :value="item.itemText"
                      />
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="默认值">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-input v-model="scope.row.defaultValue" clearable placeholder="请输入默认值" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="长度" width="70">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-input-number v-model="scope.row.columnLength" style="width: 50px;" :controls="false" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="小数位" width="70">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-input-number v-model="scope.row.columnScale" style="width: 50px;" :controls="false" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="主键" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-checkbox v-model="scope.row.isPk" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="必填" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-checkbox v-model="scope.row.isRequired" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
            </el-table>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="页面属性" name="third">
          <el-form ref="thirdTable" :model="form" size="mini" disabled>
            <el-table :data="form.modelColumns" border style="width: 100%; margin: 15px 0;">
              <el-table-column label="序号" type="index" align="center" width="55" />
              <el-table-column label="列名称">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-input v-model="scope.row.columnName" clearable placeholder="请输入列名称" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="列描述">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-input v-model="scope.row.columnComment" clearable placeholder="请输入列描述" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="插入" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-checkbox v-model="scope.row.isInsert" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="编辑" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-checkbox v-model="scope.row.isEdit" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="详情" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-checkbox v-model="scope.row.isDetail" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="列表" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-checkbox v-model="scope.row.isList" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="查询" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-checkbox v-model="scope.row.isQuery" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="查询方式" width="120">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-select v-model="scope.row.queryType" clearable placeholder="请选择">
                      <el-option
                        v-for="item in queryTypeOptions"
                        :key="item.id"
                        :label="item.itemValue"
                        :value="item.itemText"
                      />
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="标准" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-checkbox v-model="scope.row.isBindDict" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="标准字典类别" width="120">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-select v-model="scope.row.bindDictTypeId" clearable placeholder="请选择">
                      <el-option
                        v-for="item in dictTypeOptions"
                        :key="item.id"
                        :label="item.gbTypeName"
                        :value="item.id"
                      />
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="标准字典字段" width="120">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-select v-model="scope.row.bindDictColumn" clearable placeholder="请选择">
                      <el-option
                        v-for="item in gbColumnOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value">
                      </el-option>
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="显示类型" width="120">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-select v-model="scope.row.htmlType" clearable placeholder="请选择">
                      <el-option
                        v-for="item in htmlTypeOptions"
                        :key="item.id"
                        :label="item.itemValue"
                        :value="item.itemText"
                      />
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
            </el-table>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-card>
</template>

<script>
import { getDataModel, submitDataModel, createTable } from '@/api/masterdata/datamodel'
import { listDataDictType } from '@/api/standard/datadict'

export default {
  name: 'DataModelDetail',
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
      title: '数据模型详情',
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false
      },
      // 表单参数
      form: {},
      // 状态数据字典
      statusOptions: [],
      activeName: 'first',
      columnTypeOptions: [],
      queryTypeOptions: [],
      htmlTypeOptions: [],
      dictTypeOptions: [],
      gbColumnOptions: [
        { value: 'gb_code', label: '标准编码' },
        { value: 'gb_name', label: '标准名称' }
      ]
    }
  },
  created() {
    console.log('id:' + this.data.id)
    this.getDicts('sys_common_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
    this.getDicts('data_type_mysql').then(response => {
      if (response.success) {
        this.columnTypeOptions = response.data
      }
    })
    this.getDicts('data_query_type').then(response => {
      if (response.success) {
        this.queryTypeOptions = response.data
      }
    })
    this.getDicts('data_html_type').then(response => {
      if (response.success) {
        this.htmlTypeOptions = response.data
      }
    })
    listDataDictType().then(response => {
      if (response.success) {
        this.dictTypeOptions = response.data
      }
    })
  },
  mounted() {
    this.getDataModel(this.data.id)
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    /** 获取详情 */
    getDataModel: function(id) {
      getDataModel(id).then(response => {
        if (response.success) {
          this.form = response.data
        }
      })
    },
    submit() {
      submitDataModel(this.data.id).then(response => {
        if (response.success) {
          this.$message.success('提交成功')
          this.showCard()
        } else {
          this.$message.warning('提交失败')
        }
      })
    },
    createTable() {
      createTable(this.data.id).then(response => {
        if (response.success) {
          this.$message.success('建模成功')
          this.showCard()
        } else {
          this.$message.warning('建模失败')
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
