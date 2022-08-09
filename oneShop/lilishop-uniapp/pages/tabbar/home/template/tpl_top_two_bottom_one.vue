<template>
  <div class="layout">
    <div class="view-width-100">
      <div class="view-height-85" @click="modelNavigateTo(res.list[0])">
        <u-image
          class="image-mode"
          height="170rpx"
          width="100%"
          :src="res.list[0].img"
          alt
          ><u-loading slot="loading"></u-loading
        ></u-image>
      </div>
      <div class="view-height-85" @click="modelNavigateTo(res.list[1])">
        <u-image
          class="image-mode"
          height="170rpx"
          width="100%"
          :src="res.list[1].img"
          alt
          ><u-loading slot="loading"></u-loading
        ></u-image>
      </div>
    </div>
    <div class="view-width-100"  @click="modelNavigateTo(res.list[2])">
      <u-image
        class="image-mode"
        height="200rpx"
        width="100%"
        :src="res.list[2].img"
        ><u-loading slot="loading"></u-loading
      ></u-image>
    </div>
  </div>
</template>

<script>
import {modelNavigateTo} from './tpl'
export default {
  title: "上二下一",
  props: ["res"],
   data () {
    return {
      modelNavigateTo,
    }
  },
  mounted() {
 
  },
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.layout {
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: cover;
  // height: 260rpx;
  flex-direction: column;
}
// img {
//   width: 100% !important;
//   height: 85px !important;
// }
.view-width-100 {
  padding: 1px 0;
  display: flex;
  height: 200rpx;
  width: 100%;
}
.view-height-85 {
  padding: 0 1px;
  width: 50%;
}
.image-mode{
  width: 100%;
}
</style>
