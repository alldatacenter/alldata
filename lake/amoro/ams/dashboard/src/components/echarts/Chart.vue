<template>
  <a-spin :spinning="loading" class="echarts-loading">
    <div ref="echart" :style="{ width: width, height: height }" class="timeline-echarts"></div>
  </a-spin>
</template>
<script lang="ts">
import { defineComponent, onMounted, onBeforeUnmount, watch, ref, toRefs, reactive } from 'vue'
import echarts from './index'

export default defineComponent({
  props: {
    width: {
      type: String,
      default: 'auto'
    },
    height: {
      type: String,
      default: '350px'
    },
    loading: {
      type: Boolean,
      default: false
    },
    options: {
      type: Object,
      default: () => {}
    }
  },
  setup(props) {
    let echartsInst: any = null
    const { options } = toRefs(props)
    const state = reactive({
      echart: ref()
    })
    const echartsInit = () => {
      echartsInst = echarts.init(state.echart)
      echartsInst.setOption({
        ...options.value
      })
    }
    const echartsOptionsUpdate = () => {
      echartsInst.setOption({
        ...options.value
      })
      echartsInst.resize()
    }

    const resize = () => {
      if (echartsInst) {
        echartsInst.resize()
      }
    }

    watch(
      () => options.value,
      (value) => {
        value && echartsOptionsUpdate()
      }, {
        deep: true
      }
    )

    onBeforeUnmount(() => {
      window.removeEventListener('resize', resize)
    })

    onMounted(() => {
      window.addEventListener('resize', resize)
      echartsInit()
    })

    return {
      ...toRefs(state)
    }
  }
})
</script>
<style lang="less">
.echarts-loading {
  width: 100% !important;
}
.timeline-echarts {
  .echarts-tooltip-dark {
    background-color: rgba(0,0,0,.7) !important;
    line-height: 20px !important;
    border: 1px solid #E9EBF1 !important;
  }
}
</style>
