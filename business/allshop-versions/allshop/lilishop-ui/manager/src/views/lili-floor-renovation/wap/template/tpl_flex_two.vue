<template>
  <div class="layout">
    <div class="flex-two">
      <div class="flex-item">
        <img :src="res.list[0].img" alt />
      </div>
      <div class="flex-item">
        <img :src="res.list[1].img" alt />
      </div>
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
