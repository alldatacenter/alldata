<template>
  <div class="wrapper">
    <!-- 楼层装修组件 -->
    <tpl ref="tpl" />
  </div>
</template>
<script>
import tpl from "@/pages/tabbar/home/views.vue";

export default {
  data() {
    return {
      background: {
        backgroundColor: "#fff",
      },
    };
  },
  onPullDownRefresh() {
    this.$refs.tpl.init();

    uni.stopPullDownRefresh();
  },
  components: {
    tpl,
  },
};
</script>

<style lang="scss" scoped>
</style>
