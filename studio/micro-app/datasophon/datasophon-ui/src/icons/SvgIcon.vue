<!--
 * @Author: mjzhu
 * @Date: 2021-11-22 10:26:37
 * @LastEditTime: 2022-06-06 20:00:56
 * @FilePath: \ddh-ui\src\assets\img\svg\SvgIcon.vue
-->
<template>
  <svg :class="svgClass" aria-hidden="true">
    <use :xlink:href="iconName" />
  </svg>
</template>

<script>
export default {
  name: "svg-icon",
  props: {
    iconClass: {
      type: String,
      required: true,
    },
  },
  computed: {
    iconName() {
      return `#icon-${this.iconClass.toLowerCase()}`;
    },
    svgClass() {
      return `svg-icon svg-icon-${this.iconClass.toLowerCase()}`;
    },
  },
};
</script>

<style scoped>
.svg-icon {
  width: 1em;
  height: 1em;
  vertical-align: -0.2em;
  fill: currentColor;
  overflow: hidden;
  /* margin-right: 5px; */
  font-size: 14px;
}
</style>
