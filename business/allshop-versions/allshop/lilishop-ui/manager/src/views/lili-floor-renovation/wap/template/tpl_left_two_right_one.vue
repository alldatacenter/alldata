
<template>
  <div class="layout ">
    <div class="view-height-150">
      <div class="view-height-75">
        <img class="image-mode" :src="res.list[0].img" alt />
      </div>
      <div class="view-height-75">
        <img class="image-mode" :src="res.list[1].img" alt />
      </div>
    </div>
    <div class="view-height-150">
      <img class="image-mode" style="height:150px;" :src="res.list[2].img" />
    </div>
  </div>
</template>

<script>
export default {
  title: "左二右一",
  props: ["res"]
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.layout {
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;

  background-size: cover;
}
.view-height-75 {
  .image-mode {
    height: 75px;
  }
}
</style>
