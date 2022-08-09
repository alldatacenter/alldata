
<template>
  <div class="layout">
    <u-image width="140rpx" mode="aspectFit" height="140rpx" @click="modelNavigateTo(item)" class="image-mode" v-for="(item,index) in res.list" :key="index" :src="item.img" alt="">
      <u-loading slot="loading"></u-loading>
    </u-image>
  </div>
</template>

<script>
import { modelNavigateTo } from "./tpl";
export default {
  title: "五列单行图片模块",
  props: ["res"],
  data() {
    return {
      modelNavigateTo,
    };
  },
  mounted() {},
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.layout {
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: cover;
}
</style>