<template>
  <div style="display: inline-block;">
    <Icon type="ios-loading" size="18" color="#2d8cf0" class="spin-icon-load"></Icon>
  </div>
</template>

<script>
export default {
  name: "circleLoading"
};
</script>

<style lang="scss" scoped>
.spin-icon-load {
  animation: ani-demo-spin 1s linear infinite;
}
</style>

