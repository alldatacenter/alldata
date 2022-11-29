<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button v-hasPerm="['visual:set:preview']" size="mini" icon="el-icon-s-data" round @click="dataPreview">数据预览</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-form ref="form" :model="form" label-width="80px" disabled>
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
              :read-only="true"
              style="height: 300px;"
            />
          </el-col>
        </el-row>
        <el-divider content-position="left">数据模型定义</el-divider>
        <el-row style="height: 300px;padding: 0 20px;">
          <el-col :span="12" style="border: 1px dashed #999;height: 100%;">
            <div class="tag-group">
              <el-tag v-for="(item, index) in columnList" :key="index" class="draggable-tag">
                {{ item.col }}
              </el-tag>
            </div>
          </el-col>
          <el-col :span="12" style="box-shadow: 0 0 1px 1px #ccc;height: 100%;">
            <el-row>
              <el-divider content-position="left">维度列</el-divider>
              <el-col>
                <div class="draggable-wrapper">
                  <el-tag v-for="(item, index) in dimensionList" :key="index" class="draggable-item">
                    {{ item.alias ? item.alias + '(' + item.col + ')' : item.col }}
                  </el-tag>
                </div>
              </el-col>
            </el-row>
            <el-row>
              <el-divider content-position="left">指标列</el-divider>
              <el-col>
                <div class="draggable-wrapper">
                  <el-tag v-for="(item, index) in measureList" :key="index" class="draggable-item">
                    {{ item.alias ? item.alias + '(' + item.col + ')' : item.col }}
                  </el-tag>
                </div>
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
    </div>
  </el-card>
</template>

<script>
import { getDataSet } from '@/api/visual/dataset'
import { listDataSource, queryByPage } from '@/api/metadata/datasource'
import SqlEditor from '@/components/SqlEditor'

export default {
  name: 'DataSetDetail',
  components: {
    SqlEditor
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
      classCardbody: {
        overflow: 'auto',
        height: document.body.offsetHeight - 240 + 'px'
      },
      title: '数据集详情',
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
    console.log('id:' + this.data.id)
    this.getDicts('sys_common_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
    this.getDataSourceList()
  },
  mounted() {
    this.getDataSet(this.data.id)
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
    /** 获取详情 */
    getDataSet: function(id) {
      getDataSet(id).then(response => {
        if (response.success) {
          this.form = response.data
          this.columns = this.form.schemaConfig.columns || []
          if (this.columns && this.columns.length > 0) {
            this.dimensionList = this.form.schemaConfig.dimensions || []
            this.measureList = this.form.schemaConfig.measures || []
            this.columnList = this.columns.filter(x => [...this.dimensionList, ...this.measureList].every(y => y.col !== x)).map(function(item) {
              const json = {}
              json.col = item
              json.alias = ''
              return json
            })
          }
          this.$refs.sqleditor.editor.setValue(this.form.setSql)
        }
      })
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
