
<template>
  <div class="layout">
    <u-image height="175rpx" mode="aspectFit" width="175rpx" @click="modelNavigateTo(item)" class="image-mode" :src="item.img" v-for="(item,index) in res.list" :key="index">
      <u-loading slot="loading"></u-loading>
    </u-image>
  </div>
</template>

<script>
import { modelNavigateTo } from "./tpl";
export default {
  title: "四列单行图片模块",
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
img {
  width: 84px;
}
</style>