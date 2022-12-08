<template>
  <div class="layout">
    <div class="flex-two">
      请选择促销活动(最多只能展示两个活动)
    </div>
  </div>
</template>
<script>
export default {
  title: "两张横图",
  props: ["res"],
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.flex-two {
  width: 100%;
  display: flex;
  height: 110px;
  overflow: hidden;
}
.flex-item {
  width: 50%;
  > img {
    display: block;
    max-width: 100%;
    height: 100%;
  }
}
</style>
