<template>
  <div class="layout">
    <div class="flex-two">
      <div class="flex-item" @click="modelNavigateTo(res.list[0])">
        <u-image height="250rpx" width="100%" mode="scaleToFill" :src="res.list[0].img" alt>
          <u-loading slot="loading"></u-loading>
        </u-image>
      </div>
      <div class="flex-item" @click="modelNavigateTo(res.list[1])">
        <u-image height="250rpx" width="100%" mode="scaleToFill" :src="res.list[1].img" alt>
          <u-loading slot="loading"></u-loading>
        </u-image>
      </div>
    </div>
  </div>
</template>
<script>
import uImage from "@/uview-ui/components/u-image/u-image.vue";
import { modelNavigateTo } from "./tpl";
export default {
  components: { uImage },
  title: "两张横图",
  props: ["res"],
  mounted() {
   
  },
  data() {
    return {
      modelNavigateTo,
    };
  },
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.flex-two {
  width: 100%;
  display: flex;
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