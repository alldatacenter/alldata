
<template>
  <div class="layout ">
    <div class="view-height-150">
      <div class="view-height-75"  @click="modelNavigateTo(res.list[0])">
        <u-image class="image-mode"  width="100%" height="150rpx" :src="res.list[0].img" ></u-image>
      </div>
      <div class="view-height-75"  @click="modelNavigateTo(res.list[1])">
        <u-image class="image-mode" width="100%" height="150rpx"  :src="res.list[1].img" ></u-image>
      </div>
    </div>
    <div class="view-height-150"  @click="modelNavigateTo(res.list[2])">
      <u-image class="image-mode"  width="100%" height="300rpx"  :src="res.list[2].img" ></u-image>
    </div>
  </div>
</template>

<script>
import {modelNavigateTo} from './tpl'
export default {
  title: "左二右一",
  props: ["res"],
   data () {
    return {
      modelNavigateTo,
    }
  },
  mounted() {
   
  }
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.layout {
  height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  background-size: cover;
}

</style>