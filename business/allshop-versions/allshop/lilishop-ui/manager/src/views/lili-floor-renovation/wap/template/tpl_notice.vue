<template>
  <div class="layout">
    <div class="background" :style="{ backgroundColor: res.list[0].bk_color }">
      <img :src="res.list[0].img" alt="" style="float:left;width: 20px;height: 20px;margin-top: 10px;margin-left: 10px;margin-right: 10px">
      <div class="title" :style="{ color: res.list[0].color }">
        {{ context }}
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    title: "公告",
    props: ["res"],
    data() {
      return {
        index: 0,
        context:  "",
      }
    },
    mounted() {
      this.context = this.res.list[0].title[this.index].context;
      setInterval(() => {
        this.context = this.res.list[0].title[this.index].context;
        if (this.index < this.res.list[0].title.length - 1) {
          this.index++;
        } else {
          this.index = 0;
        }
      }, 3000);
    },
  };
</script>
<style lang="scss" scoped>
  @import "./tpl.scss";
  .background {
    position: absolute;
    z-index: 2;
    width: 100%;
    height: 42px;
    text-align: left;
    font-size: 10px;
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
    font-size: 10px;
    font-weight: bold;
  }
</style>
