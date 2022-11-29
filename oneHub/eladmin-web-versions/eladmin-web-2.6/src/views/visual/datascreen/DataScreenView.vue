<template>
  <div class="screen-container">
    <div class="widget-center-wrapper" :style="{ background: 'url('+ publicPath + widget.backgroundImage +') 0% 0% / 100% 100%' }">
      <vdr
        v-for="item in widget.layout"
        :key="item.i"
        :x="item.x"
        :y="item.y"
        :w="item.w"
        :h="item.h"
        :parent="true"
        :draggable="false"
        :resizable="false"
        :is-conflict-check="true"
        :grid=[20,20]
      >
        <screen-border :border-box="`${getChartProperty(item.i) ? getChartProperty(item.i).border : 'BorderBox0'}`" :border-title="getChartItem(item.i).chartName" :border-style="{ height: `${item.h}`, width: `${item.w}` }">
          <div v-loading="getChartItem(item.i).loading" :style="{backgroundColor: `${getChartProperty(item.i) ? getChartProperty(item.i).backgroundColor : 'rgba(255, 255, 255, 0.1)'}`}">
            <chart-panel v-if="getChartItem(item.i).visible" :key="item.i" :ref="`charts${item.i}`" :chart-schema="getChartItem(item.i).chartSchema" :chart-data="getChartItem(item.i).data" :chart-style="{ height: `${item.h}px`, width: `${item.w}px` }" />
            <div v-else :style="{height: `${item.h}px`, width: `${item.w}px` }" />
          </div>
        </screen-border>
      </vdr>
    </div>
  </div>
</template>

<script>
import { getDataScreen } from '@/api/visual/datascreen'
import { dataParser } from '@/api/visual/datachart'
import vdr from 'vue-draggable-resizable-gorkys'
import 'vue-draggable-resizable-gorkys/dist/VueDraggableResizable.css'
import ChartPanel from '../datachart/components/ChartPanel'
import ScreenBorder from './components/ScreenBorder'

export default {
  name: 'DataScreenView',
  components: {
    vdr,
    ChartPanel,
    ScreenBorder
  },
  data() {
    return {
      dataScreen: {},
      widget: {
        width: 1920,
        height: 1080,
        scale: 100,
        backgroundImage: 'images/bg8.jpg',
        layout: [],
        property: []
      },
      charts: [],
      timers: [],
      publicPath: process.env.BASE_URL
    }
  },
  created() {
    this.getDataScreen(this.$route.params.id)
  },
  methods: {
    getDataScreen(id) {
      getDataScreen(id).then(response => {
        if (response.success) {
          this.dataScreen = response.data
          //zrx add
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
    getChartProperty(id) {
      return this.widget.property.find(property => property.id === id)
    },
    initTimer() {
      this.widget.property.forEach((item, index) => {
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
.screen-container {
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
  .widget-center-wrapper {
    height: 100%;
    overflow: auto;
    ::v-deep .vdr {
      border: none;
    }
  }
}
</style>
