<template>
  <div class="layout">
    <div class="flex-one" @click="modelNavigateTo(res.list[0])">
      <u-image width="100%" mode="aspectFit" height="280rpx" :src="res.list[0].img" alt=""></u-image>
    </div>
  </div>
</template>
<script>
import { modelNavigateTo } from "./tpl";

export default {
  title: "单行图片模块",
  data() {
    return {
      modelNavigateTo,
    };
  },
  props: ["res"],
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.flex-one {
  width: 100%;
  display: block;
  overflow: hidden;
  > img {
    width: 100%;
    height: 100%;
  }
}
</style>