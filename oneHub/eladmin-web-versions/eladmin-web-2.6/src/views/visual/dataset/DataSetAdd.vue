<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button v-hasPerm="['visual:chart:add']" size="mini" icon="el-icon-plus" round :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{ loadingOptions.loadingText }}</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="数据源" prop="sourceId">
          <el-select v-model="form.sourceId" placeholder="请选择数据源">
            <el-option
              v-for="source in sourceOptions"
              :key="source.id"
              :label="source.sourceName"
              :value="source.id"
              :disabled="source.status === '0'"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据集名称" prop="setName">
          <el-input v-model="form.setName" placeholder="请输入数据集名称" />
        </el-form-item>
        <el-divider content-position="left">数据集</el-divider>
        <el-row>
          <el-col :span="24" style="padding: 0 20px;">
            <sql-editor
              ref="sqleditor"
              :value="form.setSql"
              style="height: 300px;"
              @changeTextarea="changeTextarea($event)"
            />
          </el-col>
        </el-row>
        <el-form-item>
          <el-button size="mini" type="primary" @click="formaterSql">Sql格式化</el-button>
          <el-button size="mini" type="primary" @click="analyseSql">Sql解析</el-button>
          <el-button v-hasPerm="['visual:set:preview']" size="mini" type="primary" @click="dataPreview">数据预览</el-button>
        </el-form-item>
        <el-divider content-position="left">数据模型定义</el-divider>
        <el-row style="height: 300px;padding: 0 20px;">
          <el-col :span="12" style="border: 1px dashed #999;height: 100%;">
            <div class="tag-group">
              <draggable v-model="columnList" :options="{sort: false, group: {name: 'col', pull:'clone', put: false}}">
                <el-tag v-for="(item, index) in columnList" :key="index" class="draggable-tag">
                  {{ item.col }}
                </el-tag>
              </draggable>
            </div>
          </el-col>
          <el-col :span="12" style="box-shadow: 0 0 1px 1px #ccc;height: 100%;">
            <el-row>
              <el-divider content-position="left">维度列</el-divider>
              <el-col>
                <draggable group="col" :list="dimensionList" class="draggable-wrapper">
                  <div v-for="(item, index) in dimensionList" :key="index" class="draggable-item">
                    <el-tag>{{ item.alias ? item.alias + '(' + item.col + ')' : item.col }}</el-tag>
                    <span v-if="item.input" class="draggable-item-handle">
                      <el-input v-model="item.alias" size="mini" placeholder="请输入内容" @blur="handleDelTagLabel(index, item)" />
                    </span>
                    <span v-else class="draggable-item-handle" @click="handleTagLabel(index, item)"><i class="el-icon-edit-outline" /></span>
                    <span class="draggable-item-handle" @click="handleDimensionTagClose(index, item)"><i class="el-icon-delete" /></span>
                  </div>
                </draggable>
              </el-col>
            </el-row>
            <el-row>
              <el-divider content-position="left">指标列</el-divider>
              <el-col>
                <draggable group="col" :list="measureList" class="draggable-wrapper">
                  <div v-for="(item, index) in measureList" :key="index" class="draggable-item">
                    <el-tag>{{ item.alias ? item.alias + '(' + item.col + ')' : item.col }}</el-tag>
                    <span v-if="item.input" class="draggable-item-handle">
                      <el-input v-model="item.alias" size="mini" placeholder="请输入内容" @blur="handleDelTagLabel(index, item)" />
                    </span>
                    <span v-else class="draggable-item-handle" @click="handleTagLabel(index, item)"><i class="el-icon-edit-outline" /></span>
                    <span class="draggable-item-handle" @click="handleMeasureTagClose(index, item)"><i class="el-icon-delete" /></span>
                  </div>
                </draggable>
              </el-col>
            </el-row>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in statusOptions"
              :key="dict.id"
              :label="dict.itemText"
            >{{ dict.itemValue }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
    </div>
    <el-drawer
      :visible.sync="drawer"
      direction="btt"
      :with-header="false"
    >
      <el-table
        :data="previewData.dataList"
        stripe
        border
        :max-height="200"
        style="width: 100%; margin: 15px 0;"
      >
        <el-table-column label="序号" width="55" align="center">
          <template slot-scope="scope">
            <span>{{ scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <template v-for="(column, index) in previewData.columnList">
          <el-table-column
            :key="index"
            :prop="column"
            :label="column"
            align="center"
            show-overflow-tooltip
          />
        </template>
      </el-table>
      <el-pagination
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :current-page.sync="previewData.pageNum"
        :page-size.sync="previewData.pageSize"
        :total="previewData.dataTotal"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-drawer>
  </el-card>
</template>

<script>
import { addDataSet, sqlAnalyse } from '@/api/visual/dataset'
import { listDataSource, queryByPage } from '@/api/metadata/datasource'
import sqlFormatter from 'sql-formatter'
import SqlEditor from '@/components/SqlEditor'
import draggable from 'vuedraggable'

export default {
  name: 'DataSetAdd',
  components: {
    SqlEditor,
    draggable
  },
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
      title: '数据集新增',
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false
      },
      // 保存按钮
      loadingOptions: {
        loading: false,
        loadingText: '保存',
        isDisabled: false
      },
      // 表单参数
      form: {
        id: undefined,
        sourceId: undefined,
        setSql: undefined,
        status: '1',
        remark: undefined
      },
      // 表单校验
      rules: {
        sourceId: [
          { required: true, message: '数据源不能为空', trigger: 'change' }
        ],
        setName: [
          { required: true, message: '数据集名称不能为空', trigger: 'blur' }
        ]
      },
      // 状态数据字典
      statusOptions: [],
      // 数据源数据字典
      sourceOptions: [],
      // 解析字段
      columns: [],
      columnList: [],
      dimensionList: [],
      measureList: [],
      drawer: false,
      previewData: {
        dataList: [],
        columnList: [],
        pageNum: 1,
        pageSize: 20,
        dataTotal: 0
      }
    }
  },
  created() {
    this.getDicts('sys_common_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
    this.getDataSourceList()
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    getDataSourceList() {
      listDataSource().then(response => {
        if (response.success) {
          this.sourceOptions = response.data
        }
      })
    },
    // 绑定编辑器value值的变化
    changeTextarea(val) {
      this.$set(this.form, 'setSql', val)
    },
    formaterSql() {
      if (!this.form.setSql) {
        return
      }
      this.$refs.sqleditor.editor.setValue(sqlFormatter.format(this.$refs.sqleditor.editor.getValue()))
    },
    analyseSql() {
      if (!this.form.setSql) {
        return
      }
      const data = {}
      data.sqlText = this.form.setSql
      sqlAnalyse(data).then(response => {
        if (response.success) {
          this.columns = response.data
          this.columnList = this.columns.map(function(item) {
            const json = {}
            json.col = item
            json.alias = ''
            return json
          })
          this.dimensionList = []
          this.measureList = []
        }
      })
    },
    handleDimensionTagClose(index, tag) {
      this.dimensionList.splice(index, 1)
      tag.alias = ''
      this.columnList.push(tag)
    },
    handleMeasureTagClose(index, tag) {
      this.measureList.splice(index, 1)
      tag.alias = ''
      this.columnList.push(tag)
    },
    handleTagLabel(index, tag) {
      this.$set(tag, 'input', true)
    },
    handleDelTagLabel(index, tag) {
      this.$delete(tag, 'input')
    },
    dataPreview() {
      if (!this.form.sourceId) {
        return
      }
      if (!this.form.setSql) {
        return
      }
      const data = {}
      data.dataSourceId = this.form.sourceId
      data.sql = this.form.setSql
      data.pageNum = this.previewData.pageNum
      data.pageSize = this.previewData.pageSize
      queryByPage(data).then(response => {
        if (response.success) {
          const { data } = response
          const dataList = data.data || []
          let columnList = []
          if (dataList.length > 0) {
            columnList = Object.keys(dataList[0])
          }
          this.previewData.dataList = dataList
          this.previewData.columnList = columnList
          this.previewData.dataTotal = data.total
          this.drawer = true
        }
      })
    },
    handleSizeChange(val) {
      this.previewData.pageNum = 1
      this.previewData.pageSize = val
      this.dataPreview()
    },
    handleCurrentChange(val) {
      this.previewData.pageNum = val
      this.dataPreview()
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          const schema = {}
          schema.columns = this.columns || []
          schema.dimensions = this.dimensionList || []
          schema.measures = this.measureList || []
          this.form.schemaConfig = schema
          this.loadingOptions.loading = true
          this.loadingOptions.loadingText = '保存中...'
          this.loadingOptions.isDisabled = true
          addDataSet(this.form).then(response => {
            if (response.success) {
              this.$message.success('保存成功')
              setTimeout(() => {
                // 2秒后跳转列表页
                this.$emit('showCard', this.showOptions)
              }, 2000)
            } else {
              this.$message.error('保存失败')
              this.loadingOptions.loading = false
              this.loadingOptions.loadingText = '保存'
              this.loadingOptions.isDisabled = false
            }
          }).catch(() => {
            this.loadingOptions.loading = false
            this.loadingOptions.loadingText = '保存'
            this.loadingOptions.isDisabled = false
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
.draggable-tag {
  margin: 10px;
  cursor: move;
}
.draggable-wrapper {
  height: 90px;
  border: 1px dashed #999;
  margin: 0 10px;
  overflow-x: hidden;
  overflow-y: auto;
  .draggable-item {
    cursor: move;
    margin: 5px 5px;
    display: inline-block;
    border: 1px solid #ebecef;
    height: 32px;
    line-height: 30px;
    border-radius: 4px;
    .draggable-item-handle {
      background-color: #ecf5ff;
      border-color: #d9ecff;
      display: inline-block;
      height: 32px;
      padding: 0 10px;
      line-height: 30px;
      font-size: 12px;
      color: #409EFF;
      border-width: 1px;
      border-style: solid;
      box-sizing: border-box;
      white-space: nowrap;
      cursor: pointer;
      margin-left: -5px;
    }
  }
}
</style>
