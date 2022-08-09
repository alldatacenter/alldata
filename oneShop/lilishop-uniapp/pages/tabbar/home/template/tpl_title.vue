<template>
  <div class="layout" :style="{textAlign: res.list[0].textAlign}"  @click="modelNavigateTo(res.list[0])" >
    <div class="background" :style="{ backgroundColor: res.list[0].bk_color}">
      <div class="title" :style="{ color: res.list[0].color }">
        {{ res.list[0].title }}
      </div>
      <div style="position: absolute;right: 10px;top:2px;color: #fff;line-height: 42px;font-size: 10px">
        <a  :style="{ color: res.list[0].color1 }" style="text-decoration: none">{{ res.list[0].title1 }}</a>
      </div>
    </div>
  </div>
</template>


<script>
import { modelNavigateTo } from "./tpl";
export default {
  title: "标题栏",
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
.background {
  // background: url("/static/title.png") no-repeat;
  position: absolute;
  z-index: 2;
  width: 100%;
  height: 84rpx;
  background-position-x: center;
  background-position-y: center;
  background-size: cover;
}

.layout {
  text-align: center;
  position: relative;
  height: 84rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  background: #ffffff;
}

.title {
  line-height: 84rpx;
  font-size: 30rpx;
  font-weight: bold;
  margin-left: 8rpx;
}
</style>
