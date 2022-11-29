<template>
  <div class="board-container">
    <div class="widget-left-container">
      <div class="widget-left-header">
        <span>{{ dataBoard.boardName }}</span>
      </div>
      <div class="widget-left-wrapper">
        <ul class="list-group">
          <li v-for="(item, index) in dataChartList" :key="item.id" class="list-group-item">
            <div class="list-group-item-text">{{ item.chartName }}</div>
            <div class="list-group-item-button"><el-button icon="el-icon-plus" type="text" size="mini" :disabled="item.disabled" @click="handleAddChart(item)"></el-button></div>
          </li>
        </ul>
      </div>
    </div>
    <div class="widget-center-container">
      <div class="widget-center-header">
        <div class="widget-center-header-collapse" @click="drawer = true"><i class="el-icon-info"></i></div>
        <div class="widget-center-header-button">
          <el-button icon="el-icon-view" type="text" @click="handlePreview">
            预览
          </el-button>
          <el-button icon="el-icon-delete" type="text" @click="handleReset">
            重置
          </el-button>
          <el-button icon="el-icon-plus" type="text" v-hasPerm="['visual:board:build']" @click="handleSubmit">
            保存
          </el-button>
          <el-button icon="el-icon-close" type="text" @click="handleCancel">
            取消
          </el-button>
        </div>
      </div>
      <div class="widget-center-wrapper">
        <grid-layout
          :layout.sync="layout"
          :col-num="24"
          :row-height="30"
          :is-draggable="true"
          :is-resizable="true"
          :is-mirrored="false"
          :vertical-compact="true"
          :use-css-transforms="true"
          :pane-container="false"
          :margin="[10, 10]"
          style="border: 1px dashed #999; height: 100%; overflow-x: hidden; overflow-y: auto;"
        >
          <grid-item
            v-for="item in layout"
            :key="item.i"
            :x="item.x"
            :y="item.y"
            :w="item.w"
            :h="item.h"
            :i="item.i"
            drag-allow-from=".vue-draggable-handle"
            @resized="handleResize"
          >
            <el-card v-loading="getChartItem(item.i).loading" class="widget-center-card" body-style="padding: 10px;">
              <div slot="header" class="widget-center-card-header vue-draggable-handle">
                <div>
                  <span>{{ getChartItem(item.i).chartName }}</span>
                </div>
                <div>
                  <i class="el-icon-delete" @click="handleDeleteChart(item.i)" />
                  <i class="el-icon-setting" @click="handleTimerChart(item.i)" />
                </div>
              </div>
              <chart-panel v-if="getChartItem(item.i).visible" :key="item.i" :ref="`charts${item.i}`" :chart-schema="getChartItem(item.i).chartSchema" :chart-data="getChartItem(item.i).data" :chart-style="{height: `${item.h * 30 + 10 * (item.h - 1) - 60}px`}" />
              <div v-else :style="{height: `${item.h * 30 + 10 * (item.h - 1) - 60}px`}"></div>
            </el-card>
          </grid-item>
        </grid-layout>
      </div>
    </div>
    <el-drawer
      size="300px"
      :visible.sync="drawer"
      :with-header="false">
      <div class="widget-board-form">
        <el-form size="mini" label-position="top">
          <el-form-item label="看板名称">
            <el-input v-model="dataBoard.boardName" size="mini" :disabled="true" />
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
    </el-drawer>
  </div>
</template>

<script>
import { getDataBoard, buildDataBoard } from '@/api/visual/databoard'
import { listDataChart, getDataChart, dataParser } from '@/api/visual/datachart'
import VueGridLayout from 'vue-grid-layout'
import ChartPanel from '../datachart/components/ChartPanel'
import { compressImg, dataURLtoBlob } from '@/utils/compressImage'

export default {
  name: 'DataBoardBuild',
  components: {
    GridLayout: VueGridLayout.GridLayout,
    GridItem: VueGridLayout.GridItem,
    ChartPanel
  },
  data() {
    return {
      dataBoard: {},
      dataChartList: [],
      layout: [],
      interval: [],
      charts: [],
      drawer: false,
      // 文件上传
      fileList: [],
      hideUpload: false
    }
  },
  created() {
    this.getDataBoard(this.$route.params.id)
    this.getDataChartList()
  },
  methods: {
    getDataBoard(id) {
      getDataBoard(id).then(response => {
        if (response.success) {
          this.dataBoard = response.data
          if (this.dataBoard.boardThumbnail) {
            const blob = dataURLtoBlob(this.dataBoard.boardThumbnail)
            const fileUrl = URL.createObjectURL(blob)
            this.fileList.push({ url: fileUrl })
            this.hideUpload = true
          }
          this.layout = this.dataBoard.boardConfig ? JSON.parse(JSON.stringify(this.dataBoard.boardConfig.layout)) : []
          this.interval = this.dataBoard.boardConfig ? JSON.parse(JSON.stringify(this.dataBoard.boardConfig.interval)) : []
          const charts = this.dataBoard.charts ? JSON.parse(JSON.stringify(this.dataBoard.charts)) : []
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
      const index = this.layout.findIndex(item => item.i === chart.id)
      if (index !== -1) {
        this.$set(chart, 'disabled', true)
        return
      }
      getDataChart(chart.id).then(response => {
        if (response.success) {
          const obj = {
            x: 0,
            y: 0,
            w: 12,
            h: 9,
            i: chart.id
          }
          this.layout.push(obj)
          const data = response.data
          this.parserChart(data)
          this.charts.push(data)
          this.$set(chart, 'disabled', true)
        }
      })
    },
    handleDeleteChart(id) {
      this.layout.splice(this.layout.findIndex(item => item.i === id), 1)
      this.charts.splice(this.charts.findIndex(item => item.id === id), 1)
      this.$set(this.dataChartList.find(item => item.id === id), 'disabled', false)
    },
    handleTimerChart(id) {
      this.$prompt('请输入定时时间间隔，输入0不定时（单位毫秒，1000 毫秒 = 1 秒）', '提示', {
        showClose: false,
        closeOnClickModal: false,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^(0|\+?[1-9][0-9]*)$/,
        inputErrorMessage: '格式不正确',
        inputValue: '5000'
      }).then(({ value }) => {
        const timer = this.interval.find(item => item.id === id)
        if (timer) {
          this.$set(timer, 'milliseconds', value)
        } else {
          this.interval.push({ id: id, milliseconds: value })
        }
      }).catch(() => {})
    },
    handlePreview() {
      const route = this.$router.resolve({ path: `/visual/board/view/${this.dataBoard.id}` })
      window.open(route.href, '_blank')
    },
    handleReset() {
      this.layout = this.dataBoard.boardConfig ? JSON.parse(JSON.stringify(this.dataBoard.boardConfig.layout)) : []
      this.interval = this.dataBoard.boardConfig ? JSON.parse(JSON.stringify(this.dataBoard.boardConfig.interval)) : []
      const charts = this.dataBoard.charts ? JSON.parse(JSON.stringify(this.dataBoard.charts)) : []
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
        id: this.dataBoard.id,
        boardThumbnail: this.dataBoard.boardThumbnail,
        boardConfig: {
          layout: this.layout,
          interval: this.interval
        }
      }
      buildDataBoard(data).then(response => {
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
        this.$set(this.dataBoard, 'boardThumbnail', result)
      })
    },
    handleRemove(file, fileList) {
      this.hideUpload = fileList.length >= 1
      this.$set(this.dataBoard, 'boardThumbnail', 'x')
    }
  }
}
</script>

<style lang="scss" scoped>
.board-container {
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
    width: calc(100% - 250px);
    flex: 1;
    flex-basis: auto;
    box-sizing: border-box;
    border-left: 1px solid #e4e7ed;
    border-right: 1px solid #e4e7ed;
    .widget-center-header {
      height: 40px;
      line-height: 40px;
      .widget-center-header-collapse {
        float: right;
        padding-right: 20px;
      }
      .widget-center-header-button {
        float: right;
        text-align: right;
        padding-right: 20px;
      }
    }
    .widget-center-wrapper {
      height: calc(100% - 40px);
      padding: 10px;
      overflow-x: hidden;
      overflow-y: auto;
      border-top: 1px solid #e4e7ed;
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
  .widget-board-form {
    padding: 20px;
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
  }
}
</style>
