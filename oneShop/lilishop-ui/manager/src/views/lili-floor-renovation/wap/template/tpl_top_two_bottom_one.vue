<template>
  <div class="layout">
    <div class="view-width-100">
      <div class="view-height-85">
        <img class="image-mode" :src="res.list[0].img" alt />
      </div>
      <div class="view-height-85">
        <img class="image-mode" :src="res.list[1].img" alt />
      </div>
    </div>
    <div class="view-width-100">
      <img class="image-mode" :src="res.list[2].img" />
    </div>
  </div>
</template>

<script>
export default {
  title: "上二下一",
  props: ["res"],
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.layout {
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: cover;
  height: 170px;
  flex-direction: column;
}
img {
  width: 100% !important;
  height: 85px !important;
}
.view-width-100 {
  padding: 1px 0;
  display: flex;
  height: 85px;
  width: 100%;
}
.view-height-85 {
  padding: 0 1px;
  width: 50%;
}
</style>
