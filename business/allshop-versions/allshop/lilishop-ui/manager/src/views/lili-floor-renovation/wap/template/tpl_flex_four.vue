
<template>
  <div class="layout">
      <img class="image-mode" :src="res.list[0].img" alt="">
      <img class="image-mode" :src="res.list[1].img" alt="">
      <img class="image-mode" :src="res.list[2].img" alt="">
      <img class="image-mode" :src="res.list[3].img" alt="">
  </div>
</template>

<script>
export default {
  title: "四列单行图片模块",
  props: ["res"],

};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.layout {
  // background: #e8e8e8;
  // height: 84px;
  display: flex;
  padding: 0 8px;
  align-items: center;
  justify-content: center;
  background-size: cover;
}

img{
  width: 84px !important;
}
</style>
