<template>
  <div class="layout">
    <div class="box">
      <u-swiper @click="clickSwiper" interval="5000" duration="500" height="300" v-if="res" name="img" :list="res.list">
        <u-loading slot="loading"></u-loading>
      </u-swiper>
    </div>
  </div>
</template>

<script>
import { modelNavigateTo } from "./tpl";
export default {
  title: "导航栏",
  props: ["res"],
  watch: {
    res: {
      handler(newValue, oldValue) {
        this.$set(this, "res", newValue);
      },
      deep: true,
    },
  },

  mounted() {
   
  },
  methods: {
    clickSwiper(index) {
      modelNavigateTo(this.res.list[index]);
    },
  },
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
</style>
