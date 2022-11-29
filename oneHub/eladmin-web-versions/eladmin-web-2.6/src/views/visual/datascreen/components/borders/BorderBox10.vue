<template>
  <div :ref="ref" class="dv-border-box-10" :style="`box-shadow: inset 0 0 25px 3px ${mergedColor[0]}`">
    <svg class="dv-border-svg-container" :width="width" :height="height">
      <polygon
        :fill="backgroundColor"
        :points="`
        4, 0 ${width - 4}, 0 ${width}, 4 ${width}, ${height - 4} ${width - 4}, ${height}
        4, ${height} 0, ${height - 4} 0, 4
      `"
      />
    </svg>

    <svg
      v-for="item in border"
      :key="item"
      width="150px"
      height="150px"
      :class="`${item} dv-border-svg-container`"
    >
      <polygon
        :fill="mergedColor[1]"
        points="40, 0 5, 0 0, 5 0, 16 3, 19 3, 7 7, 3 35, 3"
      />
    </svg>

    <div class="border-box-content">
      <slot />
    </div>
  </div>
</template>

<script>
export default {
  name: 'BorderBox10',
  props: {
    width: {
      type: Number,
      default: 0
    },
    height: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      ref: 'border-box-10',
      border: ['left-top', 'right-top', 'left-bottom', 'right-bottom'],
      backgroundColor: 'transparent',
      mergedColor: ['#1d48c4', '#d3e1f8']
    }
  }
}
</script>

<style lang="scss" scoped>
.dv-border-box-10 {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 6px;

  .dv-border-svg-container {
    position: absolute;
    display: block;
  }

  .right-top {
    right: 0px;
    transform: rotateY(180deg);
  }

  .left-bottom {
    bottom: 0px;
    transform: rotateX(180deg);
  }

  .right-bottom {
    right: 0px;
    bottom: 0px;
    transform: rotateX(180deg) rotateY(180deg);
  }

  .border-box-content {
    position: relative;
    width: 100%;
    height: 100%;
  }
}
</style>
