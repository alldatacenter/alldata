<template>
  <div class="chart-container">
    <div class="widget-left-container">
      <div class="widget-left-header">
        <span>数据集</span>
        <el-dropdown trigger="click" style="float: right; color: #499df3;" @command="handleCommand">
          <span class="el-dropdown-link">
            切换<i class="el-icon-arrow-down el-icon--right" />
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item v-for="item in dataSetOptions" :key="item.id" :command="item.id">{{ item.setName }}</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
      <div class="widget-left-field">
        <div class="widget-left-field-cate"><i class="icon iconfont icon-weidu" /><span>维度列</span></div>
        <draggable v-model="dimensions" class="widget-left-field-draggable" :options="{sort: false, ghostClass: 'ghost', group: {name: 'dimensions', pull: true, put: false}}">
          <el-tag v-for="(item, index) in dimensions" :key="index" class="draggable-label"><div>{{ item.alias ? item.alias + '(' + item.col + ')' : item.col }}</div></el-tag>
        </draggable>
      </div>
      <div class="widget-left-field">
        <div class="widget-left-field-cate"><i class="icon iconfont icon-zhibiao" /><span>指标列</span></div>
        <draggable v-model="measures" class="widget-left-field-draggable" :options="{sort: false, ghostClass: 'ghost', group: {name: 'measures', pull: true, put: false}}">
          <el-tag v-for="(item, index) in measures" :key="index" class="draggable-label"><div>{{ item.alias ? item.alias + '(' + item.col + ')' : item.col }}</div></el-tag>
        </draggable>
      </div>
    </div>
    <div class="widget-center-container">
      <div class="widget-center-header">
        <div class="widget-center-header-collapse" @click="handleCollapse"><i :class="{'is-active': isCollapse}" class="el-icon-d-arrow-right" /></div>
        <div class="widget-center-header-button">
          <el-button icon="el-icon-view" type="text" @click="handlePreview">
            预览
          </el-button>
          <el-button icon="el-icon-delete" type="text" @click="handleReset">
            重置
          </el-button>
          <el-button v-hasPerm="['visual:chart:build']" icon="el-icon-plus" type="text" @click="handleSubmit">
            保存
          </el-button>
          <el-button icon="el-icon-close" type="text" @click="handleCancel">
            取消
          </el-button>
        </div>
      </div>
      <div class="widget-center-content">
        <div class="widget-center-draggable-wrapper">
          <el-divider content-position="left">行维</el-divider>
          <div class="widget-center-draggable-text">
            <draggable group="dimensions" :list="widget.rows" class="widget-center-draggable-line">
              <el-tag v-for="(item, index) in widget.rows" :key="index" class="draggable-item" closable @close="handleKeyTagClose(index, item)">
                {{ item.alias ? item.alias + '(' + item.col + ')' : item.col }}
              </el-tag>
            </draggable>
          </div>
          <el-divider content-position="left">列维</el-divider>
          <div class="widget-center-draggable-text">
            <draggable group="dimensions" :list="widget.columns" class="widget-center-draggable-line">
              <el-tag v-for="(item, index) in widget.columns" :key="index" class="draggable-item" closable @close="handleGroupTagClose(index, item)">
                {{ item.alias ? item.alias + '(' + item.col + ')' : item.col }}
              </el-tag>
            </draggable>
          </div>
          <el-divider content-position="left">指标</el-divider>
          <div class="widget-center-draggable-text">
            <draggable group="measures" :list="widget.measures" class="widget-center-draggable-line" @change="handleValueDragChange">
              <div v-for="(item, index) in widget.measures" :key="index" class="draggable-item">
                <el-tag>{{ item.alias ? item.aggregateType + '(' + item.col + ') -> ' + item.alias : item.aggregateType + '(' + item.col + ')' }}</el-tag>
                <el-popover
                  placement="top"
                  width="400"
                  trigger="click"
                >
                  <el-radio-group v-model="item.aggregateType" size="mini">
                    <el-radio label="sum" size="mini">求和</el-radio>
                    <el-radio label="count" size="mini">计数</el-radio>
                    <el-radio label="avg" size="mini">平均值</el-radio>
                    <el-radio label="max" size="mini">最大值</el-radio>
                    <el-radio label="min" size="mini">最小值</el-radio>
                  </el-radio-group>
                  <span slot="reference" class="draggable-item-handle"><i class="el-icon-edit-outline" /></span>
                </el-popover>
                <span class="draggable-item-handle" @click="handleValueTagClose(index, item)"><i class="el-icon-delete" /></span>
              </div>
            </draggable>
          </div>
        </div>
        <div class="widget-center-pane">
          <el-tabs type="card">
            <el-tab-pane label="图表预览">
              <div class="widget-center-pane-chart">
                <chart-panel v-if="visible" id="chartPanel" ref="chartPanel" :chart-schema="widget" :chart-data="chartData.data" />
              </div>
            </el-tab-pane>
            <el-tab-pane label="查询脚本">
              <div class="widget-center-pane-script">{{ chartData.sql }}</div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>
    <div class="widget-right-container" :class="{hideRightContainer: isCollapse}">
      <el-tabs type="border-card" stretch class="widget-right-tab">
        <el-tab-pane label="图表属性">
          <div class="widget-right-pane-form">
            <el-form size="mini" label-position="top">
              <el-form-item label="图表名称">
                <el-input v-model="dataChart.chartName" :disabled="true" />
              </el-form-item>
              <el-form-item label="缩略图">
                <el-upload
                  action="#"
                  accept=".png, .jpg, .jpeg"
                  list-type="picture-card"
                  :limit="1"
                  :auto-upload="false"
                  :before-upload="beforeUpload"
                  :on-change="handleChange"
                  :on-remove="handleRemove"
                  :file-list="fileList"
                  :class="{ hideUpload: hideUpload }"
                >
                  <i slot="default" class="el-icon-plus" />
                </el-upload>
              </el-form-item>
              <el-form-item label="图表类型">
                <div class="chart-type-list">
                  <span v-for="item in chartTypes" :key="item.value" :class="item.value === widget.chartType ? 'active': ''" @click="item.status && item.rule.check(widget.rows, widget.columns, widget.measures) && changeChart(item.value)">
                    <el-tooltip :content="item.name + ':' + item.rule.text" placement="top">
                      <svg-icon class="icon" :icon-class="item.icon + (item.status && item.rule.check(widget.rows, widget.columns, widget.measures) ? '_active': '')" />
                    </el-tooltip>
                  </span>
                </div>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
        <el-tab-pane label="图表配置">
          <div class="widget-right-pane-config">
            <el-collapse v-model="collapsActiveNames" accordion>
              <el-collapse-item title="图表主题" name="1">
                <el-form size="mini">
                  <el-form-item>
                    <el-select v-model="widget.theme">
                      <el-option
                        v-for="(theme, index) in chartThemes"
                        :key="index"
                        :label="theme"
                        :value="theme"
                      />
                    </el-select>
                  </el-form-item>
                </el-form>
              </el-collapse-item>
              <el-collapse-item title="标题组件" name="2">
                <el-form size="mini">
                  <el-form-item label="是否显示">
                    <el-switch v-model="widget.options.title.show" active-text="开启" />
                  </el-form-item>
                  <el-form-item label="主标题文本">
                    <el-input v-model="widget.options.title.text" />
                  </el-form-item>
                  <el-form-item label="副标题文本">
                    <el-input v-model="widget.options.title.subtext" />
                  </el-form-item>
                  <el-form-item label="离左侧的距离">
                    <el-slider v-model="widget.options.title.leftVal" @change="widget.options.title.left = widget.options.title.leftVal + '%'" />
                  </el-form-item>
                  <el-form-item label="离上侧的距离">
                    <el-slider v-model="widget.options.title.topVal" @change="widget.options.title.top = widget.options.title.topVal + '%'" />
                  </el-form-item>
                  <el-form-item label="主标题字体大小">
                    <el-input-number v-model="widget.options.title.textStyle.fontSize" :min="1" :max="30" />
                  </el-form-item>
                  <el-form-item label="主标题文字的颜色">
                    <el-color-picker v-model="widget.options.title.textStyle.color" />
                  </el-form-item>
                  <el-form-item label="副标题字体大小">
                    <el-input-number v-model="widget.options.title.subtextStyle.fontSize" :min="1" :max="20" />
                  </el-form-item>
                  <el-form-item label="副标题文字的颜色">
                    <el-color-picker v-model="widget.options.title.subtextStyle.color" />
                  </el-form-item>
                </el-form>
              </el-collapse-item>
              <el-collapse-item title="图例组件" name="3">
                <el-form size="mini">
                  <el-form-item label="是否显示">
                    <el-switch v-model="widget.options.legend.show" active-text="开启" />
                  </el-form-item>
                  <el-form-item label="图例的类型">
                    <el-select v-model="widget.options.legend.type">
                      <el-option label="普通图例" value="plain" />
                      <el-option label="可滚动翻页的图例" value="scroll" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="离左侧的距离">
                    <el-slider v-model="widget.options.legend.leftVal" @change="widget.options.legend.left = widget.options.legend.leftVal + '%'" />
                  </el-form-item>
                  <el-form-item label="离上侧的距离">
                    <el-slider v-model="widget.options.legend.topVal" @change="widget.options.legend.top = widget.options.legend.topVal + '%'" />
                  </el-form-item>
                  <el-form-item label="列表的布局朝向">
                    <el-select v-model="widget.options.legend.orient">
                      <el-option label="横向" value="horizontal" />
                      <el-option label="纵向" value="vertical" />
                    </el-select>
                  </el-form-item>
                </el-form>
              </el-collapse-item>
              <el-collapse-item title="数据系列" name="4">
                <el-form size="mini">
                  <el-form-item>
                    <el-select v-model="widget.seriesType">
                      <el-option
                        v-for="(item, index) in chartSeriesTypes[widget.chartType]"
                        :key="index"
                        :label="item.name"
                        :value="item.value"
                      />
                    </el-select>
                  </el-form-item>
                </el-form>
              </el-collapse-item>
            </el-collapse>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script>
import { getDataChart, buildDataChart, dataParser } from '@/api/visual/datachart'
import { getDataSet, listDataSet } from '@/api/visual/dataset'
import draggable from 'vuedraggable'
import { chartTypes, chartThemes, chartSeriesTypes, chartOptions } from '@/utils/visual-chart'
import ChartPanel from './components/ChartPanel'
import { compressImg, dataURLtoBlob } from '@/utils/compressImage'

export default {
  name: 'DataChartBuild',
  components: {
    draggable,
    ChartPanel
  },
  data() {
    return {
      dataChart: {},
      widget: {
        dataSetId: undefined,
        chartType: 'table',
        rows: [],
        columns: [],
        measures: [],
        // 过滤条件
        filters: [],
        // 图表配置项
        options: chartOptions,
        // 图表主题
        theme: 'default',
        // 系列类型
        seriesType: undefined
      },
      dataSetOptions: [],
      dataSet: {},
      dimensions: [],
      measures: [],
      chartTypes,
      chartThemes,
      chartSeriesTypes,
      chartData: {
        data: [],
        sql: ''
      },
      visible: false,
      isCollapse: false,
      collapsActiveNames: '1',
      // 文件上传
      fileList: [],
      hideUpload: false
    }
  },
  created() {
    this.getDataChart(this.$route.params.id)
    this.getDataSetList()
  },
  methods: {
    getDataChart(id) {
      getDataChart(id).then(response => {
        if (response.success) {
          this.dataChart = response.data
          if (this.dataChart.chartThumbnail) {
            const blob = dataURLtoBlob(this.dataChart.chartThumbnail)
            const fileUrl = URL.createObjectURL(blob)
            this.fileList.push({ url: fileUrl })
            this.hideUpload = true
          }
          if (this.dataChart.chartConfig) {
            const chartConfig = JSON.parse(this.dataChart.chartConfig)
            getDataSet(chartConfig.dataSetId).then(response => {
              if (response.success) {
                this.dataSet = response.data
                this.dimensions = this.dataSet.schemaConfig.dimensions.filter(x => [...chartConfig.rows, ...chartConfig.columns].every(y => y.col !== x.col))
                this.measures = this.dataSet.schemaConfig.measures.filter(x => chartConfig.measures.every(y => y.col !== x.col))
              }
            })
            this.widget.dataSetId = chartConfig.dataSetId
            this.widget.chartType = chartConfig.chartType
            this.widget.rows = chartConfig.rows || []
            this.widget.columns = chartConfig.columns || []
            this.widget.measures = chartConfig.measures || []
          }
        }
      })
    },
    getDataSetList() {
      listDataSet().then(response => {
        if (response.success) {
          this.dataSetOptions = response.data
        }
      })
    },
    handleCommand(id) {
      getDataSet(id).then(response => {
        if (response.success) {
          this.dataSet = response.data
          this.widget.dataSetId = this.dataSet.id
          this.handleReset()
        }
      })
    },
    handleKeyTagClose(index, tag) {
      this.widget.rows.splice(index, 1)
      this.dimensions.push(tag)
    },
    handleGroupTagClose(index, tag) {
      this.widget.columns.splice(index, 1)
      this.dimensions.push(tag)
    },
    handleValueDragChange(tag) {
      if (tag.added) {
        this.$set(tag.added.element, 'aggregateType', 'sum')
      }
    },
    handleValueTagClose(index, tag) {
      this.widget.measures.splice(index, 1)
      tag.aggregateType = ''
      this.measures.push(tag)
    },
    handleCollapse() {
      this.isCollapse = !this.isCollapse
    },
    handlePreview() {
      dataParser(this.widget).then(response => {
        if (response.success) {
          const { data } = response
          this.chartData.data = data.data
          this.chartData.sql = data.sql
          this.visible = true
        }
      })
    },
    handleReset() {
      this.dimensions = JSON.parse(JSON.stringify(this.dataSet.schemaConfig.dimensions))
      this.measures = JSON.parse(JSON.stringify(this.dataSet.schemaConfig.measures))
      this.widget.chartType = 'table'
      this.widget.rows = []
      this.widget.columns = []
      this.widget.measures = []
      this.chartData.data = []
      this.chartData.sql = ''
      this.visible = false
    },
    handleSubmit() {
      const data = {
        id: this.dataChart.id,
        chartThumbnail: this.dataChart.chartThumbnail,
        chartConfig: JSON.stringify(this.widget)
      }
      buildDataChart(data).then(response => {
        if (response.success) {
          this.$message.success('保存成功')
        }
      })
    },
    handleCancel() {
      window.location.href = 'about:blank'
      window.close()
    },
    changeChart(chart) {
      this.widget.chartType = chart
      this.widget.seriesType = ((this.chartSeriesTypes[this.widget.chartType] || [])[0] || {}).value || undefined
    },
    beforeUpload(file) {
      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isLt2M) {
        this.$message.error('上传图片大小不能超过 2MB')
      }
      return isLt2M
    },
    handleChange(file, fileList) {
      this.hideUpload = fileList.length >= 1
      const config = {
        width: 250, // 压缩后图片的宽
        height: 150, // 压缩后图片的高
        quality: 0.8 // 压缩后图片的清晰度，取值0-1，值越小，所绘制出的图像越模糊
      }
      compressImg(file.raw, config).then(result => {
        // result 为压缩后二进制文件
        this.$set(this.dataChart, 'chartThumbnail', result)
      })
    },
    handleRemove(file, fileList) {
      this.hideUpload = fileList.length >= 1
      this.$set(this.dataChart, 'chartThumbnail', 'x')
    }
  }
}
</script>

<style lang="scss" scoped>
.chart-container {
  height: 100vh;
  width: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: row;
  flex: 1;
  flex-basis: auto;
  box-sizing: border-box;
  min-width: 0;
  ::-webkit-scrollbar {
    width: 3px;
    height: 5px;
  }
  ::-webkit-scrollbar-track-piece {
    background: #d3dce6;
  }
  ::-webkit-scrollbar-thumb {
    background: #99a9bf;
    border-radius: 10px;
  }
  .widget-left-container {
    width: 250px;
    box-sizing: border-box;
    .widget-left-header {
      height: 40px;
      line-height: 40px;
      padding: 0 20px;
      .el-dropdown {
        cursor: pointer;
      }
    }
    .widget-left-field {
      height: 250px;
      .widget-left-field-cate {
        height: 40px;
        line-height: 40px;
        text-align: center;
        border-top: 1px solid #e4e7ed;
        border-bottom: 1px solid #e4e7ed;
        i {
          margin-right: 5px;
        }
      }
      .widget-left-field-draggable {
        height: calc(100% - 40px);
        padding: 20px;
        overflow-x: hidden;
        overflow-y: auto;
        .draggable-label {
          font-size: 12px;
          display: block;
          width: 90px;
          position: relative;
          float: left;
          left: 0;
          margin: 5px;
          color: #333;
          border: 1px solid #F4F6FC;
          &:hover {
            color: #409EFF;
            border: 1px dashed #409EFF;
          }
          & > div {
            display: block;
            cursor: move;
            text-align: center;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }
    }
  }
  .widget-center-container {
    width: calc(100% - 550px);
    flex: 1;
    flex-basis: auto;
    box-sizing: border-box;
    border-left: 1px solid #e4e7ed;
    border-right: 1px solid #e4e7ed;
    .widget-center-header {
      height: 40px;
      line-height: 40px;
      border-bottom: 1px solid #e4e7ed;
      .widget-center-header-collapse {
        float: right;
        background-color: #f0f8ff;
        .is-active {
          transform: rotate(180deg);
        }
      }
      .widget-center-header-button {
        text-align: right;
        padding-right: 20px;
        float: right;
      }
    }
    .widget-center-content {
      height: 100%;
      .widget-center-draggable-wrapper {
        height: 266px;
        padding: 0 20px;
        border-bottom: 1px solid #e4e7ed;
        .widget-center-draggable-text {
          min-height: 40px;
          height: auto;
          line-height: 40px;
          border: 1px solid #d7dae2;
          background: #f4f4f7;
          overflow-x: auto;
          overflow-y: hidden;
          width: 100%;
          white-space: nowrap;
          .widget-center-draggable-line {
            min-height: 40px;
            height: 40px;
            .draggable-item {
              margin: 0 5px;
              display: inline-block;
              border: 1px solid #ebecef;
              border-radius: 4px;
              .draggable-item-handle {
                background-color: #ecf5ff;
                border-color: #d9ecff;
                display: inline-block;
                padding: 0 10px;
                line-height: 30px;
                font-size: 12px;
                color: #409EFF;
                border-width: 1px;
                border-style: solid;
                box-sizing: border-box;
                white-space: nowrap;
                cursor: pointer;
              }
            }
          }
        }
      }
      .widget-center-pane {
        height: calc(100% - 266px);
        .widget-center-pane-chart {
          height: 200px;
          padding: 0 20px;
          overflow-x: hidden;
          overflow-y: auto;
        }
        .widget-center-pane-script {
          height: 200px;
          padding: 0 20px;
          overflow-x: hidden;
          overflow-y: auto;
        }
      }
    }
  }
  .widget-right-container {
    width: 300px;
    box-sizing: border-box;
    // 折叠
    &.hideRightContainer{
      width: 0px;
    }
    .widget-right-pane-form {
      height: calc(100vh - 40px);
      overflow-x: hidden;
      overflow-y: auto;
      .hideUpload ::v-deep {
        .el-upload--picture-card {
          display: none;
        }
      }
      ::v-deep {
        .el-upload-list__item {
          width: 250px;
          height: 150px;
        }
      }
      .chart-type-list {
        width: 100%;
        display: grid;
        justify-items: center;
        align-content: center;
        grid-template-columns: repeat(5, 40px);
        grid-template-rows: repeat(4, 40px);
        grid-gap: 10px;
        span {
          height: 40px;
          width: 40px;
          line-height: 40px;
          font-size: 25px;
          cursor: pointer;
          text-align: center;
        }
        .active {
          box-shadow: 0 0 0 2px rgba(81, 130, 227, .06), inset 0 0 0 2px rgba(81, 129, 228, .6);
        }
      }
    }
    .widget-right-pane-config {
      height: calc(100vh - 40px);
      overflow-x: hidden;
      overflow-y: auto;
      .el-form-item__content {
        .el-slider {
          padding: 0 10px;
          margin-top: 20px;
        }
      }
    }
  }
}
</style>
