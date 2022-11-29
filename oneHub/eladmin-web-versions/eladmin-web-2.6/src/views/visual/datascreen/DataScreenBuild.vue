<template>
  <div class="screen-container">
    <div class="widget-left-container">
      <div class="widget-left-header">
        <span>{{ dataScreen.screenName }}</span>
      </div>
      <div class="widget-left-wrapper">
        <ul class="list-group">
          <li v-for="(item, index) in dataChartList" :key="item.id" class="list-group-item">
            <div class="list-group-item-text">{{ item.chartName }}</div>
            <div class="list-group-item-button"><el-button icon="el-icon-plus" type="text" size="mini" :disabled="item.disabled" @click="handleAddChart(item)" /></div>
          </li>
        </ul>
      </div>
    </div>
    <div class="widget-center-container">
      <div class="widget-center-header">
        <div class="widget-center-header-button">
          <el-button icon="el-icon-view" type="text" @click="handlePreview">
            预览
          </el-button>
          <el-button icon="el-icon-delete" type="text" @click="handleReset">
            重置
          </el-button>
          <el-button v-hasPerm="['visual:screen:build']" icon="el-icon-plus" type="text" @click="handleSubmit">
            保存
          </el-button>
          <el-button icon="el-icon-close" type="text" @click="handleCancel">
            取消
          </el-button>
        </div>
      </div>
      <div ref="widgetCenterSection" class="widget-center-section">
        <div class="widget-center-content" :style="{ width: contentStyle.width + 'px', height: contentStyle.height + 'px' }">
          <div class="widget-center-wrapper" :style="{ width: widget.width + 'px', height: widget.height + 'px', transform: `scale(${widget.scale / 100})`, background: 'url('+ publicPath + widget.backgroundImage +') 0% 0% / 100% 100%' }" @click="handleLayoutClick">
            <vdr
              v-for="item in widget.layout"
              :key="item.i"
              :x="item.x"
              :y="item.y"
              :w="item.w"
              :h="item.h"
              :parent="true"
              :draggable="true"
              :resizable="true"
              @dragstop="onDragStop(arguments, item)"
              @resizestop="onResizeStop(arguments, item)"
              :is-conflict-check="true"
              :grid=[20,20]
            >
              <screen-border :border-box="`${getChartProperty(item.i) ? getChartProperty(item.i).border : 'BorderBox0'}`" :border-title="getChartItem(item.i).chartName" :border-style="{ height: `${item.h}`, width: `${item.w}` }">
                <div v-loading="getChartItem(item.i).loading" :style="{backgroundColor: `${getChartProperty(item.i) ? getChartProperty(item.i).backgroundColor : 'rgba(255, 255, 255, 0.1)'}`}" @click.stop="handleItemClick(item.i)">
                  <chart-panel v-if="getChartItem(item.i).visible" :key="item.i" :ref="`charts${item.i}`" :chart-schema="getChartItem(item.i).chartSchema" :chart-data="getChartItem(item.i).data" :chart-style="{ height: `${item.h}px`, width: `${item.w}px` }" />
                  <div v-else :style="{ height: `${item.h}px`, width: `${item.w}px` }" />
                </div>
              </screen-border>
            </vdr>
          </div>
        </div>
      </div>
    </div>
    <div class="widget-right-container">
      <el-tabs v-model="activeTabName" type="border-card" stretch class="widget-right-tab">
        <el-tab-pane label="酷屏属性" name="first">
          <div class="widget-right-pane-screen">
            <el-form size="mini" label-position="top">
              <el-form-item label="酷屏宽度">
                <el-input-number v-model="widget.width" controls-position="right" :min="1280" />
              </el-form-item>
              <el-form-item label="酷屏高度">
                <el-input-number v-model="widget.height" controls-position="right" :min="800" />
              </el-form-item>
              <el-form-item label="缩放">
                <el-slider v-model="widget.scale" :format-tooltip="formatTooltip"></el-slider>
              </el-form-item>
              <el-form-item label="背景图片">
                <el-select v-model="widget.backgroundImage">
                  <el-option
                    v-for="(banner, index) in banners"
                    :key="index"
                    :label="banner.src"
                    :value="banner.src"
                  />
                </el-select>
                <el-image :src="publicPath + widget.backgroundImage" style="width: 250px; height: 150px;" />
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
            </el-form>
          </div>
        </el-tab-pane>
        <el-tab-pane label="图表属性" name="second" :disabled="true">
          <div class="widget-right-pane-chart">
            <el-form size="mini" label-position="top">
              <el-form-item label="图表名称">
                <el-input v-model="currentChartProperty.chartName" :disabled="true" style="width: 89%;" />
                <el-button v-if="currentChartProperty.id" type="danger" icon="el-icon-delete" circle @click="handleDeleteChart(currentChartProperty.id)" />
              </el-form-item>
              <el-form-item label="图表边框">
                <el-select v-model="currentChartProperty.border">
                  <el-option
                    v-for="(border, index) in borders"
                    :key="index"
                    :label="border.label"
                    :value="border.component"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="背景颜色">
                <el-color-picker v-model="currentChartProperty.backgroundColor" show-alpha />
              </el-form-item>
              <el-form-item label="图表定时时间间隔">
                <el-input-number v-model="currentChartProperty.milliseconds" controls-position="right" :min="0" :step="1000" />
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script>
import { getDataScreen, buildDataScreen } from '@/api/visual/datascreen'
import { listDataChart, getDataChart, dataParser } from '@/api/visual/datachart'
import ChartPanel from '../datachart/components/ChartPanel'
import { compressImg, dataURLtoBlob } from '@/utils/compressImage'
import vdr from 'vue-draggable-resizable-gorkys'
import 'vue-draggable-resizable-gorkys/dist/VueDraggableResizable.css'
import ScreenBorder from './components/ScreenBorder'

export default {
  name: 'DataScreenBuild',
  components: {
    vdr,
    ChartPanel,
    ScreenBorder
  },
  data() {
    return {
      dataScreen: {},
      dataChartList: [],
      widget: {
        width: 1920,
        height: 1080,
        scale: 100,
        backgroundImage: 'images/bg8.jpg',
        layout: [],
        property: []
      },
      currentChartProperty: {},
      charts: [],
      contentStyle: {
        width: 0,
        height: 0
      },
      // 文件上传
      fileList: [],
      hideUpload: false,
      // 背景图片
      publicPath: process.env.BASE_URL,
      activeTabName: 'first',
      banners: [
        { src: 'images/bg1.png' },
        { src: 'images/bg2.png' },
        { src: 'images/bg3.png' },
        { src: 'images/bg4.jpg' },
        { src: 'images/bg5.jpg' },
        { src: 'images/bg6.jpg' },
        { src: 'images/bg7.jpg' },
        { src: 'images/bg8.jpg' },
        { src: 'images/bg9.jpg' },
        { src: 'images/bg10.jpg' }
      ],
      borders: [
        { label: 'dv-border-box-0', component: 'BorderBox0' },
        { label: 'dv-border-box-1', component: 'BorderBox1' },
        { label: 'dv-border-box-2', component: 'BorderBox2' },
        { label: 'dv-border-box-3', component: 'BorderBox3' },
        { label: 'dv-border-box-4', component: 'BorderBox4' }, { label: 'dv-border-box-4-reverse', component: 'BorderBox4Reverse' },
        { label: 'dv-border-box-5', component: 'BorderBox5' }, { label: 'dv-border-box-5-reverse', component: 'BorderBox5Reverse' },
        { label: 'dv-border-box-6', component: 'BorderBox6' },
        { label: 'dv-border-box-7', component: 'BorderBox7' },
        { label: 'dv-border-box-8', component: 'BorderBox8' }, { label: 'dv-border-box-8-reverse', component: 'BorderBox8Reverse' },
        { label: 'dv-border-box-9', component: 'BorderBox9' },
        { label: 'dv-border-box-10', component: 'BorderBox10' },
        { label: 'dv-border-box-11', component: 'BorderBox11' },
        { label: 'dv-border-box-12', component: 'BorderBox12' },
        { label: 'dv-border-box-13', component: 'BorderBox13' }
      ]
    }
  },
  computed: {
    maxRows() {
      return Math.floor(this.widget.height / 30)
    }
  },
  created() {
    this.getDataScreen(this.$route.params.id)
    this.getDataChartList()
  },
  mounted() {
    const width = this.$refs.widgetCenterSection.clientWidth
    const height = this.$refs.widgetCenterSection.clientHeight
    this.contentStyle.width = width
    this.contentStyle.height = height
  },
  methods: {
    getDataScreen(id) {
      getDataScreen(id).then(response => {
        if (response.success) {
          this.dataScreen = response.data
          console.log(this.dataScreen);
          if (this.dataScreen.screenThumbnail) {
            const blob = dataURLtoBlob(this.dataScreen.screenThumbnail)
            const fileUrl = URL.createObjectURL(blob)
            this.fileList.push({ url: fileUrl })
            this.hideUpload = true
          }
          // zrx add
          if (this.dataScreen.screenConfig) {
            this.widget.width = this.dataScreen.screenConfig.width
            this.widget.height = this.dataScreen.screenConfig.height
            this.widget.scale = this.dataScreen.screenConfig.scale
            this.widget.backgroundImage = this.dataScreen.screenConfig.backgroundImage
          }
          this.widget.layout = this.dataScreen.screenConfig ? JSON.parse(JSON.stringify(this.dataScreen.screenConfig.layout)) : []
          this.widget.property = this.dataScreen.screenConfig ? JSON.parse(JSON.stringify(this.dataScreen.screenConfig.property)) : []
          const charts = this.dataScreen.charts ? JSON.parse(JSON.stringify(this.dataScreen.charts)) : []
          charts.forEach((item, index, arr) => {
            this.parserChart(item)
          })
          this.charts = charts
        }
      })
    },
    parserChart(chart) {
      this.$set(chart, 'loading', true)
      if (chart.chartConfig) {
        dataParser(JSON.parse(chart.chartConfig)).then(response => {
          if (response.success) {
            this.$set(chart, 'data', response.data.data)
            this.$set(chart, 'chartSchema', JSON.parse(chart.chartConfig))
            this.$set(chart, 'loading', false)
            this.$set(chart, 'visible', true)
          }
        })
      } else {
        this.$set(chart, 'loading', false)
      }
    },
    getChartItem(id) {
      return this.charts.find(chart => chart.id === id)
    },
    getDataChartList() {
      listDataChart().then(response => {
        if (response.success) {
          this.dataChartList = response.data
        }
      })
    },
    handleAddChart(chart) {
      const index = this.widget.layout.findIndex(item => item.i === chart.id)
      if (index !== -1) {
        this.$set(chart, 'disabled', true)
        return
      }
      getDataChart(chart.id).then(response => {
        if (response.success) {
          const obj = {
            x: 0,
            y: 0,
            w: 200,
            h: 200,
            i: chart.id
          }
          this.widget.layout.push(obj)
          const data = response.data
          this.parserChart(data)
          this.charts.push(data)
          this.$set(chart, 'disabled', true)
        }
      })
    },
    handleDeleteChart(id) {
      this.widget.layout.splice(this.widget.layout.findIndex(item => item.i === id), 1)
      this.widget.property.splice(this.widget.property.findIndex(item => item.i === id), 1)
      this.charts.splice(this.charts.findIndex(item => item.id === id), 1)
      this.$set(this.dataChartList.find(item => item.id === id), 'disabled', false)
      this.currentChartProperty = {}
      this.activeTabName = 'first'
    },
    handlePreview() {
      const route = this.$router.resolve({ path: `/visual/screen/view/${this.dataScreen.id}` })
      window.open(route.href, '_blank')
    },
    handleReset() {
      this.widget.layout = this.dataScreen.screenConfig ? JSON.parse(JSON.stringify(this.dataScreen.screenConfig.layout)) : []
      this.widget.property = this.dataScreen.screenConfig ? JSON.parse(JSON.stringify(this.dataScreen.screenConfig.property)) : []
      const charts = this.dataScreen.charts ? JSON.parse(JSON.stringify(this.dataScreen.charts)) : []
      charts.forEach((item, index, arr) => {
        this.parserChart(item)
      })
      this.charts = charts
      this.dataChartList.forEach((item, index, arr) => {
        if (charts.findIndex(chart => chart.id === item.id) === -1) {
          this.$set(item, 'disabled', false)
        } else {
          this.$set(item, 'disabled', true)
        }
      })
    },
    handleSubmit() {
      const data = {
        id: this.dataScreen.id,
        screenThumbnail: this.dataScreen.screenThumbnail,
        screenConfig: this.widget
      }
      buildDataScreen(data).then(response => {
        if (response.success) {
          this.$message.success('保存成功')
        }
      })
    },
    handleCancel() {
      window.location.href = 'about:blank'
      window.close()
    },
    handleResize(i, newH, newW, newHPx, newWPx) {
      if (this.charts.find(chart => chart.id === i).visible) {
        this.$refs[`charts${i}`][0].$children[0].$emit('resized')
      }
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
        this.$set(this.dataScreen, 'screenThumbnail', result)
      })
    },
    handleRemove(file, fileList) {
      this.hideUpload = fileList.length >= 1
      this.$set(this.dataScreen, 'screenThumbnail', 'x')
    },
    handleLayoutClick() {
      this.activeTabName = 'first'
    },
    handleItemClick(id) {
      const chart = this.charts.find(chart => chart.id === id)
      let property = this.widget.property.find(item => item.id === id)
      if (!property) {
        property = {
          id: id,
          chartName: chart.chartName,
          border: 'BorderBox0',
          milliseconds: 0,
          backgroundColor: 'rgba(255, 255, 255, 0.1)'
        }
        this.widget.property.push(property)
      }
      this.currentChartProperty = property
      this.activeTabName = 'second'
    },
    getChartProperty(id) {
      return this.widget.property.find(property => property.id === id)
    },
    onDragStop([x, y], item) {
      this.$set(item, 'x', x)
      this.$set(item, 'y', y)
    },
    onResizeStop([x, y, w, h], item) {
      this.$set(item, 'x', x)
      this.$set(item, 'y', y)
      this.$set(item, 'w', w)
      this.$set(item, 'h', h)
      if (this.charts.find(chart => chart.id === item.i).visible) {
        this.$refs[`charts${item.i}`][0].$children[0].$emit('resized')
      }
    },
    formatTooltip(val) {
      return val / 100
    }
  }
}
</script>

<style lang="scss" scoped>
.screen-container {
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
    }
    .widget-left-wrapper {
      height: calc(100% - 40px);
      padding: 20px;
      overflow-x: hidden;
      overflow-y: auto;
      border-top: 1px solid #e4e7ed;
      .list-group {
        padding: 0;
        margin: 0;
        .list-group-item {
          display: flex;
          justify-content: space-between;
          height: 30px;
          line-height: 30px;
          font-size: 14px;
          .list-group-item-text {
            width: 130px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
          .list-group-item-button {
            cursor: pointer;
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
      .widget-center-header-button {
        float: right;
        text-align: right;
        padding-right: 20px;
      }
    }
    .widget-center-section {
      height: calc(100% - 40px);
      overflow: auto;
      width: 100%;
      .widget-center-content{
        transform-origin: 0 0;
        .widget-center-wrapper {
          overflow: auto;
          user-select: none;
          transform-origin: 0 0;
          position: relative;
          ::v-deep .vdr {
            border: none;
            // 活动状态下
            &.active {
              border: 1px dashed #09f;
            }
            .handle {
              border-radius: 100%;
              background-color: #09f;
            }
          }
          .widget-center-card {
            ::v-deep .el-card__header {
              padding: 0;
              .widget-center-card-header {
                font-size: 14px;
                display: flex;
                justify-content: space-between;
                height: 30px;
                padding: 0 10px;
                line-height: 30px;
                i {
                  margin-right: 10px;
                  color: #409EFF;
                  cursor: pointer;
                }
              }
            }
          }
        }
      }
    }
  }
  .widget-right-container {
    width: 300px;
    box-sizing: border-box;
    .widget-right-pane-screen {
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
        .el-slider__runway {
          width: 90%;
        }
      }
    }
    .widget-right-pane-chart {
      height: calc(100vh - 40px);
      overflow-x: hidden;
      overflow-y: auto;
    }
  }
}
</style>
