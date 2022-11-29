<template>
  <div class="board-container">
    <div class="board-header">
      <span>{{ dataBoard.boardName }}</span>
    </div>
    <div class="board-wrapper">
      <grid-layout
        :layout.sync="layout"
        :col-num="24"
        :row-height="30"
        :is-draggable="false"
        :is-resizable="false"
        :is-mirrored="false"
        :vertical-compact="true"
        :use-css-transforms="true"
        :margin="[10, 10]"
        style="height: 100%; overflow-x: hidden; overflow-y: auto;"
      >
        <grid-item
          v-for="item in layout"
          :key="item.i"
          :x="item.x"
          :y="item.y"
          :w="item.w"
          :h="item.h"
          :i="item.i"
        >
          <el-card v-loading="getChartItem(item.i).loading" class="board-wrapper-card" body-style="padding: 10px;">
            <div slot="header" class="board-wrapper-card-header">
              <div>
                <span>{{ getChartItem(item.i).chartName }}</span>
              </div>
            </div>
            <chart-panel v-if="getChartItem(item.i).visible" :key="item.i" :ref="`charts${item.i}`" :chart-schema="getChartItem(item.i).chartSchema" :chart-data="getChartItem(item.i).data" :chart-style="{height: `${item.h * 30 + 10 * (item.h - 1) - 60}px`}" />
            <div v-else :style="{height: `${item.h * 30 + 10 * (item.h - 1) - 60}px`}"></div>
          </el-card>
        </grid-item>
      </grid-layout>
    </div>
  </div>
</template>

<script>
import { getDataBoard } from '@/api/visual/databoard'
import { dataParser } from '@/api/visual/datachart'
import VueGridLayout from 'vue-grid-layout'
import ChartPanel from '../datachart/components/ChartPanel'

export default {
  name: 'DataBoardView',
  components: {
    GridLayout: VueGridLayout.GridLayout,
    GridItem: VueGridLayout.GridItem,
    ChartPanel
  },
  data() {
    return {
      dataBoard: {},
      layout: [],
      interval: [],
      charts: [],
      timers: []
    }
  },
  created() {
    this.getDataBoard(this.$route.params.id)
  },
  methods: {
    getDataBoard(id) {
      getDataBoard(id).then(response => {
        if (response.success) {
          this.dataBoard = response.data
          this.layout = this.dataBoard.boardConfig ? JSON.parse(JSON.stringify(this.dataBoard.boardConfig.layout)) : []
          this.interval = this.dataBoard.boardConfig ? JSON.parse(JSON.stringify(this.dataBoard.boardConfig.interval)) : []
          const charts = this.dataBoard.charts ? JSON.parse(JSON.stringify(this.dataBoard.charts)) : []
          charts.forEach((item, index, arr) => {
            this.parserChart(item)
          })
          this.charts = charts
          this.$nextTick(() => {
            this.initTimer()
          })
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
    initTimer() {
      this.interval.forEach((item, index) => {
        if (item.milliseconds && item.milliseconds > 0) {
          const timer = setInterval(() => {
            const chart = this.charts.find(chart => chart.id === item.id)
            if (chart.chartConfig) {
              dataParser(JSON.parse(chart.chartConfig)).then(response => {
                if (response.success) {
                  this.$set(chart, 'data', response.data.data)
                }
              })
            }
          }, item.milliseconds)
          this.timers.push(timer)
        }
      })
    }
  },
  beforeDestroy() {
    this.timers.map((item) => {
      clearInterval(item)
    })
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
  .board-header{
    height: 40px;
    line-height: 40px;
    text-align: center;
  }
  .board-wrapper {
    height: calc(100% - 40px);
    overflow-x: hidden;
    overflow-y: auto;
    border-top: 1px solid #e4e7ed;
    .board-wrapper-card {
      ::v-deep .el-card__header {
        padding: 0;
        .board-wrapper-card-header {
          font-size: 14px;
          display: flex;
          justify-content: space-between;
          height: 30px;
          padding: 0 10px;
          line-height: 30px;
        }
      }
    }
  }
}
</style>
