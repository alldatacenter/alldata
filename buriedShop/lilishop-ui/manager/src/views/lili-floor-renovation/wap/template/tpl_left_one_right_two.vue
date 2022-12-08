
<template>
  <div class="layout">
    <div class="view-height-150">
      <img class="image-mode" :src="res.list[0].img" />
    </div>
    <div class="view-height-150">
      <div class="view-height-75">
        <img class="image-mode" :src="res.list[1].img" alt />
      </div>
      <div class="view-height-75">
        <img class="image-mode" :src="res.list[2].img" alt />
      </div>
    </div>
  </div>
</template>

<script>
export default {
  title: "左一右二",
  props: ["res"]
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
