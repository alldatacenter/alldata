<template>
  <div class="layout" :style="{ textAlign: res.list[0].textAlign }">
    <div class="background" :style="{ backgroundColor: res.list[0].bk_color }">
      <div class="title" :style="{ color: res.list[0].color }">
        {{ res.list[0].title }}
      </div>
      <div
        style="
          position: absolute;
          right: 10px;
          top: 2px;
          color: #fff;
          line-height: 42px;
          font-size: 10px;
        "
      >
        <a
          :href="res.list[0].url"
          :style="{ color: res.list[0].color1 }"
          style="text-decoration: none"
          >{{ res.list[0].title1 }}</a
        >
      </div>
    </div>
  </div>
</template>

<script>
export default {
  title: "标题栏",
  props: ["res"],
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.background {
  background-color: rgb(255, 87, 62);
  /*background: url("../../../../assets/title.png") no-repeat;*/
  position: absolute;
  z-index: 2;
  width: 100%;

  height: 42px;
  background-size: cover;
}
.layout {
  text-align: center;
  position: relative;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;

  background: #ffffff;
}
.title {
  line-height: 42px;
  font-size: 18px;
  font-weight: bold;
  margin-left: 4px;
}
</style>
